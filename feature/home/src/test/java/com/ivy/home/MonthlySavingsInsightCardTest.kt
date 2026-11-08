package com.ivy.home

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlySavingsInsightCardTest {
    @Test
    fun `positive difference is saving`() {
        val insight = calculateMonthlySavingsInsight(
            income = BigDecimal("2400.00"),
            expenses = BigDecimal("1950.00"),
        )

        assertEquals(BigDecimal("450.00"), insight.savings)
        assertEquals(MonthlySavingsStatus.SAVING, insight.status)
    }

    @Test
    fun `equal income and expenses break even`() {
        val insight = calculateMonthlySavingsInsight(
            income = BigDecimal("2400.00"),
            expenses = BigDecimal("2400.00"),
        )

        assertEquals(0, insight.savings.compareTo(BigDecimal.ZERO))
        assertEquals(MonthlySavingsStatus.BREAK_EVEN, insight.status)
    }

    @Test
    fun `expenses above income are reported as overspending`() {
        val insight = calculateMonthlySavingsInsight(
            income = BigDecimal("2400.00"),
            expenses = BigDecimal("2550.00"),
        )

        assertEquals(BigDecimal("-150.00"), insight.savings)
        assertEquals(MonthlySavingsStatus.SPENDING_EXCEEDS_INCOME, insight.status)
    }
}
