package com.ivy.security

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.base.legacy.SharedPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SecurityAndroidTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var prefs: SharedPrefs
    private lateinit var store: SecurityStore
    private lateinit var preferenceName: String
    @Before fun prepare() {
        val target = ApplicationProvider.getApplicationContext<Context>()
        directory = File(target.cacheDir, "security-test-${UUID.randomUUID()}").apply { check(mkdir()) }
        preferenceName = "security-test-${UUID.randomUUID()}"
        context = object : ContextWrapper(target) {
            override fun getNoBackupFilesDir(): File = directory
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = target.getSharedPreferences(preferenceName, mode)
        }
        prefs = SharedPrefs(context)
        store = SecurityStore(context, prefs)
    }
    @After fun cleanup() {
        context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteSharedPreferences(preferenceName)
        // Only this test's validated, uniquely named temporary directory is removed.
        check(directory.name.startsWith("security-test-"))
        directory.deleteRecursively()
    }
    @Test fun legacyFingerprintCanCreateCodeWithoutChangingUnlockMode() = runBlocking {
        prefs.putBoolean(SharedPrefs.APP_LOCK_ENABLED, true)
        assertEquals(UnlockMode.LEGACY_SYSTEM, store.settings().mode)
        val code = store.generateRecoveryCode()
        assertEquals(SecuritySettings(UnlockMode.LEGACY_SYSTEM, false, true), store.settings())
        assertEquals(CredentialResult.Success, store.verify(code, recovery = true))
    }
    @Test fun codeRotationPreservesPinAndRevokesThePreviousCode() = runBlocking {
        val old = store.configure(UnlockMode.EITHER, "123456")!!
        val replacement = store.generateRecoveryCode()
        val reopened = SecurityStore(context, prefs)
        assertEquals(SecuritySettings(UnlockMode.EITHER, true, true), reopened.settings())
        assertEquals(CredentialResult.Success, reopened.verify("123456"))
        assertEquals(CredentialResult.Incorrect, reopened.verify(old, recovery = true))
        assertEquals(CredentialResult.Success, reopened.verify(replacement, recovery = true))
        val ciphertext = File(directory, "jvy-security-v1").readText()
        assertFalse(ciphertext.contains("123456"))
        assertFalse(ciphertext.contains(replacement))
    }
    @Test fun disabledLockCannotGenerateRecoveryCredentials() = runBlocking {
        var rejected = false
        try { store.generateRecoveryCode() } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(UnlockMode.OFF, store.settings().mode)
    }
    @Test fun realActivityRecreationPreservesAuthorizationButExplicitLockInvalidatesTickets() = runBlocking {
        store.configure(UnlockMode.PIN, "123456")
        val application = ApplicationProvider.getApplicationContext<Application>()
        val session = AuthSessionController(context, store)
        application.registerActivityLifecycleCallbacks(session)
        try {
            ActivityScenario.launch(SecurityTestActivity::class.java).use { activity ->
                withContext(Dispatchers.Main) { session.refresh(initial = true) }
                var ticket = 0L
                activity.onActivity { ticket = requireNotNull(session.beginAuthentication()) }
                assertEquals(CredentialResult.Success, store.verify("123456"))
                activity.onActivity { assertTrue(session.finishAuthentication(ticket, true)) }
                val epoch = session.generation
                activity.recreate()
                activity.onActivity {
                    assertEquals(false, session.locked.value)
                    assertEquals(epoch, session.generation)
                    ticket = requireNotNull(session.beginAuthentication())
                    session.lock()
                    assertFalse(session.finishAuthentication(ticket, true))
                    assertEquals(true, session.locked.value)
                }
            }
        } finally { application.unregisterActivityLifecycleCallbacks(session) }
    }
}
