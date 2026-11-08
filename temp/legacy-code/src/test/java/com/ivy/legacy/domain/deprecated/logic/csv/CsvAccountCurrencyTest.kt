package com.ivy.legacy.domain.deprecated.logic.csv

import com.ivy.legacy.datamodel.Account
import org.junit.Assert.*
import org.junit.Test

class CsvAccountCurrencyTest {
    private val accounts = listOf(Account(name = "Main", currency = "AED", color = 0),
        Account(name = "Rewards", currency = "POINTS", color = 0))

    @Test fun `existing account currency wins when CSV omits it but cannot be explicitly relabelled`() {
        assertEquals("AED", resolveImportedCsvCurrency(" Main ", null, "USD", accounts))
        assertEquals("AED", resolveImportedCsvCurrency("main", " aed ", "USD", accounts))
        assertNull(resolveImportedCsvCurrency("Main", "USD", "AED", accounts))
    }

    @Test fun `existing custom assets retain their unit rather than becoming base currency`() {
        assertEquals("POINTS", resolveImportedCsvCurrency("Rewards", "points", "AED", accounts))
        assertNull(resolveImportedCsvCurrency("Rewards", "AED", "AED", accounts))
    }

    @Test fun `new accounts allow omitted currency but malformed explicit currency never falls back`() {
        assertEquals("AED", resolveImportedCsvCurrency("New", null, "AED", accounts))
        assertNull(resolveImportedCsvCurrency("New", "not a currency", "AED", accounts))
        assertNull(resolveImportedCsvCurrency("", "AED", "AED", accounts))
    }
}
