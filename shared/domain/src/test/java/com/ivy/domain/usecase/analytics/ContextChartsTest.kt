package com.ivy.domain.usecase.analytics

import com.ivy.base.analytics.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class ContextChartsTest {
    private val day = LocalDate.parse("2026-11-07")
    @Test fun `different native currencies never become one total`() {
        val charts = ContextCharts.positions("accounts", "Accounts", "Current", listOf(
            MoneyPosition("Bank", "AED", BigDecimal(100)), MoneyPosition("Cash", "USD", BigDecimal(20))))
        assertEquals(listOf("AED", "USD"), charts.map { it.unit })
        assertEquals(listOf(BigDecimal(100), BigDecimal(20)), charts.map { it.series.single().values.single().value })
    }
    @Test fun `non finite inputs and zero targets stay unknown`() {
        assertNull(ContextCharts.amount(Double.NaN))
        assertNull(ContextCharts.amount(Double.POSITIVE_INFINITY))
        assertNull(ContextCharts.percentage(BigDecimal.ONE, BigDecimal.ZERO))
        assertNull(ContextCharts.percentage(null, BigDecimal.TEN))
        assertNull(ContextCharts.percentage(BigDecimal.ONE, BigDecimal.TEN.negate()))
    }
    @Test fun `progress preserves refunds and overfunding instead of clipping`() {
        assertEquals(0, BigDecimal("150").compareTo(ContextCharts.percentage(BigDecimal(150), BigDecimal(100))))
        assertEquals(0, BigDecimal("-25").compareTo(ContextCharts.percentage(BigDecimal(-25), BigDecimal(100))))
    }
    @Test fun `forecast closing bar is a total not another movement`() {
        val chart = ContextCharts.bridge("forecast", "Forecast", "Existing", "AED", BigDecimal(100), BigDecimal(-125), BigDecimal(-25))
        val steps = ChartMath.waterfallSteps(chart)
        assertEquals(BigDecimal.ZERO to BigDecimal(-25), steps.last())
        assertEquals(steps, AnalyticsEngine.waterfallSteps(chart))
    }
    @Test fun `missing forecast inputs suppress the entire bridge rather than inventing zeros`() {
        val chart = ContextCharts.bridge("forecast", "Forecast", "Existing", "AED", BigDecimal(100), null, BigDecimal(200))
        assertTrue(chart.series.single().values.all { it.value == null })
        assertNotNull(chart.note)
    }
    @Test fun `principal excludes interest includes increases and does not clip credit balances`() {
        val chart = ContextCharts.principal("loan", "Loan", "AED", BigDecimal(100), day.minusDays(60), day, listOf(
            PrincipalEvent(day.minusDays(20), BigDecimal(50), true, false),
            PrincipalEvent(day.minusDays(10), BigDecimal(200), false, false),
            PrincipalEvent(day.minusDays(5), BigDecimal(20), false, true)))
        assertEquals(30, chart.series.single().values.size)
        assertEquals(BigDecimal(-50), chart.series.single().values.last().value)
    }
    @Test fun `undated loan or missing conversion never fabricates historical balances`() {
        assertTrue(ContextCharts.principal("loan", "Loan", "AED", BigDecimal.TEN, null, day, emptyList()).series.single().values.isEmpty())
        val chart = ContextCharts.principal("loan", "Loan", "AED", BigDecimal.TEN, day.minusDays(20), day, listOf(PrincipalEvent(day, null, false, false)))
        assertTrue(chart.series.single().values.isEmpty())
        assertNotNull(chart.note)
    }
    @Test fun `future repayments cannot reduce current principal`() {
        val chart = ContextCharts.principal("loan", "Loan", "AED", BigDecimal(100), day, day,
            listOf(PrincipalEvent(day.plusDays(1), BigDecimal(100), false, false)))
        assertEquals(BigDecimal(100), chart.series.single().values.single().value)
    }
    @Test fun `activity keeps native currencies refunds exact dates and bounded real buckets`() {
        fun row(date: LocalDate, value: String, income: Boolean = false) = AnalyticsEntry(UUID.randomUUID(), date, income, BigDecimal(value), null, "", "")
        val charts = ContextCharts.activity("activity", listOf(row(day.minusDays(90), "100", true) to "AED",
            row(day, "-25") to "AED", row(day, "10") to "USD"))
        val aed = charts.first { it.unit == "AED" }
        assertEquals(30, aed.series.first().values.size)
        assertEquals(BigDecimal(100), aed.series.first().values.mapNotNull { it.value }.fold(BigDecimal.ZERO, BigDecimal::add))
        assertEquals(BigDecimal(-25), aed.series.last().values.last().value)
        assertEquals(day, aed.series.last().values.last().end)
        assertFalse(aed.series.any { it.name == "Balance" })
    }
    @Test fun `no activity produces no decorative chart`() = assertTrue(ContextCharts.activity("empty", emptyList()).isEmpty())
}
