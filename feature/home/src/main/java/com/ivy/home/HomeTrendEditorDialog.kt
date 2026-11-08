package com.ivy.home

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.JvyButton
import com.ivy.design.system.JvyFormDialogFrame
import com.ivy.design.system.JvyTypography
import androidx.compose.material3.MaterialTheme

/** Presentation-only editor for the metrics visible in [HomeTrendCard]. */
@Composable
fun HomeTrendEditorDialog(
    visibleMetrics: Set<HomeTrendMetric>,
    onMetricToggle: (HomeTrendMetric) -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        HomeTrendEditorContent(visibleMetrics, onMetricToggle, onClose)
    }
}

@Composable
internal fun HomeTrendEditorContent(visibleMetrics: Set<HomeTrendMetric>, onMetricToggle: (HomeTrendMetric) -> Unit,
                                    onClose: () -> Unit) {
    JvyFormDialogFrame("Edit money trends", onClose, footer = {
        JvyButton(onClose, Modifier.fillMaxWidth()) { Text("Done") }
    }) {
            Text(
                text = "Choose the measures you want to see on Home.",
                style = JvyTypography.Supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HomeTrendMetric.values().forEach { metric ->
                TrendMetricToggleRow(
                    metric = metric,
                    checked = metric in visibleMetrics,
                    onClick = { onMetricToggle(metric) },
                )
            }
    }
}

@Composable
private fun TrendMetricToggleRow(
    metric: HomeTrendMetric,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(UI.colors.pure)
            .toggleable(checked, role = Role.Checkbox, onValueChange = { onClick() })
            .semantics {
                role = Role.Checkbox
                contentDescription = "Show ${metric.label}"
                stateDescription = if (checked) "Shown" else "Hidden"
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = metric.label,
            style = UI.typo.b2.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.Bold,
            ),
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (checked) UI.colors.primary else UI.colors.medium),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    style = UI.typo.c.style(
                        color = UI.colors.pure,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
        }
    }
}
