package com.ivy.home

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class HomeTrendPlotTest {
    private fun point(observed: Boolean = true) = HomeTrendPoint("Oct", BigDecimal.ZERO, BigDecimal.ZERO,
        BigDecimal("100"), isObserved = observed)
    @Test fun `unknown buckets split the path rather than bridging missing history`() {
        val segments = observedTrendSegments(listOf(point(), point(false), point(), point()))
        assertEquals(listOf(listOf(0), listOf(2, 3)), segments.map { it.map(TrendObservation::index) })
    }
    @Test fun `quiet observed buckets remain part of the same line`() {
        assertEquals(3, observedTrendSegments(listOf(point(), point(), point())).single().size)
    }
    @Test fun `empty and unobserved histories produce no chart paths`() {
        assertTrue(observedTrendSegments(emptyList()).isEmpty())
        assertTrue(observedTrendSegments(listOf(point(false))).isEmpty())
    }
    @Test fun `bar percentage is converted to a fraction rather than clamped at one`() {
        assertEquals(0.43f, spendingBarFraction(BigDecimal("43")), 0.00001f)
        assertEquals(0.26f, spendingBarFraction(BigDecimal("26")), 0.00001f)
        assertEquals(0f, spendingBarFraction(BigDecimal.ZERO), 0f)
        assertEquals(1f, spendingBarFraction(BigDecimal("100")), 0f)
    }
    @Test fun `chart measures keep balance distinct from cash flows`() {
        val p = HomeTrendPoint("Oct", BigDecimal("1000"), BigDecimal("400"), BigDecimal("5000"))
        assertEquals(BigDecimal("5000"), HomeTrendMetric.BALANCE.plotAmount(p))
        assertEquals(BigDecimal("1000"), HomeTrendMetric.INCOME.plotAmount(p))
        assertEquals(BigDecimal("400"), HomeTrendMetric.EXPENSES.plotAmount(p))
    }
}
