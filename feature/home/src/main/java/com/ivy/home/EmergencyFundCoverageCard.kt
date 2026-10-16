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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.utils.drawColoredShadow
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientGreen
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import java.math.BigDecimal
import java.math.RoundingMode

internal enum class EmergencyFundCoverageLevel {
    ZERO,
    LOW,
    MEDIUM,
    STRONG,
}

internal data class EmergencyFundCoverage(
    val protectedBuffer: BigDecimal,
    val monthlyExpenses: BigDecimal,
    val monthsCovered: BigDecimal,
    val level: EmergencyFundCoverageLevel,
)

/**
 * Calculates how long a protected cash buffer could cover typical monthly expenses.
 *
 * This is deliberately local to the Home feature for now. Its inputs and exact BigDecimal
 * arithmetic provide a stable seam for a future domain use case without coupling the card to
 * the database, import pipeline, or HomeViewModel.
 */
internal fun calculateEmergencyFundCoverage(
    protectedBuffer: BigDecimal,
    monthlyExpenses: BigDecimal,
): EmergencyFundCoverage {
    val normalizedBuffer = protectedBuffer.max(BigDecimal.ZERO)
    val normalizedExpenses = monthlyExpenses.max(BigDecimal.ZERO)
    val monthsCovered = if (
        normalizedExpenses.signum() == 0 || normalizedBuffer.signum() == 0
    ) {
        BigDecimal.ZERO
    } else {
        normalizedBuffer.divide(normalizedExpenses, 2, RoundingMode.HALF_UP)
    }

    val level = when {
        normalizedExpenses.signum() == 0 || normalizedBuffer.signum() == 0 ->
            EmergencyFundCoverageLevel.ZERO

        monthsCovered < BigDecimal.ONE -> EmergencyFundCoverageLevel.LOW
        monthsCovered < BigDecimal("3") -> EmergencyFundCoverageLevel.MEDIUM
        else -> EmergencyFundCoverageLevel.STRONG
    }

    return EmergencyFundCoverage(
        protectedBuffer = normalizedBuffer,
        monthlyExpenses = normalizedExpenses,
        monthsCovered = monthsCovered,
        level = level,
    )
}

/**
 * A standalone, integration-ready Home card for emergency-fund education.
 *
 * The Home screen can add this card later by passing values it already owns; this component does
 * not read state or write data itself.
 */
@Composable
fun EmergencyFundCoverageCard(
    protectedBuffer: BigDecimal,
    monthlyExpenses: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val coverage = calculateEmergencyFundCoverage(protectedBuffer, monthlyExpenses)
    val visualStyle = coverage.visualStyle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .drawColoredShadow(visualStyle.shadowColor)
            .background(visualStyle.background.asHorizontalBrush(), UI.shapes.r3)
            .testTag("emergency_fund_coverage_card"),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = "Safety cushion",
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
            Text(
                text = coverage.monthsCovered.monthsLabel(),
                style = UI.typo.b1.style(
                    color = visualStyle.textColor,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.padding(horizontal = 24.dp)) {
            AmountCurrencyB1(
                amount = coverage.protectedBuffer.toDouble(),
                currency = currency,
                textColor = visualStyle.textColor,
                amountFontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            text = coverage.educationalMessage(),
            style = UI.typo.b2.style(
                color = visualStyle.textColor,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private data class EmergencyFundVisualStyle(
    val background: Gradient,
    val textColor: androidx.compose.ui.graphics.Color,
    val shadowColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun EmergencyFundCoverage.visualStyle(): EmergencyFundVisualStyle = when (level) {
    EmergencyFundCoverageLevel.ZERO -> EmergencyFundVisualStyle(
        background = Gradient.solid(UI.colors.red),
        textColor = White,
        shadowColor = UI.colors.red,
    )

    EmergencyFundCoverageLevel.LOW -> EmergencyFundVisualStyle(
        background = Gradient(UI.colors.pureInverse, UI.colors.gray),
        textColor = UI.colors.pure,
        shadowColor = UI.colors.pureInverse,
    )

    EmergencyFundCoverageLevel.MEDIUM,
    EmergencyFundCoverageLevel.STRONG -> EmergencyFundVisualStyle(
        background = GradientGreen,
        textColor = White,
        shadowColor = GradientGreen.startColor,
    )
}

private fun EmergencyFundCoverage.educationalMessage(): String = when {
    monthlyExpenses.signum() == 0 ->
        "Add your typical monthly expenses to see how long this cushion could support you."

    level == EmergencyFundCoverageLevel.ZERO ->
        "You do not have a safety cushion yet. Start small—even one week of essential expenses is progress."

    level == EmergencyFundCoverageLevel.LOW ->
        "Your cushion covers less than one month. Aim first for one month of essential expenses."

    level == EmergencyFundCoverageLevel.MEDIUM ->
        "You have a helpful cushion. Keep building toward three months of essential expenses."

    else ->
        "Great foundation. Your cushion covers three or more months—keep it separate and refill it after use."
}

private fun BigDecimal.monthsLabel(): String {
    val rounded = setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    val unit = if (compareTo(BigDecimal.ONE) == 0) "month covered" else "months covered"
    return "$rounded $unit"
}
