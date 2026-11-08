package com.ivy.security.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.security.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    val session: AuthSessionController,
    private val store: SecurityStore,
    val capturePreferences: ScreenCapturePreferences,
) : ViewModel() {
    var message by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var recoveryCode by mutableStateOf<String?>(null)
        private set
    var recovering by mutableStateOf(false)
        private set
    var recoveryVerified by mutableStateOf(false)
        private set
    var verifiedForSettings by mutableStateOf(false)
        private set
    var promptGeneration: Long? = null
    var promptBiometricOnly: Boolean = false
    private var forSettings = false
    private var proofGeneration: Long? = null
    private var proofTime = 0L
    private fun proofValid() = proofGeneration == session.generation && android.os.SystemClock.elapsedRealtime() - proofTime < 60_000
    private fun prove() {
        verifiedForSettings = true
        proofGeneration = session.generation
        proofTime = android.os.SystemClock.elapsedRealtime()
    }
    fun beginBiometric(settings: Boolean, biometricOnly: Boolean): Boolean {
        val ticket = session.beginAuthentication() ?: return false
        // Testing a new method must not authorize changing a PIN-only configuration.
        forSettings = settings && (!biometricOnly || session.settings.value.permitsBiometric)
        promptBiometricOnly = biometricOnly
        promptGeneration = ticket
        return true
    }
    fun biometricResult(ticket: Long, success: Boolean, error: String = ""): Boolean {
        if (promptGeneration != ticket) return false
        promptGeneration = null
        if (session.finishAuthentication(ticket, success)) {
            if (forSettings) prove()
            message = ""
            return true
        } else if (error.isNotBlank()) message = error
        return false
    }
    fun useRecovery(value: Boolean) { recovering = value; message = "" }
    fun authenticationUnavailable() {
        message = "No supported authentication is enrolled. Check Android security settings, or use your Jvy PIN or recovery code if configured."
    }
    fun verify(value: String, settings: Boolean) {
        if (busy) return
        busy = true
        val recoveryAttempt = recovering
        viewModelScope.launch {
            var activeTicket: Long? = null
            try {
                val ticket = session.beginAuthentication() ?: return@launch
                activeTicket = ticket
                when (val result = store.verify(value, recoveryAttempt)) {
                    CredentialResult.Success -> {
                        if (recoveryAttempt) {
                            if (!session.finishAuthentication(ticket, true, unlock = false)) return@launch
                            recoveryVerified = true
                            prove()
                            message = "Choose and confirm a new six-digit PIN."
                        } else if (session.finishAuthentication(ticket, true)) { prove(); message = "Authenticated" }
                    }
                    CredentialResult.Incorrect -> { session.finishAuthentication(ticket, false); message = "Incorrect ${if (recovering) "recovery code" else "PIN"}." }
                    is CredentialResult.Delayed -> { session.finishAuthentication(ticket, false); message = "Try again in ${result.seconds} seconds." }
                }
            } catch (_: Exception) {
                session.requestGeneration?.let { session.finishAuthentication(it, false) }
                message = "Secure storage is unavailable. Your data has not been changed."
            }
            finally {
                activeTicket?.let { if (session.requestGeneration == it) session.finishAuthentication(it, false) }
                busy = false
            }
        }
    }
    fun configure(mode: UnlockMode, pin: String, confirmation: String, biometricVerified: Boolean = false) {
        if (busy) return
        if (session.settings.value.enabled && !proofValid()) {
            verifiedForSettings = false
            if (recoveryVerified) { recoveryVerified = false; recovering = true }
            message = "Authenticate again before changing security."
            return
        }
        if ((mode == UnlockMode.PIN || mode == UnlockMode.EITHER || recoveryVerified) && (pin.length != 6 || pin != confirmation || !pin.all(Char::isDigit))) {
            message = "Enter and confirm the same six-digit PIN."; return
        }
        if ((mode == UnlockMode.BIOMETRIC || mode == UnlockMode.EITHER) && !biometricVerified) {
            message = "Test biometrics before enabling this method."; return
        }
        busy = true
        val authorizedGeneration = session.generation
        viewModelScope.launch {
            try {
                recoveryCode = withContext(NonCancellable) {
                    try { store.configure(mode, pin.takeIf { mode == UnlockMode.PIN || mode == UnlockMode.EITHER }) }
                    finally { session.refresh(authorizedGeneration = authorizedGeneration) }
                }
                recovering = false; recoveryVerified = false; verifiedForSettings = false
                proofGeneration = null
                message = if (recoveryCode == null) "Security updated." else "Save this recovery code before continuing. It is shown once."
            } catch (_: Exception) { message = "Could not save security settings. Try again." }
            finally { busy = false }
        }
    }
    fun dismissRecovery() { recoveryCode = null; message = "Recovery code saved." }
    fun generateRecoveryCode() {
        if (busy || recoveryCode != null) return
        if (!session.settings.value.enabled) {
            message = "Enable an app lock before creating a backup code."
            return
        }
        // Recovery proof is restricted to choosing a new PIN, never credential rotation.
        if (session.locked.value != false || session.deviceLocked || recoveryVerified || !proofValid()) {
            verifiedForSettings = false
            message = "Verify your current unlock method above, then create a backup code."
            return
        }
        busy = true
        val authorizedGeneration = session.generation
        viewModelScope.launch {
            try {
                // Do not lose the result to cancellation after the atomic credential write.
                recoveryCode = withContext(NonCancellable) {
                    try { store.generateRecoveryCode() }
                    finally { session.refresh(authorizedGeneration = authorizedGeneration) }
                }
                message = "Save your new backup code. Your unlock method has not changed."
            } catch (_: Exception) { message = "Could not create a backup code. Try again." }
            finally {
                verifiedForSettings = false
                proofGeneration = null
                busy = false
            }
        }
    }
    fun setScreenCapture(allowed: Boolean) {
        // Settings cannot weaken even presentation privacy from behind the lock screen.
        if (session.locked.value != false) return
        capturePreferences.setAllowed(allowed)
    }
    override fun onCleared() {
        promptGeneration?.let { session.finishAuthentication(it, false) }
        super.onCleared()
    }
}
