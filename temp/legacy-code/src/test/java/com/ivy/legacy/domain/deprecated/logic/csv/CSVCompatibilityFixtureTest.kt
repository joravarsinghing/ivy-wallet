package com.ivy.legacy.domain.deprecated.logic.csv

import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType
import com.ivy.wallet.domain.deprecated.logic.csv.CSVMapper
import com.opencsv.CSVReaderBuilder
import java.io.InputStreamReader
import org.junit.Assert.assertTrue
import org.junit.Test

class CSVCompatibilityFixtureTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `source fixtures satisfy the registered legacy mappings`() {
        val fixtures = listOf(
            ImportType.IVY to "ivy-wallet.csv",
            ImportType.MONEY_MANAGER to "money-manager.csv",
            ImportType.WALLET_BY_BUDGET_BAKERS to "budgetbakers-wallet.csv",
            ImportType.SPENDEE to "spendee.csv",
            ImportType.MONEFY to "monefy.csv",
            ImportType.ONE_MONEY to "one-money.csv",
            ImportType.BLUE_COINS to "bluecoins.csv",
            ImportType.KTW_MONEY_MANAGER to "ktw-money-manager.csv",
            ImportType.FORTUNE_CITY to "fortune-city.csv",
            ImportType.FINANCISTO to "financisto.csv",
        )
        val mapper = CSVMapper()

        fixtures.forEach { (type, filename) ->
            val rows = javaClass.getResourceAsStream("/import-fixtures/$filename")
                ?.use { input ->
                    CSVReaderBuilder(InputStreamReader(input, Charsets.UTF_8)).build().readAll()
                }
                ?: error("Missing import fixture: $filename")

            assertTrue("$filename must contain a header and one data row", rows.size >= 2)

            val mapping = mapper.mapping(type, rows.first().joinToString(","))
            val row = rows[1]
            val requiredColumns = listOfNotNull(
                mapping.amount,
                mapping.account,
                mapping.date,
                mapping.category,
                mapping.title,
                mapping.type,
            )

            requiredColumns.forEach { index ->
                assertTrue(
                    "$type maps column $index but $filename has ${row.size} columns",
                    index < row.size,
                )
                assertTrue(
                    "$type column $index is blank in $filename",
                    row[index].isNotBlank(),
                )
            }
        }
    }
}
