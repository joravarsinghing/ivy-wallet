package com.ivy.planned.edit

import com.ivy.base.model.TransactionType
import com.ivy.design.system.ProcessDraftState
import com.ivy.legacy.datamodel.PlannedPaymentRule
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class PlannedEditorDraftTest {
    private val rule = PlannedPaymentRule(Instant.parse("2026-11-01T12:30:00Z"), null, null, true,
        TransactionType.EXPENSE, UUID.randomUUID(), 75.25, title = "Fictional\nN:S purchase", description = "")

    @Test fun `draft preserves stable creation identity amount and one time date`() {
        val restored = PlannedEditorDraft.decode(ProcessDraftState.encode(PlannedEditorDraft.fields(rule)))!!.first
        assertEquals(rule, restored)
        assertEquals(PlannedEditorDraft.fingerprint(rule), PlannedEditorDraft.fingerprint(restored))
    }
    @Test fun `baseline digest distinguishes null empty and modified values but ignores obsolete sync state`() {
        val fingerprint = PlannedEditorDraft.fingerprint(rule)
        assertNotEquals(fingerprint, PlannedEditorDraft.fingerprint(rule.copy(description = null)))
        assertNotEquals(fingerprint, PlannedEditorDraft.fingerprint(rule.copy(amount = 76.0)))
        assertEquals(fingerprint, PlannedEditorDraft.fingerprint(rule.copy(isSynced = true)))
    }
    @Test fun `corrupt and nonfinite draft cannot be applied`() {
        val fields = PlannedEditorDraft.fields(rule)
        for (change in listOf("id" to "broken", "amount" to "NaN", "start" to "not-a-date", "type" to "FUTURE")) {
            assertNull(PlannedEditorDraft.decode(ProcessDraftState.encode(fields + change)))
        }
    }
}
