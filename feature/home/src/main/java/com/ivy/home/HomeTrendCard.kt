package com.ivy.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Red
import java.math.BigDecimal
import java.time.LocalDate
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
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(UI.colors.medium, UI.shapes.r3)
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
                    style = UI.typo.b1.style(
                        color = UI.colors.pureInverse,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (state.range.isDaily) "Daily balance and cash flow" else "Period-end balance and cash flow",
                    style = UI.typo.c.style(
                        color = UI.colors.gray,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenEditor)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Edit money trends"
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                text = "Edit",
                style = UI.typo.c.style(
                    color = UI.colors.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
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
                        TrendChart(
                            state = state,
                            currency = currency,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        TrendPeriodLabels(
                            points = state.points,
                            modifier = Modifier.padding(horizontal = 16.dp),
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
                        when (state.points.count { it.hasActivity() }) {
                            1 -> {
                                Spacer(Modifier.height(8.dp))
                                TrendMessage(
                                    if (state.range.isDaily) "One day recorded. More days will make the pattern clearer." else "One month recorded. More months will make the pattern clearer."
                                )
                            }
                            2 -> {
                                Spacer(Modifier.height(8.dp))
                                TrendMessage(
                                    if (state.range.isDaily) {
                                        "Two days with activity. More history will make the pattern clearer."
                                    } else {
                                        "Two periods with activity. More history will make the pattern clearer."
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
                    TrendMessage("Your trend will appear after your first month of activity.")
                }
            }

            if (state.loading && state.hasData) {
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
private fun TrendRangeChip(
    range: HomeTrendRange,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) UI.colors.primary else UI.colors.pure)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                contentDescription = "Show ${range.label} trend"
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = range.label,
            style = UI.typo.c.style(
                color = if (selected) UI.colors.pure else UI.colors.pureInverse,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun TrendLegend(
    visibleMetrics: Set<HomeTrendMetric>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HomeTrendMetric.values()
            .filter { it in visibleMetrics }
            .forEach { metric ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(metric.color(), CircleShape),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = metric.label,
                        style = UI.typo.c.style(
                            color = UI.colors.gray,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
    }
}

@Composable
private fun TrendChart(
    state: HomeTrendState,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val plotPoints = trendPlotPoints(state)
    val hasMultipleActivityPeriods = plotPoints.size > 1
    val values = plotPoints.flatMap { point ->
        buildList {
            if (HomeTrendMetric.INCOME in state.visibleMetrics) add(point.income)
            if (HomeTrendMetric.EXPENSES in state.visibleMetrics) add(point.expenses)
            if (HomeTrendMetric.BALANCE in state.visibleMetrics) add(point.balance)
        }
    }
    val rawMinimum = values.minOrNull() ?: BigDecimal.ZERO
    val rawMaximum = values.maxOrNull() ?: BigDecimal.ONE
    val rawSpread = rawMaximum.subtract(rawMinimum).abs()
    val padding = if (rawSpread.signum() == 0) {
        rawMaximum.abs().max(BigDecimal.ONE).multiply(BigDecimal("0.10"))
    } else {
        rawSpread.multiply(BigDecimal("0.10"))
    }
    // Financial amounts that are all non-negative should keep a meaningful zero baseline.
    // Only pad below zero when the observed data actually contains a negative value.
    val minimum = if (rawMinimum.signum() >= 0) {
        BigDecimal.ZERO
    } else {
        rawMinimum.subtract(padding)
    }
    val maximum = rawMaximum.add(padding)
    val middle = minimum.add(maximum).divide(BigDecimal("2"))
    val spread = maximum.subtract(minimum)
    val chartColor = UI.colors.gray.copy(alpha = 0.22f)
    val metricColors = state.visibleMetrics.associateWith { it.color() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .width(68.dp)
                .height(112.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf(maximum, middle, minimum).forEach { amount ->
                Text(
                    text = "${compactAxisAmount(amount)} $currency",
                    style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
                )
            }
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(112.dp)
                .semantics {
                    contentDescription = "Trend chart for ${state.points.size} ${if (state.range.isDaily) "days" else "months"}. Pre-activity periods are shown as gaps."
                },
        ) {
            repeat(3) { index ->
                val y = size.height * index / 2f
                drawLine(
                    color = chartColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            HomeTrendMetric.values()
                .filter { it in state.visibleMetrics }
                .forEach { metric ->
                    val axisStart = state.points.firstOrNull()?.periodIdentity?.startDate
                    val axisEnd = state.points.lastOrNull()?.periodIdentity?.startDate?.let {
                        if (state.range.isDaily) it else it.plusMonths(1).minusDays(1)
                    }
                    val span = if (axisStart != null && axisEnd != null) {
                        java.time.temporal.ChronoUnit.DAYS.between(axisStart, axisEnd).coerceAtLeast(1)
                    } else 1
                    val points = plotPoints.map { point ->
                        val x = if (axisStart == null || axisEnd == null || axisStart == axisEnd) {
                            size.width / 2f
                        } else {
                            size.width * java.time.temporal.ChronoUnit.DAYS.between(axisStart, point.date) / span.toFloat()
                        }
                        val normalized = metric.value(point)
                            .subtract(minimum)
                            .toFloat() / spread.toFloat()
                        androidx.compose.ui.geometry.Offset(
                            x,
                            size.height - (normalized * size.height),
                        )
                    }
                    points.zipWithNext().forEach { (start, end) ->
                        if (hasMultipleActivityPeriods && start != null && end != null) {
                            drawLine(
                                color = metricColors.getValue(metric),
                                start = start,
                                end = end,
                                strokeWidth = 2.5.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            )
                        }
                    }
                    points.filterNotNull().forEach { point ->
                        drawCircle(
                            color = metricColors.getValue(metric),
                            radius = 4.dp.toPx(),
                            center = point,
                        )
                    }
                }
        }
    }
}

private data class TrendPlotPoint(
    val date: LocalDate,
    val income: BigDecimal,
    val expenses: BigDecimal,
    val balance: BigDecimal,
) {
}

private fun trendPlotPoints(state: HomeTrendState): List<TrendPlotPoint> {
    val dailySamples = state.points
        .flatMap { it.dailySamples }
        .sortedBy { it.date }
    if (!state.range.isDaily && dailySamples.isNotEmpty()) {
        return dailySamples.map {
            TrendPlotPoint(it.date, it.income, it.expenses, it.balance)
        }
    }
    return state.points
        .filter { it.isObserved }
        .mapNotNull { point ->
            point.periodIdentity.startDate?.let {
                TrendPlotPoint(it, point.income, point.expenses, point.balance)
            }
        }
}

private fun HomeTrendPoint.hasActivity(): Boolean =
    isObserved && (income.signum() != 0 || expenses.signum() != 0)

@Composable
private fun TrendPeriodLabels(
    points: List<HomeTrendPoint>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = points.firstOrNull()?.periodLabel.orEmpty(),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
        )
        if (points.size > 1) {
            Text(
                text = points.last().periodLabel,
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun TrendLatestValues(
    point: HomeTrendPoint,
    visibleMetrics: Set<HomeTrendMetric>,
    currency: String,
    hideBalance: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeTrendMetric.values()
            .filter { it in visibleMetrics }
            .forEach { metric ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metric.label,
                        style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (hideBalance) "••••" else "${groupedAmount(metric.value(point))} $currency",
                        style = UI.typo.c.style(
                            color = metric.color(),
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
    }
}

@Composable
private fun TrendMessage(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = text,
        style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
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
            .clip(RoundedCornerShape(18.dp))
            .background(UI.colors.pure.copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics {
                contentDescription = "Updating money trends"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = UI.colors.primary,
            strokeWidth = 2.dp,
        )
        Text(
            text = "Updating",
            style = UI.typo.c.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

private fun HomeTrendMetric.value(point: HomeTrendPoint): BigDecimal = when (this) {
    HomeTrendMetric.INCOME -> point.income
    HomeTrendMetric.EXPENSES -> point.expenses
    HomeTrendMetric.BALANCE -> point.balance
}

private fun HomeTrendMetric.value(point: TrendPlotPoint): BigDecimal = when (this) {
    HomeTrendMetric.INCOME -> point.income
    HomeTrendMetric.EXPENSES -> point.expenses
    HomeTrendMetric.BALANCE -> point.balance
}

@Composable
private fun HomeTrendMetric.color(): Color = when (this) {
    HomeTrendMetric.INCOME -> Green
    HomeTrendMetric.EXPENSES -> Red
    HomeTrendMetric.BALANCE -> UI.colors.primary
}

private fun groupedAmount(amount: BigDecimal): String =
    String.format(Locale.US, "%,.2f", amount.toDouble())

private fun compactAxisAmount(amount: BigDecimal): String {
    val value = amount.toDouble()
    return when {
        kotlin.math.abs(value) >= 1_000_000 -> String.format(Locale.US, "%.1fm", value / 1_000_000)
        kotlin.math.abs(value) >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000)
        else -> String.format(Locale.US, "%.0f", value)
    }
}
