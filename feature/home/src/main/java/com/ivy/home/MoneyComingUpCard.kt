package com.ivy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.domain.usecase.cashflow.PlannedCashflowType
import com.ivy.legacy.utils.formatDateOnly
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import java.math.BigDecimal

@Composable
internal fun MoneyComingUpCard(
    state: MoneyComingUpState,
    currency: String,
    hideBalance: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(UI.colors.medium, UI.shapes.r3)
            .testTag("money_coming_up_card"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = state.status == MoneyComingUpStatus.CONTENT) {
                    expanded = !expanded
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Money coming up",
                    style = UI.typo.b1.style(fontWeight = FontWeight.ExtraBold),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Next 30 days",
                    style = UI.typo.c.style(
                        color = UI.colors.gray,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            if (state.status == MoneyComingUpStatus.CONTENT) {
                Text(
                    text = if (expanded) "⌃" else "⌄",
                    style = UI.typo.b1.style(fontWeight = FontWeight.ExtraBold),
                )
            }
        }

        when (state.status) {
            MoneyComingUpStatus.LOADING -> Message("Loading forecast…")
            MoneyComingUpStatus.ERROR -> Message("Money coming up is unavailable")
            MoneyComingUpStatus.EMPTY -> {
                SummaryRow(
                    label = "Available after goals",
                    amount = state.currentAvailable,
                    currency = currency,
                    hideBalance = hideBalance,
                )
                Message("Nothing is scheduled yet. Add a planned bill or income to improve this forecast.")
            }
            MoneyComingUpStatus.CONTENT -> {
                SummaryRow(
                    label = "Income",
                    amount = state.upcomingIncome,
                    currency = currency,
                    hideBalance = hideBalance,
                    amountColor = Green,
                )
                SummaryRow(
                    label = "Expenses",
                    amount = state.upcomingExpenses,
                    currency = currency,
                    hideBalance = hideBalance,
                    amountColor = Red,
                )
                SummaryRow(
                    label = "Lowest balance",
                    amount = state.lowestProjectedAvailable,
                    currency = currency,
                    hideBalance = hideBalance,
                )
                if (state.shortfall.signum() > 0) {
                    SummaryRow(
                        label = "Projected shortfall",
                        amount = state.shortfall,
                        currency = currency,
                        hideBalance = hideBalance,
                        amountColor = Red,
                    )
                }
                if (expanded) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = "Events",
                        style = UI.typo.c.style(
                            color = UI.colors.gray,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                    state.events.forEach { event ->
                        EventRow(
                            event = event,
                            currency = currency,
                            hideBalance = hideBalance,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: BigDecimal,
    currency: String,
    hideBalance: Boolean,
    amountColor: androidx.compose.ui.graphics.Color = UI.colors.pureInverse,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = UI.typo.b2.style(fontWeight = FontWeight.Medium),
        )
        HiddenAwareAmount(
            amount = amount,
            currency = currency,
            hideBalance = hideBalance,
            color = amountColor,
        )
    }
}

@Composable
private fun EventRow(
    event: MoneyComingUpEvent,
    currency: String,
    hideBalance: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = event.date.formatDateOnly(),
            style = UI.typo.c.style(color = UI.colors.gray),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = if (event.type == PlannedCashflowType.INCOME) "Income" else "Expense",
            style = UI.typo.b2.style(fontWeight = FontWeight.Medium),
        )
        HiddenAwareAmount(
            amount = event.amount,
            currency = currency,
            hideBalance = hideBalance,
            color = if (event.type == PlannedCashflowType.INCOME) Green else Red,
        )
    }
}

@Composable
private fun HiddenAwareAmount(
    amount: BigDecimal,
    currency: String,
    hideBalance: Boolean,
    color: androidx.compose.ui.graphics.Color,
) {
    if (hideBalance) {
        Text(
            text = "••••",
            style = UI.typo.b2.style(
                color = color,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    } else {
        AmountCurrencyB1(
            amount = amount.toDouble(),
            currency = currency,
            textColor = color,
            amountFontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Message(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = text,
        style = UI.typo.b2.style(
            color = UI.colors.gray,
            fontWeight = FontWeight.Medium,
        ),
    )
}
