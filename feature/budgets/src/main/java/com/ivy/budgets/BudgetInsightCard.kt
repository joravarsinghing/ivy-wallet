package com.ivy.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.JvyShapes
import com.ivy.design.system.JvyTypography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.style
import com.ivy.legacy.utils.format

@Composable
fun BudgetInsightCard(insight: BudgetInsight, currency: String, overlappingLimits: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(JvyShapes.Card)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        Text("Budget health", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (LocalDensity.current.fontScale > 1.3f) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightValue(if (overlappingLimits) "Spent across limits" else "Spent", "${insight.totalSpent.format(currency)} $currency", Modifier.fillMaxWidth())
            InsightValue(if (overlappingLimits) "Remaining across limits" else "Remaining", "${insight.remaining.format(currency)} $currency", Modifier.fillMaxWidth())
        } else Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            InsightValue(if (overlappingLimits) "Spent across limits" else "Spent", "${insight.totalSpent.format(currency)} $currency", Modifier.weight(1f))
            InsightValue(if (overlappingLimits) "Remaining across limits" else "Remaining", "${insight.remaining.format(currency)} $currency", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                insight.overBudgetCount == 1 -> "One budget is over its limit"
                insight.overBudgetCount > 1 -> "${insight.overBudgetCount} budgets are over their limits"
                insight.atRiskCount == 1 -> "One budget is nearing its limit"
                insight.atRiskCount > 1 -> "${insight.atRiskCount} budgets are nearing their limits"
                insight.totalBudget > 0.0 -> "You are within your budget limits"
                else -> "Add category budgets to see health insights"
            },
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (overlappingLimits) Text("A purchase can count toward more than one limit. Check each budget before spending.",
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InsightValue(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = JvyTypography.AmountSmall)
    }
}
