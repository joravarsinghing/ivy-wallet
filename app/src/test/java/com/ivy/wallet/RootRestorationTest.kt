package com.ivy.wallet

import android.content.Intent
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.repository.LegalRepository
import com.ivy.legacy.IvyWalletCtx
import com.ivy.navigation.*
import com.ivy.security.AuthSessionController
import com.ivy.security.SecuritySettings
import com.ivy.security.UnlockMode
import com.ivy.wallet.domain.deprecated.logic.notification.TransactionReminderLogic
import com.ivy.wallet.migrations.MigrationsManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RootRestorationTest {
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = object : DispatchersProvider {
        override val main = dispatcher
        override val io = dispatcher
        override val default = dispatcher
    }
    private val context = mockk<IvyWalletCtx>(relaxed = true)
    private val settings = mockk<SettingsDao>()
    private val prefs = mockk<SharedPrefs>(relaxed = true)
    private val legal = mockk<LegalRepository>()
    private val validator = mockk<RestoredRouteValidator>()
    private val auth = mockk<AuthSessionController>()
    private val reminder = mockk<TransactionReminderLogic>(relaxed = true)
    private val migrations = mockk<MigrationsManager>(relaxed = true)
    private val intent = mockk<Intent>(relaxed = true)
    private val nav = Navigation()
    private val locked = MutableStateFlow<Boolean?>(true)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { settings.findAll() } returns emptyList()
        coEvery { legal.isDisclaimerAccepted() } returns true
        coEvery { validator.usable(any()) } returns true
        every { prefs.has(any()) } returns true
        every { prefs.getBoolean(SharedPrefs.ONBOARDING_COMPLETED, false) } returns true
        every { auth.locked } returns locked
        every { auth.settings } returns MutableStateFlow(SecuritySettings(UnlockMode.OFF, false, false))
    }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun vm() = RootViewModel(context, nav, settings, prefs, reminder, migrations, legal,
        auth, mockk(), mockk(), validator, dispatchers)
    private fun saved(vararg screens: Screen): List<String> = Navigation().apply {
        screens.forEach(::navigateTo)
    }.saveState()!!

    @Test fun `restores visit identity and history without authenticating or replaying intent`() = runTest(dispatcher) {
        val editor = EditTransactionScreen(null, com.ivy.base.model.TransactionType.EXPENSE)
        val state = saved(MainScreen, editor)
        val expected = Navigation().apply { restoreState(state) }.currentEntryId
        val vm = vm()
        vm.start(false, intent, state)
        advanceUntilIdle()
        assertEquals(editor, nav.currentScreen)
        assertEquals(expected, nav.currentEntryId)
        assertTrue(nav.back())
        assertEquals(MainScreen, nav.currentScreen)
        assertEquals(true, vm.appLocked.value)
        verify(exactly = 0) { intent.getSerializableExtra(any()); intent.getStringExtra(any()); auth.finishAuthentication(any(), any(), any()) }
    }

    @Test fun `removed destination falls back to retained parent and explains recovery`() = runTest(dispatcher) {
        val editor = EditPlannedScreen(UUID.randomUUID(), com.ivy.base.model.TransactionType.EXPENSE)
        coEvery { validator.usable(editor) } returns false
        val vm = vm()
        vm.start(false, intent, saved(MainScreen, PlannedPaymentsScreen, editor))
        advanceUntilIdle()
        assertEquals(PlannedPaymentsScreen, nav.currentScreen)
        assertTrue(vm.restorationNotice)
        vm.acknowledgeRestoration()
        assertFalse(vm.restorationNotice)
        assertTrue(nav.back())
        assertEquals(MainScreen, nav.currentScreen)
    }

    @Test fun `corrupt saved state never replays an external creation`() = runTest(dispatcher) {
        val vm = vm()
        vm.start(false, intent, listOf("unknown-version"))
        advanceUntilIdle()
        assertEquals(MainScreen, nav.currentScreen)
        assertTrue(vm.restorationNotice)
        verify(exactly = 0) { intent.getSerializableExtra(any()); intent.getStringExtra(any()) }
    }

    @Test fun `reset wallet cannot reopen saved financial pages`() = runTest(dispatcher) {
        every { prefs.getBoolean(SharedPrefs.ONBOARDING_COMPLETED, false) } returns false
        vm().start(false, intent, saved(MainScreen, SettingsScreen))
        advanceUntilIdle()
        assertEquals(OnboardingScreen, nav.currentScreen)
        assertTrue(nav.backStackEmpty())
    }

    @Test fun `incomplete onboarding resumes its import step with setup back history`() = runTest(dispatcher) {
        every { prefs.getBoolean(SharedPrefs.ONBOARDING_COMPLETED, false) } returns false
        val screen = ImportScreen(launchedFromOnboarding = true)
        vm().start(false, intent, saved(OnboardingScreen, screen))
        advanceUntilIdle()
        assertEquals(screen, nav.currentScreen)
        assertTrue(nav.back())
        assertEquals(OnboardingScreen, nav.currentScreen)
    }

    @Test fun `unaccepted legal notice remains above restored history`() = runTest(dispatcher) {
        coEvery { legal.isDisclaimerAccepted() } returns false
        vm().start(false, intent, saved(MainScreen, BudgetScreen))
        advanceUntilIdle()
        assertEquals(DisclaimerScreen, nav.currentScreen)
        assertTrue(nav.back())
        assertEquals(BudgetScreen, nav.currentScreen)
    }

    @Test fun `storage failure publishes no route and allows a successful retry`() = runTest(dispatcher) {
        coEvery { settings.findAll() } throws IllegalStateException("Fictional failure")
        val vm = vm()
        val state = saved(MainScreen, GoalsScreen)
        vm.start(false, intent, state)
        advanceUntilIdle()
        assertNull(nav.currentScreen)
        assertTrue(vm.startupError)
        coEvery { settings.findAll() } returns emptyList()
        vm.start(false, intent, state)
        advanceUntilIdle()
        assertEquals(GoalsScreen, nav.currentScreen)
        assertFalse(vm.startupError)
    }

    @Test fun `rotation and optional housekeeping failure do not reset the live wallet`() = runTest(dispatcher) {
        coEvery { migrations.executeMigrations() } throws IllegalStateException("Fictional failure")
        val vm = vm()
        vm.start(false, intent, saved(MainScreen, ReportScreen))
        advanceUntilIdle()
        val entry = nav.currentEntryId
        vm.start(true, intent, saved(MainScreen))
        advanceUntilIdle()
        assertEquals(ReportScreen, nav.currentScreen)
        assertEquals(entry, nav.currentEntryId)
        assertFalse(vm.startupError)
        coVerify(exactly = 1) { settings.findAll() }
    }
}
