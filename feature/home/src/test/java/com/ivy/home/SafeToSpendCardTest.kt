package com.ivy.home

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class SafeToSpendCardTest {
    @Test
    fun `balance above buffer returns the safe difference`() {
        val result = calculateSafeToSpend(
            balance = BigDecimal("2400.00"),
            protectedBuffer = BigDecimal("900.00"),
        )

        assertEquals(BigDecimal("1500.00"), result.amount)
        assertEquals(SafeToSpendStatus.SAFE, result.status)
    }

    @Test
    fun `balance equal to buffer returns zero`() {
        val result = calculateSafeToSpend(
            balance = BigDecimal("900.00"),
            protectedBuffer = BigDecimal("900.00"),
        )

        assertEquals(0, result.amount.compareTo(BigDecimal.ZERO))
        assertEquals(SafeToSpendStatus.ZERO, result.status)
    }

    @Test
    fun `balance below buffer clamps amount to zero and explains the shortfall`() {
        val result = calculateSafeToSpend(
            balance = BigDecimal("650.00"),
            protectedBuffer = BigDecimal("900.00"),
        )

        assertEquals(0, result.amount.compareTo(BigDecimal.ZERO))
        assertEquals(SafeToSpendStatus.BUFFER_EXCEEDED, result.status)
    }
}
