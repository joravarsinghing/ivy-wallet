package com.ivy.design.system

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ProcessDraftStateTest {
    @Test fun `interrupted recovery keeps its original payload until explicitly consumed`() {
        val original = ProcessDraftState.encode(mapOf("title" to "Fictional draft"))!!
        val fresh = ProcessRestoration(original)
        assertEquals(original, fresh.snapshotForSave { null })
        assertEquals(original, fresh.snapshotForSave { ProcessDraftState.encode(mapOf("title" to "Temporary placeholder")) })
        fresh.consumed = true
        assertNull(fresh.snapshotForSave { null })
        val current = ProcessDraftState.encode(mapOf("title" to "Changed draft"))!!
        assertEquals(current, fresh.snapshotForSave { current })
    }
    @Test fun `primitive drafts preserve null empty and separator-like text`() {
        val fields = mapOf("note" to "Fictional\nn|s: note", "empty" to "", "null" to null, "amount" to "1.50")
        assertEquals(fields, ProcessDraftState.decode(ProcessDraftState.encode(fields)))
    }
    @Test fun `malformed duplicate oversized and unknown-version payloads are rejected`() {
        listOf(listOf("future"), listOf("jvy-draft-1", "key"), listOf("jvy-draft-1", "key", "bad"),
            listOf("jvy-draft-1", "key", "n", "key", "sother")).forEach { assertNull(ProcessDraftState.decode(it)) }
        assertNull(ProcessDraftState.encode(mapOf("note" to "x".repeat(70000))))
    }
    @Test fun `strict value helpers reject corrupt identities and nonfinite amounts`() {
        val id = UUID.randomUUID()
        assertEquals(id, mapOf("id" to id.toString()).draftUuid("id"))
        assertTrue(runCatching { mapOf("id" to "not-an-id").draftUuid("id") }.isFailure)
        assertTrue(runCatching { mapOf("amount" to "NaN").draftDouble("amount") }.isFailure)
        assertTrue(runCatching { mapOf("flag" to "maybe").draftBoolean("flag") }.isFailure)
    }
}
