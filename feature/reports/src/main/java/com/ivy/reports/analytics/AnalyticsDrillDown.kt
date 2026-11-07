package com.ivy.reports.analytics

import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.reports.ReportFilter
import java.time.ZoneId
import java.util.UUID

/** Narrow the applied query; never fall back from a missing category to an all-category query. */
internal fun analyticsDrillDown(applied: ReportFilter, card: AnalyticsCard, point: ChartValue,
                               board: AnalyticsBoard, categories: List<Category>, zone: ZoneId): ReportFilter? {
    val category = if (card.id in setOf("categories", "category-trends")) {
        val id = point.categoryId ?: UUID.nameUUIDFromBytes("ivy:reports:unspecified-category".toByteArray())
        categories.filter { it.id.value == id }.takeIf { it.isNotEmpty() } ?: return null
    } else applied.categories
    val period = point.date?.let { date ->
        val from = maxOf(date.atStartOfDay(zone).toInstant(), board.from ?: date.atStartOfDay(zone).toInstant())
        val to = minOf((point.end ?: date).plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1),
            board.to ?: (point.end ?: date).plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1))
        if (to < from) return null
        TimePeriod(fromToRange = FromToTimeRange(from, to))
    } ?: applied.period
    return applied.copy(period = period, categories = category,
        trnTypes = if (card.id == "monthly-flow") applied.trnTypes else listOf(TransactionType.EXPENSE))
}
