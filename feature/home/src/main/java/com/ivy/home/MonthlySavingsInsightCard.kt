package com.ivy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.domain.usecase.savings.calculateSavingsRate
import com.ivy.wallet.ui.theme.Green
import java.math.BigDecimal
import java.util.Locale

internal enum class MonthlySavingsStatus {
    SAVING,
    BREAK_EVEN,
    SPENDING_EXCEEDS_INCOME,
}

internal data class MonthlySavingsInsight(
    val savings: BigDecimal,
    val status: MonthlySavingsStatus,
)

internal fun calculateMonthlySavingsInsight(
    income: BigDecimal,
    expenses: BigDecimal,
): MonthlySavingsInsight {
    val savings = calculateSavingsRate(
        income = income,
        expenses = expenses,
    ).savingsAmount
    val status = when {
        savings.signum() > 0 -> MonthlySavingsStatus.SAVING
        savings.signum() < 0 -> MonthlySavingsStatus.SPENDING_EXCEEDS_INCOME
        else -> MonthlySavingsStatus.BREAK_EVEN
    }

    return MonthlySavingsInsight(
        savings = savings,
        status = status,
    )
}

/**
 * A small, self-contained explanation of the user's monthly cash flow.
 *
 * The component intentionally owns only presentation and the simple income-minus-expenses
 * calculation. A Home screen can add it later without coupling the card to a ViewModel,
 * database, or import pipeline.
 */
@Composable
fun MonthlySavingsInsightCard(
    income: BigDecimal,
    expenses: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val insight = calculateMonthlySavingsInsight(income, expenses)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(UI.colors.medium, UI.shapes.r3)
            .testTag("monthly_savings_insight_card"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "This month",
                style = UI.typo.b1.style(
                    color = UI.colors.pureInverse,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Text(
                text = currency,
                style = UI.typo.c.style(
                    color = UI.colors.gray,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        )
        {
            MonthlyMetric(
                modifier = Modifier.weight(1f),
                label = "Income",
                amount = income,
                color = UI.colors.pureInverse,
            )
            MonthlyMetric(
                modifier = Modifier.weight(1f),
                label = "Spent",
                amount = expenses,
                color = UI.colors.pureInverse,
            )
            MonthlyMetric(
                modifier = Modifier.weight(1f),
                label = "Saved",
                amount = insight.savings,
                color = if (insight.status == MonthlySavingsStatus.SAVING) Green else UI.colors.pureInverse,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 18.dp),
            text = insight.shortMessage(),
            style = UI.typo.c.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun MonthlyMetric(
    modifier: Modifier,
    label: String,
    amount: BigDecimal,
    color: androidx.compose.ui.graphics.Color,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = UI.typo.c.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = String.format(Locale.US, "%,.2f", amount.toDouble()),
            style = UI.typo.b2.style(
                color = color,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

private fun MonthlySavingsInsight.shortMessage(): String = when (status) {
    MonthlySavingsStatus.SAVING ->
        "You spent less than you earned this month."

    MonthlySavingsStatus.BREAK_EVEN ->
        "You covered your spending, but have no cushion yet."

    MonthlySavingsStatus.SPENDING_EXCEEDS_INCOME ->
        "Spending was higher than income. Review one flexible expense."
}
