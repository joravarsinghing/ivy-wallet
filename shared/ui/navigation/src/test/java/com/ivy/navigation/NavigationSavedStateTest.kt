package com.ivy.navigation

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

class NavigationSavedStateTest {
    private val account = UUID.randomUUID()
    private val category = UUID.randomUUID()
    @Test fun `Home cash-flow eligibility survives restoration without changing ordinary charts`() {
        val range = TransactionDateRange(java.time.Instant.parse("2026-10-01T00:00:00Z"),
            java.time.Instant.parse("2026-10-31T23:59:59Z"))
        val screen = PieChartStatisticScreen(TransactionType.INCOME, dateRange = range, cashFlowOnly = true)
        assertEquals(screen, roundTrip(screen))
        assertFalse((roundTrip(PieChartStatisticScreen(TransactionType.INCOME)) as PieChartStatisticScreen).cashFlowOnly)
        val old = listOf("jvy-navigation-1", "1", UUID.randomUUID().toString(), "8", "spie", "sEXPENSE", "strue", "s", "sfalse", "n",
            "s2026-10-01T00:00:00Z", "s2026-10-31T23:59:59Z")
        val nav = Navigation()
        assertTrue(nav.restoreState(old))
        assertFalse((nav.currentScreen as PieChartStatisticScreen).cashFlowOnly)
    }
    @Test fun `bounded chart dates round trip without serializing selected transactions`() {
        val range = TransactionDateRange(java.time.Instant.parse("2026-10-05T00:00:00Z"),
            java.time.Instant.parse("2026-11-03T23:59:59.999999999Z"))
        assertEquals(range, (roundTrip(PieChartStatisticScreen(TransactionType.EXPENSE, dateRange = range)) as PieChartStatisticScreen).dateRange)
    }
    @Test fun `legacy six field pie routes still restore and partial dates cannot broaden a query`() {
        val id = UUID.randomUUID().toString()
        val legacy = listOf("jvy-navigation-1", "1", id, "6", "spie", "sEXPENSE", "strue", "s", "sfalse", "n")
        assertTrue(Navigation().restoreState(legacy))
        val partial = legacy.toMutableList().apply { this[3] = "8"; add("s2026-10-05T00:00:00Z"); add("n") }
        assertFalse(Navigation().restoreState(partial))
    }
    private fun roundTrip(screen: Screen): Screen {
        val nav = Navigation().apply { navigateTo(screen) }
        val restored = Navigation()
        assertTrue(restored.restoreState(requireNotNull(nav.saveState())))
        assertEquals(nav.currentEntryId, restored.currentEntryId)
        return requireNotNull(restored.currentScreen)
    }

    @Test fun `all simple destinations retain identity and type`() {
        listOf(MainScreen, OnboardingScreen, BalanceScreen, PlannedPaymentsScreen, CategoriesScreen, SettingsScreen,
            ReportScreen, BudgetScreen, GoalsScreen, SecurityScreen, NetWorthScreen, LoansScreen, SearchScreen,
            ExchangeRatesScreen, FeaturesScreen, ReleasesScreen,
            DisclaimerScreen, PollScreen, CSVScreen(true), CSVScreen(false), ImportScreen(true), ImportScreen(false),
            LoanDetailsScreen(UUID.randomUUID())).forEach { assertEquals(it, roundTrip(it)) }
    }
    @Test fun `transaction and planned editor route fields preserve nulls and free text`() {
        listOf(EditTransactionScreen(null, TransactionType.EXPENSE, account, category),
            EditTransactionScreen(UUID.randomUUID(), TransactionType.TRANSFER),
            EditPlannedScreen(null, TransactionType.INCOME, 1234.75, account, category, "n|s,\nFictional", ""),
            EditPlannedScreen(UUID.randomUUID(), TransactionType.EXPENSE)).forEach { assertEquals(it, roundTrip(it)) }
    }
    @Test fun `history saves only selected record IDs not amounts or private notes`() {
        val transaction = Transaction(account, TransactionType.EXPENSE, BigDecimal("987654.32"), title = "Private fictional title")
        val screen = TransactionsScreen(categoryId = category, transactions = listOf(transaction))
        val nav = Navigation().apply { navigateTo(screen) }
        val payload = requireNotNull(nav.saveState())
        assertFalse(payload.any { it.contains("987654.32") || it.contains("Private fictional title") })
        val restored = roundTrip(screen) as TransactionsScreen
        assertTrue(restored.transactions.isEmpty())
        assertEquals(listOf(transaction.id), restored.restoredTransactionIds)
    }
    @Test fun `an explicitly empty restored selection never becomes an unspecified selection`() {
        val history = roundTrip(TransactionsScreen(categoryId = category, restoredTransactionIds = emptyList())) as TransactionsScreen
        val pie = roundTrip(PieChartStatisticScreen(TransactionType.EXPENSE, restoredTransactionIds = emptyList())) as PieChartStatisticScreen
        assertNotNull(history.restoredTransactionIds)
        assertTrue(history.restoredTransactionIds!!.isEmpty())
        assertNotNull(pie.restoredTransactionIds)
        assertTrue(pie.restoredTransactionIds!!.isEmpty())
    }
    @Test fun `pie filter flags and selection IDs survive without serializing records`() {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID())
        val screen = PieChartStatisticScreen(TransactionType.TRANSFER, false, persistentListOf(account),
            treatTransfersAsIncomeExpense = true, restoredTransactionIds = ids)
        assertEquals(screen, roundTrip(screen))
    }
    @Test fun `a new process restores Back order and distinct visits with equal arguments`() {
        val nav = Navigation().apply { navigateTo(MainScreen); navigateTo(EditTransactionScreen(null, TransactionType.INCOME)) }
        val first = nav.currentEntryId
        nav.navigateTo(EditTransactionScreen(null, TransactionType.INCOME))
        val second = nav.currentEntryId
        val restored = Navigation()
        assertTrue(restored.restoreState(requireNotNull(nav.saveState())))
        assertEquals(second, restored.currentEntryId)
        assertTrue(restored.back())
        assertEquals(first, restored.currentEntryId)
        assertTrue(restored.back())
        assertEquals(MainScreen, restored.currentScreen)
        assertFalse(restored.back())
    }
    @Test fun `live rotated navigation cannot be overwritten by an old saved snapshot`() {
        val nav = Navigation().apply { navigateTo(MainScreen) }
        val saved = requireNotNull(nav.saveState())
        nav.navigateTo(GoalsScreen)
        assertFalse(nav.restoreState(saved))
        assertEquals(GoalsScreen, nav.currentScreen)
    }
    @Test fun `pending handlers are not serialized or replayed`() {
        val nav = Navigation().apply { navigateTo(MainScreen); navigateTo(GoalsScreen) }
        nav.registerModalBackHandler(UUID.randomUUID()) { error("Must not survive") }
        nav.registerScreenBackHandler(GoalsScreen) { error("Must not survive") }
        val restored = Navigation()
        assertTrue(restored.restoreState(requireNotNull(nav.saveState())))
        assertTrue(restored.onBackPressed())
        assertEquals(MainScreen, restored.currentScreen)
    }
    @Test fun `unknown corrupt truncated and duplicate entry state is rejected atomically`() {
        val id = UUID.randomUUID().toString()
        listOf(emptyList(), listOf("future", "1"), listOf("jvy-navigation-1", "1", id, "1", "sunknown"),
            listOf("jvy-navigation-1", "1", "bad-id", "1", "smain"),
            listOf("jvy-navigation-1", "2", id, "1", "smain", id, "1", "sgoals"),
            listOf("jvy-navigation-1", "1", id, "2", "scsv", "snot-a-boolean"),
            listOf("jvy-navigation-1", "1", id, "1")).forEach { payload ->
            val nav = Navigation()
            assertFalse(nav.restoreState(payload))
            assertNull(nav.currentScreen)
            assertTrue(nav.backStackEmpty())
        }
    }
    @Test fun `unbounded payloads and nonfinite prefills are never put into saved state`() {
        val nav = Navigation().apply { navigateTo(EditPlannedScreen(null, TransactionType.EXPENSE, description = "x".repeat(70000))) }
        assertNull(nav.saveState())
        val invalid = Navigation().apply { navigateTo(EditPlannedScreen(null, TransactionType.EXPENSE, amount = Double.NaN)) }
        assertNull(invalid.saveState())
    }
}
