package com.ivy.domain.usecase.analytics

import com.ivy.base.analytics.*
import com.ivy.domain.usecase.networth.WealthWorkspace
import com.ivy.data.model.wealth.isDebt
import java.math.BigDecimal

object WealthCharts {
    fun history(workspace: WealthWorkspace, itemId: String? = null): AnalyticsCard = history(
        workspace.history, workspace.current.currency, itemId)

    fun history(history: List<com.ivy.domain.usecase.networth.WealthHistoryPoint>, currency: String, itemId: String? = null): AnalyticsCard {
        val count = minOf(30, history.size)
        val values = (0 until count).map { index ->
            val group = history.subList(history.size * index / count, history.size * (index + 1) / count)
            val end = group.last()
            fun amount(point: com.ivy.domain.usecase.networth.WealthHistoryPoint): BigDecimal? = if (itemId == null)
                point.result.netWorth.takeIf { point.result.complete } else point.result.lines.firstOrNull { it.item.id == itemId && it.included }?.reportingAmount
            ChartValue("${group.first().date} – ${end.date}", amount(end).takeIf { group.all { amount(it) != null } },
                date = group.first().date, end = end.date)
        }
        return AnalyticsCard(if (itemId == null) "wealth-history" else "wealth-item-$itemId", if (itemId == null) "Net worth history" else "This item's value over time",
            "The selected history, in $currency, with at most 30 end-of-interval observations. Intervals containing unavailable values remain gaps. Manual valuations carry forward; review stale values before relying on a trend.",
            ChartKind.LINE, listOf(ChartSeries(if (itemId == null) "Net worth" else "Reported item value", Ink.BALANCE, values)), currency)
    }

    fun allocation(workspace: WealthWorkspace): AnalyticsCard? {
        val result = workspace.current
        if (!result.complete) return null
        val positions = result.lines.filter { it.item.parentId == null && it.included && it.reportingAmount?.signum() == 1 }
            .groupBy { it.item.kind }.map { (kind, rows) ->
                ChartValue(kind.name.lowercase().replace('_', ' '), rows.fold(BigDecimal.ZERO) { a, b -> a + b.reportingAmount!! })
            }.sortedByDescending { it.value }
        if (positions.isEmpty()) return null
        val slices = if (positions.size <= 5) positions else positions.take(4) + ChartValue("Other", positions.drop(4).fold(BigDecimal.ZERO) { a, b -> a + b.value!! })
        return AnalyticsCard("wealth-allocation", "Asset mix", "Included top-level assets as of ${result.asOf}. Holdings are not counted twice. Allocation is not spendable cash.",
            ChartKind.DONUT, listOf(ChartSeries("Assets", Ink.INCOME, slices)), result.currency,
            note = result.lines.mapNotNull { it.warning }.distinct().joinToString(" · ").takeIf { it.isNotBlank() })
    }

    fun changes(workspace: WealthWorkspace): AnalyticsCard? = workspace.changes.takeIf { it.isNotEmpty() }?.let { changes ->
        AnalyticsCard("wealth-changes", "What changed since last month", "Value changes from the existing wealth calculator, not claimed investment returns or causes.",
            ChartKind.WATERFALL, listOf(ChartSeries("Change", Ink.BALANCE, changes.map { ChartValue(it.label, it.amount) })), workspace.current.currency)
    }

    fun positions(workspace: WealthWorkspace, debt: Boolean): List<AnalyticsCard> = ContextCharts.positions(
        if (debt) "wealth-debts" else "wealth-assets", if (debt) "Largest debts" else "Largest assets",
        "Included positions as of ${workspace.current.asOf}. Unavailable values stay unknown, not zero.",
        workspace.current.lines.filter { line -> line.item.parentId == null && line.included &&
            ((line.reportingAmount?.signum() ?: if (line.item.kind.isDebt) -1 else 1) < 0) == debt
        }.sortedByDescending { it.reportingAmount?.abs() }.map { MoneyPosition(it.item.name, workspace.current.currency, it.reportingAmount?.abs()) },
        first = if (debt) "Amount owed" else "Value", ink = if (debt) Ink.SPENDING else Ink.INCOME)
}
