package com.ivy.data.backup

import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.TransactionEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ImportFingerprintTest {
    private val planned = TransactionEntity(UUID.randomUUID(), TransactionType.EXPENSE, 100.0,
        dueDate = Instant.parse("2026-11-01T09:00:00Z"), title = "Fictional bill")

    @Test fun `planned payments on different dates are not import duplicates`() {
        val later = planned.copy(id = UUID.randomUUID(), dueDate = planned.dueDate!!.plusSeconds(86400))
        assertNotEquals(planned.importFingerprint(), later.importFingerprint())
    }

    @Test fun `retry identity ignores regenerated ids and insignificant text spacing`() {
        assertEquals(planned.importFingerprint(), planned.copy(id = UUID.randomUUID(),
            title = "  Fictional   BILL  ").importFingerprint())
    }

    @Test fun `completed and planned payments are distinct`() {
        assertNotEquals(planned.importFingerprint(),
            planned.copy(dateTime = planned.dueDate, dueDate = null).importFingerprint())
    }

    @Test fun `CSV-only destination defaults cannot defeat income and expense retry detection`() {
        for (type in listOf(TransactionType.INCOME, TransactionType.EXPENSE)) {
            val stored = planned.copy(type = type)
            assertEquals(stored.importFingerprint(), stored.copy(toAmount = stored.amount).importFingerprint())
        }
    }

    @Test fun `completed records ignore discarded deadlines and transfers normalize implicit receiving amount`() {
        val completed = planned.copy(dateTime = planned.dueDate)
        assertEquals(completed.importFingerprint(), completed.copy(dueDate = null).importFingerprint())
        val transfer = completed.copy(type = TransactionType.TRANSFER, toAccountId = UUID.randomUUID())
        assertEquals(transfer.importFingerprint(), transfer.copy(toAmount = transfer.amount).importFingerprint())
    }
}
