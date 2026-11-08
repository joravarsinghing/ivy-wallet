package com.ivy.goals

import com.ivy.base.time.TimeProvider
import com.ivy.data.db.entity.GoalEntity
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.GoalRepository
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<GoalRepository>()
    private val accounts = mockk<AccountsAct>()
    private val currency = mockk<BaseCurrencyAct>()
    private val clock = mockk<TimeProvider>()
    private var stored: GoalEntity? = null

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { currency(Unit) } returns "USD"
        coEvery { accounts(Unit) } returns persistentListOf()
        every { clock.localDateNow() } returns LocalDate.of(2026, 10, 26)
        every { clock.utcNow() } returns LocalDate.of(2026, 10, 26).atStartOfDay().toInstant(ZoneOffset.UTC)
        coEvery { repository.findActiveGoals() } answers { listOfNotNull(stored) }
        coEvery { repository.findAllocations(any()) } returns emptyList()
        coEvery { repository.findGoal(any()) } answers { stored }
        coEvery { repository.saveGoalWithAllocations(any(), any()) } coAnswers { stored = firstArg() }
    }
    @After fun tearDown() { Dispatchers.resetMain() }
    private fun viewModel() = GoalsViewModel(repository, accounts, mockk<CalcAccBalanceAct>(), mockk<CurrencyRepository>(), currency, clock)
    private fun assertLoaded(vm: GoalsViewModel) {
        assertFalse(vm.uiState.value.loading)
        assertNull(vm.uiState.value.error)
        assertEquals("USD", vm.uiState.value.baseCurrency)
    }
    private fun goal() = Goal(name = "Laptop", type = GoalType.PURCHASE, targetAmount = 9500.0,
        currency = "USD", deadline = LocalDate.of(2027, 4, 23))

    @Test fun `save keeps the actual date and publishes authoritative persisted data`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertLoaded(vm)
        vm.onEvent(GoalsScreenEvent.GoalCreated(goal()))
        assertTrue(vm.uiState.value.saving)
        assertTrue(vm.uiState.value.goals.isEmpty())
        advanceUntilIdle()
        assertEquals(LocalDate.of(2027, 4, 23).atStartOfDay().toInstant(ZoneOffset.UTC), stored!!.deadline)
        assertEquals(LocalDate.of(2027, 4, 23), vm.uiState.value.goals.single().deadline)
        assertEquals(1L, vm.uiState.value.saveRevision)
        assertFalse(vm.uiState.value.saving)
        assertNull(vm.uiState.value.error)
    }
    @Test fun `malformed legacy deadline is rejected without a storage write`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertLoaded(vm)
        vm.onEvent(GoalsScreenEvent.GoalCreated(goal().copy(deadline = null, deadlineLabel = "Apirhfifkf1f 27289")))
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.saveGoalWithAllocations(any(), any()) }
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.saving)
        assertTrue(vm.uiState.value.goals.isEmpty())
        assertEquals(0L, vm.uiState.value.saveRevision)
    }
    @Test fun `failed save remains retryable without a phantom goal`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertLoaded(vm)
        coEvery { repository.saveGoalWithAllocations(any(), any()) } throws IllegalStateException("Storage unavailable")
        vm.onEvent(GoalsScreenEvent.GoalCreated(goal()))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.goals.isEmpty())
        assertFalse(vm.uiState.value.saving)
        assertEquals(0L, vm.uiState.value.saveRevision)
        coEvery { repository.saveGoalWithAllocations(any(), any()) } coAnswers { stored = firstArg() }
        vm.onEvent(GoalsScreenEvent.GoalCreated(goal()))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.goals.size)
        assertEquals(1L, vm.uiState.value.saveRevision)
        assertNull(vm.uiState.value.error)
    }
    @Test fun `nonfinite reservations cannot enter the database`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertLoaded(vm)
        vm.onEvent(GoalsScreenEvent.GoalUpdated(goal().copy(allocations = listOf(GoalAllocation("bank", Double.POSITIVE_INFINITY)))))
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.saveGoalWithAllocations(any(), any()) }
        assertNotNull(vm.uiState.value.error)
    }
    @Test fun `saved goal is not submitted again when refresh fails`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertLoaded(vm)
        coEvery { repository.findActiveGoals() } throws IllegalStateException("Refresh unavailable")
        vm.onEvent(GoalsScreenEvent.GoalCreated(goal()))
        advanceUntilIdle()
        assertNotNull(stored)
        assertEquals(1L, vm.uiState.value.saveRevision)
        assertFalse(vm.uiState.value.saving)
        assertTrue(vm.uiState.value.error!!.contains("was saved"))
        coEvery { repository.findActiveGoals() } answers { listOfNotNull(stored) }
        vm.onEvent(GoalsScreenEvent.Retry)
        advanceUntilIdle()
        assertLoaded(vm)
        assertEquals(1, vm.uiState.value.goals.size)
        coVerify(exactly = 1) { repository.saveGoalWithAllocations(any(), any()) }
    }
    @Test fun `saving before initial load cannot use the placeholder currency`() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.uiState.value.loading)
        vm.onEvent(GoalsScreenEvent.GoalCreated(goal().copy(currency = "AED")))
        advanceUntilIdle()
        assertLoaded(vm)
        coVerify(exactly = 0) { repository.saveGoalWithAllocations(any(), any()) }
    }
}
