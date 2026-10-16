package com.ivy.home

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSpendingBreakdownTest {
    @Test
    fun `ranks top three and groups the rest`() {
        val state = buildHomeSpendingBreakdown(
            listOf(
                row("Food", "50"),
                row("Rent", "30"),
                row("Travel", "10"),
                row("Other one", "5"),
                row("Other two", "5"),
            )
        )

        assertEquals(listOf("Food", "Rent", "Travel", "Other"), state.rows.map { it.label })
        assertEquals(BigDecimal("100"), state.total)
        assertEquals(BigDecimal("10"), state.rows.last().amount)
        assertEquals(BigDecimal("100.0000"), state.rows.sumOf { it.percentage })
    }

    @Test
    fun `empty or non positive categories produce educational empty state`() {
        val state = buildHomeSpendingBreakdown(listOf(row("Food", "0")))
        assertTrue(!state.hasSpending)
        assertTrue(state.rows.isEmpty())
    }

    private fun row(label: String, amount: String) = HomeSpendingBreakdownRow(
        categoryId = label,
        label = label,
        color = 0,
        amount = BigDecimal(amount),
        percentage = BigDecimal.ZERO,
    )
}
