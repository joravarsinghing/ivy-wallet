package com.ivy.wallet.ui.theme.components

import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalInputValidationTest {
    @Test fun `positive counts remain exact`() {
        assertEquals(1, intervalCountValue("1"))
        assertEquals(12, intervalCountValue("12"))
        assertEquals(99999, intervalCountValue("99999"))
    }
    @Test fun `cleared malformed zero negative and overflowing drafts invalidate the count`() {
        listOf("", " ", "abc", "1.5", "0", "-1", "999999999999999").forEach {
            assertEquals("Invalid draft $it must not retain the preceding count", 0, intervalCountValue(it))
        }
    }
}
