package com.ivy.home

import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.StandardTimeConverter
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeDateWindowTest {
    private fun converter(zone: String) = StandardTimeConverter(mockk<TimeProvider>().also {
        every { it.getZoneId() } returns ZoneId.of(zone)
    })
    @Test fun `thirty days in empty November include October`() {
        val range = HomeDateWindow.ending(HomeTrendRange.THIRTY_DAYS, LocalDate.parse("2026-11-03"))
        assertEquals(LocalDate.parse("2026-10-05"), range.from)
        assertEquals(LocalDate.parse("2026-11-03"), range.through)
    }
    @Test fun `calendar month ranges ignore fiscal start days and retain partial current month`() {
        val range = HomeDateWindow.ending(HomeTrendRange.THREE_MONTHS, LocalDate.parse("2026-11-03"))
        assertEquals(LocalDate.parse("2026-09-01"), range.from)
        assertEquals(LocalDate.parse("2026-11-03"), range.through)
    }
    @Test fun `UTC boundaries include the last day but never next midnight`() {
        val range = HomeDateWindow(LocalDate.parse("2026-11-03"), LocalDate.parse("2026-11-03"))
            .toUtcRange(converter("Asia/Dubai"))
        assertEquals(Instant.parse("2026-11-02T20:00:00Z"), range.from)
        assertEquals(Instant.parse("2026-11-03T20:00:00Z").minusNanos(1), range.to)
    }
    @Test fun `daylight saving uses calendar boundaries instead of fixed 24 hours`() {
        val range = HomeDateWindow(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-01"))
            .toUtcRange(converter("America/New_York"))
        assertEquals(Instant.parse("2026-11-01T04:00:00Z"), range.from)
        assertEquals(Instant.parse("2026-11-02T05:00:00Z").minusNanos(1), range.to)
    }
    @Test fun `December January rollover and navigation use exactly the same range`() {
        val converter = converter("UTC")
        val window = HomeDateWindow.ending(HomeTrendRange.SEVEN_DAYS, LocalDate.parse("2027-01-02"))
        assertEquals(LocalDate.parse("2026-12-27"), window.from)
        assertEquals(window.toUtcRange(converter).from, window.navigationRange(converter).from)
        assertEquals(window.toUtcRange(converter).to, window.navigationRange(converter).to)
    }
}
