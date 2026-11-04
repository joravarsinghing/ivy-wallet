package com.ivy.reports

import com.ivy.base.legacy.stringRes
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.repository.*
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.transaction.CalcTrnsIncomeExpenseAct
import com.ivy.wallet.domain.action.transaction.TrnsWithDateDivsAct
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ReportRestorationLifecycleTest {
    private val dispatcher = StandardTestDispatcher()
    private val transactions = mockk<TransactionRepository>()
    private val context = mockk<IvyWalletCtx>(relaxed = true)
    private val accounts = mockk<AccountsAct>()
    private val categories = mockk<CategoryRepository>()
    private val history = mockk<TrnsWithDateDivsAct>()
    private val totals = mockk<CalcTrnsIncomeExpenseAct>()
    private val base = mockk<BaseCurrencyAct>()
    private val tags = mockk<TagRepository>()
    private val saved = mockk<SavedAnalyticsRepository>()
    private val first = Account("Fictional everyday", 0, "AED")
    private val second = Account("Fictional savings", 0, "AED")
    private val allTime = TimePeriod(fromToRange = FromToTimeRange(null, null))
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(::stringRes)
        every { stringRes(any(), *anyVararg()) } returns "Unspecified"
        every { context.selectedPeriod } returns allTime
        coEvery { transactions.findAll() } returns emptyList()
        coEvery { accounts(Unit) } returns persistentListOf(first, second)
        coEvery { categories.findAll() } returns emptyList()
        coEvery { history(any()) } returns emptyList()
        coEvery { totals(any()) } returns IncomeExpenseTransferPair.zero()
        coEvery { base(Unit) } returns "AED"
        coEvery { tags.findAll() } returns emptyList()
        coEvery { saved.findAll() } returns emptyList()
    }
    @After fun teardown() { unmockkStatic(::stringRes); Dispatchers.resetMain() }
    private fun vm() = ReportViewModel(mockk(), transactions, context, mockk(), accounts, categories, history, totals, base,
        mockk(), tags, mockk(), mockk(), TestTimeConverter(), mockk(), saved, Json)

    @Test fun `restored report retains applied query overlay and all available account choices`() = runTest(dispatcher) {
        val entry = UUID.randomUUID()
        val firstVm = vm(); firstVm.start(entry, null); firstVm.awaitWork()
        val filter = firstVm.currentState().filter!!.copy(accounts = listOf(first), minAmount = 12.0)
        firstVm.onEvent(ReportScreenEvent.OnFilter(filter)); firstVm.awaitWork()
        firstVm.onEvent(ReportScreenEvent.OnFilterOverlayVisible(true)); firstVm.awaitWork()
        val payload = firstVm.savePage()!!
        val restored = vm(); restored.start(entry, payload); restored.awaitWork()
        assertEquals(filter, restored.currentState().filter)
        assertTrue(restored.currentState().filterOverlayVisible)
        assertEquals(listOf(first, second), restored.currentState().accounts)
        restored.start(entry, null); restored.awaitWork()
        assertEquals(filter, restored.currentState().filter)
        coVerify(exactly = 2) { base(Unit) }
        restored.start(entry, restored.savePage(), refresh = true); restored.awaitWork()
        assertEquals(filter, restored.currentState().filter)
        assertTrue(restored.currentState().filterOverlayVisible)
        coVerify(exactly = 3) { base(Unit) }
    }
    @Test fun `missing saved selection requests explicit review instead of silently accepting it`() = runTest(dispatcher) {
        val original = vm(); original.start(UUID.randomUUID(), null); original.awaitWork()
        original.onEvent(ReportScreenEvent.OnFilter(original.currentState().filter!!.copy(accounts = listOf(first))))
        original.awaitWork()
        val payload = original.savePage()!!
        coEvery { accounts(Unit) } returns persistentListOf(second)
        val restored = vm(); restored.start(UUID.randomUUID(), payload); restored.awaitWork()
        assertTrue(restored.restorationNotice)
        assertFalse(restored.startupFailed)
        restored.acknowledgeRestoration(); assertFalse(restored.restorationNotice)
    }
    @Test fun `queued report requests cannot publish a superseded result`() = runTest(dispatcher) {
        val vm = vm(); vm.start(UUID.randomUUID(), null); vm.awaitWork()
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        var calls = 0
        coEvery { totals(any()) } coAnswers {
            if (++calls == 1) { entered.complete(Unit); release.await() }
            IncomeExpenseTransferPair.zero()
        }
        val original = vm.currentState().filter!!
        vm.onEvent(ReportScreenEvent.OnFilter(original.copy(minAmount = 10.0)))
        entered.await()
        vm.onEvent(ReportScreenEvent.OnFilter(original.copy(minAmount = 20.0)))
        runCurrent()
        release.complete(Unit); vm.awaitWork()
        assertEquals(20.0, vm.currentState().filter!!.minAmount!!, 0.0)
        assertFalse(vm.currentState().loading)
    }
}
