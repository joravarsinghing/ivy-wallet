package com.ivy.reports.analytics

import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.reports.ReportFilter
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class AnalyticsDrillDownTest {
    private val zone = ZoneId.of("America/New_York")
    private val period = TimePeriod(fromToRange = FromToTimeRange(Instant.parse("2026-10-01T04:00:00Z"), Instant.parse("2026-11-07T12:00:00Z")))
    private val filter = ReportFilter.emptyFilter("AED").copy(period = period, includeKeywords = listOf("bus"), excludeKeywords = listOf("gift"), minAmount = 1.0, maxAmount = 300.0)
    private val card = AnalyticsCard("category-trends", "Category", "", ChartKind.LINE, emptyList(), "AED")
    private val category = Category(CategoryId(UUID.randomUUID()), NotBlankTrimmedString.unsafe("Transport"), ColorInt(0), null, 0.0)
    private val board = AnalyticsBoard("", emptyList(), from = period.fromToRange!!.from, to = period.fromToRange!!.to)

    @Test fun `category drill down preserves existing account tag keyword and amount query primitives`() {
        val point = ChartValue("Transport", BigDecimal.TEN, category.id.value)
        val result = analyticsDrillDown(filter, card, point, board, listOf(category), zone)!!
        assertEquals(listOf(category), result.categories)
        assertEquals(filter.includeKeywords, result.includeKeywords)
        assertEquals(filter.excludeKeywords, result.excludeKeywords)
        assertEquals(filter.minAmount, result.minAmount)
        assertEquals(filter.maxAmount, result.maxAmount)
        assertEquals(filter.accounts, result.accounts)
        assertEquals(filter.includedTags, result.includedTags)
        assertEquals(filter.excludedTags, result.excludedTags)
        assertEquals(filter.period, result.period)
    }
    @Test fun `missing category never broadens into all categories`() {
        assertNull(analyticsDrillDown(filter, card, ChartValue("Missing", BigDecimal.ONE, UUID.randomUUID()), board, listOf(category), zone))
    }
    @Test fun `calendar drill down uses actual local day length across DST`() {
        val date = LocalDate.parse("2026-11-01")
        val point = ChartValue(date.toString(), BigDecimal.TEN, date = date)
        val result = analyticsDrillDown(filter, card.copy(id = "calendar"), point, board, emptyList(), zone)!!
        val range = result.period!!.fromToRange!!
        assertEquals(Instant.parse("2026-11-01T04:00:00Z"), range.from)
        assertEquals(Instant.parse("2026-11-02T04:59:59.999999999Z"), range.to)
    }
    @Test fun `bucket drill down cannot extend beyond source report or today`() {
        val point = ChartValue("Bucket", BigDecimal.TEN, date = LocalDate.parse("2026-09-30"), end = LocalDate.parse("2026-11-08"))
        val result = analyticsDrillDown(filter, card.copy(id = "monthly-flow"), point, board, emptyList(), zone)!!
        assertEquals(period.fromToRange, result.period!!.fromToRange)
    }
}
