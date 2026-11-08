package com.ivy.ui

import com.ivy.ui.time.calendarMonthRows
import java.time.DayOfWeek
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class CalendarMonthRowsTest {
    @Test fun `every day appears exactly once in seven complete columns`() {
        for (year in 1900..2100) for (month in 1..12) for (start in listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)) {
            val period = YearMonth.of(year, month)
            val rows = calendarMonthRows(period, start)
            assertTrue(rows.all { it.size == 7 })
            assertEquals((1..period.lengthOfMonth()).map(period::atDay), rows.flatten().filterNotNull())
        }
    }
    @Test fun `six week months and locale week starts keep the right columns`() {
        val rows = calendarMonthRows(YearMonth.of(2026, 8), DayOfWeek.SUNDAY)
        assertEquals(6, rows.size)
        assertEquals(1, rows.first().last()!!.dayOfMonth)
        val february = calendarMonthRows(YearMonth.of(2021, 2), DayOfWeek.MONDAY)
        assertEquals(4, february.size)
        assertEquals(1, february.first().first()!!.dayOfMonth)
    }
}
