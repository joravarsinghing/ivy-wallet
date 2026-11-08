package com.ivy.design.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.ivy.ui.R

/** Loading never exposes active fields before their record has been read successfully. */
@Composable
fun JvyEditorLoading(title: String, failed: Boolean, onBack: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    JvySubsectionScaffold(title, onBack, modifier = modifier) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(JvySpacing.PageGutter),
            verticalArrangement = Arrangement.spacedBy(JvySpacing.Group)) {
            if (failed) {
                Text(stringResource(R.string.jvy_editor_open_failed), style = JvyTypography.Body,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                JvyButton(onRetry) { Text(stringResource(R.string.jvy_startup_retry)) }
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
