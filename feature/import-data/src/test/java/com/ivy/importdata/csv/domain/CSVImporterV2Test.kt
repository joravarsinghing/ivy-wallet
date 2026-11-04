package com.ivy.importdata.csv.domain

import com.ivy.base.legacy.Theme
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.*
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.importdata.csv.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Orchestration contracts with reversible stores, not a substitute for SQLite tests. */
class CSVImporterV2Test {
    private val source = Account(AccountId(UUID.randomUUID()), NotBlankTrimmedString.unsafe("Main"),
        AssetCode.from("AED").getOrNull()!!, ColorInt(0), null, true, 0.0)
    private val destination = source.copy(id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Savings"), asset = AssetCode.from("USD").getOrNull()!!)
    private val accountRows = listOf(source, destination)
    private val settings = mockk<SettingsDao>()
    private val accounts = mockk<AccountDao>()
    private val existing = mockk<TransactionDao>()
    private val categories = mockk<CategoryRepository>(relaxed = true)
    private val accountStore = mockk<AccountRepository>(relaxed = true)
    private val transactionStore = mockk<TransactionRepository>()
    private val currencies = mockk<CurrencyRepository>(relaxed = true)
    private val atomic = mockk<AtomicWriteRepository>()
    private val saved = mutableListOf<Transaction>()
    private var insideAtomic = false
    private var failAt: Int? = null
    private var writeFailure: Throwable = IllegalStateException("Fictional disk failure")
    private val status = MappingStatus(emptyList(), false)
    private fun <M> column(index: Int, metadata: M) = ColumnMapping("", "", "", index, metadata, false)
    private val important = ImportantFields(column(1, 1), status,
        column(2, TrnTypeMetadata("Income", "Expense", "Transfer")), status,
        column(0, DateMetadata.MonthFirst), status, column(3, Unit), status, column(-1, Unit), status)
    private val transfer = TransferFields(column(4, Unit), status, column(-1, Unit), status, column(5, 1), status)
    private val optional = OptionalFields(column(-1, Unit), status, column(6, Unit), status, column(-1, Unit), status)
    private val mapper = TransactionMapper(accountStore)
    private val importer = CSVImporterV2(settings, transactionStore, mapper,
        accounts, existing, categories, currencies, accountStore, TestTimeConverter(), atomic)

    init {
        coEvery { settings.findFirst() } returns SettingsEntity(Theme.LIGHT, "AED", 0.0, "Fictional")
        coEvery { accounts.findAll() } returns accountRows.map {
            AccountEntity(it.name.value, it.asset.code, 0, id = it.id.value)
        }
        coEvery { accountStore.findById(any()) } answers {
            val id = firstArg<UUID>()
            accountRows.find { it.id.value == id }
        }
        coEvery { existing.findAll() } returns emptyList()
        coEvery { categories.findAll() } returns emptyList()
        coEvery { atomic.run<Any?>(any()) } coAnswers {
            val before = saved.toList()
            insideAtomic = true
            try { firstArg<suspend () -> Any?>().invoke() } catch (failure: Throwable) {
                saved.clear(); saved.addAll(before); throw failure
            } finally { insideAtomic = false }
        }
        coEvery { transactionStore.save(any()) } answers {
            assertTrue(insideAtomic)
            if (saved.size == failAt) throw writeFailure
            saved += firstArg<Transaction>()
        }
    }

    private fun row(amount: String = "100", toAmount: String = "27", date: String = "2026-11-01", to: String = "Savings", title: String = "Fictional") =
        CSVRow(listOf(date, amount, "Transfer", "Main", to, toAmount, title))

    private suspend fun import(vararg rows: CSVRow) = importer.import(
        listOf(CSVRow(emptyList())) + rows, important, transfer, optional) { assertTrue(insideAtomic) }

    @Test fun `cross currency transfer preserves different source and destination amounts`() = runTest {
        val result = import(row())
        assertEquals(1, result.transactionsImported)
        assertTrue(result.failedRows.isEmpty())
        val savedTransfer = saved.single() as Transfer
        assertEquals(100.0, savedTransfer.fromValue.amount.value, 0.0)
        assertEquals(27.0, savedTransfer.toValue.amount.value, 0.0)
        assertEquals(source.asset, savedTransfer.fromValue.asset)
        assertEquals(destination.asset, savedTransfer.toValue.asset)
        assertEquals(source.id, savedTransfer.fromAccount)
        assertEquals(destination.id, savedTransfer.toAccount)
    }

    @Test fun `bad transfer fields and dates never create accounts or transactions`() = runTest {
        val result = import(row(date = "2026-02-30"), row(amount = "1,50"), row(toAmount = "NaN"),
            row(toAmount = "0"), row(to = "main"), row(to = ""))
        assertEquals((2..7).toList(), result.failedRows.map { it.index })
        assertTrue(saved.isEmpty())
        coVerify(exactly = 0) { accountStore.save(any()) }
        coVerify(exactly = 0) { categories.save(any()) }
    }

    @Test fun `failure after an earlier save rolls back instead of returning partial success`() = runTest {
        failAt = 1
        try { import(row(), row(title = "Second")); fail() }
        catch (failure: IllegalStateException) { assertSame(writeFailure, failure) }
        assertTrue(saved.isEmpty())
        assertFalse(insideAtomic)
        failAt = null
        assertEquals(2, import(row(), row(title = "Second")).transactionsImported)
    }

    @Test fun `cancellation after an earlier save escapes and rolls back`() = runTest {
        failAt = 1
        writeFailure = CancellationException("Fictional interruption")
        try { import(row(), row(title = "Second")); fail() }
        catch (failure: CancellationException) { assertSame(writeFailure, failure) }
        assertTrue(saved.isEmpty())
    }

    @Test fun `retrying an income import recognizes its canonical saved representation`() = runTest {
        coEvery { existing.findAll() } answers { saved.map { with(mapper) { it.toEntity() } } }
        val income = row().let { it.copy(values = it.values.toMutableList().apply { this[2] = "Income" }) }
        assertEquals(1, import(income).transactionsImported)
        val retry = import(income)
        assertEquals(0, retry.transactionsImported)
        assertEquals(1, retry.duplicatesSkipped)
        assertEquals(1, saved.size)
    }

    @Test fun `currency conflict is rejected before creating the other transfer account`() = runTest {
        val conflict = row(to = "New destination").let { it.copy(values = it.values + "USD") }
        val result = importer.import(listOf(CSVRow(emptyList()), conflict),
            important.copy(accountCurrency = column(7, Unit)), transfer, optional) {}
        assertEquals(listOf(2), result.failedRows.map { it.index })
        assertTrue(saved.isEmpty())
        coVerify(exactly = 0) { accountStore.save(any()) }
    }
}
