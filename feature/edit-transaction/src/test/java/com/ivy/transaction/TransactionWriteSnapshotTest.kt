package com.ivy.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class TransactionWriteSnapshotTest {
    private val account = UUID.randomUUID()
    private val destination = UUID.randomUUID()
    private val category = UUID.randomUUID()
    private val now = Instant.parse("2026-10-31T12:00:00Z")
    private val source = Transaction(account, TransactionType.EXPENSE, BigDecimal.ONE,
        loanId = UUID.randomUUID(), loanRecordId = UUID.randomUUID(), isSynced = true)
    private fun capture(original: Transaction = source, amount: Double = 25.5, converted: Double? = null,
                        due: Instant? = null, title: String = " Fictional fare ") =
        transactionWriteSnapshot(original, account, destination, category, TransactionType.TRANSFER, amount,
            converted, title, " Fictional note ", due, now.minusSeconds(60), now)

    @Test fun `submitted values are frozen independently of later drafts`() {
        var amount = 25.5
        val first = capture(amount = amount)
        amount = 100.0
        val second = capture(amount = amount, title = "Changed draft")
        assertEquals(BigDecimal("25.5"), first.amount)
        assertEquals("Fictional fare", first.title)
        assertEquals(BigDecimal("100.0"), second.amount)
        assertEquals("Changed draft", second.title)
    }
    @Test fun `snapshot preserves record and loan identities while applying selected accounts`() {
        val draft = capture()
        assertEquals(source.id, draft.id)
        assertEquals(source.loanId, draft.loanId)
        assertEquals(source.loanRecordId, draft.loanRecordId)
        assertEquals(account, draft.accountId)
        assertEquals(destination, draft.toAccountId)
        assertEquals(category, draft.categoryId)
        assertFalse(draft.isSynced)
    }
    @Test fun `confirmed conversion is retained without recalculating or relabelling it`() {
        assertEquals(BigDecimal("7.25"), capture(converted = 7.25).toAmount)
        assertEquals(BigDecimal("25.5"), capture().toAmount)
    }
    @Test fun `existing timestamp is not replaced by submission time`() {
        val prior = now.minusSeconds(3600)
        assertEquals(prior, capture(source.copy(dateTime = prior)).dateTime)
        assertEquals(now, capture().dateTime)
    }
    @Test fun `unpaid future entry retains null settlement and its intended payment date`() {
        val due = now.plusSeconds(86400)
        val draft = capture(due = due)
        assertNull(draft.dateTime)
        assertEquals(due, draft.dueDate)
        assertEquals(now.minusSeconds(60), draft.paidFor)
    }
    @Test fun `malformed nonfinite amounts cannot enter a queued write`() {
        for (amount in listOf(Double.NaN, Double.POSITIVE_INFINITY)) {
            try { capture(amount = amount); fail() } catch (_: IllegalArgumentException) { }
            try { capture(converted = amount); fail() } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun `current write may acknowledge its own unchanged route`() {
        val owner = UUID.randomUUID()
        assertTrue(isCurrentTransactionWrite(owner, 2, owner, 2))
    }
    @Test fun `older failed or successful auto save cannot change newer submission feedback`() {
        val owner = UUID.randomUUID()
        assertFalse(isCurrentTransactionWrite(owner, 1, owner, 2))
    }
    @Test fun `late result cannot acknowledge or close another route`() {
        assertFalse(isCurrentTransactionWrite(UUID.randomUUID(), 2, UUID.randomUUID(), 2))
    }
    @Test fun `unowned state is never treated as an active submitted route`() {
        assertFalse(isCurrentTransactionWrite(null, 2, null, 2))
    }
}
