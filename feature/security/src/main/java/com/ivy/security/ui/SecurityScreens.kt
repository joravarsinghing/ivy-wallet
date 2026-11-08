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
import com.ivy.security.SecuritySettings
import com.ivy.design.system.JvyLogo
import com.ivy.design.system.JvyShapes
import com.ivy.design.system.JvyTypography
import com.ivy.design.system.JvyButton as Button
import com.ivy.design.system.JvyOutlinedButton as OutlinedButton
import com.ivy.design.system.JvyTextButton as TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock

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
    var biometricVerified by remember { mutableStateOf(false) }
    val biometric = biometricRequest(vm, true) { if (vm.promptBiometricOnly) biometricVerified = true }
    SecuritySettingsContent(vm, settings, capturePreference ?: !settings.enabled, { nav.back() }, biometric, biometricVerified)
}

@Composable
internal fun SecuritySettingsContent(
    vm: SecurityViewModel, settings: SecuritySettings, captureAllowed: Boolean,
    onBack: () -> Unit, onBiometric: (Boolean) -> Unit, biometricVerified: Boolean,
    initialChanging: Boolean = false,
) {
    var selected by remember(settings.mode) { mutableStateOf(settings.mode) }
    var changing by rememberSaveable { mutableStateOf(initialChanging) }
    var replaceCode by remember { mutableStateOf(false) }
    // Credentials must never enter a saved-state Bundle, even during rotation.
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().systemBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                com.ivy.design.system.JvyScreenHeader("Security", onBack, horizontalPadding = 0.dp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    JvyLogo(Modifier.size(56.dp))
                    Column {
                        Text("Your wallet. Your peace of mind.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                SecuritySection {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("How you unlock", style = MaterialTheme.typography.titleMedium)
                            Text(settings.mode.label(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("Locks after a minute away, or immediately when your phone locks. Rotation and resizing do not lock it again.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { changing = !changing }, modifier = Modifier.fillMaxWidth()) { Text(if (changing) "Keep current unlock method" else "Change unlock method") }
                }
                if (settings.enabled && !vm.verifiedForSettings && !vm.recoveryVerified) SecuritySection {
                    Text("Verify it's you", style = MaterialTheme.typography.titleMedium)
                    Text("Verify your current method before creating a backup code or changing protection.", style = MaterialTheme.typography.bodySmall)
                    if (settings.permitsBiometric && !vm.recovering) Button(onClick = { onBiometric(false) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (settings.mode == UnlockMode.LEGACY_SYSTEM) "Verify fingerprint or phone PIN" else "Verify fingerprint or face")
                    }
                    if (settings.permitsPin || vm.recovering) {
                        SecretField(if (vm.recovering) "Backup / recovery code" else "Current Jvy PIN", current, { current = it }, vm.recovering)
                        OutlinedButton(onClick = { vm.verify(current, true); current = "" }, enabled = !vm.busy && current.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (vm.recovering) "Verify backup code" else "Verify PIN") }
                    }
                    if (settings.hasRecovery) TextButton(onClick = { current = ""; vm.useRecovery(!vm.recovering) }) { Text(if (vm.recovering) "Use my usual unlock method" else "Use a backup code instead") }
                }
                if (vm.message.isNotBlank()) Text(vm.message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                SecuritySection {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Key, null, tint = MaterialTheme.colorScheme.secondary)
                        Text("A backup way in", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(if (settings.hasRecovery) "You have an active backup code." else "No backup code yet. Set one up before you need it.")
                    if (!settings.enabled) TextButton(onClick = { changing = true }) { Text("Set up an app lock first") }
                    else Button(onClick = { if (settings.hasRecovery) replaceCode = true else vm.generateRecoveryCode() },
                        enabled = !vm.busy && vm.verifiedForSettings && !vm.recoveryVerified, modifier = Modifier.fillMaxWidth()) {
                        Text(if (settings.hasRecovery) "Replace backup code" else "Create backup code")
                    }
                    if (settings.enabled && !vm.verifiedForSettings && !vm.recoveryVerified) Text("Verify it's you above to enable this action.", style = MaterialTheme.typography.bodySmall)
                    Text("Use it to set a new Jvy PIN if your usual unlock fails. Creating one leaves your unlock method unchanged. A new code replaces the old one.", style = MaterialTheme.typography.bodySmall)
                    Text("Keep the code in a password manager or somewhere safe outside Jvy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (changing || vm.recoveryVerified) SecuritySection {
                    Text(if (vm.recoveryVerified) "Choose a new Jvy PIN" else "Choose your unlock method", style = MaterialTheme.typography.titleMedium)
                    if (!vm.recoveryVerified) listOf(UnlockMode.OFF, UnlockMode.PIN, UnlockMode.BIOMETRIC, UnlockMode.EITHER).forEach { mode ->
                        OutlinedCard(onClick = { selected = mode }, modifier = Modifier.fillMaxWidth().semantics {
                            role = Role.RadioButton
                            this.selected = selected == mode
                        }, shape = JvyShapes.Control,
                            colors = CardDefaults.outlinedCardColors(containerColor = if (selected == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected == mode, null)
                                Column(Modifier.weight(1f)) { Text(mode.label(), style = MaterialTheme.typography.titleSmall); Text(mode.description(), style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                    if (selected == UnlockMode.PIN || selected == UnlockMode.EITHER || vm.recoveryVerified) {
                        SecretField("New six-digit Jvy PIN", pin, { pin = it })
                        SecretField("Confirm new PIN", confirmation, { confirmation = it })
                    }
                    if (!vm.recoveryVerified && (selected == UnlockMode.BIOMETRIC || selected == UnlockMode.EITHER)) {
                        OutlinedButton(onClick = { onBiometric(true) }, modifier = Modifier.fillMaxWidth()) { Text(if (biometricVerified) "Biometrics verified" else "Test biometrics") }
                    }
                    Button(onClick = { vm.configure(if (vm.recoveryVerified) UnlockMode.PIN else selected, pin, confirmation, biometricVerified); pin = ""; confirmation = "" },
                        enabled = !vm.busy && (selected != UnlockMode.LEGACY_SYSTEM || vm.recoveryVerified), modifier = Modifier.fillMaxWidth()) { Text("Save unlock method") }
                }
                ScreenCaptureSetting(captureAllowed, vm::setScreenCapture)
            }
        }
    }
    if (replaceCode) AlertDialog(onDismissRequest = { replaceCode = false }, title = { Text("Replace your backup code?") },
        text = { Text("Your old code will stop working. Save the new code before leaving this screen. Your unlock method will stay the same.") },
        confirmButton = { TextButton(onClick = { replaceCode = false; vm.generateRecoveryCode() }) { Text("Create replacement") } },
        dismissButton = { TextButton(onClick = { replaceCode = false }) { Text("Keep existing code") } })
}

@Composable
private fun SecuritySection(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = JvyShapes.Card) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
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
        keyboardOptions = KeyboardOptions(keyboardType = if (recovery) KeyboardType.Password else KeyboardType.NumberPassword),
        textStyle = JvyTypography.AmountSmall, shape = JvyShapes.Control, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun RecoveryDialog(vm: SecurityViewModel) {
    vm.recoveryCode?.let { code ->
        AlertDialog(onDismissRequest = {}, properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn), title = { Text("Save your backup code") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text(code, style = JvyTypography.AmountSmall); Text("This recovery code can set a new Jvy PIN without deleting your entries. It is shown once. Store it in a password manager or write it down somewhere safe outside this app.") } },
            confirmButton = { TextButton(onClick = vm::dismissRecovery) { Text("I saved this code") } })
    }
}

private fun UnlockMode.label() = when (this) {
    UnlockMode.OFF -> "Off"
    UnlockMode.LEGACY_SYSTEM -> "Phone fingerprint / device PIN"
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
