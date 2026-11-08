package com.ivy.home

import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.model.IntervalType
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.TimePeriod
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class HomePeriodSelectionTest {
    private val today = LocalDate.parse("2026-11-04")
    private fun resolve(selection: HomePeriodSelection, date: LocalDate = today, start: Int = 1,
        first: LocalDate? = LocalDate.parse("2026-01-01")) = selection.resolve(date, start, first).window!!
    @Test fun `default month stops today rather than using a chart anchor`() {
        assertEquals(HomeDateWindow(LocalDate.parse("2026-11-01"), today), resolve(HomePeriodSelection.CurrentCycle))
    }
    @Test fun `October is the exact whole budget month with no rolling overlay`() {
        assertEquals(HomeDateWindow(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")),
            resolve(HomePeriodSelection.BudgetMonth(YearMonth.parse("2026-10"))))
    }
    @Test fun `custom cycle starts resolve from local today and clamp short months`() {
        assertEquals(HomeDateWindow(LocalDate.parse("2026-10-10"), today), resolve(HomePeriodSelection.CurrentCycle, start = 10))
        assertEquals(HomeDateWindow(LocalDate.parse("2024-02-29"), LocalDate.parse("2024-03-30")),
            resolve(HomePeriodSelection.BudgetMonth(YearMonth.parse("2024-02")), start = 31))
    }
    @Test fun `rolling November includes October and is independent of selected month`() {
        assertEquals(HomeDateWindow(LocalDate.parse("2026-10-06"), today), resolve(HomePeriodSelection.Rolling(30, IntervalType.DAY)))
        assertEquals(HomeDateWindow(LocalDate.parse("2026-08-05"), today), resolve(HomePeriodSelection.Rolling(3, IntervalType.MONTH)))
    }
    @Test fun `current and rolling choices advance while historical choices stay fixed`() {
        val next = today.plusMonths(1)
        val fixed = HomePeriodSelection.BudgetMonth(YearMonth.parse("2026-10"))
        assertEquals(resolve(fixed), resolve(fixed, next))
        assertNotEquals(resolve(HomePeriodSelection.CurrentCycle), resolve(HomePeriodSelection.CurrentCycle, next))
        assertNotEquals(resolve(HomePeriodSelection.Rolling(7, IntervalType.DAY)), resolve(HomePeriodSelection.Rolling(7, IntervalType.DAY), next))
    }
    @Test fun `custom dates include a single day and cap future ends explicitly`() {
        assertEquals(HomeDateWindow(today, today), resolve(HomePeriodSelection.Custom(today, today)))
        val capped = HomePeriodSelection.Custom(today.minusDays(1), today.plusDays(10)).resolve(today, 1, null)
        assertEquals(today, capped.window!!.through)
        assertTrue(capped.cappedAtToday)
    }
    @Test(expected = IllegalArgumentException::class) fun `future only ranges cannot apply`() {
        resolve(HomePeriodSelection.Custom(today.plusDays(1), today.plusDays(2)))
    }
    @Test fun `all time uses real history and an empty wallet invents none`() {
        assertNull(HomePeriodSelection.AllTime.resolve(today, 1, null).window)
        assertEquals(LocalDate.parse("2026-01-01"), resolve(HomePeriodSelection.AllTime).from)
    }
    @Test fun `open range before first activity remains valid empty history`() {
        val range = resolve(HomePeriodSelection.Custom(null, LocalDate.parse("2025-01-01")))
        assertEquals(LocalDate.parse("2025-01-01"), range.through)
    }
    @Test fun `all modes survive primitive preference and chooser round trips`() {
        val converter = TestTimeConverter()
        val modes = homeQuickPeriods + listOf(HomePeriodSelection.AllTime,
            HomePeriodSelection.BudgetMonth(YearMonth.parse("2026-10")), HomePeriodSelection.Custom(today.minusDays(2), today))
        modes.forEach {
            assertEquals(it, decodeHomePeriod(it.encode()))
            assertEquals(it, homePeriodFromLegacy(it.asTimePeriod(converter), today, converter))
        }
    }
    @Test fun `old or corrupted chart range preference cannot silently change Home default`() {
        listOf(null, "THIRTY_DAYS", "rolling|0|DAY", "month|broken", "custom|bad|", "current|extra").forEach {
            assertEquals(HomePeriodSelection.CurrentCycle, decodeHomePeriod(it))
        }
    }
    @Test fun `exact windows partition every day once with at most thirty samples`() {
        listOf(1L, 7L, 29L, 30L, 31L, 90L, 180L, 365L, 3650L).forEach { length ->
            val window = HomeDateWindow(today.minusDays(length - 1), today)
            val buckets = homeTrendSamplingWindows(window)
            assertEquals(minOf(length, 30).toInt(), buckets.size)
            assertEquals(window.from, buckets.first().from)
            assertEquals(window.through, buckets.last().through)
            assertEquals(length, buckets.sumOf { ChronoUnit.DAYS.between(it.from, it.through) + 1 })
            buckets.zipWithNext().forEach { (a, b) -> assertEquals(a.through.plusDays(1), b.from) }
        }
    }
}
