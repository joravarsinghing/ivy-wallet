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
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import java.math.BigDecimal

internal enum class SafeToSpendStatus {
    SAFE,
    ZERO,
    BUFFER_EXCEEDED,
}

internal data class SafeToSpendResult(
    val amount: BigDecimal,
    val status: SafeToSpendStatus,
)

/**
 * Calculates spendable money without dipping into the protected buffer.
 *
 * The result is deliberately kept in the Home feature for now so this card can ship
 * independently. Its BigDecimal inputs and result are compatible with a future domain use case.
 */
internal fun calculateSafeToSpend(
    balance: BigDecimal,
    protectedBuffer: BigDecimal,
): SafeToSpendResult {
    val difference = balance.subtract(protectedBuffer)
    val status = when {
        difference.signum() > 0 -> SafeToSpendStatus.SAFE
        difference.signum() == 0 -> SafeToSpendStatus.ZERO
        else -> SafeToSpendStatus.BUFFER_EXCEEDED
    }

    return SafeToSpendResult(
        amount = difference.max(BigDecimal.ZERO),
        status = status,
    )
}

/**
 * A standalone beginner-friendly explanation of what can be spent while preserving a buffer.
 *
 * It accepts plain financial values instead of a ViewModel or database dependency, so Home can
 * integrate it later without changing the component contract or the Ivy import pipeline.
 */
@Composable
fun SafeToSpendCard(
    balance: BigDecimal,
    protectedBuffer: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val result = calculateSafeToSpend(balance, protectedBuffer)
    val visualStyle = result.visualStyle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(visualStyle.background, UI.shapes.r3)
            .testTag("safe_to_spend_card"),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = "Safe to spend",
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
                amount = result.amount.toDouble(),
                currency = currency,
                textColor = visualStyle.amountColor,
                amountFontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            text = result.beginnerMessage(),
            style = UI.typo.b2.style(
                color = visualStyle.messageColor,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private data class SafeToSpendVisualStyle(
    val background: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
    val amountColor: androidx.compose.ui.graphics.Color,
    val messageColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun SafeToSpendResult.visualStyle(): SafeToSpendVisualStyle = when (status) {
    SafeToSpendStatus.SAFE -> SafeToSpendVisualStyle(
        background = UI.colors.medium,
        textColor = UI.colors.pureInverse,
        amountColor = Green,
        messageColor = UI.colors.gray,
    )

    SafeToSpendStatus.ZERO -> SafeToSpendVisualStyle(
        background = UI.colors.medium,
        textColor = UI.colors.pureInverse,
        amountColor = UI.colors.gray,
        messageColor = UI.colors.gray,
    )

    SafeToSpendStatus.BUFFER_EXCEEDED -> SafeToSpendVisualStyle(
        background = UI.colors.red,
        textColor = White,
        amountColor = White,
        messageColor = White,
    )
}

private fun SafeToSpendResult.beginnerMessage(): String = when (status) {
    SafeToSpendStatus.SAFE ->
        "You can spend this amount and still keep your protected buffer for surprises."

    SafeToSpendStatus.ZERO ->
        "Your protected buffer uses your available balance. Wait for more income before spending extra."

    SafeToSpendStatus.BUFFER_EXCEEDED ->
        "Your balance is below your protected buffer. Pause non-essential spending while you rebuild it."
}
