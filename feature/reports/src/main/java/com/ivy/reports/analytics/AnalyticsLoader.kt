package com.ivy.reports.analytics

import arrow.core.toOption
import com.ivy.base.model.LoanRecordType
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.read.GoalAllocationDao
import com.ivy.data.db.dao.read.GoalDao
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.dao.read.LoanRecordDao
import com.ivy.domain.usecase.networth.WealthWorkspaceUseCase
import com.ivy.legacy.IvyWalletCtx
import com.ivy.wallet.domain.pure.transaction.isWalletCashFlowTransaction
import com.ivy.reports.ReportScreenState
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

/** No observers, writes, automatic FX capture, forecasts, or background work outside Explore. */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(private val loader: AnalyticsLoader) : ViewModel() {
    suspend fun load(state: ReportScreenState, section: ExploreSection) = loader.load(state, section)
}

class AnalyticsLoader @Inject constructor(
    private val exchange: ExchangeAct,
    private val wealth: WealthWorkspaceUseCase,
    private val goals: GoalDao,
    private val allocations: GoalAllocationDao,
    private val loans: LoanDao,
    private val records: LoanRecordDao,
    private val context: IvyWalletCtx,
    private val time: TimeProvider,
    private val converter: TimeConverter,
) {
    suspend fun load(state: ReportScreenState, section: ExploreSection): AnalyticsBoard = withContext(Dispatchers.Default) {
        when (section) {
            ExploreSection.SPENDING, ExploreSection.CASH_FLOW -> flows(state, section)
            ExploreSection.WEALTH -> wealth(state)
            ExploreSection.GOALS -> goals(state)
            ExploreSection.DEBT -> debt(state)
        }
    }

    private suspend fun flows(state: ReportScreenState, section: ExploreSection): AnalyticsBoard {
        val zone = time.getZoneId()
        val now = time.utcNow()
        val range = state.filter?.period?.toRange(context.startDayOfMonth, converter, time)
        val endInstant = minOf(range?.to ?: now, now)
        val settled = state.transactions.filter { !it.isDeleted && it.dateTime != null && it.dateTime!! <= endInstant && isWalletCashFlowTransaction(it) }
        val from = range?.from?.atZone(zone)?.toLocalDate() ?: settled.minOfOrNull { it.dateTime!!.atZone(zone).toLocalDate() } ?: time.localDateNow()
        val to = endInstant.atZone(zone).toLocalDate()
        val currency = state.baseCurrency
        val accounts = state.accounts.associateBy { it.id }
        val categories = state.categories.associateBy { it.id.value }
        val rates = mutableMapOf<String, BigDecimal?>()
        val entries = mutableListOf<AnalyticsEntry>()
        var missing = 0
        for (trn in settled) {
            val account = accounts[trn.accountId]
            if (account == null) { missing++; continue }
            val source = account.currency ?: currency
            val rate = if (source == currency) BigDecimal.ONE else {
                if (source !in rates) rates[source] = exchange(ExchangeAct.Input(
                    ExchangeData(currency, source.toOption()), BigDecimal.ONE
                )).orNull()?.takeIf { it.signum() > 0 }
                rates[source]
            }
            if (rate == null) { missing++; continue }
            val date = trn.dateTime!!.atZone(zone).toLocalDate()
            if (date < from || date > to) continue
            entries += AnalyticsEntry(trn.id, date, trn.type == TransactionType.INCOME,
                trn.amount.multiply(rate), trn.categoryId, categories[trn.categoryId]?.name?.value ?: "Uncategorized", trn.title.orEmpty())
        }
        val notices = mutableListOf("Applied report filters · actual income and expenses only, excluding transfers and balance adjustments. Available wallet FX quotes, not historical rates. CSV export retains the original transaction report.")
        if (missing > 0) {
            // Do not display plausible but incomplete totals, ratios, or category shares.
            return AnalyticsBoard("$from – $to · $currency", emptyList(), notices + "$missing entries need an available account or conversion rate. Correct them before exploring this report.")
        }
        if (to < from) return AnalyticsBoard("No elapsed dates", emptyList(), listOf("Choose a period that includes today or earlier."))
        val cards = if (section == ExploreSection.SPENDING) AnalyticsEngine.spending(entries, from, to, currency)
                    else AnalyticsEngine.cashFlow(entries, from, to, currency)
        return AnalyticsBoard("$from – $to · $currency", cards, notices, from = range?.from, to = endInstant)
    }

    private suspend fun wealth(state: ReportScreenState): AnalyticsBoard {
        val day = time.localDateNow()
        val workspace = wealth.load(day, state.baseCurrency, months = 12, allowLegacyMigration = false)
        if (workspace.data.items.isEmpty()) return AnalyticsBoard("Wealth workspace · as of $day", emptyList(),
            listOf("Set up and review your assets and debts in Net worth to unlock wealth charts. Opening Explore never migrates or writes financial records."))
        val result = workspace.current
        val currency = result.currency
        val series = listOf(
            ChartSeries("What I own", Ink.INCOME, listOf(ChartValue(day.toString(), result.assets))),
            ChartSeries("What I owe", Ink.SPENDING, listOf(ChartValue(day.toString(), result.liabilities))),
        )
        val cards = mutableListOf(AnalyticsCard("position", "What I own and owe", "Today's reviewed wealth workspace, not this report's transaction filters. Incomplete totals are estimates.", ChartKind.BARS, series, currency,
            note = if (result.complete) null else "Incomplete picture: review inclusion, values and rates in Net worth."))
        cards += AnalyticsCard("wealth-history", "Net-worth history", "Twelve month-end observations, with today as the last point. Incomplete dates remain gaps; they are not zero.", ChartKind.LINE,
            listOf(ChartSeries("Net worth", Ink.BALANCE, workspace.history.map { point ->
                ChartValue(point.date.toString(), point.result.netWorth.takeIf { point.result.complete }, date = point.date)
            })), currency)
        val assets = result.lines.filter { it.item.parentId == null && it.included && it.reportingAmount?.signum() == 1 }.groupBy { it.item.kind }
            .map { (kind, rows) -> ChartValue(kind.name.lowercase().replace('_', ' '), rows.fold(BigDecimal.ZERO) { sum, line -> sum + line.reportingAmount!! }) }.sortedByDescending { it.value }
        if (result.complete && assets.isNotEmpty()) {
            val slices = if (assets.size <= 5) assets else assets.take(4) + ChartValue("Other", assets.drop(4).fold(BigDecimal.ZERO) { a, b -> a + b.value!! })
            cards += AnalyticsCard("allocation", "Asset mix", "Share of included positive asset positions, valued in $currency. This is allocation, not liquidity or investment advice.", ChartKind.DONUT,
                listOf(ChartSeries("Assets", Ink.INCOME, slices)), currency)
        }
        if (workspace.changes.isNotEmpty()) cards += AnalyticsCard("wealth-changes", "What changed since last month", "Dated changes from the wealth calculator, available only when both observations are complete. These are value changes, not claimed causes.", ChartKind.WATERFALL,
            listOf(ChartSeries("Change", Ink.BALANCE, workspace.changes.map { ChartValue(it.label, it.amount) })), currency, advanced = true)
        val warnings = result.lines.filter { it.included }.mapNotNull { it.warning }.distinct()
        return AnalyticsBoard("Wealth workspace · as of $day · $currency", cards,
            listOf("Transaction report filters do not redefine your wealth. Manage linked accounts, reviews and valuations in Net worth.") + warnings)
    }

    private suspend fun goals(state: ReportScreenState): AnalyticsBoard {
        val goalRows = goals.findActive()
        val allocationRows = allocations.findAll().groupBy { it.goalId }
        val accountMap = state.accounts.associateBy { it.id }
        val currency = state.baseCurrency
        val cards = goalRows.map { goal ->
            val rows = allocationRows[goal.id].orEmpty()
            val unsupported = rows.any { accountMap[it.accountId] == null || (accountMap[it.accountId]?.currency ?: currency) != currency || !it.reservedAmount.isFinite() || it.reservedAmount < 0 }
            val target = goal.targetAmount.takeIf { it.isFinite() && it > 0 }?.let(BigDecimal::valueOf)
            val reserved = if (unsupported) null else rows.fold(BigDecimal.ZERO) { sum, row -> sum + BigDecimal.valueOf(row.reservedAmount) }
            AnalyticsCard("goal-${goal.id}", goal.name, "Current reservations, not transfers. Deadline: ${goal.deadline?.atZone(time.getZoneId())?.toLocalDate() ?: "None"}.", ChartKind.BARS,
                listOf(ChartSeries("Reserved", Ink.INCOME, listOf(ChartValue("Current", reserved))), ChartSeries("Target", Ink.BALANCE, listOf(ChartValue("Current", target)))), currency,
                note = if (unsupported || target == null) "Review the target, unavailable accounts or foreign-currency reservations in Goals." else null)
        }
        return AnalyticsBoard("Active goals · today · $currency", cards, listOf("Goal allocations store the current reservation only, not a contribution history. No historical timeline is inferred from edit dates. Report filters do not change reservations."))
    }

    private suspend fun debt(state: ReportScreenState): AnalyticsBoard {
        val today = time.localDateNow()
        val zone = time.getZoneId()
        val rows = records.findAll().groupBy { it.loanId }
        val accounts = state.accounts.associateBy { it.id }
        val cards = mutableListOf<AnalyticsCard>()
        for (loan in loans.findAll()) {
            val currency = accounts[loan.accountId]?.currency ?: state.baseCurrency
            val events = rows[loan.id].orEmpty().filter { !it.isDeleted && it.dateTime.atZone(zone).toLocalDate() <= today }.sortedBy { it.dateTime }
            val start = loan.dateTime?.toLocalDate()
            val initial = loan.amount.takeIf { it.isFinite() }?.let(BigDecimal::valueOf)
            fun amount(record: com.ivy.data.db.entity.LoanRecordEntity): BigDecimal? {
                val source = if (record.accountId == null) currency else accounts[record.accountId]?.currency ?: state.baseCurrency
                if (record.accountId != null && record.accountId !in accounts) return null
                return (record.convertedAmount ?: record.amount.takeIf { source == currency })?.takeIf { it.isFinite() }?.let(BigDecimal::valueOf)
            }
            val review = initial == null || (loan.accountId != null && loan.accountId !in accounts) || events.any { amount(it) == null }
            val direction = if (loan.type == com.ivy.data.model.LoanType.LEND) "Money lent" else "Money borrowed"
            val samples = if (start != null && start <= today && !review) AnalyticsEngine.buckets(start, today).map { (from, end) ->
                val balance = events.filter { !it.interest && it.dateTime.atZone(zone).toLocalDate() <= end }.fold(initial!!) { sum, event ->
                    if (event.loanRecordType == LoanRecordType.INCREASE) sum + amount(event)!! else sum - amount(event)!!
                }
                ChartValue(end.toString(), balance, date = from, end = end)
            } else emptyList()
            cards += AnalyticsCard("loan-${loan.id}", loan.name, "$direction · native loan currency. Principal increases add to the amount; principal repayments reduce it. Interest does not reduce principal.", ChartKind.LINE,
                listOf(ChartSeries("Amount remaining", Ink.BALANCE, samples)), currency,
                note = if (review) "Review the loan account, amount or repayment conversions in Loans." else if (start == null) "Add a loan date in Loans before showing historical balances." else null)
            val repayments = events.filter { it.loanRecordType == LoanRecordType.DECREASE }
            if (!review && repayments.isNotEmpty()) cards += AnalyticsCard("repay-${loan.id}", "${loan.name}: repayment split", "Recorded repayments only; not an amortization estimate or APR calculation.", ChartKind.BARS,
                listOf(ChartSeries("Paid", Ink.SPENDING, listOf(
                    ChartValue("Principal", repayments.filterNot { it.interest }.fold(BigDecimal.ZERO) { a, b -> a + amount(b)!! }),
                    ChartValue("Interest", repayments.filter { it.interest }.fold(BigDecimal.ZERO) { a, b -> a + amount(b)!! })
                ))), currency, advanced = true)
        }
        return AnalyticsBoard("Loans · through $today · individual loan currencies", cards, listOf("All recorded loans, including money lent, independently of transaction report filters. No currencies are added together. Edit loans and repayments in Loans."))
    }
}
