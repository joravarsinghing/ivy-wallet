package com.ivy.data.repository

import org.junit.Assert.*
import org.junit.Test

class WalletCurrencySafetyTest {
    @Test fun `existing implicit amounts cannot be reinterpreted`() {
        try {
            requireSafeWalletCurrencyChange("AED", "USD", true)
            fail("Currency change should be blocked")
        } catch (_: WalletCurrencyChangeBlocked) { }
    }
    @Test fun `same currency and case changes do not prevent normal settings edits`() {
        requireSafeWalletCurrencyChange("AED", "AED", true)
        requireSafeWalletCurrencyChange("AED", "aed", true)
    }
    @Test fun `first-run confirmation establishes units without pretending to convert`() {
        requireSafeWalletCurrencyChange(null, "AED", true)
    }
    @Test fun `independent reporting currency can change without implicit records`() {
        requireSafeWalletCurrencyChange("AED", "USD", false)
    }
    @Test(expected = IllegalArgumentException::class)
    fun `empty currency is not accepted even in an empty wallet`() {
        requireSafeWalletCurrencyChange(null, " ", false)
    }
}
