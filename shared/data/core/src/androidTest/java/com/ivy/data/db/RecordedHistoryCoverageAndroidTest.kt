package com.ivy.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/** Compile-only until an Android device/emulator is explicitly available. */
@RunWith(AndroidJUnit4::class)
class RecordedHistoryCoverageAndroidTest {
    @Test fun aggregateHandlesEmptyNullDeletedAndAccountFilteredRows() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), IvyRoomDatabase::class.java).build()
        try {
            val account = UUID.randomUUID()
            val first = Instant.parse("2026-10-01T09:00:00Z")
            assertNull(db.transactionDao.findFirstSettledTime(listOf(account)))
            val planned = TransactionEntity(account, TransactionType.EXPENSE, 10.0, dueDate = first.minusSeconds(86400))
            db.writeTransactionDao.save(planned)
            db.writeTransactionDao.save(planned.copy(id = UUID.randomUUID(), dateTime = first.minusSeconds(86400), isDeleted = true))
            db.writeTransactionDao.save(planned.copy(id = UUID.randomUUID(), accountId = UUID.randomUUID(), dateTime = first.minusSeconds(86400)))
            assertNull(db.transactionDao.findFirstSettledTime(listOf(account)))
            db.writeTransactionDao.save(planned.copy(id = UUID.randomUUID(), dateTime = first))
            assertEquals(first, db.transactionDao.findFirstSettledTime(listOf(account)))
            assertNull(db.transactionDao.findFirstSettledTime(emptyList()))
        } finally { db.close() }
    }
}
