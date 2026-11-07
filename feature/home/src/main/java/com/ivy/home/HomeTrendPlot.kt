package com.ivy.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
@OptIn(ExperimentalAnimationApi::class)
internal fun TrendPlot(state: HomeTrendState, currency: String, style: HomeTrendPlotStyle, modifier: Modifier = Modifier) {
    val scene = remember(state.points, state.visibleMetrics, state.window) { prepareTrendScene(state) }
    // A currency change discards outgoing plots rather than relabelling their old amounts.
    // Hidden-balance mode removes this entire composable, including outgoing animation layers.
    key(currency) {
        val transition = updateTransition(scene, label = "Trend update")
        transition.Crossfade(modifier, tween(JvyMotion.FeedbackMillis)) { frame ->
            // Initial display is complete. Replacement datasets draw on, even after a slow load.
            // This is a reveal of exact cached geometry, not a morph through fictitious values.
            val reveal = remember(transition, frame) {
                HomeTrendRevealMotion(animateOnEntry = transition.currentState != frame)
            }
            if (reveal.animateOnEntry) LaunchedEffect(reveal) {
                reveal.reveal()
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(currency, Modifier.align(Alignment.End), style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TrendOverlayPlot(frame.plot, style, frame.firstLabel, frame.lastLabel,
                    Modifier.fillMaxWidth(), reveal = { reveal.progress.value })
            }
        }
    }
}

private data class CachedTrendPath(val line: Path, val area: Path, val color: Color, val fill: Brush,
                                  val stroke: Stroke, val filled: Boolean)

@Composable
private fun TrendOverlayPlot(band: TrendBand, style: HomeTrendPlotStyle, firstLabel: String, lastLabel: String?,
                             modifier: Modifier = Modifier, reveal: () -> Float) {
    val colors = band.series.associate { it.metric to it.metric.plotColor() }
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val zeroInk = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val verticalInset = with(LocalDensity.current) { JvyTypography.Label.lineHeight.toPx() / 2f }
    val height = 176.dp
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(Modifier.fillMaxWidth().height(height).testTag("home_trend_plot").semantics {
                contentDescription = "Balance line with income and spending filled areas on the same money scale and zero baseline. Areas overlap, not stack. Unknown periods remain gaps; a single observation has no trend line. Exact values follow."
            }.drawWithCache {
                val inset = 4.dp.toPx()
                val topInset = verticalInset.coerceAtLeast(inset).coerceAtMost(size.height / 2f)
                val plotWidth = (size.width - inset * 2).coerceAtLeast(1f)
                val plotHeight = (size.height - topInset * 2).coerceAtLeast(1f)
                val zero = topInset + plotHeight * band.scale.y(BigDecimal.ZERO)
                // Cache paths, strokes and brushes on data/size changes, not animation frames.
                val paths = band.series.flatMap { series ->
                    val color = colors.getValue(series.metric)
                    val isBalance = series.metric == HomeTrendMetric.BALANCE
                    val stroke = Stroke(if (isBalance) 2.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = if (series.metric == HomeTrendMetric.EXPENSES)
                            PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())) else null)
                    // Fade each flow toward zero, not toward the bottom of a much taller balance scale.
                    // This changes opacity only: every series still uses the same money coordinates.
                    var fillTop = zero
                    var fillBottom = zero
                    series.segments.forEach { segment -> segment.forEach { point ->
                        val y = topInset + plotHeight * point.y
                        fillTop = minOf(fillTop, y)
                        fillBottom = maxOf(fillBottom, y)
                    } }
                    val fillHeight = (fillBottom - fillTop).coerceAtLeast(1f)
                    val fill = if (isBalance) Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.01f)))
                    else Brush.verticalGradient(
                        0f to color.copy(alpha = if (fillTop == zero) 0.08f else 0.48f),
                        ((zero - fillTop) / fillHeight).coerceIn(0f, 1f) to color.copy(alpha = 0.08f),
                        1f to color.copy(alpha = if (fillBottom == zero) 0.08f else 0.48f),
                        startY = fillTop, endY = fillTop + fillHeight)
                    series.segments.filter { it.size > 1 }.map { segment ->
                        val line = Path().apply {
                            segment.forEachIndexed { index, point ->
                                val x = inset + plotWidth * point.x
                                val y = topInset + plotHeight * point.y
                                if (index == 0) moveTo(x, y)
                                else if (isBalance) lineTo(x, y)
                                else {
                                    val previous = segment[index - 1]
                                    val previousX = inset + plotWidth * previous.x
                                    val previousY = topInset + plotHeight * previous.y
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
                            !isBalance || style == HomeTrendPlotStyle.ShadedLine)
                    }
                }
                val gridStroke = 0.5.dp.toPx()
                onDrawBehind {
                    repeat(3) { i ->
                        val y = topInset + plotHeight * i / 2f
                        drawLine(grid, Offset(inset, y), Offset(size.width - inset, y), gridStroke)
                    }
                    // Animation reads happen only while drawing; cached paths/brushes do not rebuild.
                    clipRect(right = size.width * reveal().coerceIn(0f, 1f)) {
                        // Areas first; balance last and above every fill.
                        for (index in paths.indices) {
                            val path = paths[index]
                            if (path.filled) drawPath(path.area, path.fill)
                        }
                        for (index in paths.indices) {
                            val path = paths[index]
                            drawPath(path.line, path.color, style = path.stroke)
                        }
                    }
                    // Neutral zero stays legible where both zero-flow series coincide.
                    drawLine(zeroInk, Offset(inset, zero), Offset(size.width - inset, zero), 0.75.dp.toPx())
                }
            })
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).testTag("home_trend_dates"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(firstLabel, Modifier.weight(1f), style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                lastLabel?.let { Text(it, Modifier.weight(1f), style = JvyTypography.Label,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Column(Modifier.width(IntrinsicSize.Max).height(height).testTag("home_trend_axis"),
            verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
            band.scale.ticks.forEach { tick ->
                Text(compactAxisAmount(tick), style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
