package com.ivy.networth

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import io.mockk.mockk

class WealthCsvExportTest {
    @Test fun `history and summary retain distinct dated file names and immutable content`() {
        val day = LocalDate.of(2026, 11, 1)
        val csv = "Date,Name,Value\r\n2026-11-01,Fictional item,1200.50\r\n"
        assertEquals("Jvy_wealth_history_2026-11-01.csv", WealthCsvExport.create(day, true, csv).fileName)
        assertEquals("Jvy_wealth_2026-11-01.csv", WealthCsvExport.create(day, false, csv).fileName)
        assertEquals(csv, WealthCsvExport.create(day, true, csv).content)
    }
    @Test fun `an unavailable view cannot produce an apparently successful blank export`() {
        assertTrue(runCatching { WealthCsvExport.create(LocalDate.of(2026, 11, 1), false, "") }.isFailure)
    }
    @Test fun `pending export is live viewmodel memory and never enters process saved state`() {
        fun vm() = NetWorthViewModel(mockk(), mockk(), mockk(relaxed = true))
        val retained = vm()
        val request = WealthCsvExport.create(LocalDate.of(2026, 11, 1), true, "Fictional private CSV")
        retained.pendingExport = request
        assertSame(request, retained.pendingExport)
        assertFalse(retained.saveView()!!.joinToString().contains(request.content))
        assertNull(vm().pendingExport)
    }
}
