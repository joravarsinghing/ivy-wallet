package com.ivy.home

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.collections.immutable.toImmutableList

class HomeTrendSamplingTest {
    @Test fun `every range from thirty days up has thirty real nonoverlapping buckets`() {
        for (end in listOf("2026-11-04", "2024-02-29", "2027-01-01", "2026-11-01").map(LocalDate::parse)) {
            HomeTrendRange.entries.forEach { range ->
                val windows = homeTrendSamplingWindows(range, end)
                val entire = HomeDateWindow.ending(range, end)
                assertEquals(if (range == HomeTrendRange.SEVEN_DAYS) 7 else 30, windows.size)
                assertEquals(entire.from, windows.first().from)
                assertEquals(entire.through, windows.last().through)
                windows.zipWithNext().forEach { (a, b) -> assertEquals(a.through.plusDays(1), b.from) }
                val durations = windows.map { ChronoUnit.DAYS.between(it.from, it.through) + 1 }
                assertTrue(durations.min() > 0)
                assertTrue(durations.max() - durations.min() <= 1)
                assertEquals(ChronoUnit.DAYS.between(entire.from, entire.through) + 1, durations.sum())
            }
        }
    }
    @Test fun `interval sums preserve refunds income expense and endpoint balances exactly`() {
        val windows = homeTrendSamplingWindows(HomeTrendRange.SIX_MONTHS, LocalDate.parse("2026-11-04"))
        val from = windows.first().from
        val daily = (0..ChronoUnit.DAYS.between(from, windows.last().through).toInt()).map { offset ->
            Triple(from.plusDays(offset.toLong()), BigDecimal("12.34"), if (offset % 7 == 0) BigDecimal("-2.10") else BigDecimal("3.45"))
        }
        var balance = BigDecimal("999.99")
        val points = windows.map { window ->
            val rows = daily.filter { it.first in window.from..window.through }
            val income = rows.fold(BigDecimal.ZERO) { total, row -> total + row.second }
            val spent = rows.fold(BigDecimal.ZERO) { total, row -> total + row.third }
            balance += income - spent
            HomeTrendPoint(window.displayLabel(), income, spent, balance,
                periodIdentity = HomeTrendPeriodIdentity.Interval(window.from, window.through))
        }
        val covered = coverHomeTrendSamples(points, from)
        assertEquals(daily.fold(BigDecimal.ZERO) { a, b -> a + b.second }, covered.fold(BigDecimal.ZERO) { a, b -> a + b.income })
        assertEquals(daily.fold(BigDecimal.ZERO) { a, b -> a + b.third }, covered.fold(BigDecimal.ZERO) { a, b -> a + b.expenses })
        assertEquals(balance, covered.last().balance)
        val scene = prepareTrendScene(HomeTrendState(range = HomeTrendRange.SIX_MONTHS, points = covered.toImmutableList()))
        assertEquals(30, scene.plot.series.last().segments.single().size)
        assertEquals(0f, scene.plot.series.last().segments.single().first().x, 0f)
        assertEquals(1f, scene.plot.series.last().segments.single().last().x, 0f)
        val window = HomeDateWindow(windows.first().from, windows.last().through)
        val dated = prepareTrendScene(HomeTrendState(range = HomeTrendRange.SIX_MONTHS,
            points = covered.toImmutableList(), window = window))
        val firstEndFraction = ChronoUnit.DAYS.between(window.from, windows.first().through).toFloat() /
            ChronoUnit.DAYS.between(window.from, window.through)
        assertEquals(firstEndFraction, dated.plot.series.last().segments.single().first().x, 0.00001f)
    }
    @Test fun `coverage keeps unknown prehistory and authentic quiet zero balances`() {
        val windows = homeTrendSamplingWindows(HomeTrendRange.THREE_MONTHS, LocalDate.parse("2026-11-04"))
        val raw = windows.map { HomeTrendPoint(it.displayLabel(), BigDecimal.ZERO, BigDecimal.ZERO,
            isObserved = false, periodIdentity = HomeTrendPeriodIdentity.Interval(it.from, it.through)) }
        assertTrue(coverHomeTrendSamples(raw, null).none { it.isObserved })
        assertTrue(coverHomeTrendSamples(raw, windows.first().from.minusDays(1)).all { it.isObserved && it.balance == BigDecimal.ZERO })
        val mid = windows[10].through
        val result = coverHomeTrendSamples(raw, mid)
        assertTrue(result.take(10).none { it.isObserved })
        assertTrue(result.drop(10).all { it.isObserved })
        assertEquals(30, result.map { it.periodKey }.distinct().size)
    }
}
