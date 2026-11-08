package com.ivy.legacy.data.model

import com.ivy.data.model.IntervalType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TimePeriodSavedStateTest {
    @Test fun `calendar identity and year survive without needing resources during decoding`() {
        val result = TimePeriodSavedState.decode(requireNotNull(TimePeriodSavedState.encode(TimePeriod(Month(10, "October"), 2025))))!!
        assertEquals(10, result.month?.monthValue)
        assertEquals(2025, result.year)
    }
    @Test fun `explicit timestamps preserve offsets instants and open ended ranges`() {
        listOf(FromToTimeRange(null, null), FromToTimeRange(Instant.parse("2025-11-01T04:00:00Z"), null),
            FromToTimeRange(null, Instant.parse("2026-01-01T00:00:00Z"))).forEach { range ->
            val period = TimePeriod(fromToRange = range)
            assertEquals(period, TimePeriodSavedState.decode(requireNotNull(TimePeriodSavedState.encode(period))))
        }
    }
    @Test fun `positive last N selections round trip for every unit`() {
        IntervalType.entries.forEach {
            val period = TimePeriod(lastNRange = LastNTimeRange(12, it))
            assertEquals(period, TimePeriodSavedState.decode(requireNotNull(TimePeriodSavedState.encode(period))))
        }
    }
    @Test fun `malformed future reversed and nonpositive periods are rejected`() {
        listOf(listOf("month", "13", "2026"), listOf("month", "1", "overflow"), listOf("future", "1", "DAY"),
            listOf("last", "0", "DAY"), listOf("last", "1", "unknown"),
            listOf("range", "2026-11-02T00:00:00Z", "2026-11-01T00:00:00Z"), emptyList()).forEach {
            assertNull(TimePeriodSavedState.decode(it))
        }
    }
}
