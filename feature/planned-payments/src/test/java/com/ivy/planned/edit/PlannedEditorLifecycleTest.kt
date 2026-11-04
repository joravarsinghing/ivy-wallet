package com.ivy.planned.edit

import com.ivy.base.model.TransactionType
import com.ivy.base.threading.DispatchersProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.repository.CategoryRepository
import com.ivy.design.system.ProcessDraftState
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.PlannedPaymentsScreen
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.deprecated.logic.PlannedPaymentWriter
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PlannedEditorLifecycleTest {
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = object : DispatchersProvider {
        override val main = dispatcher
        override val io = dispatcher
        override val default = dispatcher
    }
    private val nav = Navigation()
    private val accounts = mockk<AccountsAct>()
    private val accountDao = mockk<AccountDao>()
    private val categories = mockk<CategoryRepository>()
    private val settings = mockk<SettingsDao>()
    private val reader = mockk<PlannedPaymentRuleDao>()
    private val writer = mockk<PlannedPaymentWriter>()
    private val account = AccountEntity("Fictional bank", "AED", 0)
    private val screen = EditPlannedScreen(null, TransactionType.EXPENSE)
    private val rule = PlannedPaymentRule(Instant.parse("2026-11-01T12:00:00Z"), null, null, true,
        TransactionType.EXPENSE, account.id, 90.0, title = "Fictional bill")

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        nav.navigateTo(PlannedPaymentsScreen); nav.navigateTo(screen)
        coEvery { accounts(Unit) } returns persistentListOf(account.toLegacyDomain())
        coEvery { accountDao.findById(account.id) } returns account
        coEvery { categories.findAll() } returns emptyList()
        coEvery { reader.findById(any()) } returns null
        coEvery { writer.save(any(), any(), any()) } answers { firstArg() }
    }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun vm() = EditPlannedViewModel(accountDao, categories, settings, nav, reader, writer,
        mockk(), mockk(), accounts, TestTimeConverter(), dispatchers)

    @Test fun `fresh process restores stable draft and does not save or generate automatically`() = runTest(dispatcher) {
        val first = vm(); first.start(screen); advanceUntilIdle()
        first.onEvent(EditPlannedScreenEvent.OnTitleChanged("Fictional laptop"))
        first.onEvent(EditPlannedScreenEvent.OnAmountChanged(1250.5))
        first.onEvent(EditPlannedScreenEvent.OnRuleChanged(LocalDateTime.of(2027, 1, 2, 12, 0), true, null, null))
        val saved = first.saveDraft()!!
        val fresh = vm(); fresh.start(screen, saved); advanceUntilIdle()
        assertTrue(fresh.ready)
        val fields = ProcessDraftState.decode(fresh.saveDraft())!!
        assertEquals(ProcessDraftState.decode(saved)!!["id"], fields["id"])
        assertEquals("Fictional laptop", fields["title"])
        assertEquals("1250.5", fields["amount"])
        assertEquals("true", fields["oneTime"])
        coVerify(exactly = 0) { writer.save(any(), any(), any()) }
    }

    @Test fun `one time save freezes submission before queue and waits for acknowledgement`() = runTest(dispatcher) {
        val vm = vm(); vm.start(screen); advanceUntilIdle()
        vm.onEvent(EditPlannedScreenEvent.OnAmountChanged(80.0))
        vm.onEvent(EditPlannedScreenEvent.OnRuleChanged(LocalDateTime.of(2027, 1, 2, 12, 0), true, null, null))
        val gate = CompletableDeferred<Unit>()
        var submitted: PlannedPaymentRule? = null
        coEvery { writer.save(any(), any(), any()) } coAnswers {
            submitted = firstArg(); gate.await(); firstArg()
        }
        vm.onEvent(EditPlannedScreenEvent.OnSave())
        vm.onEvent(EditPlannedScreenEvent.OnAmountChanged(90.0))
        runCurrent()
        assertEquals(80.0, submitted!!.amount, 0.0)
        assertEquals(screen, nav.currentScreen)
        assertTrue(nav.onBackPressed())
        assertEquals(screen, nav.currentScreen)
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(PlannedPaymentsScreen, nav.currentScreen)
        coVerify(exactly = 1) { writer.save(any(), true, null) }
    }

    @Test fun `missing existing record is retryable and is never converted into creation`() = runTest(dispatcher) {
        val edit = screen.copy(plannedPaymentRuleId = rule.id)
        nav.navigateTo(edit)
        val vm = vm(); vm.start(edit); advanceUntilIdle()
        assertFalse(vm.ready); assertTrue(vm.loadError)
        vm.onEvent(EditPlannedScreenEvent.OnSave())
        coVerify(exactly = 0) { writer.save(any(), any(), any()) }
        coEvery { reader.findById(rule.id) } returns rule.toEntity()
        vm.start(edit); advanceUntilIdle()
        assertTrue(vm.ready); assertFalse(vm.loadError)
    }

    @Test fun `creation committed before interruption returns to parent without a duplicate`() = runTest(dispatcher) {
        coEvery { reader.findById(rule.id) } returns rule.toEntity()
        val saved = ProcessDraftState.encode(PlannedEditorDraft.fields(rule) + mapOf("creating" to "true"))!!
        vm().start(screen, saved); advanceUntilIdle()
        assertEquals(PlannedPaymentsScreen, nav.currentScreen)
        coVerify(exactly = 0) { writer.save(any(), any(), any()) }
    }

    @Test fun `changed baseline loads current data instead of overwriting with a stale draft`() = runTest(dispatcher) {
        val edit = screen.copy(plannedPaymentRuleId = rule.id)
        nav.navigateTo(edit)
        coEvery { reader.findById(rule.id) } returns rule.copy(amount = 200.0).toEntity()
        val saved = ProcessDraftState.encode(PlannedEditorDraft.fields(rule) + mapOf("creating" to "false", "baseline" to PlannedEditorDraft.fingerprint(rule)))!!
        val vm = vm(); vm.start(edit, saved); advanceUntilIdle()
        assertTrue(vm.ready)
        assertEquals("200.0", ProcessDraftState.decode(vm.saveDraft())!!["amount"])
        coVerify(exactly = 0) { writer.save(any(), any(), any()) }
    }
}
