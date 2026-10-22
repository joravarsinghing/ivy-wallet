package com.ivy.security

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.ivy.base.legacy.SharedPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

enum class UnlockMode { OFF, LEGACY_SYSTEM, PIN, BIOMETRIC, EITHER }
data class SecuritySettings(val mode: UnlockMode, val hasPin: Boolean, val hasRecovery: Boolean) {
    val enabled get() = mode != UnlockMode.OFF
    val permitsPin get() = mode == UnlockMode.PIN || mode == UnlockMode.EITHER
    val permitsBiometric get() = mode == UnlockMode.BIOMETRIC || mode == UnlockMode.EITHER || mode == UnlockMode.LEGACY_SYSTEM
}
sealed interface CredentialResult {
    data object Success : CredentialResult
    data object Incorrect : CredentialResult
    data class Delayed(val seconds: Long) : CredentialResult
}

/** AES-GCM records under noBackupFilesDir; neither credentials nor throttles enter exports. */
@Singleton
class SecurityStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val legacyPrefs: SharedPrefs,
) {
    private val mutex = Mutex()
    private val file get() = File(context.noBackupFilesDir, "jvy-security-v1")
    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun bytes(value: String) = Base64.decode(value, Base64.NO_WRAP)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("jvy-lock-v1", null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("jvy-lock-v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
    }
    private fun read(): JSONObject {
        if (!file.exists() && !File(file.path + ".bak").exists()) return JSONObject().put("mode", if (legacyPrefs.getBoolean(SharedPrefs.APP_LOCK_ENABLED, false)) "LEGACY_SYSTEM" else "OFF")
        val serialized = android.util.AtomicFile(file).openRead().bufferedReader().use { it.readText() }
        val parts = serialized.split(':')
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes(parts[0])))
        return JSONObject(String(cipher.doFinal(bytes(parts[1])), Charsets.UTF_8))
    }
    private fun write(record: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = b64(cipher.iv) + ":" + b64(cipher.doFinal(record.toString().toByteArray(Charsets.UTF_8)))
        val atomic = android.util.AtomicFile(file)
        val stream = atomic.startWrite()
        try { stream.write(encrypted.toByteArray()); atomic.finishWrite(stream) }
        catch (error: Exception) { atomic.failWrite(stream); throw error }
    }
    suspend fun settings(): SecuritySettings = withContext(Dispatchers.IO) { mutex.withLock {
        val record = read()
        // Persist the legacy migration before financial restores can change the old flag.
        if (!file.exists()) write(record)
        SecuritySettings(UnlockMode.valueOf(record.getString("mode")), record.has("pinHash"), record.has("recoveryHash"))
    } }
    private fun install(record: JSONObject, prefix: String, secret: CharArray) {
        val salt = PinCrypto.salt()
        record.put(prefix + "Salt", b64(salt)).put(prefix + "Hash", b64(PinCrypto.derive(secret, salt)))
    }
    suspend fun configure(mode: UnlockMode, pin: String? = null): String? = withContext(Dispatchers.IO) { mutex.withLock {
        val record = read()
        if (pin != null) {
            require(pin.length == 6 && pin.all(Char::isDigit)) { "Use exactly six digits." }
            val chars = pin.toCharArray()
            try { install(record, "pin", chars) } finally { chars.fill('\u0000') }
        }
        require(mode != UnlockMode.PIN && mode != UnlockMode.EITHER || record.has("pinHash")) { "Set a PIN first." }
        val recovery = if (mode != UnlockMode.OFF && mode != UnlockMode.LEGACY_SYSTEM) PinCrypto.recoveryCode() else null
        if (recovery != null) install(record, "recovery", PinCrypto.normalizeRecovery(recovery).toCharArray())
        if (mode == UnlockMode.OFF) {
            listOf("pinHash", "pinSalt", "recoveryHash", "recoverySalt").forEach(record::remove)
        }
        if (mode == UnlockMode.BIOMETRIC) {
            record.remove("pinHash"); record.remove("pinSalt")
        }
        record.put("mode", mode.name).put("failures", 0).put("retryUntil", 0L)
        write(record)
        legacyPrefs.putBoolean(SharedPrefs.APP_LOCK_ENABLED, mode != UnlockMode.OFF)
        recovery
    } }
    suspend fun verify(value: String, recovery: Boolean = false): CredentialResult = withContext(Dispatchers.IO) { mutex.withLock {
        val record = read()
        val now = SystemClock.elapsedRealtime()
        val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
        val failures = record.optInt("failures")
        if (record.optInt("boot", boot) != boot && PinCrypto.retryDelay(failures) > 0) {
            record.put("retryUntil", now + PinCrypto.retryDelay(failures)).put("boot", boot)
            write(record)
        }
        val remaining = record.optLong("retryUntil") - now
        if (remaining > 0) return@withLock CredentialResult.Delayed((remaining + 999) / 1000)
        val prefix = if (recovery) "recovery" else "pin"
        val normalized = if (recovery) PinCrypto.normalizeRecovery(value) else value
        val chars = normalized.toCharArray()
        val valid = try { record.has(prefix + "Hash") && PinCrypto.matches(chars, bytes(record.getString(prefix + "Salt")), bytes(record.getString(prefix + "Hash"))) }
        finally { chars.fill('\u0000') }
        if (valid) {
            record.put("failures", 0).put("retryUntil", 0L)
            // Configuration atomically replaces this code after the new PIN is confirmed.
            // Keeping it until then prevents process death mid-recovery from locking out the user.
            write(record)
            CredentialResult.Success
        } else {
            val count = failures + 1
            val delay = PinCrypto.retryDelay(count)
            record.put("failures", count).put("retryUntil", now + delay).put("boot", boot)
            write(record)
            if (delay > 0) CredentialResult.Delayed(delay / 1000) else CredentialResult.Incorrect
        }
    } }
}
