package com.ivy.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.design.system.ProcessDraftState
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class TransactionEditorDraftTest {
    private val transaction = Transaction(UUID.randomUUID(), TransactionType.TRANSFER, BigDecimal("120.25"),
        UUID.randomUUID(), BigDecimal("32.74"), title = "Fictional transfer", description = "\nn:s|note", dateTime = Instant.parse("2026-11-01T12:00:00Z"))

    @Test fun `draft round trip preserves identity foreign amount and dates without a write`() {
        assertEquals(transaction, TransactionEditorDraft.decode(ProcessDraftState.encode(TransactionEditorDraft.fields(transaction)))!!.first)
    }
    @Test fun `linked metadata affects baseline but is never restored from editable draft`() {
        val linked = transaction.copy(loanId = UUID.randomUUID(), loanRecordId = UUID.randomUUID())
        assertNotEquals(TransactionEditorDraft.fingerprint(transaction), TransactionEditorDraft.fingerprint(linked))
        val restored = TransactionEditorDraft.decode(ProcessDraftState.encode(TransactionEditorDraft.fields(linked)))!!.first
        assertNull(restored.loanId)
        assertNull(restored.loanRecordId)
    }
    @Test fun `database numeric precision and obsolete sync flags do not create false conflicts`() {
        assertEquals(TransactionEditorDraft.fingerprint(transaction),
            TransactionEditorDraft.fingerprint(transaction.copy(amount = BigDecimal("120.250000"), isSynced = true)))
    }
    @Test fun `malformed IDs dates and nonfinite or negative amounts refuse restoration`() {
        for (field in listOf("id" to "bad", "date" to "tomorrow", "amount" to "NaN", "amount" to "-1", "toAmount" to "Infinity")) {
            assertNull(TransactionEditorDraft.decode(ProcessDraftState.encode(TransactionEditorDraft.fields(transaction) + field)))
        }
    }
}
