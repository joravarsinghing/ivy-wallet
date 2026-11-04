package com.ivy.home

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Nonoverlapping calendar intervals cover every selected date exactly once. */
fun homeTrendSamplingWindows(range: HomeTrendRange, end: LocalDate): List<HomeDateWindow> {
    val window = HomeDateWindow.ending(range, end)
    val days = ChronoUnit.DAYS.between(window.from, window.through) + 1
    val count = minOf(range.sampleCount.toLong(), days).toInt()
    return (0 until count).map { index ->
        HomeDateWindow(
            window.from.plusDays(days * index / count),
            window.from.plusDays(days * (index + 1) / count - 1),
        )
    }
}

/** Preserve calculated values, including real zero balances, without inventing prehistory. */
fun coverHomeTrendSamples(points: List<HomeTrendPoint>, firstRecordedActivity: LocalDate?): List<HomeTrendPoint> {
    val first = firstRecordedActivity ?: points.firstOrNull { it.isObserved }?.periodIdentity?.startDate
    return points.map { point ->
        val through = (point.periodIdentity as? HomeTrendPeriodIdentity.Interval)?.through
            ?: point.periodIdentity.startDate
        point.copy(isObserved = first != null && through != null && through >= first)
    }
}
