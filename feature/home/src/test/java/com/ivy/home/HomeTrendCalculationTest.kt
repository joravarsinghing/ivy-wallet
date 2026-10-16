package com.ivy.home

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
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

    @Test
    fun `explicit identities sort by period identity rather than display label`() {
        val result = buildHomeTrendPoints(
            points = listOf(
                explicitMonth("December label", "2026-12", income = "30"),
                explicitMonth("January label", "2027-01", income = "40"),
                explicitMonth("November label", "2026-11", income = "20"),
            ),
            range = HomeTrendRange.THREE_MONTHS,
        )

        assertEquals(
            listOf("2026-11", "2026-12", "2027-01"),
            result.map { (it.periodIdentity as HomeTrendPeriodIdentity.Month).yearMonth.toString() },
        )
    }

    @Test
    fun `daily ranges produce daily buckets`() {
        val result = buildHomeTrendPoints(
            points = listOf(
                explicitDay("Oct 1", "2026-10-01", income = "100"),
                explicitDay("Oct 3", "2026-10-03", expenses = "20"),
            ),
            range = HomeTrendRange.SEVEN_DAYS,
            anchorDate = LocalDate.parse("2026-10-03"),
        )

        assertEquals(7, result.size)
        assertEquals(
            LocalDate.parse("2026-09-27"),
            (result.first().periodIdentity as HomeTrendPeriodIdentity.Day).date,
        )
        assertEquals(
            LocalDate.parse("2026-10-03"),
            (result.last().periodIdentity as HomeTrendPeriodIdentity.Day).date,
        )
        assertTrue(result[0].isObserved.not())
        assertTrue(result[4].isObserved)
        assertEquals(BigDecimal.ZERO, result[5].income)
        assertEquals(BigDecimal("20"), result.last().expenses)
    }

    @Test
    fun `thirty day range keeps pre activity null and fills later quiet days with zeroes`() {
        val result = buildHomeTrendPoints(
            points = listOf(
                explicitDay("first entry", "2026-10-01", income = "100"),
            ),
            range = HomeTrendRange.THIRTY_DAYS,
            anchorDate = LocalDate.parse("2026-10-10"),
        )

        assertEquals(30, result.size)
        assertTrue(result.take(20).none { it.isObserved })
        assertTrue(result.drop(20).all { it.isObserved })
        assertEquals(BigDecimal("100"), result[20].income)
        assertTrue(result.drop(21).all { it.income == BigDecimal.ZERO && it.expenses == BigDecimal.ZERO })
    }

    @Test
    fun `monthly coverage aggregates daily points and fills post activity months`() {
        val result = buildHomeTrendPoints(
            points = listOf(
                explicitDay("Jan 1", "2027-01-01", income = "100"),
                explicitDay("Jan 20", "2027-01-20", expenses = "20"),
            ),
            range = HomeTrendRange.THREE_MONTHS,
            anchorDate = LocalDate.parse("2027-02-15"),
        )

        assertEquals(3, result.size)
        assertFalse(result[0].isObserved)
        assertTrue(result[1].isObserved)
        assertEquals(BigDecimal("100"), result[1].income)
        assertEquals(BigDecimal("20"), result[1].expenses)
        assertTrue(result[2].isObserved)
        assertEquals(BigDecimal.ZERO, result[2].income)
    }

    @Test
    fun `single explicit point is retained without inventing a trend`() {
        val result = buildHomeTrendPoints(
            points = listOf(explicitDay("only", "2026-10-01", income = "50")),
            range = HomeTrendRange.SEVEN_DAYS,
        )

        assertEquals(7, result.size)
        assertEquals(1, result.count { it.isObserved && it.income == BigDecimal("50") })
        assertTrue(isSingleHomeTrendPeriod(result))
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

    private fun explicitDay(
        label: String,
        date: String,
        income: String = "0",
        expenses: String = "0",
    ) = HomeTrendPoint(
        periodLabel = label,
        income = BigDecimal(income),
        expenses = BigDecimal(expenses),
        periodIdentity = HomeTrendPeriodIdentity.Day(LocalDate.parse(date)),
    )

    private fun explicitMonth(
        label: String,
        month: String,
        income: String = "0",
        expenses: String = "0",
    ) = HomeTrendPoint(
        periodLabel = label,
        income = BigDecimal(income),
        expenses = BigDecimal(expenses),
        periodIdentity = HomeTrendPeriodIdentity.Month(YearMonth.parse(month)),
    )
}
