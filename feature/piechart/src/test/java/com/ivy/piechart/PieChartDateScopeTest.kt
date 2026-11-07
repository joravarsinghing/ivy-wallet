package com.ivy.piechart

import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.base.threading.DispatchersProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.TransactionDateRange
import com.ivy.piechart.action.PieChartAct
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PieChartDateScopeTest {
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = object : DispatchersProvider {
        override val main = dispatcher
        override val io = dispatcher
        override val default = dispatcher
    }
    private val settings = mockk<SettingsDao>()
    private val context = mockk<IvyWalletCtx>(relaxed = true)
    private val chart = mockk<PieChartAct>()
    private val preferences = mockk<SharedPrefs>(relaxed = true)
    private val range = TransactionDateRange(
        Instant.parse("2026-10-05T00:00:00Z"), Instant.parse("2026-11-03T23:59:59.999999999Z"))
    private val explicitPeriod = TimePeriod(fromToRange = FromToTimeRange(range.from, range.to))

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { context.selectedPeriod } returns explicitPeriod
        every { context.startDayOfMonth } returns 15
        coEvery { settings.findFirst() } returns SettingsEntity(Theme.DARK, "AED", 0.0, "Fictional")
        coEvery { chart(any()) } returns PieChartAct.Output(0.0, persistentListOf())
    }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun vm() = PieChartStatisticViewModel(settings, context, chart, preferences,
        mockk(), TestTimeConverter(), mockk(), dispatchers)

    @Test fun `Home drill down retains exact dates and refuses a competing period change`() = runTest(dispatcher) {
        val vm = vm()
        vm.onEvent(PieChartStatisticEvent.OnStart(PieChartStatisticScreen(
            TransactionType.EXPENSE, filterExcluded = false, dateRange = range)))
        advanceUntilIdle()
        vm.onEvent(PieChartStatisticEvent.OnSetPeriod(TimePeriod()))
        vm.onEvent(PieChartStatisticEvent.OnSelectNextMonth)
        vm.onEvent(PieChartStatisticEvent.OnSelectPreviousMonth)
        vm.onEvent(PieChartStatisticEvent.OnShowMonthModal(TimePeriod()))
        advanceUntilIdle()
        coVerify(exactly = 1) { chart(match {
            it.range == explicitPeriod.fromToRange && !it.filterExcluded && !it.useExplicitTransactions
        }) }
        verify(exactly = 0) { context.updateSelectedPeriodInMemory(any()) }
    }

    @Test fun `ordinary chart still supports changing its own period`() = runTest(dispatcher) {
        val vm = vm()
        vm.onEvent(PieChartStatisticEvent.OnStart(PieChartStatisticScreen(TransactionType.EXPENSE)))
        advanceUntilIdle()
        val changed = TimePeriod(fromToRange = FromToTimeRange(
            Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T23:59:59Z")))
        vm.onEvent(PieChartStatisticEvent.OnSetPeriod(changed))
        advanceUntilIdle()
        verify(exactly = 1) { context.updateSelectedPeriodInMemory(changed) }
        coVerify(exactly = 1) { chart(match { it.range == changed.fromToRange && it.filterExcluded }) }
        coVerify(exactly = 2) { chart(any()) }
    }

    @Test fun `Home drill down passes its cash-flow eligibility with the exact dates`() = runTest(dispatcher) {
        vm().onEvent(PieChartStatisticEvent.OnStart(PieChartStatisticScreen(TransactionType.INCOME,
            dateRange = range, cashFlowOnly = true)))
        advanceUntilIdle()
        coVerify(exactly = 1) { chart(match { it.cashFlowOnly && it.filterExcluded && it.range == explicitPeriod.fromToRange }) }
    }
}
