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
import androidx.compose.material3.TextButton

/** Presentation-only editor for the cards shown on Home. */
@Composable
fun HomeDashboardEditorDialog(
    visibleCards: Set<HomeDashboardCardId>,
    onCardToggle: (HomeDashboardCardId) -> Unit,
    onClose: () -> Unit,
    onRestoreDefaults: () -> Unit,
    cards: List<HomeDashboardCardDefinition> = defaultHomeDashboardCards,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        HomeDashboardEditorContent(visibleCards, onCardToggle, onClose, onRestoreDefaults, cards)
    }
}

@Composable
internal fun HomeDashboardEditorContent(visibleCards: Set<HomeDashboardCardId>, onCardToggle: (HomeDashboardCardId) -> Unit,
                                        onClose: () -> Unit, onRestoreDefaults: () -> Unit,
                                        cards: List<HomeDashboardCardDefinition> = defaultHomeDashboardCards) {
    JvyFormDialogFrame("Edit Home", onClose, footer = {
        Column(Modifier.fillMaxWidth()) {
            TextButton(onRestoreDefaults, Modifier.fillMaxWidth()) { Text("Restore defaults") }
            JvyButton(onClose, Modifier.fillMaxWidth()) { Text("Done") }
        }
    }) {
            Text(
                text = "Choose the cards you want to see on Home.",
                style = JvyTypography.Supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            cards.forEach { card ->
                HomeDashboardToggleRow(
                    card = card,
                    checked = card.id in visibleCards,
                    onClick = { onCardToggle(card.id) },
                )
            }
    }
}

@Composable
private fun HomeDashboardToggleRow(
    card: HomeDashboardCardDefinition,
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
                contentDescription = "Show ${card.label}"
                stateDescription = if (checked) "Shown" else "Hidden"
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.label,
                style = UI.typo.b2.style(
                    color = UI.colors.pureInverse,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = card.description,
                style = UI.typo.c.style(
                    color = UI.colors.gray,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        Spacer(Modifier.size(12.dp))
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

