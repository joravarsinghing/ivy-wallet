package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.IvyDividerLine

@Suppress("UnusedParameter")
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun ModalAmountSection(
    label: String,
    currency: String,
    amount: Double,
    modifier: Modifier = Modifier,
    Header: (@Composable () -> Unit)? = null,
    amountPaddingTop: Dp = 48.dp,
    amountPaddingBottom: Dp = 48.dp,
    enabled: Boolean = true,
    showAmountModal: () -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        Header?.invoke()
        Spacer(Modifier.height(amountPaddingTop.coerceAtMost(16.dp)))
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(label, style = com.ivy.design.system.JvyTypography.Supporting,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            com.ivy.design.system.JvyOutlinedButton(showAmountModal,
                Modifier.fillMaxWidth().testTag("amount_balance"), enabled = enabled) {
                // This is the explicitly editable amount, not a read-only balance.
                Text(if (amount.isFinite()) com.ivy.ui.money.formatDecimalMoney(amount.toBigDecimal(), currency) else "Enter a valid amount.",
                    style = com.ivy.design.system.JvyTypography.AmountSmall)
            }
        }
        Spacer(Modifier.height(amountPaddingBottom.coerceAtMost(16.dp)))
    }
}
