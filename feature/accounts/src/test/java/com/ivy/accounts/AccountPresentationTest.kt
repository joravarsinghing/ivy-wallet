package com.ivy.accounts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class AccountPresentationTest {
    @Test fun `all hidden balances are redacted including missing values`() {
        assertEquals("••••", accountMoney(BigDecimal("1250000.50"), "AED", true))
        assertEquals("••••", accountMoney(null, "AED", true))
    }
    @Test fun `unknown is not silently formatted as zero`() {
        assertEquals("Balance unavailable", accountMoney(null, "AED", false))
        assertEquals("0.00 AED", accountMoney(BigDecimal.ZERO, "AED", false))
    }
    @Test fun `amounts retain grouped two decimal formatting and negative balances`() {
        assertEquals("1,250,000.50 AED", accountMoney(BigDecimal("1250000.5"), "AED", false))
        assertEquals("-500.00 AED", accountMoney(BigDecimal("-500"), "AED", false))
    }
    @Test fun `account icons stay visible without changing readable user colors`() {
        val green = Color(0xFF176C50)
        assertEquals(green, accountAccent(green, Color.White))
        listOf(Color.White, Color(0xFF1C1C21), Color.Black).forEach { surface ->
            listOf(green, Color.White, Color.Black, Color.Transparent, Color.Yellow).forEach { chosen ->
                val ink = accountAccent(chosen, surface)
                val contrast = (maxOf(ink.luminance(), surface.luminance()) + .05f) /
                    (minOf(ink.luminance(), surface.luminance()) + .05f)
                assertTrue("Icon contrast $contrast", contrast >= 3f)
            }
        }
    }
}
