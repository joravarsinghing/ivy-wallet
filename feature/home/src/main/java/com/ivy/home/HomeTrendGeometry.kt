package com.ivy.home

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit

/** Rendering-only projection. Never used for financial totals, storage or exported values. */
internal data class TrendScale(val minimum: BigDecimal, val maximum: BigDecimal) {
    fun y(value: BigDecimal): Float = 1f - (value - minimum)
        .divide(maximum - minimum, 12, RoundingMode.HALF_UP).toFloat()
    val ticks: List<BigDecimal> get() = listOf(maximum, (minimum + maximum) / BigDecimal("2"), minimum)
}

internal data class TrendCoordinate(val x: Float, val y: Float)
internal data class TrendSeries(val metric: HomeTrendMetric, val segments: List<List<TrendCoordinate>>)
internal data class TrendBand(val scale: TrendScale, val series: List<TrendSeries>)
internal data class TrendScene(
    val periods: List<HomeTrendPeriodIdentity>,
    val firstLabel: String,
    val lastLabel: String?,
    val plot: TrendBand,
)

/** One date domain, one money scale and one zero baseline for all visible measures. */
internal fun prepareTrendScene(state: HomeTrendState): TrendScene {
    val segments = observedTrendSegments(state.points)
    val observed = segments.flatten()
    fun plotDate(point: HomeTrendPoint) = (point.periodIdentity as? HomeTrendPeriodIdentity.Interval)?.through
        ?: point.periodIdentity.startDate
    val start = state.window?.from ?: state.points.firstOrNull()?.let(::plotDate)
    val end = state.window?.through ?: state.points.lastOrNull()?.let(::plotDate)
    fun x(entry: TrendObservation): Float {
        val date = plotDate(entry.point)
        return when {
            start != null && end != null && date != null && end > start ->
                ChronoUnit.DAYS.between(start, date).toFloat() / ChronoUnit.DAYS.between(start, end)
            state.points.size > 1 -> entry.index.toFloat() / (state.points.size - 1)
            else -> 0.5f
        }.coerceIn(0f, 1f)
    }
    fun series(metric: HomeTrendMetric, scale: TrendScale): TrendSeries = TrendSeries(metric,
        segments.map { segment -> segment.map { entry ->
            TrendCoordinate(x(entry), scale.y(metric.plotAmount(entry.point)))
        } })

    val metrics = listOf(HomeTrendMetric.INCOME, HomeTrendMetric.EXPENSES, HomeTrendMetric.BALANCE)
        .filter { it in state.visibleMetrics }
    val values = observed.flatMap { entry -> metrics.map { it.plotAmount(entry.point) } }
    val low = values.minOrNull() ?: BigDecimal.ZERO
    val high = values.maxOrNull() ?: BigDecimal.ZERO
    val padding = (high - low).abs().max(high.abs()).max(low.abs())
        .max(BigDecimal.ONE).multiply(BigDecimal("0.1"))
    val scale = TrendScale(if (low.signum() < 0) low - padding else BigDecimal.ZERO,
        if (high.signum() > 0) high + padding else if (low.signum() < 0) BigDecimal.ZERO else BigDecimal.ONE)
    val format = java.time.format.DateTimeFormatter.ofPattern(
        if (start?.year != end?.year) "d MMM yy" else "d MMM", java.util.Locale.getDefault())
    fun axisLabel(point: HomeTrendPoint, first: Boolean): String {
        return when (val identity = point.periodIdentity) {
            is HomeTrendPeriodIdentity.Interval -> (if (first) identity.from else identity.through).format(format)
            is HomeTrendPeriodIdentity.Day -> identity.date.format(format)
            else -> point.periodLabel
        }
    }
    return TrendScene(state.points.map { it.periodIdentity }, state.points.firstOrNull()?.let { axisLabel(it, true) }.orEmpty(),
        state.points.lastOrNull()?.let { axisLabel(it, false) }?.takeIf { state.points.size > 1 },
        TrendBand(scale, metrics.map { series(it, scale) }))
}
