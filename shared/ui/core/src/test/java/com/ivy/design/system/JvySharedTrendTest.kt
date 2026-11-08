package com.ivy.design.system

import com.ivy.base.analytics.*
import com.ivy.design.system.chart.*
import androidx.compose.ui.graphics.luminance
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class JvySharedTrendTest {
    @Test fun `cash flow tiles have readable foregrounds in every theme`() {
        listOf(JvySummaryColors.Income to JvySummaryColors.OnIncome, JvySummaryColors.Expenses to JvySummaryColors.OnExpenses).forEach { (bg, fg) ->
            val ratio = (maxOf(bg.luminance(), fg.luminance()) + .05f) / (minOf(bg.luminance(), fg.luminance()) + .05f)
            assertTrue(ratio >= 4.5f)
        }
    }
    @Test fun `shared renderer preserves gaps signs equal scales and actual dates`() {
        val date = LocalDate.parse("2026-11-01")
        val values = listOf(ChartValue("first", BigDecimal.TEN, date = date), ChartValue("gap", null, date = date.plusDays(1)),
            ChartValue("refund", BigDecimal(-5), date = date.plusDays(9)))
        val card = AnalyticsCard("test", "Activity", "", ChartKind.LINE,
            listOf(ChartSeries("Income", Ink.INCOME, values), ChartSeries("Spent", Ink.SPENDING, values)), "AED")
        val band = jvyLineBand(card)
        assertEquals(2, band.series.first().segments.size)
        assertEquals(band.series.first().segments, band.series.last().segments)
        assertEquals(0f, band.series.first().segments.first().first().x, .0001f)
        assertEquals(1f, band.series.first().segments.last().first().x, .0001f)
        assertTrue(band.scale.minimum < BigDecimal.ZERO)
    }
}
