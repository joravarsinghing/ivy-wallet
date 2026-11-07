package com.ivy.home

import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class HomeTrendPresentationTest {
    @Test fun `axis distinguishes nonzero subunit amounts from zero`() {
        assertEquals("0", compactAxisAmount(BigDecimal.ZERO))
        assertEquals("0.25", compactAxisAmount(BigDecimal("0.25")))
        assertEquals("-0.125", compactAxisAmount(BigDecimal("-0.125")))
        assertEquals("1e-8", compactAxisAmount(BigDecimal("0.00000001")))
        assertEquals("-1.23e-12", compactAxisAmount(BigDecimal("-0.000000000001234")))
    }
    @Test fun `large display values stay bounded without converting money to double`() {
        assertEquals("12.4k", compactAxisAmount(BigDecimal("12350")))
        assertEquals("-1.2m", compactAxisAmount(BigDecimal("-1230000")))
        assertEquals("1b", compactAxisAmount(BigDecimal("1000000000")))
        assertEquals("2.5t", compactAxisAmount(BigDecimal("2500000000000")))
        assertEquals("1e1000", compactAxisAmount(BigDecimal("1e1000")))
    }
    @Test fun `axis formatting never mutates the exact displayed amount source`() {
        val amount = BigDecimal("123456.7890123456789")
        compactAxisAmount(amount)
        assertEquals("123456.7890123456789", amount.toPlainString())
    }
    private fun point(date: String) = HomeTrendPoint("Legacy duplicate date label", BigDecimal.ONE, BigDecimal.ONE,
        BigDecimal.TEN, periodIdentity = HomeTrendPeriodIdentity.Day(LocalDate.parse(date)))
    @Test fun `day axes use actual dates not verbose interval labels`() {
        val points = listOf(point("2026-10-01"), point("2026-10-30"))
        val scene = prepareTrendScene(HomeTrendState(points = points.toImmutableList()))
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        assertEquals(LocalDate.parse("2026-10-01").format(formatter), scene.firstLabel)
        assertEquals(LocalDate.parse("2026-10-30").format(formatter), scene.lastLabel)
    }
    @Test fun `cross year labels retain both years without altering their shared date domain`() {
        val points = listOf(point("2026-12-27"), point("2027-01-02"))
        val scene = prepareTrendScene(HomeTrendState(points = points.toImmutableList()))
        assertTrue(scene.firstLabel.endsWith("26"))
        assertTrue(scene.lastLabel!!.endsWith("27"))
        assertEquals(0f, scene.plot.series.last().segments.single().first().x, 0f)
        assertEquals(1f, scene.plot.series.last().segments.single().last().x, 0f)
    }
}
