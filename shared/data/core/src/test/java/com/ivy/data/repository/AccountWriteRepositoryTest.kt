package com.ivy.data.repository

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.dao.fake.FakeSettingsDao
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.write.WriteAccountDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.base.model.TransactionType
import java.time.Instant
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.mapper.AccountMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Transaction fake tests orchestration/cache timing, not SQLite's rollback implementation. */
class AccountWriteRepositoryTest {
    private val rows = mutableMapOf<UUID, AccountEntity>()
    private val ledger = mutableListOf<UUID>()
    private val openings = mutableMapOf<UUID, TransactionEntity>()
    private val events = mutableListOf<DataWriteEvent>()
    private val dao = mockk<AccountDao>()
    private val writer = mockk<WriteAccountDao>()
    private val observer = mockk<DataObserver>()
    private val settings = FakeSettingsDao()
    private val mapper = AccountMapper(CurrencyRepository(settings, settings, TestDispatchersProvider))
    private val accounts = AccountRepository(mapper, dao, writer, TestDispatchersProvider,
        RepositoryMemoFactory(observer, TestDispatchersProvider))
    private val original = Account(AccountId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        NotBlankTrimmedString.unsafe("Fictional bank"), AssetCode.USD, ColorInt(0), null, true, 1.0)
    private val changed = original.copy(name = NotBlankTrimmedString.unsafe("Updated bank"), orderNum = 5.0)
    private var cancelAfterCommit = false
    private var failMetadata = false
    private val writes = AccountWriteRepository(
        transaction = { block ->
            val beforeRows = rows.toMap()
            val beforeLedger = ledger.toList()
            val beforeOpenings = openings.toMap()
            try { block() } catch (failure: Throwable) {
                rows.clear(); rows.putAll(beforeRows)
                ledger.clear(); ledger.addAll(beforeLedger)
                openings.clear(); openings.putAll(beforeOpenings)
                throw failure
            }
            if (cancelAfterCommit) throw CancellationException("Fictional lost commit acknowledgement")
        },
        writeAccount = { row ->
            rows[row.id] = row
            if (failMetadata) throw IllegalStateException("Fictional metadata failure")
        }, accounts = accounts, mapper = mapper, dispatchers = TestDispatchersProvider,
        readAccount = { rows[it] }, readOpening = { openings[it] })

    init {
        coEvery { dao.findById(any()) } answers { rows[firstArg<UUID>()] }
        coEvery { dao.findAll(any()) } answers { rows.values.sortedBy { it.orderNum } }
        coEvery { dao.findMaxOrderNum() } answers { rows.values.maxOfOrNull { it.orderNum } }
        coEvery { observer.post(any()) } answers { events += firstArg<DataWriteEvent>(); Unit }
    }

    private fun seed() { rows[original.id.value] = with(mapper) { original.toEntity() } }

    @Test fun `failed creation leaves no metadata ledger or event`() = runTest {
        try { writes.save(original) { ledger += original.id.value; error("Fictional failure") }; fail() }
        catch (_: IllegalStateException) { }
        assertTrue(rows.isEmpty()); assertTrue(ledger.isEmpty()); assertTrue(events.isEmpty())
        assertNull(accounts.findById(original.id))
    }

    @Test fun `failed edit preserves persisted and previously memoized account`() = runTest {
        seed(); accounts.findAll()
        try {
            writes.save(changed) {
                assertEquals(changed, accounts.findById(original.id))
                error("Fictional adjustment failure")
            }; fail()
        } catch (_: IllegalStateException) { }
        assertEquals(listOf(original), accounts.findAll())
        assertEquals(original, accounts.findById(original.id))
        assertTrue(events.isEmpty())
    }

    @Test fun `success publishes once only after the adjustment commits`() = runTest {
        writes.save(original) { assertTrue(events.isEmpty()); ledger += original.id.value }
        assertEquals(listOf(original.id.value), ledger)
        assertEquals(listOf(DataWriteEvent.SaveAccounts(listOf(original))), events)
        assertEquals(original, accounts.findById(original.id))
    }

    @Test fun `staged account is read fresh across dispatchers without leaking into memo`() = runTest {
        seed(); accounts.findAll()
        try {
            writes.save(changed) {
                withContext(Dispatchers.Default) {
                    assertEquals(changed, accounts.findById(original.id))
                    assertEquals(listOf(changed), accounts.findAll())
                    assertEquals(5.0, accounts.findMaxOrderNum(), 0.0)
                }
                error("Fictional rollback")
            }; fail()
        } catch (_: IllegalStateException) { }
        assertEquals(listOf(original), accounts.findAll())
        assertEquals(1.0, accounts.findMaxOrderNum(), 0.0)
    }

    @Test fun `precommit cancellation rolls back both writes without publishing`() = runTest {
        seed(); accounts.findAll()
        try {
            writes.save(changed) { ledger += changed.id.value; throw CancellationException() }; fail()
        } catch (_: CancellationException) { }
        assertEquals(listOf(original), accounts.findAll())
        assertTrue(ledger.isEmpty()); assertTrue(events.isEmpty())
    }

    @Test fun `lost commit acknowledgement invalidates previously memoized reads`() = runTest {
        seed(); accounts.findAll(); cancelAfterCommit = true
        try { writes.save(changed) { ledger += changed.id.value }; fail() }
        catch (_: CancellationException) { }
        assertEquals(listOf(changed), accounts.findAll())
        assertEquals(listOf(changed.id.value), ledger)
        assertTrue(events.isEmpty())
    }

    @Test fun `metadata failure cannot run the adjustment`() = runTest {
        failMetadata = true
        try { writes.save(original) { fail("Must not adjust") }; fail() }
        catch (_: IllegalStateException) { }
        assertTrue(rows.isEmpty()); assertTrue(events.isEmpty())
    }

    @Test fun `account writes cannot interleave their adjustments`() = runTest {
        val order = mutableListOf<String>()
        listOf(original, changed).map { account -> async {
            writes.save(account) {
                order += "start-${account.orderNum}"; delay(1); order += "end-${account.orderNum}"
            }
        } }.awaitAll()
        assertEquals(listOf("start-1.0", "end-1.0", "start-5.0", "end-5.0"), order)
        assertEquals(2, events.size)
    }

    @Test fun `large ledger mapping reuses only its isolated transaction read cache`() = runTest {
        seed()
        val other = original.copy(id = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000002")))
        rows[other.id.value] = with(mapper) { other.toEntity() }
        writes.save(changed) {
            repeat(1_000) {
                assertEquals(changed, accounts.findById(changed.id))
                assertEquals(other, accounts.findById(other.id))
            }
            coVerify(exactly = 0) { dao.findById(changed.id.value) }
            coVerify(exactly = 1) { dao.findById(other.id.value) }
        }
        writes.save(original) {
            assertEquals(original, accounts.findById(original.id))
        }
        assertEquals(original, accounts.findById(original.id))
    }

    private fun opening(balance: Double) = TransactionEntity(original.id.value,
        if (balance > 0) TransactionType.INCOME else TransactionType.EXPENSE, kotlin.math.abs(balance),
        dateTime = Instant.parse("2026-10-31T12:00:00Z"), id = accountOpeningTransactionId(original.id.value))

    @Test fun `retry of a zero opening does not reset later activity`() = runTest {
        var initialized = 0
        writes.create(original, 0.0) { initialized++ }
        ledger += UUID.randomUUID()
        writes.create(original.copy(orderNum = 6.0), 0.0) { initialized++ }
        assertEquals(1, initialized); assertEquals(1, ledger.size)
        assertEquals(original, accounts.findById(original.id))
    }

    @Test fun `positive opening retries retain one opening and preserve later activity`() = runTest {
        var initialized = 0
        repeat(2) {
            writes.create(original, 25.0) { initialized++; openings[opening(25.0).id] = opening(25.0) }
            ledger += UUID.randomUUID()
        }
        assertEquals(1, initialized); assertEquals(1, openings.size); assertEquals(2, ledger.size)
    }

    @Test fun `negative opening retries retain one expense opening`() = runTest {
        repeat(2) { writes.create(original, -25.0) { openings[opening(-25.0).id] = opening(-25.0) } }
        assertEquals(listOf(opening(-25.0)), openings.values.toList())
    }

    @Test fun `a changed target after committed creation requires review instead of another adjustment`() = runTest {
        writes.create(original, 25.0) { openings[opening(25.0).id] = opening(25.0) }
        try { writes.create(original, 30.0) { fail("Must not reinitialize") }; fail() }
        catch (_: AccountWriteConflict.AlreadySaved) { }
        assertEquals(listOf(opening(25.0)), openings.values.toList())
    }

    @Test fun `a changed draft after committed creation cannot overwrite its account`() = runTest {
        writes.create(original, 0.0) { }
        try { writes.create(changed, 0.0) { fail("Must not reinitialize") }; fail() }
        catch (_: AccountWriteConflict.AlreadySaved) { }
        assertEquals(original, accounts.findById(original.id))
    }

    @Test fun `a deleted creation cannot be resurrected by a retry`() = runTest {
        seed(); rows[original.id.value] = rows.getValue(original.id.value).copy(isDeleted = true)
        try { writes.create(original, 0.0) { fail("Must not initialize") }; fail() }
        catch (_: AccountWriteConflict.MissingAccount) { }
        assertTrue(rows.getValue(original.id.value).isDeleted)
    }

    @Test fun `missing edit cannot recreate an account`() = runTest {
        try { writes.edit(changed) { fail("Must not adjust") }; fail() }
        catch (_: AccountWriteConflict.MissingAccount) { }
        assertTrue(rows.isEmpty())
    }

    @Test fun `deleted edit cannot resurrect an account`() = runTest {
        seed(); rows[original.id.value] = rows.getValue(original.id.value).copy(isDeleted = true)
        try { writes.edit(changed) { fail("Must not adjust") }; fail() }
        catch (_: AccountWriteConflict.MissingAccount) { }
        assertTrue(rows.getValue(original.id.value).isDeleted)
    }

    @Test fun `failed opening rolls back and the same intent can be retried`() = runTest {
        try {
            writes.create(original, 25.0) { openings[opening(25.0).id] = opening(25.0); error("Fictional failure") }; fail()
        } catch (_: IllegalStateException) { }
        assertTrue(rows.isEmpty()); assertTrue(openings.isEmpty())
        writes.create(original, 25.0) { openings[opening(25.0).id] = opening(25.0) }
        assertEquals(1, rows.size); assertEquals(1, openings.size)
    }

    @Test fun `lost creation acknowledgement retries without another opening`() = runTest {
        cancelAfterCommit = true
        try { writes.create(original, 25.0) { openings[opening(25.0).id] = opening(25.0) }; fail() }
        catch (_: CancellationException) { }
        cancelAfterCommit = false
        writes.create(original, 25.0) { fail("Already committed") }
        assertEquals(1, rows.size); assertEquals(1, openings.size)
    }

    @Test fun `simultaneous same intent creates one opening`() = runTest {
        var initialized = 0
        (1..3).map { async {
            writes.create(original, 25.0) {
                initialized++; delay(1); openings[opening(25.0).id] = opening(25.0)
            }
        } }.awaitAll()
        assertEquals(1, initialized); assertEquals(1, openings.size)
    }

    @Test fun `a missing required opening transaction rolls back creation`() = runTest {
        try { writes.create(original, 25.0) { }; fail() }
        catch (_: IllegalStateException) { }
        assertTrue(rows.isEmpty()); assertTrue(openings.isEmpty()); assertTrue(events.isEmpty())
    }
}
