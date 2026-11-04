package com.ivy.legacy.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.legacy.stringRes
import com.ivy.base.model.TransactionType
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.findContrastTextColor

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@Composable
fun IncomeExpensesCards(
    history: List<TransactionHistoryItem>,
    currency: String,
    income: Double,
    expenses: Double,

    hasAddButtons: Boolean,
    itemColor: Color,

    incomeHeaderCardClicked: () -> Unit = {},
    expenseHeaderCardClicked: () -> Unit = {},
    onAddTransaction: (TransactionType) -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        val stacked = maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f
        val cards: @Composable (Modifier) -> Unit = { cardModifier ->
        HeaderCard(
            modifier = cardModifier,
            title = stringRes(R.string.income_uppercase),
            currencyCode = currency,
            amount = income,
            transactionCount = history
                .filterIsInstance(Transaction::class.java)
                .count { it.type == TransactionType.INCOME },
            addButtonText = if (hasAddButtons) stringResource(R.string.add_income) else null,
            isIncome = true,

            itemColor = itemColor,
            onHeaderCardClicked = { incomeHeaderCardClicked() }
        ) {
            onAddTransaction(TransactionType.INCOME)
        }

        HeaderCard(
            modifier = cardModifier,
            title = stringRes(R.string.expenses_uppercase),
            currencyCode = currency,
            amount = expenses,
            transactionCount = history
                .filterIsInstance(Transaction::class.java)
                .count { it.type == TransactionType.EXPENSE },
            addButtonText = if (hasAddButtons) stringResource(R.string.add_expense) else null,
            isIncome = false,

            itemColor = itemColor,
            onHeaderCardClicked = { expenseHeaderCardClicked() }
        ) {
            onAddTransaction(TransactionType.EXPENSE)
        }

        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { cards(Modifier.fillMaxWidth()) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                cards(Modifier.weight(1f))
            }
        }
    }
}

@Composable
@Suppress("ParameterNaming")
private fun HeaderCard(
    title: String,
    currencyCode: String,
    amount: Double,
    transactionCount: Int,

    isIncome: Boolean,
    addButtonText: String?,

    itemColor: Color,

    modifier: Modifier = Modifier,
    onHeaderCardClicked: () -> Unit = {},
    onAddClick: () -> Unit
) {
    val contrastColor = findContrastTextColor(itemColor)

    Column(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHeaderCardClicked() }
                .padding(vertical = 8.dp),
        ) {
                Text(
                    text = title,
                    style = UI.typo.c.style(
                        color = contrastColor,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = com.ivy.design.system.financialText(amount.format(currencyCode)),
                    style = UI.typo.b1.style(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            Text(
                text = "$transactionCount ${stringRes(R.string.transactions)}",
                style = UI.typo.c.style(
                    color = contrastColor.copy(alpha = 0.72f),
                    fontWeight = FontWeight.Medium,
                ),
            )
        }

        if (addButtonText != null) {
            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { onAddClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                text = addButtonText,
                style = UI.typo.c.style(
                    color = if (isIncome) Green else contrastColor,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}
