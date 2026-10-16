package com.ivy.home

import androidx.compose.foundation.background
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
import com.ivy.legacy.utils.drawColoredShadow
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientGreen
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import java.math.BigDecimal

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
    val visualStyle = insight.visualStyle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .drawColoredShadow(visualStyle.shadowColor)
            .background(visualStyle.background.asHorizontalBrush(), UI.shapes.r3)
            .testTag("monthly_savings_insight_card"),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = "Monthly savings",
            style = UI.typo.b1.style(
                color = visualStyle.textColor,
                fontWeight = FontWeight.ExtraBold,
            ),
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AmountCurrencyB1(
                amount = insight.savings.toDouble(),
                currency = currency,
                textColor = visualStyle.textColor,
                amountFontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            text = insight.educationalMessage(),
            style = UI.typo.b2.style(
                color = visualStyle.textColor,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private data class MonthlySavingsVisualStyle(
    val background: Gradient,
    val textColor: androidx.compose.ui.graphics.Color,
    val shadowColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun MonthlySavingsInsight.visualStyle(): MonthlySavingsVisualStyle = when (status) {
    MonthlySavingsStatus.SAVING -> MonthlySavingsVisualStyle(
        background = GradientGreen,
        textColor = White,
        shadowColor = GradientGreen.startColor,
    )

    MonthlySavingsStatus.BREAK_EVEN -> MonthlySavingsVisualStyle(
        background = Gradient(UI.colors.pureInverse, UI.colors.gray),
        textColor = UI.colors.pure,
        shadowColor = UI.colors.pureInverse,
    )

    MonthlySavingsStatus.SPENDING_EXCEEDS_INCOME -> MonthlySavingsVisualStyle(
        background = Gradient.solid(UI.colors.red),
        textColor = White,
        shadowColor = UI.colors.red,
    )
}

private fun MonthlySavingsInsight.educationalMessage(): String = when (status) {
    MonthlySavingsStatus.SAVING ->
        "You spent less than you earned. Saving this difference builds room for surprises and future goals."

    MonthlySavingsStatus.BREAK_EVEN ->
        "You covered your spending, but there is no cushion yet. Try setting aside a small amount next month."

    MonthlySavingsStatus.SPENDING_EXCEEDS_INCOME ->
        "Your spending was higher than your income. Review one flexible expense before next month to get back on track."
}
