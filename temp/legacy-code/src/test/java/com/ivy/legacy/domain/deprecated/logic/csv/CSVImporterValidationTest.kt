package com.ivy.legacy.domain.deprecated.logic.csv

import com.ivy.base.legacy.Theme
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.wallet.domain.deprecated.logic.csv.model.RowMapping
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

@Suppress("DEPRECATION")
class CSVImporterValidationTest {
    private val accountId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val settings = mockk<SettingsDao>()
    private val accounts = mockk<AccountDao>()
    private val existingTransactions = mockk<TransactionDao>()
    private val categories = mockk<CategoryRepository>(relaxed = true)
    private val accountStore = mockk<AccountRepository>(relaxed = true)
    private val transactionStore = mockk<TransactionRepository>(relaxed = true)
    private val currencies = mockk<CurrencyRepository>(relaxed = true)
    private val atomic = mockk<AtomicWriteRepository>()
    private var inTransaction = false

    private val mapping = RowMapping(type = 2, amount = 1, account = 3, date = 0,
        category = null, title = 4, dueDate = 5, toAccount = 6, transferAmount = 7, toAmount = 8)

    private fun importer(): CSVImporter {
        coEvery { atomic.run<Any?>(any()) } coAnswers {
            check(!inTransaction)
            inTransaction = true
            try { firstArg<suspend () -> Any?>().invoke() } finally { inTransaction = false }
        }
        coEvery { settings.findFirst() } returns SettingsEntity(Theme.LIGHT, "AED", 0.0, "Fictional")
        coEvery { accounts.findAll() } returns listOf(AccountEntity("Main", "AED", 0, id = accountId))
        coEvery { existingTransactions.findAll() } returns emptyList()
        coEvery { categories.findAll() } returns emptyList()
        coEvery { accountStore.findById(any()) } returns Account(AccountId(accountId),
            NotBlankTrimmedString.from("Main").getOrNull()!!, AssetCode.from("AED").getOrNull()!!,
            ColorInt(0), null, true, 0.0)
        return CSVImporter(settings, accounts, existingTransactions, categories,
            transactionStore, TransactionMapper(accountStore), accountStore, currencies, TestTimeConverter(), atomic)
    }

    private fun csv(rows: List<List<String>>): String =
        (listOf(listOf("Date", "Amount", "Type", "Account", "Title", "Due", "To", "Transfer", "To amount")) + rows)
            .joinToString("\n") { row -> row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } }

    @Test fun `invalid rows are retained with source line numbers and do not create financial records`() = runTest {
        val importer = importer()
        val rows = listOf(
            listOf("not a date", "10", "Expense", "New account", "A", "", "", "", ""),
            listOf("not a date", "10", "Expense", "New account", "B", "2026-11-01", "", "", ""),
            listOf("2026-10-01", "10", "Expense", "New account", "C", "bad due date", "", "", ""),
            listOf("2026-10-01", "NaN", "Expense", "New account", "D", "", "", "", ""),
            listOf("2026-10-01", "1,50", "Expense", "New account", "E", "", "", "", ""),
            listOf("not a date", "", "Transfer", "New account", "F", "", "New destination", "10", "10"),
            listOf("2026-10-01", "", "Transfer", "New account", "G", "", "New destination", "10", "Infinity"),
            listOf("2026-10-01", "", "Transfer", "New account", "H", "", "New destination", "10", "0"),
            listOf("2026-10-01", "", "Transfer", "New account", "I", "", "New destination", "10", "-5"),
            listOf("2026-10-01", "", "Transfer", "New account", "J", "", "", "10", "10"),
            listOf("2026-10-01", "", "Transfer", "New account", "K", "", "new account", "10", "10"),
        )
        val result = importer.import(csv(rows), mapping) {}
        assertEquals(11, result.rowsFound)
        assertEquals(0, result.transactionsImported)
        assertEquals(0, result.accountsImported)
        assertEquals(0, result.categoriesImported)
        assertEquals((2..12).toList(), result.failedRows.map { it.index })
        assertEquals(rows, result.failedRows.map { it.content })
        coVerify(exactly = 0) { transactionStore.save(any()) }
        coVerify(exactly = 0) { accountStore.save(any()) }
        coVerify(exactly = 0) { categories.save(any()) }
    }

    @Test fun `unmappable transfer is reported instead of silently disappearing`() = runTest {
        val importer = importer()
        val row = listOf("2026-10-01", "", "Transfer", "Main", "Fictional", "", "", "10", "10")
        // Split-transfer sources have no destination column until the join step.
        val result = importer.import(csv(listOf(row)), mapping.copy(toAccount = null)) {}
        assertEquals(0, result.transactionsImported)
        assertEquals(listOf(2), result.failedRows.map { it.index })
        assertEquals(row, result.failedRows.single().content)
    }

    @Test fun `writes and progress share the atomic boundary and cancellation escapes`() = runTest {
        val importer = importer()
        val cancellation = CancellationException("Fictional cancellation")
        coEvery { transactionStore.save(any()) } coAnswers {
            assertTrue(inTransaction)
            throw cancellation
        }
        try {
            importer.import(csv(listOf(listOf("2026-10-01", "10", "Income", "Main", "Fictional", "", "", "", ""))), mapping) {
                assertTrue(inTransaction)
            }
            fail("Cancellation must reach the Room transaction coordinator")
        } catch (actual: CancellationException) { assertSame(cancellation, actual) }
        assertFalse(inTransaction)
        coVerify(exactly = 1) { atomic.run<Any?>(any()) }
    }

    @Test fun `a valid grouped amount still becomes one income record in the existing account`() = runTest {
        val importer = importer()
        val result = importer.import(csv(listOf(
            listOf("2026-10-01 09:00:00", "1,234.56", "Income", "Main", "Fictional salary", "", "", "", ""))), mapping) {}
        assertEquals(1, result.transactionsImported)
        assertTrue(result.failedRows.isEmpty())
        assertEquals(0, result.accountsImported)
        val saved = slot<Transaction>()
        coVerify(exactly = 1) { transactionStore.save(capture(saved)) }
        assertTrue(saved.captured is Income)
        val income = saved.captured as Income
        assertEquals(1234.56, income.value.amount.value, 0.0)
        assertEquals(AccountId(accountId), income.account)
    }
}
