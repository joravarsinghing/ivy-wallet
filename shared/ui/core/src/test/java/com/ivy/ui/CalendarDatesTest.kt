package com.ivy.ui

import com.ivy.ui.time.calendarDateFromMillis
import com.ivy.ui.time.calendarDateMillis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class CalendarDatesTest {
    @Test fun `calendar selection is UTC not device midnight`() {
        val day = LocalDate.of(2026, 10, 26)
        val millis = calendarDateMillis(day)
        assertEquals(day, calendarDateFromMillis(millis))
        assertNotEquals(day, Instant.ofEpochMilli(millis).atZone(ZoneId.of("America/Los_Angeles")).toLocalDate())
    }
    @Test fun `leap days and historical dates round trip exactly`() {
        listOf(LocalDate.of(2028, 2, 29), LocalDate.of(1960, 1, 2), LocalDate.of(2027, 4, 23)).forEach {
            assertEquals(it, calendarDateFromMillis(calendarDateMillis(it)))
        }
    }
    @Test fun `negative epoch milliseconds do not truncate to the wrong day`() {
        assertEquals(LocalDate.of(1969, 12, 31), calendarDateFromMillis(-1L))
    }
}
