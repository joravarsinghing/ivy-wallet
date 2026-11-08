package com.ivy.search

import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.repository.CategoryRepository
import com.ivy.design.system.ProcessDraftState
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.transaction.AllTrnsAct
import com.ivy.wallet.domain.action.transaction.TrnsWithDateDivsAct
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SearchLifecycleTest {
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = object : DispatchersProvider {
        override val main = dispatcher; override val io = dispatcher; override val default = dispatcher
    }
    private val history = mockk<TrnsWithDateDivsAct>()
    private val accounts = mockk<AccountsAct>()
    private val categories = mockk<CategoryRepository>()
    private val currency = mockk<BaseCurrencyAct>()
    private val all = mockk<AllTrnsAct>()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { all(Unit) } returns emptyList()
        coEvery { history(any()) } returns emptyList()
        coEvery { accounts(Unit) } returns persistentListOf()
        coEvery { categories.findAll() } returns emptyList()
        coEvery { currency(Unit) } returns "AED"
    }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun vm() = SearchViewModel(history, accounts, categories, currency, all, mockk(), dispatchers)

    @Test fun `new process restores its query and returning refreshes without resetting it`() = runTest(dispatcher) {
        val entry = UUID.randomUUID()
        val vm = vm()
        vm.start(entry, ProcessDraftState.encode(mapOf("query" to "Fictional train")))
        advanceUntilIdle()
        assertEquals("Fictional train", vm.currentState().searchQuery)
        vm.start(entry, null); advanceUntilIdle()
        assertEquals("Fictional train", ProcessDraftState.decode(vm.saveQuery())!!["query"])
        coVerify(exactly = 2) { all(Unit) }
        vm.start(UUID.randomUUID(), null); advanceUntilIdle()
        assertEquals("", vm.currentState().searchQuery)
    }
    @Test fun `slower old request cannot replace newer results even after cancellation insensitive work`() = runTest(dispatcher) {
        val oldResult = mockk<TransactionHistoryItem>()
        val latestResult = mockk<TransactionHistoryItem>()
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        coEvery { history(any()) } coAnswers {
            if (++calls == 1) withContext(NonCancellable) { gate.await(); listOf(oldResult) } else listOf(latestResult)
        }
        val vm = vm()
        vm.onEvent(SearchEvent.Search("old")); runCurrent()
        vm.onEvent(SearchEvent.Search("new")); runCurrent()
        assertEquals(listOf(latestResult), vm.currentState().transactions)
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(listOf(latestResult), vm.currentState().transactions)
        assertEquals("new", vm.currentState().searchQuery)
        assertFalse(vm.currentState().loading)
    }
    @Test fun `failed search retains query and has an acknowledged retry`() = runTest(dispatcher) {
        coEvery { all(Unit) } throws IllegalStateException("Fictional read failure")
        val vm = vm(); vm.onEvent(SearchEvent.Search("bus")); advanceUntilIdle()
        assertTrue(vm.currentState().failed)
        assertFalse(vm.currentState().loading)
        assertEquals("bus", vm.currentState().searchQuery)
        coEvery { all(Unit) } returns emptyList()
        vm.onEvent(SearchEvent.Search("bus")); advanceUntilIdle()
        assertFalse(vm.currentState().failed)
    }
}
