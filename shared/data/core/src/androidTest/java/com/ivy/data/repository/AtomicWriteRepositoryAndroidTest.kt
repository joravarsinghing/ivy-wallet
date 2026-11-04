package com.ivy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.model.TransactionType
import com.ivy.data.DataObserver
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.db.entity.LoanEntity
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.model.LoanType
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.AssociationId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.mapper.TagMapper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/** Compiling these tests is not a claim of connected SQLite execution. */
@RunWith(AndroidJUnit4::class)
class AtomicWriteRepositoryAndroidTest {
    private lateinit var db: IvyRoomDatabase
    private lateinit var atomic: AtomicWriteRepository
    private lateinit var tags: TagRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), IvyRoomDatabase::class.java).build()
        atomic = AtomicWriteRepository(db, TestDispatchersProvider)
        tags = TagRepository(TagMapper(), db.tagDao, db.tagAssociationDao, db.writeTagDao, db.writeTagAssociationDao,
            TestDispatchersProvider, RepositoryMemoFactory(DataObserver(), TestDispatchersProvider), atomic)
    }
    @After fun close() { db.close() }

    @Test fun failedRelatedLoanTransactionRollsBackBothRows() = runBlocking {
        val loan = LoanEntity("Fictional loan", 250.0, LoanType.BORROW)
        try {
            atomic.run {
                db.writeLoanDao.save(loan)
                db.writeTransactionDao.save(TransactionEntity(UUID.randomUUID(), TransactionType.INCOME, 250.0,
                    dateTime = Instant.parse("2026-10-31T12:00:00Z"), loanId = loan.id))
                error("Fictional second-write failure")
            }; fail()
        } catch (_: IllegalStateException) { }
        assertNull(db.loanDao.findById(loan.id))
        assertTrue(db.transactionDao.findAllByLoanId(loan.id).isEmpty())
    }

    @Test fun failedTagDeletionRestoresRowAssociationsAndMemo() = runBlocking {
        val mapper = TagMapper()
        val tag = with(mapper) { createNewTag(TagId(UUID.randomUUID()), NotBlankTrimmedString.unsafe("Fictional trip")) }
        tags.save(tag)
        tags.associateTagToEntity(AssociationId(UUID.randomUUID()), tag.id)
        tags.findAll()
        try {
            atomic.run { tags.deleteById(tag.id); error("Fictional later failure") }; fail()
        } catch (_: IllegalStateException) { }
        assertEquals(tag, tags.findById(tag.id))
        assertEquals(1, db.tagAssociationDao.findAll().size)
    }

    @Test fun successfulTagDeletionRemovesAssociationsWithoutDeletingTransactions() = runBlocking {
        val mapper = TagMapper()
        val tag = with(mapper) { createNewTag(TagId(UUID.randomUUID()), NotBlankTrimmedString.unsafe("Fictional trip")) }
        val transaction = TransactionEntity(UUID.randomUUID(), TransactionType.EXPENSE, 25.0, dateTime = Instant.parse("2026-10-31T12:00:00Z"))
        db.writeTransactionDao.save(transaction)
        tags.save(tag)
        tags.associateTagToEntity(AssociationId(transaction.id), tag.id)
        tags.deleteById(tag.id)
        assertNull(tags.findById(tag.id))
        assertTrue(db.tagAssociationDao.findAll().isEmpty())
        assertEquals(transaction, db.transactionDao.findById(transaction.id))
    }
}
