package com.ivy.home

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.wallet.domain.action.category.LegacyCategoryIncomeWithAccountFiltersAct
import com.ivy.wallet.domain.action.transaction.HistoryWithDateDivsAct
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import io.mockk.*
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class HomeRangeActivityLoaderTest {
    private val history = mockk<HistoryWithDateDivsAct>()
    private val categories = mockk<LegacyCategoryIncomeWithAccountFiltersAct>()
    private val loader = HomeRangeActivityLoader(history, categories)
    private val account = com.ivy.legacy.datamodel.Account(name = "Fictional", color = 0, currency = "AED")
    private val entry = Transaction(account.id, TransactionType.EXPENSE, BigDecimal("42.25"),
        dateTime = Instant.parse("2026-10-27T09:00:00Z"))
    private val window = HomeDateWindow.ending(HomeTrendRange.THIRTY_DAYS, LocalDate.parse("2026-11-03"))

    @Test fun `activity and spending consume the same October inclusive query for November`() = runTest {
        coEvery { history(any()) } returns listOf(entry).toImmutableList()
        coEvery { categories(any()) } answers {
            val input = firstArg<LegacyCategoryIncomeWithAccountFiltersAct.Input>()
            assertEquals(listOf(entry), input.transactions)
            assertEquals(listOf(account), input.accountFilterList)
            IncomeExpenseTransferPair.zero().copy(expense = BigDecimal("42.25"))
        }
        val result = loader.load(window, TestTimeConverter(), "AED", listOf(account), emptyList())
        assertEquals(listOf(entry.id), result.history.filterIsInstance<Transaction>().map { it.id })
        assertEquals(BigDecimal("42.25"), result.spending.total)
        coVerify(exactly = 1) { history(match { it.range == window.toUtcRange(TestTimeConverter()) && it.baseCurrency == "AED" }) }
    }

    @Test fun `empty selected range stays empty rather than fetching a calendar month or all time`() = runTest {
        coEvery { history(any()) } returns emptyList<com.ivy.base.legacy.TransactionHistoryItem>().toImmutableList()
        coEvery { categories(any()) } returns IncomeExpenseTransferPair.zero()
        val result = loader.load(window, TestTimeConverter(), "AED", listOf(account), emptyList())
        assertTrue(result.history.isEmpty())
        assertFalse(result.spending.hasSpending)
        coVerify(exactly = 1) { history(any()) }
    }
}
