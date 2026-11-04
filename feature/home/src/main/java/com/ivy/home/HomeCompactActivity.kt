package com.ivy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.findContrastTextColor
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
    modifier: Modifier = Modifier,
    dateWindow: HomeDateWindow? = null,
) {
    val nav = navigation()
    val recent = compactHistory(history, 7)
    val planned = overdue.trns + upcoming.trns
    Column(modifier = modifier.fillMaxWidth()) {
        if (planned.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                .clip(com.ivy.design.system.JvyShapes.Card).background(UI.colors.medium).padding(vertical = 18.dp)) {
                Text(stringResource(R.string.planned_payments_inline), Modifier.padding(horizontal = 20.dp),
                    style = UI.typo.b1.style(UI.colors.pureInverse, FontWeight.ExtraBold))
                Spacer(Modifier.size(12.dp))
                planned.take(3).forEach { transaction ->
                    CompactActivityRow(transaction = transaction, baseData = baseData, hideBalance = hideBalance,
                        actionLabel = if (transaction.type == TransactionType.INCOME) "Get" else "Pay",
                        onClick = { onPayOrGet(transaction) }, onAction = { onPayOrGet(transaction) },
                        onSkip = { onSkipTransaction(transaction) })
                }
                if (overdue.trns.isNotEmpty()) Text("Skip all overdue",
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp).clickable { onSkipAllTransactions(overdue.trns) },
                    style = UI.typo.c.style(UI.colors.gray, FontWeight.Bold))
            }
            Spacer(Modifier.size(16.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(com.ivy.design.system.JvyShapes.Card)
                .background(UI.colors.medium)
                .padding(vertical = 18.dp),
        ) {
            HomeCardHeading(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                heading = {
                    Text("Recent activity", style = UI.typo.b1.style(UI.colors.pureInverse, FontWeight.ExtraBold))
                    Text(dateWindow?.let { stringResource(R.string.home_chart_range_caption, it.displayLabel()) }
                        ?: stringResource(R.string.home_chart_range_loading),
                        style = UI.typo.c.style(UI.colors.gray, FontWeight.Medium))
                },
                action = {
                    com.ivy.design.system.JvyTextButton(onClick = {
                        nav.navigateTo(TransactionsScreen(allTransactions = true))
                    }) { Text("View all") }
                },
            )
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
            if (recent.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_activity_period_empty),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    style = UI.typo.c.style(UI.colors.gray, FontWeight.Medium),
                )
            }
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
@OptIn(ExperimentalLayoutApi::class)
internal fun CompactActivityRow(
    transaction: Transaction,
    baseData: AppBaseData,
    hideBalance: Boolean,
    onClick: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
) {
    val category = baseData.categories.firstOrNull { it.id.value == transaction.categoryId }
    val account = baseData.accounts.firstOrNull { it.id == transaction.accountId }
    val color = category?.color?.value?.toComposeColor() ?: UI.colors.gray
    val amountColor = if (transaction.type == TransactionType.INCOME) UI.colors.primary else UI.colors.pureInverse
    val amount = if (hideBalance) "••••" else com.ivy.design.system.financialText(
        "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${groupedCompactAmount(transaction.amount)} ${account?.currency ?: baseData.baseCurrency}")
    val stacked = LocalDensity.current.fontScale > 1.3f || actionLabel != null || amount.length > 20
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(30.dp).background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                ItemIconSDefaultIcon(
                    modifier = Modifier.size(22.dp),
                    iconName = category?.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_s,
                    tint = findContrastTextColor(color),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = transaction.title?.takeIf { it.isNotBlank() } ?: "Transaction",
                    maxLines = if (stacked) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    style = UI.typo.c.style(UI.colors.pureInverse, FontWeight.Bold),
                )
                Text(
                    text = account?.name ?: "Account",
                    maxLines = if (stacked) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    style = UI.typo.c.style(UI.colors.gray, FontWeight.Medium),
                )
            }
            if (!stacked) Text(amount, style = UI.typo.c.style(amountColor, FontWeight.Bold))
        }
        if (stacked) FlowRow(Modifier.fillMaxWidth().padding(start = 42.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(amount, style = UI.typo.c.style(amountColor, FontWeight.Bold))
            if (actionLabel != null && onAction != null) {
                com.ivy.design.system.JvyTextButton(onAction) { Text(actionLabel) }
            }
        }
    }
}

private fun groupedCompactAmount(amount: BigDecimal): String =
    String.format(Locale.US, "%,.2f", amount.abs().toDouble())
