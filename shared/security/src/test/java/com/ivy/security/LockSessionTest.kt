package com.ivy.security

import org.junit.Assert.*
import org.junit.Test

class LockSessionTest {
    @Test fun `resize does not reset authorization but real background expires at sixty seconds`() {
        var time = 0L
        val session = LockSession { time }
        session.configure(true, initial = true)
        assertTrue(session.authenticate(session.generation))
        repeat(20) { session.configure(true); session.foreground(false) }
        assertFalse(session.locked)
        session.background(); time = 59_999; session.foreground(false)
        assertFalse(session.locked)
        session.background(); time += 60_000; session.foreground(false)
        assertTrue(session.locked)
    }
    @Test fun `screen lock invalidates an outstanding successful callback`() {
        val session = LockSession { 0L }
        session.configure(true, true)
        val request = session.generation
        session.lock()
        assertFalse(session.authenticate(request))
        assertTrue(session.locked)
    }
    @Test fun `device lock always overrides grace and new process is locked`() {
        val session = LockSession { 0L }
        session.configure(true, true); session.authenticate(session.generation)
        session.foreground(true)
        assertTrue(session.locked)
        assertTrue(LockSession { 0L }.apply { configure(true, true) }.locked)
    }
    @Test fun `throttling begins at fifth failure and is capped`() {
        assertEquals(0L, PinCrypto.retryDelay(4))
        assertEquals(30_000L, PinCrypto.retryDelay(5))
        assertEquals(900_000L, PinCrypto.retryDelay(100))
        val code = PinCrypto.recoveryCode()
        assertEquals(32, PinCrypto.normalizeRecovery(code).length)
    }
    @Test fun `PIN verification uses random salts and rejects a wrong PIN`() {
        val a = PinCrypto.salt(); val b = PinCrypto.salt()
        assertFalse(a.contentEquals(b))
        val hash = PinCrypto.derive("123456".toCharArray(), a)
        assertTrue(PinCrypto.matches("123456".toCharArray(), a, hash))
        assertFalse(PinCrypto.matches("123457".toCharArray(), a, hash))
    }
    @Test fun `time spent authenticating an already locked session does not invalidate the prompt`() {
        var time = 0L
        val session = LockSession { time }
        session.configure(true, true)
        val epoch = session.generation
        session.background(); time = 120_000; session.foreground(false)
        assertEquals(epoch, session.generation)
        assertTrue(session.authenticate(epoch))
    }
}
