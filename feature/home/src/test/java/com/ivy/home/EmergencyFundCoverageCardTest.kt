package com.ivy.home

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class EmergencyFundCoverageCardTest {
    @Test
    fun `zero buffer reports zero coverage`() {
        val coverage = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal.ZERO,
            monthlyExpenses = BigDecimal("2400"),
        )

        assertEquals(BigDecimal.ZERO, coverage.monthsCovered)
        assertEquals(EmergencyFundCoverageLevel.ZERO, coverage.level)
    }

    @Test
    fun `less than one month is low coverage`() {
        val coverage = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal("1200"),
            monthlyExpenses = BigDecimal("2400"),
        )

        assertEquals(BigDecimal("0.50"), coverage.monthsCovered)
        assertEquals(EmergencyFundCoverageLevel.LOW, coverage.level)
    }

    @Test
    fun `one to under three months is medium coverage`() {
        val coverage = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal("4800"),
            monthlyExpenses = BigDecimal("2400"),
        )

        assertEquals(BigDecimal("2.00"), coverage.monthsCovered)
        assertEquals(EmergencyFundCoverageLevel.MEDIUM, coverage.level)
    }

    @Test
    fun `three or more months is strong coverage`() {
        val coverage = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal("7200"),
            monthlyExpenses = BigDecimal("2400"),
        )

        assertEquals(BigDecimal("3.00"), coverage.monthsCovered)
        assertEquals(EmergencyFundCoverageLevel.STRONG, coverage.level)
    }

    @Test
    fun `missing monthly expenses stays safe and reports zero coverage`() {
        val coverage = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal("7200"),
            monthlyExpenses = BigDecimal.ZERO,
        )

        assertEquals(BigDecimal.ZERO, coverage.monthsCovered)
        assertEquals(EmergencyFundCoverageLevel.ZERO, coverage.level)
    }
}
