package com.ivy.domain.usecase.emergencyfund

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import org.junit.Test

class EmergencyFundCoverageCalculatorTest {

    @Test
    fun `returns no expenses when current period expenses are zero`() {
        val result = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal("2500.00"),
            currentPeriodExpenses = BigDecimal.ZERO,
        )

        result.monthsCovered.shouldBeNull()
        result.status shouldBe EmergencyFundCoverageStatus.NO_EXPENSES
    }

    @Test
    fun `classifies coverage below one month`() {
        val result = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal("499.99"),
            currentPeriodExpenses = BigDecimal("500.00"),
        )

        result.monthsCovered shouldBe BigDecimal("0.99998")
        result.status shouldBe EmergencyFundCoverageStatus.BELOW_ONE_MONTH
    }

    @Test
    fun `classifies one to three months including one month`() {
        val result = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal("1500.00"),
            currentPeriodExpenses = BigDecimal("625.00"),
        )

        result.monthsCovered shouldBe BigDecimal("2.4")
        result.status shouldBe EmergencyFundCoverageStatus.ONE_TO_THREE_MONTHS
    }

    @Test
    fun `classifies three or more months including exactly three`() {
        val result = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal("900.00"),
            currentPeriodExpenses = BigDecimal("300.00"),
        )

        result.monthsCovered shouldBe BigDecimal("3")
        result.status shouldBe EmergencyFundCoverageStatus.THREE_PLUS_MONTHS
    }

    @Test
    fun `preserves exact fractional arithmetic`() {
        val result = calculateEmergencyFundCoverage(
            protectedBuffer = BigDecimal("0.30"),
            currentPeriodExpenses = BigDecimal("0.20"),
        )

        result.monthsCovered shouldBe BigDecimal("1.5")
        result.status shouldBe EmergencyFundCoverageStatus.ONE_TO_THREE_MONTHS
    }
}
