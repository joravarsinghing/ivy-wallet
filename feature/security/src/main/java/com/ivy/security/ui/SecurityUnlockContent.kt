package com.ivy.security.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.security.SecuritySettings
import com.ivy.security.UnlockMode
import com.ivy.design.system.JvyLogo
import com.ivy.design.system.JvyButton as Button
import com.ivy.design.system.JvyOutlinedButton as OutlinedButton
import com.ivy.design.system.JvyTextButton as TextButton

/** Presentation only: the session controller remains the sole authority to unlock. */
@Composable
internal fun SecurityUnlockContent(
    settings: SecuritySettings, busy: Boolean, message: String, storageError: String?,
    recovering: Boolean, recoveryVerified: Boolean, onBiometric: () -> Unit,
    onVerify: (String) -> Unit, onRecovery: (Boolean) -> Unit,
    onResetPin: (String, String) -> Unit,
) {
    // Deliberately not saveable: secrets never enter saved-instance-state bundles.
    var secret by remember(recovering) { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var help by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().systemBarsPadding().imePadding(), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                JvyLogo(Modifier.size(88.dp))
                Text("Jvy Wallet", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp))
                    Text(if (recoveryVerified) "Choose a new PIN" else if (recovering) "Recover your wallet" else "Welcome back", style = MaterialTheme.typography.titleMedium)
                }
                Text(if (recovering || recoveryVerified) "Recover access without deleting your entries." else "Your money stays private. Unlock to pick up where you left off.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (recoveryVerified) {
                    SecretField("New six-digit Jvy PIN", pin, { pin = it })
                    SecretField("Confirm your new PIN", confirmation, { confirmation = it })
                    Button(onClick = { onResetPin(pin, confirmation) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Save PIN & continue") }
                } else {
                    if (settings.permitsBiometric && !recovering) {
                        Button(onClick = onBiometric, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Icon(Icons.Outlined.Fingerprint, null); Spacer(Modifier.width(8.dp))
                            Text(if (settings.mode == UnlockMode.LEGACY_SYSTEM) "Use fingerprint or phone PIN" else "Unlock with fingerprint or face")
                        }
                    }
                    if (settings.permitsPin || recovering) {
                        if (settings.permitsBiometric && !recovering) Text("or use your Jvy PIN", style = MaterialTheme.typography.bodySmall)
                        SecretField(if (recovering) "Your recovery code" else "Six-digit Jvy PIN", secret, { secret = it }, recovering)
                        Button(onClick = { onVerify(secret); secret = "" }, enabled = !busy && secret.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (recovering) "Verify recovery code" else "Unlock with PIN") }
                    }
                    if (settings.hasRecovery) TextButton(onClick = { onRecovery(!recovering) }, enabled = !busy) { Text(if (recovering) "Back to unlock" else if (settings.permitsPin) "Forgot PIN? Use recovery code" else "Use my recovery code") }
                    if (settings.mode == UnlockMode.LEGACY_SYSTEM && !recovering) Text("Fingerprint not working? Android's prompt also offers your phone PIN, password or pattern.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                if (busy) CircularProgressIndicator(Modifier.size(28.dp))
                if (message.isNotBlank()) Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                storageError?.let { Text(it, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { help = true }) { Text("Trouble unlocking?") }
                Text("Private on this device", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text("Let's get you back in") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (settings.mode == UnlockMode.LEGACY_SYSTEM) Text("Tap Use fingerprint or phone PIN. In Android's prompt, choose your phone PIN, password or pattern if fingerprint recognition does not work.")
            if (settings.permitsPin) Text("Your Jvy PIN is the six-digit PIN you chose in Jvy, not necessarily your phone's PIN.")
            Text(if (settings.hasRecovery) "Your saved recovery code lets you choose a new Jvy PIN without removing your data." else "This unlock setup has no Jvy backup code yet. After unlocking, open Security, verify your current method and create a backup code. You can keep using fingerprint.")
            Text("There is no hidden bypass. Do not uninstall Jvy or clear its storage to troubleshoot: local entries could be lost. Try your enrolled method or saved recovery code first.")
        } }, confirmButton = { TextButton(onClick = { help = false }) { Text("Got it") } },
        dismissButton = if (settings.hasRecovery && !recoveryVerified) ({ TextButton(onClick = { help = false; onRecovery(true) }) { Text("Use recovery code") } }) else null)
}

@Composable
internal fun ScreenCaptureSetting(allowed: Boolean, onChange: (Boolean) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Screenshots & screen recording", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = allowed, onCheckedChange = { if (it) confirm = true else onChange(false) }, modifier = Modifier.semantics { contentDescription = "Allow screenshots and screen recording" })
            }
            Text(if (allowed) "Allowed on unlocked wallet screens. Useful for sharing and marketing." else "Blocked on wallet screens to keep your finances private.")
            Text("Unlock, PIN and recovery-code screens always stay protected. This takes effect after leaving Security. Capture also allows unlocked previews in Android's recent-apps view.", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Allow screen capture?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text("Screenshots, recordings, screen sharing and recent-app previews may show your balances and transactions while Jvy is unlocked. Use sample data or hide balances before creating marketing images. PIN and recovery screens remain protected.") } },
        confirmButton = { TextButton(onClick = { confirm = false; onChange(true) }) { Text("Allow on unlocked screens") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep private") } })
}
