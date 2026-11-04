package com.ivy.importdata.csvimport.flow

import com.ivy.data.backup.CSVRow
import com.ivy.design.system.FinancialVisibility
import org.junit.Assert.*
import org.junit.Test

class ImportResultPresentationTest {
    private val row = CSVRow(7, listOf("Fictional account", "9456.34", "Bad date"))

    @Test fun `hidden balances cannot leak money or names from arbitrary CSV columns`() {
        val text = failedImportRowText(row, FinancialVisibility(hidden = true))
        assertEquals("Row 7: ••••", text)
        assertFalse(text.contains("9456"))
        assertFalse(text.contains("Fictional"))
    }

    @Test fun `visible row review retains the original column order and line number`() {
        assertEquals("Row 7: Fictional account · 9456.34 · Bad date",
            failedImportRowText(row, FinancialVisibility()))
    }
}
