package com.ivy.legacy.domain.deprecated.logic.csv

import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType
import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.StandardTimeConverter
import io.mockk.every
import io.mockk.mockk
import com.ivy.wallet.domain.deprecated.logic.csv.CSVMapper
import com.ivy.wallet.domain.deprecated.logic.csv.model.RowMapping
import com.opencsv.CSVReaderBuilder
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId

class CsvValueParsingTest {
    private fun mapping(dateOnlyFormat: String? = null) = RowMapping(
        amount = 1, account = 3, date = 0, category = null, title = null,
        dateOnlyFormat = dateOnlyFormat)

    @Test fun `unknown or absent dates do not receive an invented historical timestamp`() {
        listOf(null, "", "not a date", "2026-02-30", "2026-02-29", "31/04/2026",
            "Thu, 31 Apr 2026 09:00:00 GMT").forEach {
            assertNull("Must reject $it", parseImportedCsvDate(mapping(), it))
        }
    }

    @Test fun `declared source format is respected instead of guessing month and day`() {
        val european = mapping("dd/MM/yyyy")
        assertEquals(LocalDateTime.of(2026, 4, 3, 12, 0),
            parseImportedCsvDate(european, "03/04/2026"))
        assertNull(parseImportedCsvDate(european, "12/31/2026"))
    }

    @Test fun `valid dates retain wall time until the injected timezone conversion`() {
        assertEquals(LocalDateTime.of(2024, 2, 29, 12, 0),
            parseImportedCsvDate(mapping(), "2024-02-29"))
        assertEquals(LocalDateTime.of(2026, 10, 1, 9, 0),
            parseImportedCsvDate(mapping(), "2026-10-01 09:00:00"))
    }

    @Test fun `Dubai wall time is converted once and explicit offsets remain authoritative`() {
        val provider = mockk<TimeProvider>()
        every { provider.getZoneId() } returns ZoneId.of("Asia/Dubai")
        val converter = StandardTimeConverter(provider)
        assertEquals(Instant.parse("2026-10-01T05:00:00Z"),
            parseImportedCsvTimestamp(mapping(), "2026-10-01 09:00:00", converter))
        assertEquals(Instant.parse("2026-10-01T08:00:00Z"),
            parseImportedCsvTimestamp(mapping(), "2026-10-01", converter))
        for (value in listOf("2026-10-01T09:00:00Z", "2026-10-01T11:00:00+02:00",
            "Thu, 1 Oct 2026 09:00:00 GMT")) {
            assertEquals(value, Instant.parse("2026-10-01T09:00:00Z"),
                parseImportedCsvTimestamp(mapping(), value, converter))
        }
    }

    @Test fun `historical conversion uses that dates daylight saving offset not todays`() {
        val provider = mockk<TimeProvider>()
        every { provider.getZoneId() } returns ZoneId.of("America/New_York")
        val converter = StandardTimeConverter(provider)
        assertEquals(Instant.parse("2026-01-15T14:00:00Z"),
            parseImportedCsvTimestamp(mapping(), "2026-01-15 09:00:00", converter))
        assertEquals(Instant.parse("2026-07-15T13:00:00Z"),
            parseImportedCsvTimestamp(mapping(), "2026-07-15 09:00:00", converter))
    }

    @Test fun `nonfinite and ambiguous decimal comma amounts are rejected rather than reinterpreted`() {
        listOf(null, "", "NaN", "Infinity", "-Infinity", "1e999", "1,50", "12,34.56", "1.234,56", "10f", "0x1p2").forEach {
            assertNull("Must reject $it", parseImportedCsvAmount(it))
        }
    }

    @Test fun `canonical and correctly grouped amounts preserve sign and value`() {
        assertEquals(1234.56, parseImportedCsvAmount("1,234.56")!!, 0.0)
        assertEquals(-1234.56, parseImportedCsvAmount("-1234.56")!!, 0.0)
        assertEquals(1250000.25, parseImportedCsvAmount(" 1,250,000.25 ")!!, 0.0)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test fun `all ten supported source fixtures still parse their supplied dates`() {
        val fixtures = listOf(
            ImportType.IVY to "ivy-wallet.csv", ImportType.MONEY_MANAGER to "money-manager.csv",
            ImportType.WALLET_BY_BUDGET_BAKERS to "budgetbakers-wallet.csv", ImportType.SPENDEE to "spendee.csv",
            ImportType.MONEFY to "monefy.csv", ImportType.ONE_MONEY to "one-money.csv",
            ImportType.BLUE_COINS to "bluecoins.csv", ImportType.KTW_MONEY_MANAGER to "ktw-money-manager.csv",
            ImportType.FORTUNE_CITY to "fortune-city.csv", ImportType.FINANCISTO to "financisto.csv",
        )
        val mapper = CSVMapper()
        for ((type, file) in fixtures) {
            val rows = checkNotNull(javaClass.getResourceAsStream("/import-fixtures/$file")).use {
                CSVReaderBuilder(InputStreamReader(it, Charsets.UTF_8)).build().readAll()
            }
            val mapping = mapper.mapping(type, rows.first().joinToString(","))
            val row = rows[1]
            val date = row[mapping.date] + (mapping.timeOnly?.let { " ${row[it]}" } ?: "")
            assertNotNull("Source $type rejected $date", parseImportedCsvDate(mapping, date))
        }
    }
}
