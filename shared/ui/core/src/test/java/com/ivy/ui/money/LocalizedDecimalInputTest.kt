package com.ivy.ui.money

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class LocalizedDecimalInputTest {
    @Test fun `comma decimals are not silently multiplied by one hundred`() {
        assertEquals("1.50", canonicalDecimalInput("1,50", Locale.GERMANY))
        assertNull(canonicalDecimalInput("1,50", Locale.US))
    }
    @Test fun `valid grouping follows the selected locale`() {
        assertEquals("1234.56", canonicalDecimalInput("1,234.56", Locale.US))
        assertEquals("1234.56", canonicalDecimalInput("1.234,56", Locale.GERMANY))
        assertEquals("1234.56", canonicalDecimalInput("1 234,56", Locale.FRANCE))
        assertNull(canonicalDecimalInput("1.234,56", Locale.US))
        assertNull(canonicalDecimalInput("1,234.56", Locale.GERMANY))
        assertNull(canonicalDecimalInput("12,34.56", Locale.US))
    }
    @Test fun `canonical storage preserves a trailing decimal and exact precision`() {
        assertEquals("", canonicalDecimalInput("", Locale.US))
        assertEquals("12.", canonicalDecimalInput("12,", Locale.GERMANY))
        assertEquals("0.12345678", canonicalDecimalInput("0,12345678", Locale.GERMANY, 8))
        assertNull(canonicalDecimalInput("0,123", Locale.GERMANY, 2))
        assertNull(canonicalDecimalInput("9".repeat(25), Locale.US))
        assertNull(canonicalDecimalInput("1e3", Locale.US))
    }
    @Test fun `localized digits normalize without floating point conversion`() {
        assertEquals("123.45", canonicalDecimalInput("١٢٣٫٤٥", Locale.forLanguageTag("ar-AE")))
    }
    @Test fun `display and cursor mappings use matching localized separators`() {
        listOf(Locale.US to "1234.50", Locale.GERMANY to "1234,50").forEach { (locale, draft) ->
            val transformed = GroupedLocalizedDecimalInput(locale).filter(AnnotatedString(draft))
            assertEquals(if (locale == Locale.US) "1,234.50" else "1.234,50", transformed.text.text)
            (0..draft.length).forEach { offset ->
                val displayOffset = transformed.offsetMapping.originalToTransformed(offset)
                assertTrue(displayOffset in 0..transformed.text.length)
                assertEquals(offset, transformed.offsetMapping.transformedToOriginal(displayOffset))
            }
        }
    }
}
