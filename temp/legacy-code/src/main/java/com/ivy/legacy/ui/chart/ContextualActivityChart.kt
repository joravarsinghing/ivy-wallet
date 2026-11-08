package com.ivy.legacy.ui.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.base.analytics.AnalyticsCard
import com.ivy.base.analytics.ChartKind
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.domain.usecase.analytics.AnalyticsEntry
import com.ivy.domain.usecase.analytics.ContextCharts
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.pure.transaction.isWalletCashFlowTransaction
import com.ivy.wallet.domain.pure.transaction.accountCashFlow
import java.util.UUID
import java.math.BigDecimal
import com.ivy.design.system.chart.JvyContextCharts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

fun activityChartSnapshot(id: String, history: List<TransactionHistoryItem>, accounts: List<Account>, zone: ZoneId,
                          now: java.time.Instant = java.time.Instant.now(), accountId: UUID? = null,
                          includeTransfers: Boolean = false, windowFrom: java.time.LocalDate? = null,
                          windowThrough: java.time.LocalDate? = null): List<AnalyticsCard> {
    val currencies = accounts.associate { it.id to it.currency }
    val settled = history.filterIsInstance<Transaction>().filter { !it.isDeleted && it.dateTime != null && it.dateTime!! <= now &&
        (isWalletCashFlowTransaction(it) || accountId != null && includeTransfers && it.type == TransactionType.TRANSFER) }
    if (settled.isEmpty()) return listOf(AnalyticsCard(id, "Activity trend", "Matching settled activity", ChartKind.LINE,
        emptyList(), accountId?.let { currencies[it] }.orEmpty(), note = "No activity in this period. Add an entry or choose another period."))
    if (accountId != null) {
        val currency = currencies[accountId]
        if (currency.isNullOrBlank()) return listOf(AnalyticsCard(id, "Activity trend", "Matching account activity", ChartKind.LINE,
            emptyList(), "", note = "Account currency is unavailable. Review the account."))
        val entries = settled.flatMap { transaction ->
            val (income, spent) = accountCashFlow(transaction, accountId, includeTransfers)
            listOf(true to income, false to spent).map { (incoming, amount) ->
                AnalyticsEntry(transaction.id, transaction.dateTime!!.atZone(zone).toLocalDate(), incoming,
                    amount, transaction.categoryId, "", "") to currency
            }
        }
        return ContextCharts.activity(id, entries, windowFrom, windowThrough).map { it.copy(explanation =
            "Native $currency account activity. ${if (includeTransfers) "Transfers included using each account's stored amount." else "Transfers excluded."} Balance adjustments excluded. This is cash flow, not balance.") }
    }
    if (settled.any { it.accountId !in currencies || currencies[it.accountId].isNullOrBlank() }) return listOf(
        AnalyticsCard(id, "Activity trend", "Matching settled activity", ChartKind.LINE, emptyList(), "",
            note = "An account or its native currency is unavailable. Review it before drawing this chart."))
    return ContextCharts.activity(id, settled.map { transaction ->
        AnalyticsEntry(transaction.id, transaction.dateTime!!.atZone(zone).toLocalDate(), transaction.type == TransactionType.INCOME,
            transaction.amount, transaction.categoryId, "", "") to currencies.getValue(transaction.accountId)!!
    })
}

@Composable
fun ContextualActivityChart(id: String, history: List<TransactionHistoryItem>, accounts: List<Account>, baseCurrency: String,
                            modifier: Modifier = Modifier, accountId: UUID? = null, includeTransfers: Boolean = false,
                            windowFrom: java.time.LocalDate? = null, windowThrough: java.time.LocalDate? = null) {
    val zone = ZoneId.systemDefault()
    // Keep the complete, explicitly dated card while recomputing; cancelled requests cannot publish.
    val charts by produceState<List<AnalyticsCard>>(
        listOf(AnalyticsCard(id, "Activity trend", "Matching settled activity", ChartKind.LINE, emptyList(), "",
            note = "Loading activity…")), id, history, accounts, baseCurrency, zone, accountId, includeTransfers, windowFrom, windowThrough) {
        value = withContext(Dispatchers.Default) {
            activityChartSnapshot(id, history, accounts.map { if (it.currency == null) it.copy(currency = baseCurrency) else it }, zone,
                accountId = accountId, includeTransfers = includeTransfers, windowFrom = windowFrom, windowThrough = windowThrough)
        }
    }
    JvyContextCharts(charts, modifier = modifier)
}
