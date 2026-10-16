package com.ivy.home

import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Builds the populated monthly points that should be displayed for a Home trend.
 *
 * A point is considered populated when it contains income or expenses. Empty
 * calendar periods are intentionally omitted so a newly installed app can show
 * a useful single-month trend without inventing zero-value history.
 */
fun buildHomeTrendPoints(
    points: List<HomeTrendPoint>,
    range: HomeTrendRange,
): List<HomeTrendPoint> {
    return points
        .withIndex()
        .filter { (_, point) -> point.hasActivity() }
        .sortedWith(
            compareBy<IndexedValue<HomeTrendPoint>> { (_, point) -> monthSortKey(point.periodLabel) }
                .thenBy { it.index },
        )
        .takeLast(range.months)
        .map { it.value }
}

/** Returns the exact savings amount for a monthly point. */
fun calculateHomeTrendSavings(
    income: BigDecimal,
    expenses: BigDecimal,
): BigDecimal = income.subtract(expenses)

/** Returns true when there are no populated periods to plot. */
fun isHomeTrendNoHistory(points: List<HomeTrendPoint>): Boolean = points.none { it.hasActivity() }

/** Returns true when the populated trend contains exactly one monthly period. */
fun isSingleMonthHomeTrend(points: List<HomeTrendPoint>): Boolean =
    points.count { it.hasActivity() } == 1

private fun HomeTrendPoint.hasActivity(): Boolean =
    income.signum() != 0 || expenses.signum() != 0

/**
 * Month labels are normally generated in ISO form, but the fallback keeps the
 * calculation deterministic for imported or display-formatted labels too.
 */
private fun monthSortKey(label: String): String {
    val normalized = label.trim()
    val yearMonth = parseYearMonth(normalized)
    return yearMonth?.toString() ?: normalized.lowercase(Locale.ROOT)
}

private fun parseYearMonth(label: String): YearMonth? {
    val formats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM"),
        DateTimeFormatter.ofPattern("yyyy/MM"),
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH),
    )

    return formats.firstNotNullOfOrNull { formatter ->
        try {
            when (formatter) {
                DateTimeFormatter.ISO_LOCAL_DATE -> YearMonth.from(formatter.parse(label))
                else -> YearMonth.parse(label, formatter)
            }
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
