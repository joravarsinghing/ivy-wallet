package com.ivy.planned.list

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
fun CashFlowForecastCard(forecast: CashFlowForecast, currency: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(UI.colors.medium)
            .padding(16.dp),
    ) {
        Text("Cash-flow forecast", style = UI.typo.b1.style(color = UI.colors.pureInverse, fontWeight = FontWeight.ExtraBold))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            ForecastValue("Recurring net", forecast.recurringNet.format(currency), Modifier.weight(1f))
            ForecastValue("Next period", forecast.nextPeriodNet.format(currency), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            ForecastValue("Current balance", forecast.currentBalance.format(currency), Modifier.weight(1f))
            ForecastValue("Projected balance", forecast.projectedBalance.format(currency), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Uses your current balance plus recurring and upcoming planned payments.",
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun ForecastValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium))
        Text(value, style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold))
    }
}
