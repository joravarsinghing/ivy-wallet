package com.ivy.exchangerates.modal

import org.junit.Assert.*
import org.junit.Test

class ManualRateValidationTest {
    @Test fun `invalid and same-currency rates are rejected`() {
        listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertFalse(validManualRate("AED", "USD", it))
        }
        assertFalse(validManualRate("AED", "AED", 1.0))
        assertFalse(validManualRate("AED", " ", 1.0))
    }
    @Test fun `valid fractional rates and normalized codes are accepted`() {
        assertTrue(validManualRate("AED", " usd ", 0.272294))
    }
}
