package com.ivy.legacy.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class CryptoDisplayPrecisionTest {
    private fun withLocale(locale: Locale, block: () -> Unit) {
        val before = Locale.getDefault()
        try { Locale.setDefault(locale); block() } finally { Locale.setDefault(before) }
    }

    @Test fun `default crypto formatter keeps its established precision`() = withLocale(Locale.US) {
        for (value in listOf(0.0, 1.0, 12345.123456789, 0.000000001)) {
            assertEquals(value.formatCrypto(9), value.formatCrypto())
        }
    }

    @Test fun `tiny receiving amounts retain value without padding eighteen trailing digits`() = withLocale(Locale.US) {
        assertEquals("0.000000000001", 0.000000000001.formatCrypto(18).removeSuffix(localDecimalSeparator()))
        assertEquals("0.000000000000000001", 0.000000000000000001.formatCrypto(18).removeSuffix(localDecimalSeparator()))
        assertEquals("12,345.00123", 12345.00123.formatCrypto(18).removeSuffix(localDecimalSeparator()))
    }

    @Test fun `higher precision still uses locale decimal and grouping separators`() = withLocale(Locale.GERMANY) {
        assertEquals("12.345,00123", 12345.00123.formatCrypto(18).removeSuffix(localDecimalSeparator()))
        assertEquals("1", 1.0.formatCrypto(18).removeSuffix(localDecimalSeparator()))
    }
}
