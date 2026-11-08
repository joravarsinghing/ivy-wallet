package com.ivy.ui

import com.ivy.ui.time.CalendarDateBounds
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CalendarDateBoundsTest {
    private val day = LocalDate.of(2027, 4, 23)
    @Test fun `absent bounds do not manufacture restrictions`() {
        assertTrue(day in CalendarDateBounds())
        assertEquals(day, CalendarDateBounds().displayDate(day))
    }
    @Test fun `inclusive limits apply independently`() {
        assertTrue(day in CalendarDateBounds(earliest = day))
        assertFalse(day.minusDays(1) in CalendarDateBounds(earliest = day))
        assertTrue(day in CalendarDateBounds(latest = day))
        assertFalse(day.plusDays(1) in CalendarDateBounds(latest = day))
    }
    @Test fun `invalid current value displays a valid month without making it a confirmed selection`() {
        val limits = CalendarDateBounds(day, day.plusMonths(2))
        assertFalse(day.minusYears(1) in limits)
        assertEquals(day, limits.displayDate(day.minusYears(1)))
        assertEquals(day.plusMonths(2), limits.displayDate(day.plusYears(1)))
    }
    @Test fun `existing years beyond the former picker default remain reachable`() {
        val future = LocalDate.of(2150, 4, 23)
        assertTrue(future.year in CalendarDateBounds().years(future))
        val past = LocalDate.of(1850, 4, 23)
        assertTrue(past.year in CalendarDateBounds().years(past))
    }
    @Test fun `bounded year list matches the supported interval`() {
        assertEquals(2027..2028, CalendarDateBounds(day, day.plusYears(1)).years(day))
    }
}
