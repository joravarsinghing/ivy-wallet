package com.ivy.design.system

import org.junit.Assert.*
import org.junit.Test

class EditorSubmissionTest {
    @Test fun `terminal failures require review rather than silently resubmitting`() {
        val state = EditorSubmission(terminalFailure = { true }, failureMessage = { "Close and review." })
        var count = 0
        state.submit({ count++; it(Result.failure(IllegalStateException("Private detail"))) }, {})
        state.submit({ count++ }, {})
        assertTrue(state.requiresReview); assertFalse(state.saving)
        assertEquals("Close and review.", state.error); assertEquals(1, count)
    }
    @Test fun `duplicate taps cannot submit while pending`() {
        val state = EditorSubmission()
        var count = 0
        repeat(3) { state.submit({ count++ }, {}) }
        assertEquals(1, count)
        assertTrue(state.saving)
    }
    @Test fun `only acknowledged success closes editor exactly once`() {
        val state = EditorSubmission()
        var done: ((Result<Unit>) -> Unit)? = null
        var closed = 0
        state.submit({ done = it }, { closed++ })
        assertEquals(0, closed)
        done!!(Result.success(Unit))
        done!!(Result.success(Unit))
        assertEquals(1, closed)
        assertFalse(state.saving)
    }
    @Test fun `failure retains editor and permits retry without exception text leak`() {
        val state = EditorSubmission()
        var closed = false
        state.submit({ it(Result.failure(IllegalStateException("private account name"))) }, { closed = true })
        assertFalse(closed)
        assertFalse(state.saving)
        assertNotNull(state.error)
        assertFalse(state.error!!.contains("private account"))
        state.submit({ it(Result.success(Unit)) }, { closed = true })
        assertTrue(closed)
        assertNull(state.error)
    }
    @Test fun `stale failure cannot overwrite a later request`() {
        val state = EditorSubmission()
        var old: ((Result<Unit>) -> Unit)? = null
        state.submit({ old = it; it(Result.success(Unit)) }, {})
        state.submit({}, {})
        old!!(Result.failure(Exception()))
        assertTrue(state.saving)
        assertNull(state.error)
    }
    @Test fun `synchronous dispatcher failure stays recoverable`() {
        val state = EditorSubmission()
        state.submit({ throw IllegalStateException() }, { fail("Must not close") })
        assertFalse(state.saving)
        assertNotNull(state.error)
    }
}
