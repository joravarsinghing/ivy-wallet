package com.ivy.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.utils.format

@Composable
fun BudgetInsightCard(insight: BudgetInsight, currency: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(UI.colors.medium)
            .padding(16.dp),
    ) {
        Text("Budget health", style = UI.typo.b1.style(color = UI.colors.pureInverse, fontWeight = FontWeight.ExtraBold))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            InsightValue("Spent", insight.totalSpent.format(currency))
            InsightValue("Remaining", insight.remaining.format(currency))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                insight.overBudgetCount > 0 -> "${insight.overBudgetCount} budget(s) exceeded"
                insight.atRiskCount > 0 -> "${insight.atRiskCount} budget(s) are nearing their limit"
                insight.totalBudget > 0.0 -> "You are within your budget limits"
                else -> "Add category budgets to see health insights"
            },
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun InsightValue(label: String, value: String) {
    Column {
        Text(label, style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium))
        Text(value, style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold))
    }
}
