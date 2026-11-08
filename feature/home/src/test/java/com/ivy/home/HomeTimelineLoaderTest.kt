package com.ivy.home

import arrow.core.Some
import com.ivy.base.legacy.Transaction
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.base.model.TransactionType
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.action.category.LegacyCategoryIncomeWithAccountFiltersAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.action.transaction.HistoryWithDateDivsAct
import com.ivy.wallet.domain.action.transaction.LegacyCalcTrnsIncomeExpenseAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import io.mockk.*
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class HomeTimelineLoaderTest {
    private val converter = TestTimeConverter()
    private val history = mockk<HistoryWithDateDivsAct>()
    private val exchange = mockk<ExchangeAct>()
    private val closing = mockk<CalcWalletBalanceAct>()
    private val reducer = LegacyCalcTrnsIncomeExpenseAct(exchange)
    private val activity = HomeRangeActivityLoader(history, LegacyCategoryIncomeWithAccountFiltersAct(reducer))
    private val loader = HomeTimelineLoader(history, reducer, closing, activity, converter)
    private val included = Account("Fictional bank", 0, "AED")
    private val foreign = Account("Fictional USD", 0, "USD")
    private val excluded = Account("Excluded", 0, "AED", includeInBalance = false)
    private val window = HomeDateWindow(LocalDate.parse("2026-10-06"), LocalDate.parse("2026-11-04"))
    private fun transaction(type: TransactionType, amount: String, account: Account = included, title: String? = null) =
        Transaction(account.id, type, BigDecimal(amount), dateTime = Instant.parse("2026-10-27T10:00:00Z"), title = title)
    private fun prepare(entries: List<Transaction>) {
        coEvery { history(any()) } answers {
            val range = firstArg<HistoryWithDateDivsAct.Input>().range
            entries.filter { it.dateTime!! >= range.from && it.dateTime!! <= range.to }.toImmutableList()
        }
        coEvery { exchange(any()) } answers {
            val input = firstArg<ExchangeAct.Input>()
            Some(input.amount * if (input.data.fromCurrency.orNull() == "USD") BigDecimal("3.67") else BigDecimal.ONE)
        }
        coEvery { closing(any()) } returns BigDecimal("1000.00")
    }
    private fun request(bounds: HomeDateWindow? = window) = HomeTimelineRequest(HomePeriodSelection.CurrentCycle,
        bounds, "AED", listOf(included, foreign, excluded), emptyList(), LocalDate.parse("2026-01-01"))

    @Test fun `summary and plot share real converted income expenses refunds and exclusions`() = runTest {
        prepare(listOf(transaction(TransactionType.INCOME, "100.25"), transaction(TransactionType.EXPENSE, "20.00"),
            transaction(TransactionType.EXPENSE, "-5.00"), transaction(TransactionType.INCOME, "10.00", foreign),
            transaction(TransactionType.INCOME, "900.00", excluded), transaction(TransactionType.TRANSFER, "500.00"),
            transaction(TransactionType.INCOME, "700.00", title = "Adjust balance")))
        val result = loader.load(request())
        assertEquals(0, BigDecimal("136.95").compareTo(result.totals.income))
        assertEquals(0, BigDecimal("15.00").compareTo(result.totals.expense))
        assertEquals(result.points.sumOf { it.income }, result.totals.income)
        assertEquals(result.points.sumOf { it.expenses }, result.totals.expense)
        assertEquals(0, result.totals.expense.compareTo(result.activity.spending.total))
        assertEquals(7, result.activity.history.filterIsInstance<Transaction>().size) // activity still shows transfers/adjustments
        assertEquals(30, result.points.size)
    }
    @Test fun `exact historical month and arbitrary custom bounds are not replaced by rolling presets`() = runTest {
        prepare(emptyList())
        val custom = HomeDateWindow(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-11"))
        val result = loader.load(request(custom))
        assertEquals(2, result.points.size)
        coVerify { history(match { it.range == custom.toUtcRange(converter) }) }
        assertTrue(result.points.all { it.isObserved && it.income.signum() == 0 }) // genuine quiet coverage
    }
    @Test fun `all time without history never fabricates a date query or balance point`() = runTest {
        val result = loader.load(request(null))
        assertTrue(result.points.isEmpty())
        assertTrue(result.activity.history.isEmpty())
        coVerify(exactly = 0) { history(any()) }
        coVerify(exactly = 0) { closing(any()) }
    }
    @Test fun `long history work stays bounded to thirty buckets plus the shared activity query`() = runTest {
        prepare(emptyList())
        val long = HomeDateWindow(LocalDate.parse("2020-01-01"), LocalDate.parse("2026-11-04"))
        val result = loader.load(request(long))
        assertEquals(30, result.points.size)
        coVerify(exactly = 31) { history(any()) }
        coVerify(exactly = 30) { closing(any()) }
    }
    @Test fun `superseded request stops before issuing another bucket query`() = runTest {
        prepare(emptyList())
        try {
            loader.load(request()) { false }
            fail("Obsolete request must not complete")
        } catch (_: kotlinx.coroutines.CancellationException) { }
        coVerify(exactly = 0) { history(any()) }
    }
    @Test fun `refreshes and currency changes invalidate old publications even for equal date windows`() {
        val guard = HomeTimelineGeneration()
        val first = guard.next()
        val second = guard.next()
        assertFalse(guard.owns(first))
        assertTrue(guard.owns(second))
        guard.next()
        assertFalse(guard.owns(second))
    }
    @Test fun `storage failure cannot produce a partial timeline`() = runTest {
        prepare(emptyList())
        coEvery { closing(any()) } throws IllegalStateException("Fictional failure")
        try { loader.load(request()); fail("Must not return partial data") } catch (_: IllegalStateException) { }
    }
}
