package com.ivy.loans.loan

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.ivy.base.analytics.*
import com.ivy.base.model.LoanRecordType
import com.ivy.data.model.LoanType
import com.ivy.domain.usecase.analytics.*
import com.ivy.design.system.chart.JvyContextCharts
import com.ivy.design.system.chart.JvyAnalyticsCard
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.loans.loandetails.LoanDetailsScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.math.BigDecimal

@Composable
internal fun LoanOverviewChart(loans: List<DisplayLoan>) {
    val charts = remember(loans) { ContextCharts.positions("loan-remaining", "Loans at a glance",
        "Principal still left on the displayed loans. Borrowed money and money lent are labelled separately. Currencies are never added together; interest is not principal.",
        loans.map { item -> MoneyPosition((if (item.loan.type == LoanType.LEND) "Lent · " else "Borrowed · ") + item.loan.name,
            item.currencyCode ?: "Review currency", if (item.currencyCode == null) null else ContextCharts.amount(item.loanTotalAmount - item.amountPaid)) },
        first = "Amount remaining") }
    JvyContextCharts(charts)
}

internal fun loanDetailChartModels(state: LoanDetailsScreenState, today: LocalDate, zone: ZoneId): List<AnalyticsCard> {
    val loan = state.loan ?: return emptyList()
    val currency = state.selectedLoanAccount?.currency ?: state.baseCurrency
    val accountMap = state.accounts.associateBy { it.id }
    val events = state.displayLoanRecords.filterNot { it.loanRecord.isDeleted }.map { display ->
        val record = display.loanRecord
        val native = if (record.accountId == null) currency else accountMap[record.accountId]?.currency
        PrincipalEvent(record.dateTime.atZone(zone).toLocalDate(), ContextCharts.amount(record.convertedAmount ?: record.amount)
            .takeIf { record.convertedAmount != null || native == currency }, record.loanRecordType == LoanRecordType.INCREASE, record.interest)
    }
    val trend = ContextCharts.principal("loan-${loan.id}", "Principal remaining over time", currency,
        ContextCharts.amount(loan.amount), loan.dateTime?.toLocalDate(), today, events)
    val repayments = events.filter { !it.increase && it.date <= today }
    if (repayments.isEmpty()) return listOf(trend)
    val unknown = repayments.any { it.amount == null }
    fun paid(interest: Boolean) = if (unknown) null else repayments.filter { it.interest == interest }.fold(BigDecimal.ZERO) { a, b -> a + b.amount!! }
    return listOf(trend, AnalyticsCard("loan-payments-${loan.id}", "Where your repayments went",
        "Recorded principal and interest payments through $today, in $currency. Not an APR estimate or a forecast.", ChartKind.BARS,
        listOf(ChartSeries("Paid", Ink.SPENDING, listOf(ChartValue("Principal", paid(false)), ChartValue("Interest", paid(true))))), currency,
        note = if (unknown) "A repayment needs a currency conversion review." else null))
}

@Composable
internal fun LoanDetailsCharts(state: LoanDetailsScreenState) {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    key(state.loan, state.displayLoanRecords, state.accounts, state.baseCurrency, today, zone) {
        val cards by produceState<List<AnalyticsCard>>(emptyList()) {
            value = withContext(Dispatchers.Default) { loanDetailChartModels(state, today, zone) }
        }
        cards.forEach { JvyAnalyticsCard(it) }
    }
}
