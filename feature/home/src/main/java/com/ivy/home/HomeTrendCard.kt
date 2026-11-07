package com.ivy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.JvyShapes
import com.ivy.design.system.JvyTypography
import com.ivy.design.system.JvyTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * A reusable, state-driven Home trend card.
 *
 * This component deliberately does not persist preferences or dispatch Home events. The caller
 * owns those decisions and receives the user's range and metric changes through callbacks.
 */
@Composable
fun HomeTrendCard(
    state: HomeTrendState,
    currency: String,
    hideBalance: Boolean,
    onRangeSelected: (HomeTrendRange) -> Unit,
    onMetricToggle: (HomeTrendMetric) -> Unit,
    onOpenEditor: () -> Unit,
    onCloseEditor: () -> Unit,
    modifier: Modifier = Modifier,
    plotStyle: HomeTrendPlotStyle = HomeTrendPlotStyle.Line,
) {
    val hidden = hideBalance || com.ivy.design.system.LocalFinancialVisibility.current.hidden
    // Range chips can change while the previous, explicitly dated result is still loading.
    val intervalSamples = state.points.any { it.periodIdentity is HomeTrendPeriodIdentity.Interval } ||
        state.points.isEmpty() && !state.range.isDaily
    val dailySamples = if (state.points.isEmpty()) state.range.isDaily
        else state.points.all { it.periodIdentity is HomeTrendPeriodIdentity.Day }
    var showValues by rememberSaveable(state.window, state.range) { mutableStateOf(false) }
    val refreshing = state.loading && state.hasData && !hidden && state.visibleMetrics.isNotEmpty()
    val showRefreshHint by produceState(false, refreshing, state.range) {
        value = false
        if (refreshing) {
            delay(350)
            value = true
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(JvyShapes.Card)
            .background(MaterialTheme.colorScheme.surfaceVariant, JvyShapes.Card)
            .testTag("home_trend_card"),
    ) {
        // Reserve the strip's height: slow refresh feedback never overlays or moves the plot.
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (refreshing && showRefreshHint) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp).testTag("home_trend_refresh")
                    .semantics { contentDescription = "Updating money trends" },
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HomeCardHeading(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp),
            heading = {
                Text(
                    text = "Balance trend",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                if (state.window == null) Text(
                    text = when {
                        intervalSamples -> "30 points across this range"
                        dailySamples -> "Daily balance and cash flow"
                        else -> "Period-end balance and cash flow"
                    },
                    style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.window?.let { Text(stringResource(R.string.home_chart_range_caption, it.displayLabel()),
                    style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            action = { JvyTextButton(onOpenEditor) { Text("Edit") } },
        )

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            when {
                hidden -> TrendMessage("Balances hidden. Reveal them to see your trend.")
                state.visibleMetrics.isEmpty() -> {
                    TrendMessage("Choose at least one measure in Edit to see your trend.")
                }

                state.hasData -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TrendLegend(
                            visibleMetrics = state.visibleMetrics,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        TrendPlot(
                            state = state,
                            currency = currency,
                            style = plotStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        state.points
                            .filter { it.isObserved }
                            .takeIf { it.isNotEmpty() }
                            ?.let { observedPoints ->
                            val rangeTotals = HomeTrendPoint(
                                periodLabel = "Selected range",
                                income = observedPoints.fold(BigDecimal.ZERO) { total, point ->
                                    total + point.income
                                },
                                expenses = observedPoints.fold(BigDecimal.ZERO) { total, point ->
                                    total + point.expenses
                                },
                                balance = observedPoints.last().balance,
                            )
                            TrendLatestValues(
                                point = rangeTotals,
                                visibleMetrics = state.visibleMetrics,
                                currency = currency,
                                hideBalance = hidden,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        JvyTextButton(onClick = { showValues = !showValues }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(if (showValues) "Hide values" else "View values")
                        }
                        if (showValues && intervalSamples) Text(
                            "Each point groups a few days. Income and spending are totals for those days; balance is the amount at the end.",
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (showValues) state.points.filter { it.isObserved }.forEach { point ->
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(point.periodLabel, style = MaterialTheme.typography.titleSmall)
                                state.visibleMetrics.forEach { metric -> Text("${metric.label}: ${groupedAmount(metric.value(point))} $currency",
                                    style = JvyTypography.Supporting) }
                            }
                        }
                        when (state.points.count { it.isObserved }) {
                            1 -> {
                                Spacer(Modifier.height(8.dp))
                                TrendMessage(
                                    if (dailySamples) "One observed day. More history is needed to show a trend." else "One observed interval. More history is needed to show a trend."
                                )
                            }
                            2 -> {
                                Spacer(Modifier.height(8.dp))
                                TrendMessage(
                                    if (dailySamples) {
                                        "Two observed days. More history will make the pattern clearer."
                                    } else {
                                        "Two observed intervals. More history will make the pattern clearer."
                                    }
                                )
                            }
                        }
                    }
                }

                state.loading -> {
                    TrendLoadingPlaceholder()
                }

                else -> {
                    TrendMessage("Record your first transaction to start a balance history.")
                }
            }

        }

        Spacer(Modifier.height(18.dp))
    }

    if (state.editorVisible) {
        HomeTrendEditorDialog(
            visibleMetrics = state.visibleMetrics,
            onMetricToggle = onMetricToggle,
            onClose = onCloseEditor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendLegend(
    visibleMetrics: Set<HomeTrendMetric>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HomeTrendMetric.values()
            .filter { it in visibleMetrics }
            .forEach { metric ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = metric.plotColor()
                    Spacer(Modifier.size(width = 20.dp, height = 12.dp).drawWithCache {
                        val dash = if (metric == HomeTrendMetric.EXPENSES)
                            androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())) else null
                        val stroke = 2.dp.toPx()
                        onDrawBehind {
                            drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                                androidx.compose.ui.geometry.Offset(size.width, size.height / 2), stroke, pathEffect = dash)
                        }
                    })
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = metric.label,
                        style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
    }
}


@Composable
private fun TrendLatestValues(point: HomeTrendPoint, visibleMetrics: Set<HomeTrendMetric>, currency: String,
                              hideBalance: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(HomeTrendMetric.BALANCE, HomeTrendMetric.INCOME, HomeTrendMetric.EXPENSES).filter { it in visibleMetrics }.forEach { metric ->
            if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) {
                Column { Text(metric.label, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (hideBalance) "••••" else "${groupedAmount(metric.value(point))} $currency", style = JvyTypography.AmountSmall, color = metric.color()) }
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(metric.label, Modifier.weight(1f), style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (hideBalance) "••••" else "${groupedAmount(metric.value(point))} $currency", Modifier.weight(2f), style = JvyTypography.AmountSmall,
                    color = metric.color(), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

@Composable
private fun TrendMessage(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = text,
        style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TrendLoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp),
        contentAlignment = Alignment.Center,
    ) {
        TrendLoadingPill()
    }
}

@Composable
private fun TrendLoadingPill() {
    Row(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics {
                contentDescription = "Updating money trends"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
        Text(
            text = "Updating",
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun HomeTrendMetric.value(point: HomeTrendPoint): BigDecimal = when (this) {
    HomeTrendMetric.INCOME -> point.income
    HomeTrendMetric.EXPENSES -> point.expenses
    HomeTrendMetric.BALANCE -> point.balance
}


@Composable
private fun HomeTrendMetric.color(): Color = when (this) {
    HomeTrendMetric.INCOME -> MaterialTheme.colorScheme.secondary
    HomeTrendMetric.EXPENSES -> MaterialTheme.colorScheme.error
    HomeTrendMetric.BALANCE -> MaterialTheme.colorScheme.primary
}

private fun groupedAmount(amount: BigDecimal): String =
    String.format(Locale.US, "%,.2f", amount.toDouble())

internal fun compactAxisAmount(amount: BigDecimal): String {
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
