package com.ivy.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ivy.design.system.JvyTypography
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit

enum class HomeTrendPlotStyle { Line, ShadedLine }
internal data class TrendObservation(val index: Int, val point: HomeTrendPoint)

/** Unknown buckets split paths; real zero-activity buckets do not. */
internal fun observedTrendSegments(points: List<HomeTrendPoint>): List<List<TrendObservation>> {
    val result = mutableListOf<List<TrendObservation>>()
    var current = mutableListOf<TrendObservation>()
    points.forEachIndexed { index, point ->
        if (point.isObserved) current += TrendObservation(index, point)
        else if (current.isNotEmpty()) { result += current; current = mutableListOf() }
    }
    if (current.isNotEmpty()) result += current
    return result
}

internal fun HomeTrendMetric.plotAmount(point: HomeTrendPoint): BigDecimal = when (this) {
    HomeTrendMetric.INCOME -> point.income
    HomeTrendMetric.EXPENSES -> point.expenses
    HomeTrendMetric.BALANCE -> point.balance
}

@Composable
internal fun HomeTrendMetric.plotColor(): Color = when (this) {
    HomeTrendMetric.INCOME -> MaterialTheme.colorScheme.secondary
    HomeTrendMetric.EXPENSES -> MaterialTheme.colorScheme.error
    HomeTrendMetric.BALANCE -> MaterialTheme.colorScheme.primary
}

@Composable
internal fun TrendPlot(state: HomeTrendState, currency: String, style: HomeTrendPlotStyle, modifier: Modifier = Modifier) {
    val segments = observedTrendSegments(state.points)
    val values = segments.flatten().flatMap { entry -> state.visibleMetrics.map { it.plotAmount(entry.point) } }
    val low = values.minOrNull() ?: BigDecimal.ZERO
    val high = values.maxOrNull() ?: BigDecimal.ONE
    val padding = (high - low).abs().max(high.abs().multiply(BigDecimal("0.1"))).max(BigDecimal.ONE).multiply(BigDecimal("0.1"))
    val minimum = if (low.signum() >= 0) BigDecimal.ZERO else low - padding
    val maximum = high + padding
    val span = maximum - minimum
    val colors = state.visibleMetrics.associateWith { it.plotColor() }
    val grid = MaterialTheme.colorScheme.outlineVariant
    val startDate = state.points.firstOrNull()?.periodIdentity?.startDate
    val endDate = state.points.lastOrNull()?.periodIdentity?.startDate
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(currency, style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val axisWidth = if (LocalDensity.current.fontScale > 1.3f) 88.dp else 64.dp
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.width(axisWidth).height(136.dp), verticalArrangement = Arrangement.SpaceBetween) {
            listOf(maximum, (minimum + maximum).divide(BigDecimal("2")), minimum).forEach {
                Text(compactAxisAmount(it), style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Canvas(Modifier.weight(1f).height(136.dp).semantics {
            contentDescription = "Balance and cash flow over ${state.points.size} ${if (state.range.isDaily) "days" else "months"}. Unknown periods remain gaps. Exact totals follow the chart."
        }) {
            val inset = 4.dp.toPx()
            val width = (size.width - 2 * inset).coerceAtLeast(1f)
            val top = 4.dp.toPx()
            val height = (size.height - top * 2).coerceAtLeast(1f)
            fun y(value: BigDecimal) = top + height * (1f - (value - minimum).divide(span, 12, RoundingMode.HALF_UP).toFloat())
            val baseline = y(BigDecimal.ZERO).coerceIn(top, top + height)
            repeat(3) { i ->
                val row = top + height * i / 2f
                drawLine(grid, Offset(inset, row), Offset(size.width - inset, row), 1.dp.toPx())
            }
            if (minimum.signum() < 0 && maximum.signum() > 0) drawLine(grid, Offset(inset, baseline), Offset(size.width - inset, baseline), 1.dp.toPx())
            // Balance alone gets shading; income and spending must never look stacked.
            state.visibleMetrics.sortedBy { if (it == HomeTrendMetric.BALANCE) 0 else 1 }.forEach { metric ->
                val color = colors.getValue(metric)
                segments.forEach { segment ->
                    val offsets = segment.map { entry ->
                        val date = entry.point.periodIdentity.startDate
                        val fraction = if (startDate != null && endDate != null && date != null && endDate > startDate)
                            ChronoUnit.DAYS.between(startDate, date).toFloat() / ChronoUnit.DAYS.between(startDate, endDate).toFloat()
                        else if (state.points.size > 1) entry.index.toFloat() / (state.points.size - 1) else 0.5f
                        Offset(inset + width * fraction.coerceIn(0f, 1f), y(metric.plotAmount(entry.point)))
                    }
                    if (offsets.size == 1) drawCircle(color, 3.dp.toPx(), offsets.single())
                    else {
                        val line = Path().apply { moveTo(offsets.first().x, offsets.first().y); offsets.drop(1).forEach { lineTo(it.x, it.y) } }
                        if (style == HomeTrendPlotStyle.ShadedLine && metric == HomeTrendMetric.BALANCE) {
                            val area = Path().apply { addPath(line); lineTo(offsets.last().x, baseline); lineTo(offsets.first().x, baseline); close() }
                            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.01f))))
                        }
                        val effect = when (metric) {
                            HomeTrendMetric.BALANCE -> null
                            HomeTrendMetric.INCOME -> PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx()))
                            HomeTrendMetric.EXPENSES -> PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 3.dp.toPx()))
                        }
                        drawPath(line, color, style = Stroke(if (metric == HomeTrendMetric.BALANCE) 2.5.dp.toPx() else 2.dp.toPx(),
                            cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect))
                    }
                }
            }
        }
    }
    }
}
