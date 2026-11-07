package com.ivy.reports.analytics

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class AnalyticsEngineTest {
    private val from = LocalDate.parse("2026-10-01")
    private val category = UUID.randomUUID()
    private fun row(day: Int, amount: String, income: Boolean = false, id: UUID? = category) =
        AnalyticsEntry(UUID.randomUUID(), from.plusDays(day.toLong()), income, BigDecimal(amount), id, "Transport", "Bus")
    private fun values(card: AnalyticsCard) = card.series.first().values

    @Test fun `category totals preserve refunds and exact decimals`() {
        val card = AnalyticsEngine.spending(listOf(row(0, "0.10"), row(0, "0.20"), row(1, "-0.05")), from, from.plusDays(29), "AED").first()
        assertEquals(BigDecimal("0.25"), values(card).single().value)
    }
    @Test fun `negative category has no misleading donut share`() {
        val cards = AnalyticsEngine.spending(listOf(row(0, "-50"), row(1, "20", id = UUID.randomUUID())), from, from.plusDays(29), "AED")
        assertFalse(cards.any { it.kind == ChartKind.DONUT })
    }
    @Test fun `empty report does not fabricate charts`() {
        assertTrue(AnalyticsEngine.spending(emptyList(), from, from, "AED").isEmpty())
        assertTrue(AnalyticsEngine.cashFlow(emptyList(), from, from, "AED").isEmpty())
    }
    @Test fun `cash flow waterfall reconciles income spending and refunds`() {
        val cards = AnalyticsEngine.cashFlow(listOf(row(0, "1000", true), row(1, "200"), row(2, "-50")), from, from.plusDays(29), "AED")
        val steps = values(cards.single { it.id == "waterfall" })
        assertEquals(listOf(BigDecimal("1000"), BigDecimal("-150"), BigDecimal("850")), steps.map { it.value })
    }
    @Test fun `savings rate is undefined without positive income`() {
        assertNull(AnalyticsEngine.savingsRate(BigDecimal.ZERO, BigDecimal.TEN))
        assertNull(AnalyticsEngine.savingsRate(BigDecimal.TEN.negate(), BigDecimal.TEN))
        assertEquals(0, BigDecimal("75").compareTo(AnalyticsEngine.savingsRate(BigDecimal("100"), BigDecimal("25"))))
        assertEquals(0, BigDecimal("110").compareTo(AnalyticsEngine.savingsRate(BigDecimal("100"), BigDecimal("-10"))))
    }
    @Test fun `bucket boundaries cover every date once across leap years and long history`() {
        listOf(1L, 7L, 30L, 31L, 90L, 180L, 366L, 3650L).forEach { days ->
            val start = LocalDate.parse("2024-02-01")
            val end = start.plusDays(days - 1)
            val buckets = AnalyticsEngine.buckets(start, end)
            assertEquals(minOf(days, 30).toInt(), buckets.size)
            assertEquals(start, buckets.first().first)
            assertEquals(end, buckets.last().second)
            assertEquals(days, buckets.sumOf { ChronoUnit.DAYS.between(it.first, it.second) + 1 })
            buckets.zipWithNext().forEach { (a, b) -> assertEquals(a.second.plusDays(1), b.first) }
        }
        assertTrue(AnalyticsEngine.buckets(from, from.minusDays(1)).isEmpty())
    }
    @Test fun `histogram includes maximum exactly once and preserves count`() {
        val bins = AnalyticsEngine.histogram(listOf("1", "20", "20.01", "40", "60", "80", "100").map(::BigDecimal), "AED")
        assertEquals(BigDecimal(7), bins.fold(BigDecimal.ZERO) { a, b -> a + b.value!! })
        assertEquals(BigDecimal(2), bins.last().value)
    }
    @Test fun `refunds do not enter positive purchase distribution`() {
        val cards = AnalyticsEngine.spending(listOf(row(0, "25"), row(1, "-25")), from, from.plusDays(29), "AED")
        val count = values(cards.single { it.id == "sizes" }).fold(BigDecimal.ZERO) { a, b -> a + b.value!! }
        assertEquals(BigDecimal.ONE, count)
    }
    @Test fun `statistical spread requires twenty positive entries per category`() {
        assertFalse(AnalyticsEngine.spending((0..18).map { row(0, "$it") }, from, from, "AED").any { it.kind == ChartKind.BOX })
        val cards = AnalyticsEngine.spending((1..20).map { row(0, "$it") }, from, from, "AED")
        val box = cards.single { it.kind == ChartKind.BOX }
        assertEquals(0, BigDecimal("10.50").compareTo(values(box)[2].value))
        assertTrue(box.advanced)
    }
    @Test fun `pre-observation dates remain unknown and quiet later dates are zero`() {
        val cards = AnalyticsEngine.cashFlow(listOf(row(5, "30")), from, from.plusDays(9), "AED")
        val heat = values(cards.single { it.kind == ChartKind.HEATMAP })
        assertTrue(heat.take(5).all { it.value == null })
        assertEquals(BigDecimal("30"), heat[5].value)
        assertEquals(BigDecimal.ZERO, heat.last().value)
    }
    @Test fun `cumulative spending keeps prehistory gaps and reconciles the final total`() {
        val cards = AnalyticsEngine.spending(listOf(row(5, "30"), row(6, "-10")), from, from.plusDays(29), "AED")
        val pace = values(cards.single { it.id == "pace" })
        assertNull(pace.first().value)
        assertEquals(BigDecimal("20"), pace.last().value)
        assertEquals(30, pace.size)
    }
    @Test fun `all history totals are retained without unbounded plotted points`() {
        val rows = (0..40).map { offset -> row(0, "100", true).copy(date = from.plusMonths(offset.toLong())) }
        val cards = AnalyticsEngine.cashFlow(rows, from, from.plusMonths(40), "AED")
        val flow = cards.single { it.id == "monthly-flow" }.series.first().values
        assertEquals(30, flow.size)
        assertEquals(BigDecimal("4100"), flow.mapNotNull { it.value }.fold(BigDecimal.ZERO, BigDecimal::add))
    }
    @Test fun `more than five categories use Other without changing shares`() {
        val rows = (1..8).map { row(0, "$it", id = UUID.randomUUID()) }
        val slices = values(AnalyticsEngine.spending(rows, from, from, "AED").single { it.kind == ChartKind.DONUT })
        assertEquals(5, slices.size)
        assertEquals("Other", slices.last().label)
        assertEquals(BigDecimal(36), slices.fold(BigDecimal.ZERO) { a, b -> a + b.value!! })
    }
    @Test fun `cash flow final waterfall bar is a total and wealth changes remain increments`() {
        fun card(id: String) = AnalyticsCard(id, "Test", "", ChartKind.WATERFALL,
            listOf(ChartSeries("Steps", Ink.BALANCE, listOf("100", "-25", "75").map { ChartValue("Step", BigDecimal(it)) })), "AED")
        val flow = AnalyticsEngine.waterfallSteps(card("waterfall"))
        assertEquals(BigDecimal.ZERO to BigDecimal("75"), flow.last())
        val changes = AnalyticsEngine.waterfallSteps(card("wealth-changes"))
        assertEquals(BigDecimal("75") to BigDecimal("150"), changes.last())
    }
}
