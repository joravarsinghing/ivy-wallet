package com.ivy.design.system

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Shared, non-scrolling title/Close row for modern forms and legacy modal adapters. */
@Composable
fun JvyDialogHeader(title: String?, onClose: () -> Unit, modifier: Modifier = Modifier,
                    closeEnabled: Boolean = true) {
    Row(modifier.fillMaxWidth().padding(start = JvySpacing.PageGutter, end = JvySpacing.Related,
        top = JvySpacing.Related, bottom = JvySpacing.Related),
        horizontalArrangement = Arrangement.spacedBy(JvySpacing.Related, Alignment.End),
        verticalAlignment = Alignment.CenterVertically) {
        if (title != null) Text(title, Modifier.weight(1f).semantics { heading() },
            style = JvyTypography.SectionTitle, color = MaterialTheme.colorScheme.onSurface)
        JvyCloseButton(onClose, Modifier.testTag("modal_close_button"), enabled = closeEnabled)
    }
}

/** Bounded, IME-safe form frame. Close and submit stay outside the scrolling fields. */
@Composable
fun JvyFormDialogFrame(title: String, onClose: () -> Unit, footer: @Composable () -> Unit,
                       modifier: Modifier = Modifier,
                       bodyPadding: PaddingValues = PaddingValues(horizontal = JvySpacing.PageGutter, vertical = JvySpacing.Related),
                       closeEnabled: Boolean = true,
                       content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(JvySpacing.ModalMargin), contentAlignment = Alignment.Center) {
        Surface(Modifier.widthIn(max = 640.dp).fillMaxWidth().heightIn(max = minOf(maxHeight, 720.dp)),
            shape = JvyShapes.Sheet, color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column {
                JvyDialogHeader(title, onClose, closeEnabled = closeEnabled)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bodyPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.fillMaxWidth().padding(horizontal = JvySpacing.PageGutter, vertical = JvySpacing.ActionInset)) { footer() }
            }
        }
    }
}
