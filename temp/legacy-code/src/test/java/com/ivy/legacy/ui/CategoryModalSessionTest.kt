package com.ivy.legacy.ui

import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import org.junit.Assert.*
import org.junit.Test

class CategoryModalSessionTest {
    @Test fun `retained modal copy shares pending write and rejects duplicate submit`() {
        val original = CategoryModalData(category = null)
        var writes = 0
        var complete: ((Result<Unit>) -> Unit)? = null
        original.submission.submit({ writes++; complete = it }, {})
        val retained = original.copy(autoFocusKeyboard = false)
        assertEquals(original.id, retained.id)
        assertSame(original.submission, retained.submission)
        assertTrue(retained.submission.saving)
        retained.submission.submit({ writes++ }, {})
        assertEquals(1, writes)
        complete!!(Result.failure(IllegalStateException("private detail")))
        assertFalse(retained.submission.saving)
        assertNotNull(retained.submission.error)
        assertFalse(retained.submission.error!!.contains("private detail"))
    }

    @Test fun `new modal does not inherit another sessions pending state`() {
        val original = CategoryModalData(category = null)
        original.submission.submit({}, {})
        val next = CategoryModalData(category = null)
        assertNotEquals(original.id, next.id)
        assertNotSame(original.submission, next.submission)
        assertFalse(next.submission.saving)
        assertNull(next.submission.error)
    }
}
