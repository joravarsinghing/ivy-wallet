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
                com.ivy.design.system.chart.JvyTrendPlot(frame.plot.sharedBand(), frame.firstLabel, frame.lastLabel,
                    Modifier.fillMaxWidth(), shadeBalance = style == HomeTrendPlotStyle.ShadedLine, reveal = { reveal.progress.value })
            }
        }
    }
}


private fun TrendBand.sharedBand() = com.ivy.design.system.chart.JvyLineBand(
    com.ivy.design.system.chart.JvyLineScale(scale.minimum, scale.maximum),
    series.map { series -> com.ivy.design.system.chart.JvyLineSeries(when (series.metric) {
        HomeTrendMetric.INCOME -> com.ivy.base.analytics.Ink.INCOME
        HomeTrendMetric.EXPENSES -> com.ivy.base.analytics.Ink.SPENDING
        HomeTrendMetric.BALANCE -> com.ivy.base.analytics.Ink.BALANCE
    }, series.segments.map { segment -> segment.map { com.ivy.design.system.chart.JvyLineCoordinate(it.x, it.y) } }) })
