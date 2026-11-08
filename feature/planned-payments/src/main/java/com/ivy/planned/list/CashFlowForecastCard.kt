package com.ivy.planned.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalDensity
import com.ivy.design.system.JvyShapes
import com.ivy.design.system.JvyTypography
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.legacy.utils.format

@Composable
fun CashFlowForecastCard(forecast: CashFlowForecast, currency: String, modifier: Modifier = Modifier) {
    val values = listOf(
        "Recurring income minus spending" to forecast.recurringNet.format(currency),
        "Next period change" to forecast.nextPeriodNet.format(currency),
        "Current balance" to forecast.currentBalance.format(currency),
        "Projected balance" to forecast.projectedBalance.format(currency),
    )
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        shape = JvyShapes.Card,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cash-flow forecast · $currency", style = JvyTypography.SectionTitle)
            val largeText = LocalDensity.current.fontScale > 1.3f
            BoxWithConstraints {
                if (largeText || maxWidth < 320.dp) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    values.forEach { (label, value) -> ForecastValue(label, value) }
                } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    values.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, value) -> ForecastValue(label, value, Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Text("Uses your current balance plus recurring and upcoming planned payments.",
                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ForecastValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(com.ivy.design.system.financialText(value), style = JvyTypography.AmountSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
