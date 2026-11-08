package com.ivy.design.system.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ivy.base.analytics.*
import com.ivy.design.system.*
import java.math.BigDecimal
import java.math.RoundingMode

data class JvyLineScale(val minimum: BigDecimal, val maximum: BigDecimal) {
    fun y(value: BigDecimal): Float = 1f - (value - minimum).divide(maximum - minimum, 12, RoundingMode.HALF_UP).toFloat()
    val ticks: List<BigDecimal> get() = listOf(maximum, (minimum + maximum) / BigDecimal("2"), minimum)
}
data class JvyLineCoordinate(val x: Float, val y: Float)
data class JvyLineSeries(val ink: Ink, val segments: List<List<JvyLineCoordinate>>)
data class JvyLineBand(val scale: JvyLineScale, val series: List<JvyLineSeries>)

/** Shared Home-style geometry. Null values split paths; quiet observed periods remain real zeroes. */
fun jvyLineBand(card: AnalyticsCard): JvyLineBand {
    val values = card.series.flatMap { it.values }.mapNotNull { it.value }
    val low = values.minOrNull() ?: BigDecimal.ZERO
    val high = values.maxOrNull() ?: BigDecimal.ZERO
    val padding = (high - low).abs().max(high.abs()).max(low.abs()).max(BigDecimal.ONE) * BigDecimal("0.1")
    val scale = JvyLineScale(if (low.signum() < 0) low - padding else BigDecimal.ZERO,
        if (high.signum() > 0) high + padding else if (low.signum() < 0) BigDecimal.ZERO else BigDecimal.ONE)
    val dates = card.series.flatMap { it.values }.mapNotNull { it.end }
    val from = dates.minOrNull()?.toEpochDay()
    val through = dates.maxOrNull()?.toEpochDay()
    return JvyLineBand(scale, card.series.sortedBy { if (it.ink == Ink.BALANCE) 1 else 0 }.map { series ->
        val segments = mutableListOf<List<JvyLineCoordinate>>()
        var segment = mutableListOf<JvyLineCoordinate>()
        series.values.forEachIndexed { index, point ->
            val amount = point.value
            if (amount == null) {
                if (segment.isNotEmpty()) segments += segment
                segment = mutableListOf()
            } else {
                val day = point.end?.toEpochDay()
                val x = if (day != null && from != null && through != null && through > from)
                    (day - from).toFloat() / (through - from).toFloat()
                    else if (series.values.size > 1) index.toFloat() / (series.values.size - 1) else .5f
                segment += JvyLineCoordinate(x, scale.y(amount))
            }
        }
        if (segment.isNotEmpty()) segments += segment
        JvyLineSeries(series.ink, segments)
    })
}

private data class CachedTrendPath(val line: Path, val area: Path, val color: Color, val fill: Brush,
                                  val stroke: Stroke, val filled: Boolean)

@Composable
fun JvyTrendPlot(band: JvyLineBand, firstLabel: String, lastLabel: String?,
                 modifier: Modifier = Modifier, shadeBalance: Boolean = false, reveal: () -> Float = { 1f }) {
    val palette = JvyChartColors.palette()
    val colors = band.series.associate { it.ink to when (it.ink) { Ink.INCOME -> palette.income; Ink.SPENDING -> palette.spending; Ink.BALANCE -> palette.balance } }
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val zeroInk = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val verticalInset = with(LocalDensity.current) { JvyTypography.Label.lineHeight.toPx() / 2f }
    val height = 176.dp
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(Modifier.fillMaxWidth().height(height).testTag("home_trend_plot").semantics {
                contentDescription = "Time-series chart on one money scale. Income and spending areas, when present, overlap rather than stack. Unknown periods remain gaps; isolated observations have no trend line. Exact values are available in the card."
            }.drawWithCache {
                val inset = 4.dp.toPx()
                val topInset = verticalInset.coerceAtLeast(inset).coerceAtMost(size.height / 2f)
                val plotWidth = (size.width - inset * 2).coerceAtLeast(1f)
                val plotHeight = (size.height - topInset * 2).coerceAtLeast(1f)
                val zero = topInset + plotHeight * band.scale.y(BigDecimal.ZERO)
                // Cache paths, strokes and brushes on data/size changes, not animation frames.
                val paths = band.series.flatMap { series ->
                    val color = colors.getValue(series.ink)
                    val isBalance = series.ink == Ink.BALANCE
                    val stroke = Stroke(if (isBalance) 2.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = if (series.ink == Ink.SPENDING)
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
                            !isBalance || shadeBalance)
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
                Text(jvyCompactAxisAmount(tick), style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

internal fun jvyCompactAxisAmount(amount: BigDecimal): String {
    if (amount.signum() == 0) return "0"
    val magnitude = amount.abs()
    if (magnitude < BigDecimal("0.001") || magnitude >= BigDecimal("1e15")) {
        val rounded = amount.round(java.math.MathContext(3)).stripTrailingZeros()
        val exponent = rounded.precision() - rounded.scale() - 1
        return "${rounded.movePointLeft(exponent).toPlainString()}e$exponent"
    }
    val exponent = when {
        magnitude >= BigDecimal("1e12") -> 12
        magnitude >= BigDecimal("1e9") -> 9
        magnitude >= BigDecimal("1e6") -> 6
        magnitude >= BigDecimal("1e3") -> 3
        else -> 0
    }
    val value = amount.movePointLeft(exponent)
    val rounded = if (exponent > 0) value.setScale(1, java.math.RoundingMode.HALF_UP)
        else value.round(java.math.MathContext(4))
    val suffix = when (exponent) { 12 -> "t"; 9 -> "b"; 6 -> "m"; 3 -> "k"; else -> "" }
    return rounded.stripTrailingZeros().toPlainString() + suffix
}
