package com.ivy.base.analytics

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

enum class ChartKind { BARS, LINE, DONUT, HEATMAP, WATERFALL, SCATTER, BOX }
enum class Ink { BALANCE, INCOME, SPENDING }

data class ChartValue(val label: String, val value: BigDecimal?, val categoryId: UUID? = null,
                      val date: LocalDate? = null, val end: LocalDate? = date)
data class ChartSeries(val name: String, val ink: Ink, val values: List<ChartValue>)
data class AnalyticsCard(
    val id: String, val title: String, val explanation: String, val kind: ChartKind,
    val series: List<ChartSeries>, val unit: String, val advanced: Boolean = false,
    val note: String? = null, val lastIsTotal: Boolean = false,
)
object ChartMath {
    fun waterfallSteps(card: AnalyticsCard): List<Pair<BigDecimal, BigDecimal>> {
        var running = BigDecimal.ZERO
        val values = card.series.firstOrNull()?.values.orEmpty()
        return values.mapIndexed { index, point ->
            val total = card.lastIsTotal || card.id == "waterfall"
            val isTotal = total && index == values.lastIndex
            val before = if (isTotal) BigDecimal.ZERO else running
            running = if (isTotal) point.value ?: before else before + (point.value ?: BigDecimal.ZERO)
            before to running
        }
    }
}
