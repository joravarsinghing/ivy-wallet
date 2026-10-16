package com.ivy.domain.usecase.safetospend

import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import org.junit.Test

class SafeToSpendCalculatorTest {

    @Test
    fun `returns positive safe amount when balance is above protected buffer`() {
        val result = calculateSafeToSpend(
            currentBalance = BigDecimal("2500.00"),
            protectedBuffer = BigDecimal("1000.00"),
        )

        result.safeToSpend shouldBe BigDecimal("1500.00")
        result.status shouldBe SafeToSpendStatus.POSITIVE
        result.status.explanation shouldBe
            "This amount can be spent while keeping your protected buffer intact."
    }

    @Test
    fun `returns zero when balance equals protected buffer`() {
        val result = calculateSafeToSpend(
            currentBalance = BigDecimal("1000.00"),
            protectedBuffer = BigDecimal("1000.00"),
        )

        result.safeToSpend shouldBe BigDecimal("0.00")
        result.status shouldBe SafeToSpendStatus.ZERO
        result.status.explanation shouldBe
            "Your balance matches your protected buffer, so there is nothing safe to spend."
    }

    @Test
    fun `returns negative amount when protected buffer exceeds balance`() {
        val result = calculateSafeToSpend(
            currentBalance = BigDecimal("750.00"),
            protectedBuffer = BigDecimal("1000.00"),
        )

        result.safeToSpend shouldBe BigDecimal("-250.00")
        result.status shouldBe SafeToSpendStatus.BUFFER_EXCEEDED
        result.status.explanation shouldBe
            "Your protected buffer is larger than your balance, so spending should pause until the balance recovers."
    }

    @Test
    fun `preserves exact decimal arithmetic`() {
        val result = calculateSafeToSpend(
            currentBalance = BigDecimal("100.305"),
            protectedBuffer = BigDecimal("0.105"),
        )

        result.safeToSpend shouldBe BigDecimal("100.200")
        result.status shouldBe SafeToSpendStatus.POSITIVE
    }
}
