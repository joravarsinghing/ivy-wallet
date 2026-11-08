package com.ivy.design.system

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/** Planning pages share one back location, action shape and restrained empty-state hierarchy. */
@Composable
// Preserve the established positional subtitle/backModifier API for callers.
@Suppress("ComposeParameterOrder")
fun JvyScreenHeader(title: String, onBack: (() -> Unit)?, subtitle: String? = null, backModifier: Modifier = Modifier,
                    horizontalPadding: Dp = JvySpacing.PageGutter,
                    backEnabled: Boolean = true,
                    actions: @Composable RowScope.() -> Unit = {}) {
    val largeText = LocalDensity.current.fontScale > 1.3f && onBack != null
    Column(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) JvyBackButton(onBack, backModifier,
                tint = MaterialTheme.colorScheme.onBackground, enabled = backEnabled)
            if (largeText) Spacer(Modifier.weight(1f)) else {
                if (onBack != null) Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = JvyTypography.PageTitle, color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() })
                    subtitle?.let { Text(it, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            actions()
        }
        if (largeText) {
            Spacer(Modifier.height(JvySpacing.Related))
            Text(title, style = JvyTypography.PageTitle, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun JvyPrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        JvyButton(onClick, Modifier.padding(horizontal = JvySpacing.PageGutter, vertical = JvySpacing.ActionInset).fillMaxWidth(), enabled = enabled) { Text(label) }
    }
}

@Composable
fun JvyEmptyState(title: String, description: String, icon: ImageVector,
                  modifier: Modifier = Modifier,
                  actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = JvyShapes.Control, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, null, Modifier.padding(16.dp).size(24.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(title, style = JvyTypography.SectionTitle, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Text(description, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        actionLabel?.let { JvyButton(onAction) { Text(it) } }
    }
}
