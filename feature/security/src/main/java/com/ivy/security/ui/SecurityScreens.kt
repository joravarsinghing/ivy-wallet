package com.ivy.security.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.navigation.navigation
import com.ivy.security.UnlockMode

private tailrec fun Context.activity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
private fun biometricRequest(vm: SecurityViewModel, settings: Boolean, onVerified: () -> Unit = {}): (Boolean) -> Unit {
    val context = LocalContext.current
    val activity = context.activity()
    val latestVerified by rememberUpdatedState(onVerified)
    fun promptFor(ticket: Long): BiometricPrompt? = activity?.let {
            BiometricPrompt(it, ContextCompat.getMainExecutor(it), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (vm.biometricResult(ticket, true)) latestVerified()
                }
                override fun onAuthenticationError(code: Int, text: CharSequence) { vm.biometricResult(ticket, false, text.toString()) }
            })
    }
    // Reattach with the original request identity. Old callbacks cannot authorize a retry.
    remember(activity, vm) { vm.promptGeneration?.let(::promptFor) }
    return { biometricOnly ->
        val legacy = !biometricOnly && vm.session.settings.value.mode == UnlockMode.LEGACY_SYSTEM
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            (if (legacy) BiometricManager.Authenticators.DEVICE_CREDENTIAL else 0)
        if (activity != null && BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS && vm.beginBiometric(settings, biometricOnly)) {
            val ticket = vm.promptGeneration!!
            val prompt = promptFor(ticket)!!
            val builder = BiometricPrompt.PromptInfo.Builder().setTitle("Unlock Jvy Wallet")
                .setAllowedAuthenticators(authenticators).setConfirmationRequired(false)
            if (!legacy) builder.setNegativeButtonText("Cancel")
            prompt.authenticate(builder.build())
        } else if (vm.session.requestGeneration == null && !vm.session.deviceLocked) vm.authenticationUnavailable()
    }
}

@Composable
fun SecurityLockScreen() {
    val vm: SecurityViewModel = viewModel()
    val settings by vm.session.settings.collectAsState()
    val storageError by vm.session.storageError.collectAsState()
    val biometric = biometricRequest(vm, false)
    LaunchedEffect(vm.session.generation) {
        if (settings.permitsBiometric && vm.session.requestGeneration == null) biometric(false)
    }
    SecurityUnlockContent(settings, vm.busy, vm.message, storageError, vm.recovering, vm.recoveryVerified,
        onBiometric = { biometric(false) }, onVerify = { vm.verify(it, false) },
        onRecovery = vm::useRecovery, onResetPin = { pin, confirmation -> vm.configure(UnlockMode.PIN, pin, confirmation) })
}

@Composable
fun SecuritySettingsScreen() {
    val vm: SecurityViewModel = viewModel()
    val settings by vm.session.settings.collectAsState()
    val capturePreference by vm.capturePreferences.allowed.collectAsState()
    val nav = navigation()
    var selected by remember(settings.mode) { mutableStateOf(if (settings.mode == UnlockMode.LEGACY_SYSTEM) UnlockMode.EITHER else settings.mode) }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var biometricVerified by remember { mutableStateOf(false) }
    val biometric = biometricRequest(vm, true) { if (vm.promptBiometricOnly) biometricVerified = true }
    Surface(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = { nav.back() }) { Text("Back") }
        Text("Security", style = MaterialTheme.typography.headlineMedium)
        Text("Your wallet, your privacy", style = MaterialTheme.typography.titleMedium)
        ScreenCaptureSetting(allowed = capturePreference ?: !settings.enabled, onChange = vm::setScreenCapture)
        HorizontalDivider()
        Text("How you unlock", style = MaterialTheme.typography.titleLarge)
        Text("Now using: ${settings.mode.label()}")
        Text("Jvy locks after a minute away, or as soon as your phone locks. Rotating or resizing does not ask you to unlock again.", style = MaterialTheme.typography.bodyMedium)
        listOf(UnlockMode.OFF, UnlockMode.PIN, UnlockMode.BIOMETRIC, UnlockMode.EITHER).forEach { mode ->
            OutlinedCard(onClick = { selected = mode }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected == mode, { selected = mode })
                    Column(Modifier.weight(1f)) { Text(mode.label(), style = MaterialTheme.typography.titleSmall); Text(mode.description(), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (settings.enabled && !vm.verifiedForSettings) {
            Text("Authenticate to change protection.")
            if (settings.permitsPin || vm.recovering) {
                SecretField(if (vm.recovering) "Recovery code" else "Current PIN", current, { current = it }, vm.recovering)
                Button(onClick = { vm.verify(current, true); current = "" }, enabled = !vm.busy) { Text("Verify") }
            }
            if (settings.permitsBiometric) Button(onClick = { biometric(false) }) { Text("Verify with biometrics") }
            if (settings.hasRecovery) TextButton(onClick = { vm.useRecovery(!vm.recovering) }) { Text("Use recovery code") }
        }
        if (selected == UnlockMode.PIN || selected == UnlockMode.EITHER || vm.recoveryVerified) {
            SecretField("New six-digit PIN", pin, { pin = it })
            SecretField("Confirm new PIN", confirmation, { confirmation = it })
        }
        if (selected == UnlockMode.BIOMETRIC || selected == UnlockMode.EITHER) {
            Button(onClick = { biometric(true) }) { Text(if (biometricVerified) "Biometrics verified" else "Test biometrics") }
        }
        Button(onClick = { vm.configure(if (vm.recoveryVerified) UnlockMode.PIN else selected, pin, confirmation, biometricVerified); pin = ""; confirmation = "" }, enabled = !vm.busy) { Text("Save security settings") }
        Text(vm.message)
        if (vm.busy) CircularProgressIndicator()
        Text("Recovery codes", style = MaterialTheme.typography.titleMedium)
        Text(if (settings.hasRecovery) "You have a recovery code. Keep it outside this app. Saving a new unlock method replaces it with a new code shown once." else "No recovery code is set up yet. Choose a Jvy PIN or biometrics and save your settings to create one. Your phone's PIN is not a Jvy PIN.")
    }
    }
    }
}

/** Root-level confirmation survives leaving the lock screen after a successful PIN reset. */
@Composable
fun SecurityRecoveryHost() {
    val vm: SecurityViewModel = viewModel()
    val locked by vm.session.locked.collectAsState()
    if (locked == false) RecoveryDialog(vm)
}

@Composable
internal fun SecretField(label: String, value: String, change: (String) -> Unit, recovery: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = { change(if (recovery) it.take(40) else it.filter(Char::isDigit).take(6)) },
        label = { Text(label) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = if (recovery) KeyboardType.Password else KeyboardType.NumberPassword), modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth())
}

@Composable
private fun RecoveryDialog(vm: SecurityViewModel) {
    vm.recoveryCode?.let { code ->
        AlertDialog(onDismissRequest = {}, properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn), title = { Text("Save your recovery code") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text(code); Text("Store this code somewhere safe. It can reset your Jvy PIN without deleting your financial data. This screen is shown once.") } },
            confirmButton = { TextButton(onClick = vm::dismissRecovery) { Text("I saved this code") } })
    }
}

private fun UnlockMode.label() = when (this) {
    UnlockMode.OFF -> "Off"
    UnlockMode.LEGACY_SYSTEM -> "Existing device authentication"
    UnlockMode.PIN -> "Jvy PIN"
    UnlockMode.BIOMETRIC -> "Biometrics"
    UnlockMode.EITHER -> "PIN or biometrics"
}

private fun UnlockMode.description() = when (this) {
    UnlockMode.OFF -> "Open Jvy without an app lock."
    UnlockMode.LEGACY_SYSTEM -> "Your existing Android fingerprint, face, or device PIN."
    UnlockMode.PIN -> "Use a separate six-digit PIN just for Jvy."
    UnlockMode.BIOMETRIC -> "Use your enrolled fingerprint or face, with a recovery code as backup."
    UnlockMode.EITHER -> "Choose fingerprint/face or your Jvy PIN each time."
}
