package com.ivy.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ivy.design.system.JvyTypography
import com.ivy.design.system.JvyChartColors
import com.ivy.design.system.JvyMotion
import java.math.BigDecimal

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
internal fun HomeTrendMetric.plotColor(): Color = JvyChartColors.palette().let { palette -> when (this) {
    HomeTrendMetric.INCOME -> palette.income
    HomeTrendMetric.EXPENSES -> palette.spending
    HomeTrendMetric.BALANCE -> palette.balance
} }

@Composable
internal fun TrendPlot(state: HomeTrendState, currency: String, style: HomeTrendPlotStyle, modifier: Modifier = Modifier) {
    val scene = remember(state.points, state.visibleMetrics) { prepareTrendScene(state) }
    // A currency change discards outgoing plots rather than relabelling their old amounts.
    // Hidden-balance mode removes this entire composable, including outgoing animation layers.
    key(currency) {
        Crossfade(scene, modifier, tween(JvyMotion.TransitionMillis), label = "Trend update") { frame ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(currency, style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TrendOverlayPlot(frame.plot, style, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(frame.firstLabel, style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    frame.lastLabel?.let { Text(it, style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

private data class CachedTrendPath(val line: Path, val area: Path, val color: Color, val fill: Brush,
                                  val stroke: Stroke, val filled: Boolean, val underlay: Stroke?)

@Composable
private fun TrendOverlayPlot(band: TrendBand, style: HomeTrendPlotStyle, modifier: Modifier) {
    val colors = band.series.associate { it.metric to it.metric.plotColor() }
    val grid = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val zeroInk = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val axisWidth = if (LocalDensity.current.fontScale > 1.3f) 88.dp else 60.dp
    val height = 160.dp
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.width(axisWidth).height(height), verticalArrangement = Arrangement.SpaceBetween) {
            band.scale.ticks.forEach { tick ->
                Text(compactAxisAmount(tick), style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.weight(1f).height(height).semantics {
            contentDescription = "Balance line with income and spending filled areas on the same money scale and zero baseline. Areas overlap, not stack. Unknown periods remain gaps; a single observation has no trend line. Exact values follow."
        }.drawWithCache {
            val inset = 4.dp.toPx()
            val plotWidth = (size.width - inset * 2).coerceAtLeast(1f)
            val plotHeight = (size.height - inset * 2).coerceAtLeast(1f)
            val zero = inset + plotHeight * band.scale.y(BigDecimal.ZERO)
            // Cache paths, strokes and brushes on data/size changes, not animation frames.
            val paths = band.series.flatMap { series ->
                val color = colors.getValue(series.metric)
                val isBalance = series.metric == HomeTrendMetric.BALANCE
                val stroke = Stroke(if (isBalance) 2.75.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round, join = StrokeJoin.Round,
                    pathEffect = if (series.metric == HomeTrendMetric.EXPENSES)
                        PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())) else null)
                val fill = Brush.verticalGradient(if (!isBalance) listOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.7f))
                    else listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.01f)))
                series.segments.filter { it.size > 1 }.map { segment ->
                    val line = Path().apply {
                        segment.forEachIndexed { index, point ->
                            val x = inset + plotWidth * point.x
                            val y = inset + plotHeight * point.y
                            if (index == 0) moveTo(x, y)
                            else if (isBalance) lineTo(x, y)
                            else {
                                val previous = segment[index - 1]
                                val previousX = inset + plotWidth * previous.x
                                val previousY = inset + plotHeight * previous.y
                                val controlWidth = (x - previousX) / 3f
                                // Bounded easing through real samples: no overshoot or invented peaks.
                                cubicTo(previousX + controlWidth, previousY, x - controlWidth, y, x, y)
                            }
                        }
                    }
                    val area = Path().apply {
                        addPath(line)
                        lineTo(inset + plotWidth * segment.last().x, zero)
                        lineTo(inset + plotWidth * segment.first().x, zero)
                        close()
                    }
                    CachedTrendPath(line, area, color, fill, stroke,
                        !isBalance || style == HomeTrendPlotStyle.ShadedLine,
                        if (isBalance) Stroke(4.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round) else null)
                }
            }
            val gridStroke = 1.dp.toPx()
            onDrawBehind {
                repeat(3) { i ->
                    val y = inset + plotHeight * i / 2f
                    drawLine(grid, Offset(inset, y), Offset(size.width - inset, y), gridStroke)
                }
                // Areas first; balance last and above every fill, including values above the balance.
                for (index in paths.indices) {
                    val path = paths[index]
                    if (path.filled) drawPath(path.area, path.fill)
                }
                for (index in paths.indices) {
                    val path = paths[index]
                    // A quiet neutral edge keeps purple readable when a flow crosses the balance.
                    path.underlay?.let { drawPath(path.line, surface, style = it) }
                    drawPath(path.line, path.color, style = path.stroke)
                }
                // Neutral zero stays legible where both zero-flow series coincide.
                drawLine(zeroInk, Offset(inset, zero), Offset(size.width - inset, zero), gridStroke)
            }
        })
    }
}
