package com.ivy.reports.analytics

import arrow.core.None
import arrow.core.Some
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.LoanRecordType
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.db.dao.read.*
import com.ivy.data.db.entity.*
import com.ivy.domain.usecase.networth.WealthWorkspaceUseCase
import com.ivy.domain.usecase.networth.WealthWorkspace
import com.ivy.domain.usecase.networth.WealthResult
import com.ivy.domain.usecase.networth.WealthLine
import com.ivy.data.model.wealth.*
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import com.ivy.reports.ReportFilter
import com.ivy.reports.ReportScreenState
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.*

class AnalyticsLoaderTest {
    private val exchange = mockk<ExchangeAct>()
    private val wealth = mockk<WealthWorkspaceUseCase>()
    private val goals = mockk<GoalDao>()
    private val allocations = mockk<GoalAllocationDao>()
    private val loans = mockk<LoanDao>()
    private val records = mockk<LoanRecordDao>()
    private val context = mockk<IvyWalletCtx> { every { startDayOfMonth } returns 1 }
    private val now = Instant.parse("2026-11-07T12:00:00Z")
    private val time = mockk<TimeProvider> {
        every { getZoneId() } returns ZoneId.of("America/New_York")
        every { utcNow() } returns now
        every { localDateNow() } returns LocalDate.parse("2026-11-07")
    }
    private val account = Account("Everyday", 0, "AED")
    private val filter = ReportFilter.emptyFilter("AED").copy(period = TimePeriod(fromToRange = FromToTimeRange(null, null)))
    private fun state(vararg rows: Transaction) = ReportScreenState(baseCurrency = "AED", accounts = persistentListOf(account),
        filter = filter, transactions = persistentListOf(*rows))
    private fun trn(amount: String = "10", type: TransactionType = TransactionType.EXPENSE, title: String? = null) =
        Transaction(account.id, type, BigDecimal(amount), dateTime = now.minusSeconds(86400), title = title)
    private fun loader() = AnalyticsLoader(exchange, wealth, goals, allocations, loans, records, context, time, TestTimeConverter())

    @Test fun `settled cash flows exclude transfers adjustments deleted and future entries`() = runTest {
        val board = loader().load(state(trn("100", TransactionType.INCOME), trn("30"), trn("-5"), trn("1000", TransactionType.TRANSFER),
            trn("2000", TransactionType.INCOME, " Adjust balance "), trn("500").copy(dateTime = null, dueDate = now),
            trn("700").copy(dateTime = now.plusSeconds(20)), trn("900").copy(isDeleted = true)), ExploreSection.CASH_FLOW)
        val steps = board.cards.single { it.id == "waterfall" }.series.single().values
        assertEquals(listOf(BigDecimal("100"), BigDecimal("-25"), BigDecimal("75")), steps.map { it.value })
        coVerify(exactly = 0) { exchange(any()) }
    }
    @Test fun `missing conversion blocks incomplete ratios rather than treating missing money as zero`() = runTest {
        coEvery { exchange(any()) } returns None
        val input = state(trn()).copy(accounts = persistentListOf(account.copy(currency = "EUR")))
        val board = loader().load(input, ExploreSection.SPENDING)
        assertTrue(board.cards.isEmpty())
        assertTrue(board.notices.last().contains("conversion rate"))
    }
    @Test fun `foreign currency conversion is cached per currency and uses exact decimals`() = runTest {
        coEvery { exchange(any()) } returns Some(BigDecimal("1.25"))
        val input = state(trn("0.10"), trn("0.20")).copy(accounts = persistentListOf(account.copy(currency = "EUR")))
        val board = loader().load(input, ExploreSection.SPENDING)
        assertEquals(0, BigDecimal("0.375").compareTo(board.cards.first().series.single().values.single().value))
        coVerify(exactly = 1) { exchange(any()) }
    }
    @Test fun `all time starts with real local history rather than epoch sentinel`() = runTest {
        val board = loader().load(state(trn().copy(dateTime = Instant.parse("2026-11-01T02:00:00Z"))), ExploreSection.CASH_FLOW)
        assertTrue(board.caption.startsWith("2026-10-31"))
    }
    @Test fun `future-only report offers correction without fictitious history`() = runTest {
        val input = state().copy(filter = filter.copy(period = TimePeriod(fromToRange = FromToTimeRange(now.plusSeconds(86400), null))))
        val board = loader().load(input, ExploreSection.SPENDING)
        assertTrue(board.cards.isEmpty())
        assertTrue(board.notices.single().contains("earlier"))
    }
    @Test fun `goal charts never convert or discard unsupported reservations`() = runTest {
        val goal = GoalEntity("Emergency", GoalKind.EMERGENCY_FUND, 1000.0)
        coEvery { goals.findActive() } returns listOf(goal)
        coEvery { allocations.findAll() } returns listOf(GoalAllocationEntity(goal.id, account.id, 100.0))
        val board = loader().load(state().copy(accounts = persistentListOf(account.copy(currency = "EUR"))), ExploreSection.GOALS)
        assertNull(board.cards.single().series.first().values.single().value)
        assertNotNull(board.cards.single().note)
        coVerify(exactly = 0) { exchange(any()) }
    }
    @Test fun `interest does not reduce principal while increases and repayments do`() = runTest {
        val loan = LoanEntity("Loan", 1000.0, com.ivy.data.model.LoanType.BORROW, accountId = account.id, dateTime = LocalDateTime.parse("2026-10-01T12:00:00"))
        coEvery { loans.findAll() } returns listOf(loan)
        coEvery { records.findAll() } returns listOf(
            LoanRecordEntity(loan.id, 100.0, dateTime = now.minusSeconds(300), interest = true),
            LoanRecordEntity(loan.id, 250.0, dateTime = now.minusSeconds(200)),
            LoanRecordEntity(loan.id, 50.0, dateTime = now.minusSeconds(100), loanRecordType = LoanRecordType.INCREASE),
        )
        val board = loader().load(state(), ExploreSection.DEBT)
        assertEquals(0, BigDecimal("800").compareTo(board.cards.first().series.single().values.last().value))
        val split = board.cards.last().series.single().values
        assertEquals(0, BigDecimal("250").compareTo(split[0].value))
        assertEquals(0, BigDecimal("100").compareTo(split[1].value))
    }
    @Test fun `missing loan start does not fabricate a historical line`() = runTest {
        coEvery { loans.findAll() } returns listOf(LoanEntity("Legacy loan", 1000.0, com.ivy.data.model.LoanType.BORROW))
        coEvery { records.findAll() } returns emptyList()
        val card = loader().load(state(), ExploreSection.DEBT).cards.single()
        assertTrue(card.series.single().values.isEmpty())
        assertTrue(card.note!!.contains("date"))
    }
    @Test fun `wealth reader is read only and allocation does not double count holdings`() = runTest {
        val day = time.localDateNow()
        val parent = WealthItem("parent", "Portfolio", WealthKind.INVESTMENT, "AED", createdDate = "2026-01-01")
        val child = parent.copy(id = "child", parentId = parent.id)
        val result = WealthResult(day, "AED", listOf(
            WealthLine(parent, BigDecimal(100), BigDecimal(100), null, null, warning = "Valuation needs a freshness review"),
            WealthLine(child, BigDecimal(100), BigDecimal(100), null, null)
        ), BigDecimal(100), BigDecimal.ZERO, emptyList())
        val data = WealthBackup(items = listOf(parent, child))
        coEvery { wealth.load(day, "AED", 12, allowLegacyMigration = false) } returns WealthWorkspace(data, result, emptyList(), emptyList(), emptyList(), null)
        val board = loader().load(state(), ExploreSection.WEALTH)
        val allocation = board.cards.single { it.id == "allocation" }
        assertEquals(BigDecimal(100), allocation.series.single().values.single().value)
        assertTrue(board.notices.contains("Valuation needs a freshness review"))
        coVerify(exactly = 1) { wealth.load(day, "AED", 12, allowLegacyMigration = false) }
        coVerify(exactly = 0) { wealth.captureWalletRates() }
    }
}
