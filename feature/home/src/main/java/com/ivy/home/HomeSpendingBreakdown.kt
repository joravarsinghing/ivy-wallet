package com.ivy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.navigation
import com.ivy.wallet.ui.theme.toComposeColor
import java.math.BigDecimal
import java.util.Locale

data class HomeSpendingBreakdownRow(
    val categoryId: String?,
    val label: String,
    val color: Int,
    val amount: BigDecimal,
    val percentage: BigDecimal,
)

data class HomeSpendingBreakdownState(
    val total: BigDecimal = BigDecimal.ZERO,
    val rows: List<HomeSpendingBreakdownRow> = emptyList(),
) {
    val hasSpending: Boolean get() = total.signum() > 0 && rows.isNotEmpty()
}

fun buildHomeSpendingBreakdown(
    rows: List<HomeSpendingBreakdownRow>,
    maxCategories: Int = 3,
): HomeSpendingBreakdownState {
    val positive = rows.filter { it.amount.signum() > 0 }.sortedByDescending { it.amount }
    val total = positive.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount }
    if (total.signum() == 0) return HomeSpendingBreakdownState()

    val visible = positive.take(maxCategories).map { it.copy(percentage = percentOf(it.amount, total)) }
    val otherAmount = positive.drop(maxCategories).fold(BigDecimal.ZERO) { sum, row -> sum + row.amount }
    val finalRows = if (otherAmount.signum() > 0) {
        visible + HomeSpendingBreakdownRow(
            categoryId = null,
            label = "Other",
            color = 0xFF777777.toInt(),
            amount = otherAmount,
            percentage = percentOf(otherAmount, total),
        )
    } else visible
    return HomeSpendingBreakdownState(total = total, rows = finalRows)
}

private fun percentOf(amount: BigDecimal, total: BigDecimal): BigDecimal =
    amount.multiply(BigDecimal(100)).divide(total, 4, java.math.RoundingMode.HALF_UP)

@Composable
fun HomeSpendingBreakdownCard(
    state: HomeSpendingBreakdownState,
    currency: String,
    hideBalance: Boolean,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(UI.colors.medium)
            .clickable(onClick = { nav.navigateTo(ReportScreen) })
            .semantics {
                role = Role.Button
                contentDescription = "Open spending reports"
            }
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text(
            text = "Where your money went",
            style = UI.typo.b1.style(color = UI.colors.pureInverse, fontWeight = FontWeight.ExtraBold),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Top spending categories for this period",
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(18.dp))

        if (!state.hasSpending) {
            Text(
                text = "Categorized expenses will appear here as you track them.",
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
            )
        } else {
            state.rows.forEachIndexed { index, row ->
                SpendingBreakdownRow(row, currency, hideBalance)
                if (index != state.rows.lastIndex) Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun SpendingBreakdownRow(
    row: HomeSpendingBreakdownRow,
    currency: String,
    hideBalance: Boolean,
) {
    val color = row.color.toComposeColor()
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(color, CircleShape))
            Spacer(Modifier.size(8.dp))
            Text(
                text = row.label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = UI.typo.c.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold),
            )
            Text(
                text = if (hideBalance) "••••" else "${groupedHomeAmount(row.amount)} $currency",
                style = UI.typo.c.style(color = color, fontWeight = FontWeight.Bold),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(UI.colors.pure),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(row.percentage.toFloat().coerceIn(0f, 1f))
                        .height(7.dp)
                        .background(color, RoundedCornerShape(10.dp)),
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(
                text = "${row.percentage.setScale(0, java.math.RoundingMode.HALF_UP)}%",
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
            )
        }
    }
}

private fun groupedHomeAmount(amount: BigDecimal): String =
    String.format(Locale.US, "%,.2f", amount.toDouble())
