package com.ivy.domain.usecase.savings

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import org.junit.Test

class SavingsRateCalculatorTest {

    @Test
    fun `calculates savings and rate for positive income`() {
        val result = calculateSavingsRate(
            income = BigDecimal("2500.00"),
            expenses = BigDecimal("1875.00"),
        )

        result.savingsAmount shouldBe BigDecimal("625.00")
        result.savingsRatePercentage shouldBe BigDecimal("25.0000000000")
        result.status shouldBe SavingsRateStatus.CALCULATED
    }

    @Test
    fun `returns explanatory status when income is zero`() {
        val result = calculateSavingsRate(
            income = BigDecimal.ZERO,
            expenses = BigDecimal("100.00"),
        )

        result.savingsAmount shouldBe BigDecimal("-100.00")
        result.savingsRatePercentage.shouldBeNull()
        result.status shouldBe SavingsRateStatus.ZERO_INCOME
        result.status.explanation shouldBe "Savings rate is unavailable because income is zero."
    }

    @Test
    fun `returns explanatory status when income is negative`() {
        val result = calculateSavingsRate(
            income = BigDecimal("-100.00"),
            expenses = BigDecimal("25.00"),
        )

        result.savingsAmount shouldBe BigDecimal("-125.00")
        result.savingsRatePercentage.shouldBeNull()
        result.status shouldBe SavingsRateStatus.NEGATIVE_INCOME
        result.status.explanation shouldBe "Savings rate is unavailable because income is negative."
    }

    @Test
    fun `preserves decimal arithmetic for fractional amounts`() {
        val result = calculateSavingsRate(
            income = BigDecimal("0.30"),
            expenses = BigDecimal("0.10"),
        )

        result.savingsAmount shouldBe BigDecimal("0.20")
        result.savingsRatePercentage shouldBe BigDecimal("66.6666666667")
        result.status shouldBe SavingsRateStatus.CALCULATED
    }

    @Test
    fun `calculates negative savings rate when expenses exceed positive income`() {
        val result = calculateSavingsRate(
            income = BigDecimal("100.00"),
            expenses = BigDecimal("125.00"),
        )

        result.savingsAmount shouldBe BigDecimal("-25.00")
        result.savingsRatePercentage shouldBe BigDecimal("-25.0000000000")
        result.status shouldBe SavingsRateStatus.CALCULATED
    }
}
