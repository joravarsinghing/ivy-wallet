package com.ivy.design.system

import org.junit.Assert.*
import org.junit.Test

class FinancialVisibilityTest {
    @Test fun `hidden text never includes an amount currency percentage or sign`() {
        listOf("12,345.67 AED", "-42.00", "+23%", "0.00 USD", "1 / 100 EUR").forEach {
            assertEquals("••••", FinancialVisibility(hidden = true).text(it))
        }
    }
    @Test fun `visible text is unchanged`() {
        val input = "1.234,56 EUR"
        assertEquals(input, FinancialVisibility().text(input))
    }
}
