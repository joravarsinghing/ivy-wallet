package com.ivy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.model.TransactionType
import com.ivy.data.DataObserver
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.mapper.AccountMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/** Real SQLite rollback evidence requires connected execution, not only compilation. */
@RunWith(AndroidJUnit4::class)
class AccountWriteRepositoryAndroidTest {
    private lateinit var db: IvyRoomDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var writes: AccountWriteRepository
    private val original = Account(AccountId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        NotBlankTrimmedString.unsafe("Fictional bank"), AssetCode.USD, ColorInt(0), null, true, 1.0)
    private val changed = original.copy(name = NotBlankTrimmedString.unsafe("Updated bank"))
    private val adjustment = TransactionEntity(original.id.value, TransactionType.INCOME, 25.0,
        dateTime = Instant.parse("2026-10-31T12:00:00Z"), id = accountOpeningTransactionId(original.id.value))

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),
            IvyRoomDatabase::class.java).build()
        val mapper = AccountMapper(CurrencyRepository(db.settingsDao, db.writeSettingsDao, TestDispatchersProvider))
        accounts = AccountRepository(mapper, db.accountDao, db.writeAccountDao, TestDispatchersProvider,
            RepositoryMemoFactory(DataObserver(), TestDispatchersProvider))
        writes = AccountWriteRepository(db, accounts, mapper, TestDispatchersProvider)
    }

    @After fun close() { db.close() }

    @Test fun failedOpeningRollsBackAccountAndTransaction() = runBlocking {
        try {
            writes.save(original) {
                assertEquals(original, accounts.findById(original.id))
                db.writeTransactionDao.save(adjustment)
                error("Fictional opening failure")
            }; fail("Expected failure")
        } catch (_: IllegalStateException) { }
        assertNull(db.accountDao.findById(original.id.value))
        assertTrue(db.transactionDao.findAll().isEmpty())
        assertNull(accounts.findById(original.id))
    }

    @Test fun failedEditRestoresPreviouslyCachedAccount() = runBlocking {
        writes.save(original) { }
        accounts.findAll()
        try {
            writes.edit(changed) {
                assertEquals(changed, accounts.findById(original.id))
                db.writeTransactionDao.save(adjustment)
                error("Fictional edit failure")
            }; fail("Expected failure")
        } catch (_: IllegalStateException) { }
        assertEquals(original.name.value, db.accountDao.findById(original.id.value)?.name)
        assertEquals(listOf(original), accounts.findAll())
        assertTrue(db.transactionDao.findAll().isEmpty())
    }

    @Test fun successfulWritePersistsBothRecords() = runBlocking {
        writes.save(original) { db.writeTransactionDao.save(adjustment) }
        assertEquals(original.name.value, db.accountDao.findById(original.id.value)?.name)
        assertEquals(listOf(adjustment), db.transactionDao.findAll())
        assertEquals(original, accounts.findById(original.id))
    }

    @Test fun cancellationBeforeCommitRollsBackBothRecords() = runBlocking {
        try {
            writes.save(original) {
                db.writeTransactionDao.save(adjustment)
                throw CancellationException("Fictional cancellation")
            }; fail("Expected cancellation")
        } catch (_: CancellationException) { }
        assertTrue(db.accountDao.findAll().isEmpty())
        assertTrue(db.transactionDao.findAll().isEmpty())
    }

    @Test fun retriedCreationPreservesLaterActivity() = runBlocking {
        writes.create(original, 25.0) { db.writeTransactionDao.save(adjustment) }
        val later = adjustment.copy(id = UUID.randomUUID(), type = TransactionType.EXPENSE, amount = 5.0)
        db.writeTransactionDao.save(later)
        writes.create(original.copy(orderNum = 9.0), 25.0) { fail("Must not reopen") }
        assertEquals(setOf(adjustment, later), db.transactionDao.findAll().toSet())
        assertEquals(original, accounts.findById(original.id))
    }

    @Test fun changedCreationDraftCannotOverwriteCommittedAccount() = runBlocking {
        writes.create(original, 25.0) { db.writeTransactionDao.save(adjustment) }
        try { writes.create(changed, 25.0) { fail("Must not reopen") }; fail() }
        catch (_: AccountWriteConflict.AlreadySaved) { }
        assertEquals(original, accounts.findById(original.id))
        assertEquals(listOf(adjustment), db.transactionDao.findAll())
    }

    @Test fun staleEditCannotResurrectDeletedAccount() = runBlocking {
        writes.create(original, 0.0) { }
        db.writeAccountDao.deleteById(original.id.value)
        try { writes.edit(changed) { fail("Must not adjust") }; fail() }
        catch (_: AccountWriteConflict.MissingAccount) { }
        assertNull(db.accountDao.findById(original.id.value))
    }
}
