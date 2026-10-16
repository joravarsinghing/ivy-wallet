package com.ivy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.ui.theme.toComposeColor
import java.math.BigDecimal
import java.util.Locale

@Composable
fun HomeCompactActivity(
    baseData: AppBaseData,
    upcoming: LegacyDueSection,
    overdue: LegacyDueSection,
    history: List<TransactionHistoryItem>,
    hideBalance: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
) {
    val nav = navigation()
    val recent = compactHistory(history, 7)
    val planned = overdue.trns + upcoming.trns
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(UI.colors.medium)
            .padding(vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Recent activity", style = UI.typo.b1.style(UI.colors.pureInverse, FontWeight.ExtraBold))
                Text("Your latest money moves", style = UI.typo.c.style(UI.colors.gray, FontWeight.Medium))
            }
            Text(
                text = "View all",
                modifier = Modifier.clickable { nav.navigateTo(TransactionsScreen()) },
                style = UI.typo.c.style(UI.colors.primary, FontWeight.Bold),
            )
        }
        if (planned.isNotEmpty()) {
            Spacer(Modifier.size(12.dp))
            planned.take(3).forEach { transaction ->
                CompactActivityRow(
                    transaction = transaction,
                    baseData = baseData,
                    hideBalance = hideBalance,
                    actionLabel = if (transaction.type == TransactionType.INCOME) "Get" else "Pay",
                    onClick = { onPayOrGet(transaction) },
                    onAction = { onPayOrGet(transaction) },
                    onSkip = { onSkipTransaction(transaction) },
                )
            }
            if (overdue.trns.isNotEmpty()) {
                Text(
                    text = "Skip all overdue",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).clickable {
                        onSkipAllTransactions(overdue.trns)
                    },
                    style = UI.typo.c.style(UI.colors.gray, FontWeight.Bold),
                )
            }
        }
        if (recent.isNotEmpty()) {
            Spacer(Modifier.size(8.dp))
            recent.forEach { item ->
                when (item) {
                    is TransactionHistoryDateDivider -> {
                        Text(
                            text = "${item.date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${item.date.dayOfMonth}",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = UI.typo.c.style(UI.colors.gray, FontWeight.Bold),
                        )
                    }
                    is Transaction -> CompactActivityRow(
                        transaction = item,
                        baseData = baseData,
                        hideBalance = hideBalance,
                        onClick = { nav.navigateTo(EditTransactionScreen(item.id, item.type)) },
                    )
                }
            }
        }
        if (planned.isEmpty() && recent.isEmpty()) {
            Text(
                text = "Your activity will appear here after your first transaction.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                style = UI.typo.c.style(UI.colors.gray, FontWeight.Medium),
            )
        }
    }
}

private fun compactHistory(history: List<TransactionHistoryItem>, maxTransactions: Int): List<TransactionHistoryItem> {
    val result = mutableListOf<TransactionHistoryItem>()
    var count = 0
    history.forEach { item ->
        if (count >= maxTransactions) return@forEach
        result += item
        if (item is Transaction) count++
    }
    return result
}

@Composable
private fun CompactActivityRow(
    transaction: Transaction,
    baseData: AppBaseData,
    hideBalance: Boolean,
    actionLabel: String? = null,
    onClick: () -> Unit,
    onAction: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
) {
    val category = baseData.categories.firstOrNull { it.id.value == transaction.categoryId }
    val account = baseData.accounts.firstOrNull { it.id == transaction.accountId }
    val color = category?.color?.value?.toComposeColor() ?: UI.colors.gray
    val amountColor = if (transaction.type == TransactionType.INCOME) UI.colors.primary else UI.colors.pureInverse
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(30.dp).background(color, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.title?.takeIf { it.isNotBlank() } ?: "Transaction",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = UI.typo.c.style(UI.colors.pureInverse, FontWeight.Bold),
            )
            Text(
                text = account?.name ?: "Account",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = UI.typo.c.style(UI.colors.gray, FontWeight.Medium),
            )
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                modifier = Modifier.clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 4.dp),
                style = UI.typo.c.style(UI.colors.primary, FontWeight.Bold),
            )
        }
        Text(
            text = if (hideBalance) "••••" else "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${groupedCompactAmount(transaction.amount)} ${account?.currency ?: baseData.baseCurrency}",
            style = UI.typo.c.style(amountColor, FontWeight.Bold),
        )
    }
}

private fun groupedCompactAmount(amount: BigDecimal): String =
    String.format(Locale.US, "%,.2f", amount.abs().toDouble())
