package com.ivy.security

import org.junit.Assert.*
import org.junit.Test

class ScreenCapturePolicyTest {
    @Test fun `capture is never allowed while locked or initializing`() {
        listOf<Boolean?>(null, true).forEach { locked ->
            listOf<Boolean?>(null, false, true).forEach { preference ->
                assertTrue(shouldProtectScreenCapture(locked, false, preference, false))
            }
        }
    }
    @Test fun `credentials remain protected even when capture was explicitly enabled`() {
        assertTrue(shouldProtectScreenCapture(false, true, true, true))
        assertTrue(shouldProtectScreenCapture(false, false, true, true))
    }
    @Test fun `explicit preference controls unlocked financial screens regardless of app lock`() {
        listOf(false, true).forEach { enabled ->
            assertFalse(shouldProtectScreenCapture(false, enabled, true, false))
            assertTrue(shouldProtectScreenCapture(false, enabled, false, false))
        }
    }
    @Test fun `existing privacy defaults are preserved until the user chooses`() {
        assertTrue(shouldProtectScreenCapture(false, true, null, false))
        assertFalse(shouldProtectScreenCapture(false, false, null, false))
    }
}
