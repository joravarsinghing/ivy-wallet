package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.model.TransactionType
import com.ivy.data.db.dao.read.*
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.PlannedPaymentRuleEntity
import com.ivy.data.model.IntervalType
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.legacy.datamodel.PlannedPaymentRule
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

/** Transactional fake verifies writer ownership; actual SQLite rollback still needs a device. */
class PlannedPaymentWriterTest {
    private val rows = mutableMapOf<UUID, PlannedPaymentRuleEntity>()
    private val atomic = mockk<AtomicWriteRepository>()
    private val reader = mockk<PlannedPaymentRuleDao>()
    private val dao = mockk<WritePlannedPaymentRuleDao>()
    private val accounts = mockk<AccountDao>()
    private val categories = mockk<CategoryDao>()
    private val generator = mockk<PlannedPaymentsGenerator>()
    private val transactions = mockk<TransactionRepository>(relaxed = true)
    private val account = AccountEntity("Fictional bank", color = 0)
    private val rule = PlannedPaymentRule(Instant.parse("2026-11-01T12:00:00Z"), null, null, true,
        TransactionType.EXPENSE, account.id, 120.0, title = "Fictional rent")
    private val writer = PlannedPaymentWriter(atomic, reader, dao, accounts, categories, generator, transactions)
    private var generations = 0

    init {
        coEvery { atomic.run<Any?>(any()) } coAnswers {
            val before = rows.toMap()
            try { firstArg<suspend () -> Any?>().invoke() } catch (error: Throwable) {
                rows.clear(); rows.putAll(before); throw error
            }
        }
        coEvery { reader.findById(any()) } answers { rows[firstArg()] }
        coEvery { dao.save(any()) } answers { firstArg<PlannedPaymentRuleEntity>().let { rows[it.id] = it } }
        coEvery { dao.deleteById(any()) } answers { rows.remove(firstArg<UUID>()); Unit }
        coEvery { accounts.findById(account.id) } returns account
        coEvery { categories.findById(any()) } returns null
        coEvery { generator.generate(any(), true) } answers { generations++ }
    }

    @Test fun `one time payment needs no recurrence fields and commits exactly once`() = runTest {
        repeat(2) { assertEquals(rule, writer.save(rule, true, null)) }
        assertEquals(1, rows.size)
        assertEquals(1, generations)
    }
    @Test fun `failed generation rolls back the rule and retry retains creation identity`() = runTest {
        coEvery { generator.generate(any(), true) } throws IllegalStateException("Fictional failure")
        assertTrue(runCatching { writer.save(rule, true, null) }.isFailure)
        assertTrue(rows.isEmpty())
        coEvery { generator.generate(any(), true) } just Runs
        writer.save(rule, true, null)
        assertEquals(rule.id, rows.values.single().id)
    }
    @Test fun `changed committed creation requires review without regenerating`() = runTest {
        writer.save(rule, true, null)
        assertTrue(runCatching { writer.save(rule.copy(amount = 400.0), true, null) }.exceptionOrNull() is PlannedPaymentConflict)
        assertEquals(120.0, rows.values.single().amount, 0.0)
        assertEquals(1, generations)
    }
    @Test fun `deleted existing edit cannot resurrect a payment`() = runTest {
        assertTrue(runCatching { writer.save(rule, false, rule) }.exceptionOrNull() is PlannedPaymentConflict)
        assertTrue(rows.isEmpty())
        assertEquals(0, generations)
    }
    @Test fun `stale baseline cannot overwrite a newer rule`() = runTest {
        val first = writer.save(rule, true, null)
        writer.save(first.copy(amount = 180.0), false, first)
        assertTrue(runCatching { writer.save(first.copy(amount = 999.0), false, first) }.exceptionOrNull() is PlannedPaymentConflict)
        assertEquals(180.0, rows.values.single().amount, 0.0)
        assertEquals(2, generations)
    }
    @Test fun `identical edit retry acknowledges the preceding commit without replacing instances`() = runTest {
        val first = writer.save(rule, true, null)
        val changed = first.copy(description = "Fictional update")
        repeat(2) { writer.save(changed, false, first) }
        assertEquals(2, generations)
    }
    @Test fun `invalid amounts transfer and incomplete recurrence never write`() = runTest {
        val invalid = listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -1.0).map { rule.copy(amount = it) } +
            listOf(rule.copy(type = TransactionType.TRANSFER), rule.copy(startDate = null),
                rule.copy(oneTime = false), rule.copy(oneTime = false, intervalN = 0, intervalType = IntervalType.MONTH))
        invalid.forEach { assertTrue(runCatching { writer.save(it, true, null) }.isFailure) }
        assertTrue(rows.isEmpty())
        assertEquals(0, generations)
    }
    @Test fun `missing or deleted accounts and removed categories require review`() = runTest {
        coEvery { accounts.findById(account.id) } returns null
        assertTrue(runCatching { writer.save(rule, true, null) }.exceptionOrNull() is PlannedPaymentConflict)
        coEvery { accounts.findById(account.id) } returns account.copy(isDeleted = true)
        assertTrue(runCatching { writer.save(rule, true, null) }.exceptionOrNull() is PlannedPaymentConflict)
        coEvery { accounts.findById(account.id) } returns account
        assertTrue(runCatching { writer.save(rule.copy(categoryId = UUID.randomUUID()), true, null) }.exceptionOrNull() is PlannedPaymentConflict)
        assertTrue(rows.isEmpty())
    }
    @Test fun `delete failure preserves the rule until related entries can be removed`() = runTest {
        writer.save(rule, true, null)
        coEvery { transactions.deletedByRecurringRuleIdAndNoDateTime(rule.id) } throws IllegalStateException("Fictional failure")
        assertTrue(runCatching { writer.delete(rule.id) }.isFailure)
        assertEquals(1, rows.size)
        coEvery { transactions.deletedByRecurringRuleIdAndNoDateTime(rule.id) } just Runs
        writer.delete(rule.id)
        assertTrue(rows.isEmpty())
    }
    @Test fun `cancellation rolls back without a false success acknowledgement`() = runTest {
        coEvery { generator.generate(any(), true) } throws CancellationException()
        assertTrue(runCatching { writer.save(rule, true, null) }.exceptionOrNull() is CancellationException)
        assertTrue(rows.isEmpty())
    }
}
