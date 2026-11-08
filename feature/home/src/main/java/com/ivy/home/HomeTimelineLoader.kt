package com.ivy.home

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.Category
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.action.transaction.HistoryWithDateDivsAct
import com.ivy.wallet.domain.action.transaction.LegacyCalcTrnsIncomeExpenseAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

/** The existing chart cash-flow rule, now shared with Home's summary. Ledger math is untouched. */
internal fun isHomeCashFlow(transaction: Transaction): Boolean =
    com.ivy.wallet.domain.pure.transaction.isWalletCashFlowTransaction(transaction)

data class HomeTimelineRequest(
    val selection: HomePeriodSelection,
    val window: HomeDateWindow?,
    val currency: String,
    val accounts: List<Account>,
    val categories: List<Category>,
    val firstActivity: LocalDate?,
)

data class HomeTimelineResult(val points: List<HomeTrendPoint>, val activity: HomeRangeActivity) {
    val totals: IncomeExpensePair get() = IncomeExpensePair(
        points.sumOf { it.income }, points.sumOf { it.expenses })
}

/** Builds a complete result off the UI thread; no intermediate card is published. */
class HomeTimelineLoader @Inject constructor(
    private val history: HistoryWithDateDivsAct,
    private val cashFlow: LegacyCalcTrnsIncomeExpenseAct,
    private val balance: CalcWalletBalanceAct,
    private val activity: HomeRangeActivityLoader,
    private val converter: TimeConverter,
) {
    suspend fun load(request: HomeTimelineRequest, isCurrent: () -> Boolean = { true }): HomeTimelineResult {
        val window = request.window ?: return HomeTimelineResult(emptyList(),
            HomeRangeActivity(emptyList<com.ivy.base.legacy.TransactionHistoryItem>().toImmutableList(), HomeSpendingBreakdownState()))
        val buckets = homeTrendSamplingWindows(window)
        val allowedAccounts = request.accounts.filter { it.includeInBalance }.map { it.id }.toSet()
        val points = buckets.map { bucket ->
            if (!isCurrent()) throw kotlinx.coroutines.CancellationException("Superseded Home period")
            val range = bucket.toUtcRange(converter)
            val transactions = history(HistoryWithDateDivsAct.Input(range, request.currency))
                .filterIsInstance<Transaction>().filter { it.accountId in allowedAccounts && isHomeCashFlow(it) }
            val totals = cashFlow(LegacyCalcTrnsIncomeExpenseAct.Input(transactions, request.currency, request.accounts))
            val closing = balance(CalcWalletBalanceAct.Input(baseCurrency = request.currency, range = ClosedTimeRange.to(range.to)))
            HomeTrendPoint(bucket.displayLabel(), totals.income, totals.expense, closing,
                isObserved = transactions.isNotEmpty(), periodIdentity = if (bucket.from == bucket.through)
                    HomeTrendPeriodIdentity.Day(bucket.from) else HomeTrendPeriodIdentity.Interval(bucket.from, bucket.through))
        }
        if (!isCurrent()) throw kotlinx.coroutines.CancellationException("Superseded Home period")
        return HomeTimelineResult(coverHomeTrendSamples(points, request.firstActivity),
            activity.load(window, converter, request.currency, request.accounts, request.categories, filterExcluded = true))
    }
}

/** Request identity is independent of the window: edits/refreshes must also invalidate old work. */
internal class HomeTimelineGeneration {
    @Volatile private var value = 0L
    fun next(): Long = ++value
    fun owns(generation: Long): Boolean = generation == value
}
