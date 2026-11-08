package com.ivy.design.system

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ivy.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class JvyLegalDocument(val title: String, val resource: Int) {
    TERMS("Terms of use", R.raw.jvy_terms), PRIVACY("Privacy policy", R.raw.jvy_privacy),
    LICENSE("Open-source license", R.raw.jvy_license)
}

/** Local, versioned documents: no browser, remote markdown or ledger/network access. */
@Composable
fun JvyLegalDialog(document: JvyLegalDocument, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var showing by rememberSaveable(document) { mutableStateOf(document) }
    val context = LocalContext.current
    val body by produceState<String?>(null, showing, context) {
        value = null
        value = withContext(Dispatchers.IO) {
            context.resources.openRawResource(showing.resource).bufferedReader().use { it.readText() }
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        JvyFormDialogFrame(showing.title, onClose, modifier = modifier, footer = {
            JvyButton(onClose) { Text("Done") }
        }) {
            Text(body ?: "Loading document…", style = JvyTypography.Body)
            if (showing == JvyLegalDocument.TERMS) JvyTextButton({ showing = JvyLegalDocument.LICENSE }) { Text("Read the open-source license") }
            if (showing == JvyLegalDocument.LICENSE) JvyTextButton({ showing = JvyLegalDocument.TERMS }) { Text("Back to terms") }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun JvyLegalLinks(modifier: Modifier = Modifier) {
    var document by rememberSaveable { mutableStateOf<JvyLegalDocument?>(null) }
    androidx.compose.foundation.layout.FlowRow(modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        JvyOutlinedButton({ document = JvyLegalDocument.TERMS }) { Text("Terms of use") }
        JvyOutlinedButton({ document = JvyLegalDocument.PRIVACY }) { Text("Privacy policy") }
    }
    document?.let { JvyLegalDialog(it, onClose = { document = null }) }
}
