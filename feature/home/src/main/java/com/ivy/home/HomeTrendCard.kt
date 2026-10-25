package com.ivy.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.util.Locale

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
    var showValues by rememberSaveable(state.range) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(JvyShapes.Card)
            .background(MaterialTheme.colorScheme.surfaceVariant, JvyShapes.Card)
            .testTag("home_trend_card"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Balance trend",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (state.range.isDaily) "Daily balance and cash flow" else "Period-end balance and cash flow",
                    style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            JvyTextButton(onOpenEditor) { Text("Edit") }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeTrendRange.values().forEach { range ->
                TrendRangeChip(
                    range = range,
                    selected = range == state.range,
                    onClick = { onRangeSelected(range) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            when {
                hideBalance -> TrendMessage("Balances hidden. Reveal them to see your trend.")
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
                                hideBalance = hideBalance,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        JvyTextButton(onClick = { showValues = !showValues }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(if (showValues) "Hide values" else "View values")
                        }
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
                                    if (state.range.isDaily) "One observed day. More history is needed to show a trend." else "One observed month. More history is needed to show a trend."
                                )
                            }
                            2 -> {
                                Spacer(Modifier.height(8.dp))
                                TrendMessage(
                                    if (state.range.isDaily) {
                                        "Two observed days. More history will make the pattern clearer."
                                    } else {
                                        "Two observed months. More history will make the pattern clearer."
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

            if (!hideBalance && state.visibleMetrics.isNotEmpty() && state.loading && state.hasData) {
                TrendLoadingOverlay()
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

@Composable
private fun TrendRangeChip(range: HomeTrendRange, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected, onClick, label = { Text(range.label) }, shape = JvyShapes.Control)
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
                    Canvas(Modifier.size(width = 20.dp, height = 12.dp)) {
                        if (metric != HomeTrendMetric.BALANCE) drawRect(color.copy(alpha = 0.3f))
                        drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                            androidx.compose.ui.geometry.Offset(size.width, size.height / 2), 2.dp.toPx(),
                            pathEffect = if (metric == HomeTrendMetric.EXPENSES) androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(5.dp.toPx(), 3.dp.toPx())) else null)
                    }
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
private fun TrendLoadingOverlay() {
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
            .clip(JvyShapes.Control)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
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
    val value = amount.toDouble()
    return when {
        kotlin.math.abs(value) >= 1_000_000 -> String.format(Locale.US, "%.1fm", value / 1_000_000)
        kotlin.math.abs(value) >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000)
        else -> String.format(Locale.US, "%.0f", value)
    }
}
