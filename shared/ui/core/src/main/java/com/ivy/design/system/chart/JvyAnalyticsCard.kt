package com.ivy.design.system.chart

import com.ivy.base.analytics.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ivy.design.system.*
import com.ivy.ui.money.formatDecimalMoney
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

@Composable
fun JvyAnalyticsCard(card: AnalyticsCard, modifier: Modifier = Modifier, horizontalPadding: androidx.compose.ui.unit.Dp = 24.dp,
                     hidden: Boolean = false, compact: Boolean = false, onRecords: ((ChartValue) -> Unit)? = null) {
    val conceal = hidden || LocalFinancialVisibility.current.hidden
    var expanded by rememberSaveable(card.id) { mutableStateOf(false) }
    var limit by rememberSaveable(card.id) { mutableIntStateOf(20) }
    var showContext by rememberSaveable(card.id) { mutableStateOf(false) }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 8.dp),
            shape = JvyShapes.Card, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(card.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() })
                if (!compact || showContext) Text(card.explanation, modifier = Modifier.widthIn(max = 640.dp), style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (card.series.any { s -> s.values.any { it.value != null } }) card.note?.let { Text(it, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (conceal) {
                    Text(FinancialVisibility.REDACTED, style = JvyTypography.AmountSmall, color = MaterialTheme.colorScheme.onSurface)
                } else {
                    ChartLegend(if (card.kind == ChartKind.WATERFALL) listOf(
                        ChartSeries("Positive step", Ink.INCOME, emptyList()),
                        ChartSeries("Negative step", Ink.SPENDING, emptyList()),
                    ) else card.series)
                    if (card.series.none { s -> s.values.any { it.value != null } }) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 176.dp), contentAlignment = Alignment.CenterStart) {
                            Text(card.note ?: "No chartable observations yet. Your activity will appear here.", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (card.kind == ChartKind.LINE && card.series.all { series ->
                        series.values.zipWithNext().none { (a, b) -> a.value != null && b.value != null }
                    }) {
                        Column(Modifier.fillMaxWidth().heightIn(min = 176.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Not enough consecutive observations for a trend. Latest exact amounts follow.",
                            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        card.series.forEach { series -> series.values.filter { it.value != null }.takeLast(1).forEach { point ->
                            Text("${series.name} · ${point.label}", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurface)
                            Text(displayValue(point.value!!, card.unit), style = JvyTypography.AmountSmall, color = MaterialTheme.colorScheme.onSurface)
                        } }
                        }
                    } else {
                        AnalyticsPlot(card, compact)
                        val dated = card.series.firstOrNull()?.values.orEmpty().filter { it.date != null }
                        if (dated.isNotEmpty()) Text("${dated.first().date} – ${dated.last().end}",
                            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (card.series.any { it.values.isNotEmpty() }) JvyTextButton({ expanded = !expanded }) { Text(if (expanded) "Hide values" else "View values") }
                    if (expanded) {
                        // Bounded disclosure; do not compose an entire all-history result eagerly.
                        val values = remember(card) { card.series.flatMap { series -> series.values.map { series.name to it } } }
                        values.take(limit).forEach { (name, point) ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("$name · ${point.label}", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurface)
                                Text(point.value?.let { displayValue(it, card.unit) } ?: "Not available",
                                    style = JvyTypography.AmountSmall, color = MaterialTheme.colorScheme.onSurface)
                                if (onRecords != null && (card.id in setOf("categories", "category-trends") || point.date != null && card.id in setOf("pace", "calendar", "monthly-flow"))) {
                                    JvyTextButton({ onRecords(point) }) { Text("Open transaction report") }
                                }
                            }
                        }
                        if (values.size > limit) JvyTextButton({ limit += 20 }) { Text("More values") }
                    }
                }
                if (compact) JvyTextButton({ showContext = !showContext }) { Text(if (showContext) "Hide explanation" else "About this chart") }
            }
        }
    }
}

private fun displayValue(value: BigDecimal, unit: String): String = when (unit) {
    "%" -> "${value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()}%"
    "transactions" -> "${value.toPlainString()} transactions"
    else -> formatDecimalMoney(value, unit)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartLegend(series: List<ChartSeries>) {
    val palette = JvyChartColors.palette()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        series.forEach { item ->
            val color = item.ink.color(palette)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.size(24.dp, 12.dp).drawWithCache {
                    val dash = if (item.ink == Ink.SPENDING) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null
                    onDrawBehind { drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.dp.toPx(), pathEffect = dash) }
                })
                Text(item.name, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun Ink.color(palette: JvyChartPalette) = when (this) {
    Ink.BALANCE -> palette.balance
    Ink.INCOME -> palette.income
    Ink.SPENDING -> palette.spending
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AnalyticsPlot(card: AnalyticsCard, compact: Boolean) {
    if (card.kind == ChartKind.LINE) {
        val band = remember(card) { jvyLineBand(card) }
        val values = card.series.firstOrNull()?.values.orEmpty()
        val dates = values.mapNotNull { it.end }
        val format = java.time.format.DateTimeFormatter.ofPattern(if (dates.firstOrNull()?.year != dates.lastOrNull()?.year) "d MMM yy" else "d MMM")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(card.unit, Modifier.align(Alignment.End), style = JvyTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            JvyTrendPlot(band, dates.firstOrNull()?.format(format) ?: values.firstOrNull()?.label.orEmpty(),
                (dates.lastOrNull()?.format(format) ?: values.lastOrNull()?.label)?.takeIf { values.size > 1 })
        }
        return
    }
    val palette = JvyChartColors.palette()
    val ink = card.series.map { it.ink.color(palette) }
    val axis = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .25f)
    val additionalSlice = MaterialTheme.colorScheme.tertiary
    val otherSlice = MaterialTheme.colorScheme.onSurfaceVariant
    val all = card.series.flatMap { it.values }.mapNotNull { it.value }
    val steps = remember(card) { ChartMath.waterfallSteps(card) }
    val domain = if (card.kind == ChartKind.WATERFALL) steps.flatMap { listOf(it.first, it.second) } else all
    val low = minOf(domain.minOrNull() ?: BigDecimal.ZERO, BigDecimal.ZERO)
    val high = maxOf(domain.maxOrNull() ?: BigDecimal.ONE, BigDecimal.ZERO)
    val span = (high - low).takeIf { it.signum() > 0 } ?: BigDecimal.ONE
    fun fraction(value: BigDecimal) = value.subtract(low).divide(span, MathContext.DECIMAL64).toFloat()
    // BigDecimal normalization happens before Float drawing; huge native values stay finite.
    val normalized = remember(card, low, span) { card.series.map { series -> series.values.map { it.value?.let(::fraction) } } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (card.kind != ChartKind.DONUT && card.kind != ChartKind.HEATMAP) {
            Text("${displayValue(low, card.unit)} — ${displayValue(high, card.unit)}", style = JvyTypography.Supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (card.kind == ChartKind.BARS && card.series.size == 1) {
            // Horizontal bars preserve long labels and reflow at 200% text. Exact values are native text.
            card.series.forEachIndexed { index, series ->
                series.values.take(if (compact) 3 else 10).forEach { value ->
                    if (compact) {
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(value.label, Modifier.weight(1f), style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurface)
                            Text(value.value?.let { displayValue(it, card.unit) } ?: "Not available", style = JvyTypography.AmountSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                    Text("${series.name} · ${value.label}", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurface)
                    Text(value.value?.let { displayValue(it, card.unit) } ?: "Not available", style = JvyTypography.AmountSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    }
                    val start = fraction(BigDecimal.ZERO)
                    val end = value.value?.let(::fraction)
                    Spacer(Modifier.fillMaxWidth().height(8.dp).drawWithCache {
                        onDrawBehind {
                            drawRect(axis)
                            if (end != null) drawRect(ink[index], Offset(minOf(start, end) * size.width, 0f), Size(abs(end - start) * size.width, size.height))
                        }
                    })
                }
            }
            if (card.series.any { it.values.size > if (compact) 3 else 10 }) Text("More entries in View values.", style = JvyTypography.Supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        val summary = "${card.title}. ${card.kind.name.lowercase()} chart. Exact observations available in View values."
        Spacer(Modifier.fillMaxWidth().height(if (compact) 128.dp else 176.dp).semantics { contentDescription = summary }.drawWithCache {
            val stroke = Stroke(width = 2.dp.toPx())
            val dashed = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
            val paths = normalized.mapIndexed { seriesIndex, values ->
                Path().apply {
                    var open = false
                    val positions = card.series[seriesIndex].values
                    val firstDate = positions.firstOrNull()?.end?.toEpochDay()
                    val lastDate = positions.lastOrNull()?.end?.toEpochDay()
                    values.forEachIndexed { index, value ->
                        if (value == null) open = false else {
                            val day = positions[index].end?.toEpochDay()
                            val x = if (day != null && firstDate != null && lastDate != null && lastDate > firstDate)
                                (day - firstDate).toFloat() / (lastDate - firstDate).toFloat() * size.width
                            else index.toFloat() / (values.size - 1).coerceAtLeast(1) * size.width
                            val y = size.height * (1 - value)
                            if (open) lineTo(x, y) else { moveTo(x, y); open = true }
                        }
                    }
                }
            }
            val slices = card.series.firstOrNull()?.values.orEmpty()
            val total = slices.mapNotNull { it.value?.takeIf { v -> v.signum() > 0 } }.fold(BigDecimal.ZERO, BigDecimal::add)
            val sliceColors = listOf(palette.balance, palette.income, palette.spending, additionalSlice, otherSlice)
            val waterLow = minOf(BigDecimal.ZERO, steps.minOfOrNull { minOf(it.first, it.second) } ?: BigDecimal.ZERO)
            val waterHigh = maxOf(BigDecimal.ZERO, steps.maxOfOrNull { maxOf(it.first, it.second) } ?: BigDecimal.ONE)
            val waterSpan = (waterHigh - waterLow).takeIf { it.signum() > 0 } ?: BigDecimal.ONE
            fun wy(value: BigDecimal) = (1 - (value - waterLow).divide(waterSpan, MathContext.DECIMAL64).toFloat()) * size.height
            val zeroY = (1 - fraction(BigDecimal.ZERO)) * size.height
            val heatMax = all.maxOfOrNull { it.abs() }?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
            val heat = slices.map { it.value?.let { value -> value.abs().divide(heatMax, MathContext.DECIMAL64).toFloat() } }
            onDrawBehind {
                when (card.kind) {
                    ChartKind.LINE -> {
                        for (i in 0..2) drawLine(axis, Offset(0f, size.height * i / 2), Offset(size.width, size.height * i / 2), 1.dp.toPx())
                        paths.forEachIndexed { i, path -> drawPath(path, ink[i], style = if (card.series[i].ink == Ink.SPENDING) dashed else stroke) }
                    }
                    ChartKind.DONUT -> if (total.signum() > 0) {
                        val diameter = minOf(size.width, size.height) - 24.dp.toPx()
                        val origin = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                        var angle = -90f
                        slices.forEachIndexed { i, point ->
                            val sweep = (point.value ?: BigDecimal.ZERO).divide(total, MathContext.DECIMAL64).toFloat() * 360
                            drawArc(sliceColors[i % sliceColors.size], angle, sweep, false, origin, Size(diameter, diameter), style = Stroke(20.dp.toPx()))
                            angle += sweep
                        }
                    }
                    ChartKind.HEATMAP -> {
                        val values = heat
                        val rows = (values.size + 6) / 7
                        val width = size.width / 7
                        val height = size.height / rows.coerceAtLeast(1)
                        values.forEachIndexed { i, value ->
                            val color = if (slices[i].value?.signum() == -1) palette.income else palette.spending
                            drawRect(if (value == null) axis else color.copy(alpha = .15f + value.coerceIn(0f, 1f) * .85f),
                                Offset((i % 7) * width + 2.dp.toPx(), (i / 7) * height + 2.dp.toPx()), Size((width - 4.dp.toPx()).coerceAtLeast(0f), (height - 4.dp.toPx()).coerceAtLeast(0f)))
                        }
                    }
                    ChartKind.WATERFALL -> {
                        drawLine(axis, Offset(0f, wy(BigDecimal.ZERO)), Offset(size.width, wy(BigDecimal.ZERO)), 1.dp.toPx())
                        val width = size.width / steps.size.coerceAtLeast(1)
                        steps.forEachIndexed { i, (before, after) ->
                            drawRect(if (after >= before) palette.income else palette.spending,
                                Offset(i * width + width * .15f, minOf(wy(before), wy(after))), Size(width * .7f, abs(wy(after) - wy(before))))
                        }
                    }
                    ChartKind.SCATTER -> {
                        drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                        val xValues = normalized.getOrNull(0).orEmpty()
                        val yValues = normalized.getOrNull(1).orEmpty()
                        xValues.forEachIndexed { i, x ->
                            val y = yValues.getOrNull(i)
                            if (x != null && y != null) drawCircle(palette.balance, 3.dp.toPx(), Offset(x * size.width, (1 - y) * size.height))
                        }
                    }
                    ChartKind.BOX -> {
                        val height = size.height / normalized.size.coerceAtLeast(1)
                        normalized.forEachIndexed { i, values ->
                            if (values.size == 5 && values.all { it != null }) {
                                val y = (i + .5f) * height
                                val xs = values.map { it!! * size.width }
                                drawLine(palette.balance, Offset(xs[0], y), Offset(xs[4], y), 2.dp.toPx())
                                drawRect(palette.balance.copy(alpha = .3f), Offset(xs[1], y - height * .25f), Size(xs[3] - xs[1], height * .5f))
                                drawLine(palette.balance, Offset(xs[2], y - height * .3f), Offset(xs[2], y + height * .3f), 2.dp.toPx())
                            }
                        }
                    }
                    ChartKind.BARS -> {
                        val count = normalized.maxOfOrNull { it.size } ?: 0
                        val groupWidth = size.width / count.coerceAtLeast(1)
                        val barWidth = groupWidth * .7f / normalized.size.coerceAtLeast(1)
                        drawLine(axis, Offset(0f, zeroY), Offset(size.width, zeroY), 1.dp.toPx())
                        normalized.forEachIndexed { seriesIndex, values -> values.forEachIndexed { i, value ->
                            if (value != null) {
                                val y = (1 - value) * size.height
                                drawRect(ink[seriesIndex], Offset(i * groupWidth + groupWidth * .15f + seriesIndex * barWidth, minOf(y, zeroY)),
                                    Size(barWidth * .85f, abs(y - zeroY)))
                            }
                        } }
                    }
                }
            }
        })
        if (card.kind == ChartKind.WATERFALL && compact) Text(card.series.firstOrNull()?.values.orEmpty().joinToString(" → ") { it.label },
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (card.kind == ChartKind.WATERFALL && !compact) Text("Steps run left to right in the order shown in View values. Positive steps add; negative steps subtract.",
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (card.kind == ChartKind.WATERFALL && !compact) card.series.firstOrNull()?.values.orEmpty().take(8).forEachIndexed { index, point ->
            Text("${index + 1}. ${point.label} · ${point.value?.let { displayValue(it, card.unit) } ?: "Not available"}",
                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurface)
        }
        if (card.kind == ChartKind.HEATMAP) Text("Dates run left to right, then down. Pale = smaller amounts; stronger = larger. Green indicates refunds. Use View values for dates and signed amounts.",
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (card.kind == ChartKind.DONUT) Text("Slices run clockwise from the top, in the order below.", style = JvyTypography.Supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (card.kind == ChartKind.DONUT) card.series.firstOrNull()?.values.orEmpty().forEachIndexed { i, value ->
            Text("${i + 1}. ${value.label} · ${displayValue(value.value ?: BigDecimal.ZERO, card.unit)}", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
