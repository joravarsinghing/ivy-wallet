package com.ivy.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinCrypto {
    const val ITERATIONS = 600_000
    fun salt(): ByteArray = ByteArray(16).also(SecureRandom()::nextBytes)
    fun derive(secret: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret, salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    fun matches(secret: CharArray, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(derive(secret, salt), expected)

    fun recoveryCode(): String {
        val bytes = ByteArray(16).also(SecureRandom()::nextBytes)
        return bytes.joinToString("") { "%02X".format(it) }.chunked(4).joinToString("-")
    }
    fun normalizeRecovery(value: String): String = value.replace("-", "").replace(" ", "").uppercase()
    fun retryDelay(failures: Int): Long = if (failures < 5) 0L else
        (30_000L * (1L shl (failures - 5).coerceAtMost(5))).coerceAtMost(900_000L)
}
