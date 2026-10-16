package com.ivy.home

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTrendCalculationTest {
    @Test
    fun `keeps calendar periods in chronological order and trims to requested range`() {
        val points = listOf(
            point("2026-04", income = "100", expenses = "20"),
            point("2026-01", income = "90", expenses = "10"),
            point("2026-03", income = "0", expenses = "0", isObserved = false),
            point("2026-02", income = "80", expenses = "15"),
            point("2026-05", income = "120", expenses = "30"),
        )

        val result = buildHomeTrendPoints(points, HomeTrendRange.THREE_MONTHS)

        assertEquals(listOf("2026-03", "2026-04", "2026-05"), result.map { it.periodLabel })
        assertFalse(result[0].isObserved)
        assertTrue(result[1].isObserved)
    }

    @Test
    fun `keeps a single populated month without requiring history`() {
        val result = buildHomeTrendPoints(
            points = listOf(
                point("2026-10", income = "3850.00", expenses = "335.40"),
            ),
            range = HomeTrendRange.TWELVE_MONTHS,
        )

        assertEquals(1, result.size)
        assertEquals("2026-10", result.single().periodLabel)
        assertFalse(isHomeTrendNoHistory(result))
        assertTrue(isSingleMonthHomeTrend(result))
    }

    @Test
    fun `empty periods are treated as no history`() {
        val points = listOf(
            point("2026-09", income = "0.00", expenses = "0.00", isObserved = false),
            point("2026-10", income = "0", expenses = "0", isObserved = false),
        )

        assertTrue(isHomeTrendNoHistory(points))
        assertFalse(isSingleMonthHomeTrend(points))
        val result = buildHomeTrendPoints(points, HomeTrendRange.SIX_MONTHS)
        assertEquals(2, result.size)
        assertTrue(result.none { it.isObserved })
    }

    @Test
    fun `calculates savings with exact BigDecimal arithmetic`() {
        assertEquals(
            BigDecimal("3514.60"),
            calculateHomeTrendSavings(
                income = BigDecimal("3850.00"),
                expenses = BigDecimal("335.40"),
            ),
        )
    }

    @Test
    fun `supports display formatted month labels deterministically`() {
        val points = listOf(
            point("October 2026", income = "30", expenses = "5"),
            point("August 2026", income = "10", expenses = "2"),
            point("September 2026", income = "20", expenses = "3"),
        )

        val result = buildHomeTrendPoints(points, HomeTrendRange.TWELVE_MONTHS)

        assertEquals(
            listOf("August 2026", "September 2026", "October 2026"),
            result.map { it.periodLabel },
        )
    }

    private fun point(
        periodLabel: String,
        income: String,
        expenses: String,
        isObserved: Boolean = true,
    ) = HomeTrendPoint(
        periodLabel = periodLabel,
        income = BigDecimal(income),
        expenses = BigDecimal(expenses),
        isObserved = isObserved,
    )
}
