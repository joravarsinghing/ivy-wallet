package com.ivy.exchangerates

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RateFreshnessTest {
    private val today = LocalDate.of(2026, 11, 8)
    @Test fun `fresh stale unknown and future quotes remain distinguishable`() {
        assertTrue(rateFreshnessLabel("2026-11-08", today).contains("Today's"))
        assertTrue(rateFreshnessLabel("2026-11-07", today).contains("Yesterday's"))
        assertTrue(rateFreshnessLabel("2026-11-01", today).contains("stale"))
        assertTrue(rateFreshnessLabel(null, today).contains("unknown"))
        assertTrue(rateFreshnessLabel("invalid", today).contains("unknown"))
        assertTrue(rateFreshnessLabel("2026-11-09", today).contains("review"))
    }
}
