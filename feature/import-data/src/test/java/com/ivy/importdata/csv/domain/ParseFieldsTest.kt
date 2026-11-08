package com.ivy.importdata.csv.domain

import com.ivy.importdata.csv.DateMetadata
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ParseFieldsTest {
    @Test fun `amount parsing never removes ambiguous separators or trailing garbage`() {
        for (value in listOf("1,50", "12,34.56", "12.5 AED", "12.5garbage", "1 234,56", "1.2.3", "NaN", "Infinity", "1e309", "10f", "0x1p2")) {
            assertNull(value, parseAmount(value, 1))
        }
        assertEquals(1234.56, parseAmount("1,234.56", 1)!!, 0.0)
        assertEquals(12.5, parseAmount("-12.5", 1)!!, 0.0)
        assertEquals(0.125, parseAmount("12.5", -100)!!, 0.0)
        assertNull(parseAmount("1e308", 100))
    }

    @Test fun `dates obey chosen day order and reject nonexistent calendar dates`() {
        assertEquals(LocalDateTime.of(2026, 2, 3, 0, 0), parseDate("03/02/2026", DateMetadata.DateFirst))
        assertEquals(LocalDateTime.of(2026, 3, 2, 0, 0), parseDate("03/02/2026", DateMetadata.MonthFirst))
        assertNull(parseDate("31/02/2026", DateMetadata.DateFirst))
        assertNull(parseDate("02/29/2026", DateMetadata.MonthFirst))
        assertNull(parseDate("03/02/2026!", DateMetadata.DateFirst))
        assertEquals(LocalDateTime.of(2024, 2, 29, 9, 30), parseDate("29/02/2024 09:30", DateMetadata.DateFirst))
    }
}
