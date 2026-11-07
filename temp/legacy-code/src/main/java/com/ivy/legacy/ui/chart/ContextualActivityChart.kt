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
import com.ivy.design.system.chart.JvyContextCharts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

fun activityChartSnapshot(id: String, history: List<TransactionHistoryItem>, accounts: List<Account>, zone: ZoneId,
                          now: java.time.Instant = java.time.Instant.now()): List<AnalyticsCard> {
    val currencies = accounts.associate { it.id to it.currency }
    val settled = history.filterIsInstance<Transaction>().filter { !it.isDeleted && it.dateTime != null && it.dateTime!! <= now && isWalletCashFlowTransaction(it) }
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
                            modifier: Modifier = Modifier) {
    if (history.isEmpty()) return
    val zone = ZoneId.systemDefault()
    key(id, history, accounts, baseCurrency, zone) {
        val charts by produceState<List<AnalyticsCard>?>(null) {
            value = withContext(Dispatchers.Default) {
                activityChartSnapshot(id, history, accounts.map { if (it.currency == null) it.copy(currency = baseCurrency) else it }, zone)
            }
        }
        val result = charts
        if (result == null) Box(modifier.fillMaxWidth().height(176.dp).padding(24.dp)) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else JvyContextCharts(result, modifier = modifier)
    }
}
