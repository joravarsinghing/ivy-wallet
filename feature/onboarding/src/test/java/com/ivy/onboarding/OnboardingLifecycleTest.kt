package com.ivy.onboarding

import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Theme
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.write.WriteSettingsDao
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.data.repository.CategoryRepository
import com.ivy.navigation.Navigation
import com.ivy.navigation.OnboardingScreen
import com.ivy.onboarding.viewmodel.OnboardingViewModel
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.PreloadDataLogic
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingLifecycleTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val dispatchers = object : DispatchersProvider {
        override val main = dispatcher
        override val io = dispatcher
        override val default = dispatcher
    }
    private val nav = Navigation()
    private val screen = OnboardingScreen
    private val settings = mockk<SettingsDao>()
    private val writer = mockk<WriteSettingsDao>(relaxed = true)
    private val accounts = mockk<AccountsAct>()
    private val categories = mockk<CategoryRepository>()
    private val preload = mockk<PreloadDataLogic>(relaxed = true)
    private val prefs = mockk<SharedPrefs>(relaxed = true)
    private val accountDao = mockk<com.ivy.data.db.dao.read.AccountDao>()
    private val stored = SettingsEntity(Theme.DARK, "AED", 1000.0, "")

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        nav.navigateTo(screen)
        coEvery { settings.findFirstOrNull() } returns stored
        coEvery { settings.findFirst() } returns stored
        coEvery { accounts(Unit) } returns persistentListOf()
        coEvery { accountDao.findAll() } returns emptyList()
        coEvery { categories.findAll() } returns emptyList()
        every { preload.accountSuggestions(any()) } returns persistentListOf()
        every { preload.categorySuggestions() } returns persistentListOf()
    }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun vm() = OnboardingViewModel(nav, accountDao, settings, mockk(), mockk(), categories,
        mockk(), accounts, mockk(relaxed = true), writer, prefs, mockk(relaxed = true), preload,
        mockk(relaxed = true), dispatchers)

    @Test fun `recreation at currency does not restart splash or reset selected step`() = runTest(dispatcher) {
        val vm = vm()
        vm.start(screen, true)
        advanceUntilIdle()
        vm.onEvent(OnboardingEvent.StartFresh)
        advanceUntilIdle()
        assertEquals(OnboardingState.CURRENCY, vm.state.value)
        vm.start(screen, false)
        advanceUntilIdle()
        assertEquals(OnboardingState.CURRENCY, vm.state.value)
        coVerify(exactly = 1) { settings.findFirstOrNull() }
        coVerify(exactly = 0) { writer.save(any()) }
    }

    @Test fun `accounts and categories survive recreation without duplicate preparation`() = runTest(dispatcher) {
        val vm = vm()
        vm.start(screen, true)
        advanceUntilIdle()
        vm.onEvent(OnboardingEvent.SetBaseCurrency(IvyCurrency.fromCode("AED")!!))
        advanceUntilIdle()
        assertEquals(OnboardingState.ACCOUNTS, vm.state.value)
        vm.start(screen, false)
        advanceUntilIdle()
        assertEquals(OnboardingState.ACCOUNTS, vm.state.value)
        vm.onEvent(OnboardingEvent.OnAddAccountsDone)
        advanceUntilIdle()
        assertEquals(OnboardingState.CATEGORIES, vm.state.value)
        vm.start(screen, true)
        advanceUntilIdle()
        assertEquals(OnboardingState.CATEGORIES, vm.state.value)
        verify(exactly = 1) { preload.accountSuggestions("AED") }
        verify(exactly = 1) { preload.categorySuggestions() }
        coVerify(exactly = 1) { writer.saveInitialCurrency(match { it.currency == "AED" }) }
    }

    @Test fun `recreated pending suggestion keeps its actual controller and completion`() = runTest(dispatcher) {
        val vm = vm()
        vm.start(screen, true)
        advanceUntilIdle()
        var complete: ((Result<Unit>) -> Unit)? = null
        vm.accountSuggestionSubmission.submit({ complete = it }, {})
        vm.start(screen, false)
        advanceUntilIdle()
        assertTrue(vm.accountSuggestionSubmission.saving)
        complete!!(Result.failure(IllegalStateException("Fictional failure")))
        assertFalse(vm.accountSuggestionSubmission.saving)
        assertNotNull(vm.accountSuggestionSubmission.error)
    }

    @Test fun `in flight startup is not duplicated by recreation`() = runTest(dispatcher) {
        val vm = vm()
        vm.start(screen, true)
        runCurrent()
        vm.start(screen, false)
        advanceUntilIdle()
        assertEquals(OnboardingState.LOGIN, vm.state.value)
        coVerify(exactly = 1) { settings.findFirstOrNull() }
    }

    @Test fun `initial read failure is retryable without inventing settings`() = runTest(dispatcher) {
        val vm = vm()
        coEvery { settings.findFirstOrNull() } throws IllegalStateException("Fictional failure")
        vm.start(screen, true)
        advanceUntilIdle()
        assertTrue(vm.startupError.value)
        coVerify(exactly = 0) { writer.save(any()) }
        coEvery { settings.findFirstOrNull() } returns stored
        vm.start(screen, true)
        advanceUntilIdle()
        assertFalse(vm.startupError.value)
        assertEquals(OnboardingState.LOGIN, vm.state.value)
    }

    @Test fun `fresh route entry initializes independently of retained viewmodel`() = runTest(dispatcher) {
        val vm = vm()
        vm.start(screen, true)
        advanceUntilIdle()
        vm.onEvent(OnboardingEvent.StartFresh)
        advanceUntilIdle()
        nav.navigateTo(screen)
        vm.start(screen, false)
        advanceUntilIdle()
        assertEquals(OnboardingState.LOGIN, vm.state.value)
        coVerify(exactly = 2) { settings.findFirstOrNull() }
    }

    @Test fun `new process restores each setup step without preloading or completing onboarding`() = runTest(dispatcher) {
        for (step in listOf(OnboardingState.CURRENCY, OnboardingState.ACCOUNTS, OnboardingState.CATEGORIES)) {
            nav.navigateTo(screen)
            val fresh = vm()
            fresh.start(screen, false, com.ivy.design.system.ProcessDraftState.encode(mapOf("step" to step.name)))
            advanceUntilIdle()
            assertEquals(step, fresh.state.value)
            assertNotNull(fresh.saveDraft())
        }
        coVerify(exactly = 0) { preload.preloadAccounts(); preload.preloadCategories(); writer.saveInitialCurrency(any()) }
        verify(exactly = 0) { prefs.putBoolean(SharedPrefs.ONBOARDING_COMPLETED, true) }
    }

    @Test fun `unknown saved setup step is ignored without writing user data`() = runTest(dispatcher) {
        val fresh = vm()
        fresh.start(screen, false, com.ivy.design.system.ProcessDraftState.encode(mapOf("step" to "future-step")))
        advanceUntilIdle()
        assertEquals(OnboardingState.LOGIN, fresh.state.value)
        coVerify(exactly = 0) { writer.save(any()) }
    }
}
