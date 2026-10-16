package com.ivy.home

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds points for the selected range.
 *
 * New callers should provide points with an explicit [HomeTrendPeriodIdentity]. For those
 * points, periods before the first observed activity are unobserved, while periods after
 * activity begins with no source transactions are observed zeroes. The optional [anchorDate]
 * makes the window deterministic when the chart is anchored to a selected day/month.
 *
 * The legacy constructor remains supported for existing snapshots and callers. Its historical
 * trim-and-sort behavior is retained when every point uses the compatibility identity.
 */
fun buildHomeTrendPoints(
    points: List<HomeTrendPoint>,
    range: HomeTrendRange,
    anchorDate: LocalDate? = null,
): List<HomeTrendPoint> {
    if (points.isEmpty()) return emptyList()

    return if (points.all { it.periodIdentity is HomeTrendPeriodIdentity.Legacy }) {
        buildLegacyHomeTrendPoints(points, range)
    } else {
        buildCoveredHomeTrendPoints(points, range, anchorDate)
    }
}

/**
 * Aggregates explicit points into calendar buckets and fills the requested coverage window.
 * This is intentionally pure: it does not read or write transactions.
 */
private fun buildCoveredHomeTrendPoints(
    points: List<HomeTrendPoint>,
    range: HomeTrendRange,
    anchorDate: LocalDate?,
): List<HomeTrendPoint> {
    val explicitPoints = points.filter { it.periodIdentity.startDate != null }
    if (explicitPoints.isEmpty()) return emptyList()

    val anchor = anchorDate ?: explicitPoints.maxOf { it.periodIdentity.startDate!! }
    val end = periodStart(anchor, range.bucket)
    val firstObserved = explicitPoints
        .filter { it.isObserved && it.periodIdentity.bucket.canContributeTo(range.bucket) }
        .minOfOrNull { it.periodIdentity.startDate!! }

    val bucketed = explicitPoints
        .filter { it.periodIdentity.bucket.canContributeTo(range.bucket) }
        .groupBy { periodStart(it.periodIdentity.startDate!!, range.bucket) }
        .mapValues { (_, bucketPoints) ->
            HomeTrendPoint(
                periodLabel = bucketPoints.first().periodLabel,
                income = bucketPoints.fold(BigDecimal.ZERO) { total, point -> total + point.income },
                expenses = bucketPoints.fold(BigDecimal.ZERO) { total, point -> total + point.expenses },
                isObserved = bucketPoints.any { it.isObserved },
                periodIdentity = identityFor(
                    bucket = range.bucket,
                    start = periodStart(bucketPoints.first().periodIdentity.startDate!!, range.bucket),
                ),
                dailySamples = bucketPoints
                    .flatMap { it.dailySamples }
                    .sortedBy { it.date }
                    .toImmutableList(),
            )
        }

    return (range.periodCount - 1 downTo 0).map { offset ->
        val period = shiftPeriod(end, range.bucket, -offset)
        val existing = bucketed[period]
        val beforeActivity = firstObserved == null || period.isBefore(firstObserved)

        when {
            beforeActivity -> existing?.copy(isObserved = false) ?: emptyPoint(period, range.bucket, observed = false)
            existing != null -> existing.copy(isObserved = true)
            else -> emptyPoint(period, range.bucket, observed = true)
        }
    }
}

private fun buildLegacyHomeTrendPoints(
    points: List<HomeTrendPoint>,
    range: HomeTrendRange,
): List<HomeTrendPoint> = points
    .withIndex()
    .sortedWith(
        compareBy<IndexedValue<HomeTrendPoint>> { (_, point) -> point.periodIdentity.startDate }
            .thenBy { it.index },
    )
    .takeLast(range.months)
    .map { it.value }

private fun HomeTrendBucket.canContributeTo(target: HomeTrendBucket): Boolean = when (target) {
    HomeTrendBucket.DAY -> this == HomeTrendBucket.DAY
    HomeTrendBucket.MONTH -> this == HomeTrendBucket.MONTH || this == HomeTrendBucket.DAY
}

private fun periodStart(date: LocalDate, bucket: HomeTrendBucket): LocalDate = when (bucket) {
    HomeTrendBucket.DAY -> date
    HomeTrendBucket.MONTH -> date.withDayOfMonth(1)
}

private fun shiftPeriod(date: LocalDate, bucket: HomeTrendBucket, amount: Int): LocalDate = when (bucket) {
    HomeTrendBucket.DAY -> date.plusDays(amount.toLong())
    HomeTrendBucket.MONTH -> date.plusMonths(amount.toLong()).withDayOfMonth(1)
}

private fun identityFor(
    bucket: HomeTrendBucket,
    start: LocalDate,
): HomeTrendPeriodIdentity = when (bucket) {
    HomeTrendBucket.DAY -> HomeTrendPeriodIdentity.Day(start)
    HomeTrendBucket.MONTH -> HomeTrendPeriodIdentity.Month(YearMonth.from(start))
}

private fun emptyPoint(
    period: LocalDate,
    bucket: HomeTrendBucket,
    observed: Boolean,
): HomeTrendPoint {
    val identity = when (bucket) {
        HomeTrendBucket.DAY -> HomeTrendPeriodIdentity.Day(period)
        HomeTrendBucket.MONTH -> HomeTrendPeriodIdentity.Month(YearMonth.from(period))
    }
    return HomeTrendPoint(
        periodLabel = period.toString(),
        income = BigDecimal.ZERO,
        expenses = BigDecimal.ZERO,
        isObserved = observed,
        periodIdentity = identity,
    )
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

/** Returns true when exactly one calendar bucket has source activity. */
fun isSingleHomeTrendPeriod(points: List<HomeTrendPoint>): Boolean =
    points.count { it.hasActivity() } == 1

private fun HomeTrendPoint.hasActivity(): Boolean =
    isObserved
