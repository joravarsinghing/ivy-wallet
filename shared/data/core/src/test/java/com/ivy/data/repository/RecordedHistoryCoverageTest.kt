package com.ivy.data.repository

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.model.TransactionType
import com.ivy.data.db.dao.fake.FakeTransactionDao
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.model.AccountId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class RecordedHistoryCoverageTest {
    @Test fun `coverage excludes planned deleted and unrelated account records`() = runTest {
        val dao = FakeTransactionDao()
        val account = UUID.randomUUID()
        val first = Instant.parse("2026-10-01T09:00:00Z")
        dao.save(TransactionEntity(account, TransactionType.INCOME, 50.0, dateTime = first))
        dao.save(TransactionEntity(account, TransactionType.EXPENSE, 50.0, dateTime = first.plusSeconds(3600)))
        dao.save(TransactionEntity(account, TransactionType.INCOME, 10.0, dateTime = first.minusSeconds(86400), isDeleted = true))
        dao.save(TransactionEntity(account, TransactionType.EXPENSE, 10.0, dueDate = first.minusSeconds(86400)))
        dao.save(TransactionEntity(UUID.randomUUID(), TransactionType.INCOME, 10.0, dateTime = first.minusSeconds(86400)))
        assertEquals(first, dao.findFirstSettledTime(listOf(account)))
        assertNull(dao.findFirstSettledTime(emptyList()))
    }

    @Test fun `no live accounts does not issue an unfiltered database query`() = runTest {
        val dao = mockk<TransactionDao>()
        val repo = TransactionRepository(mockk(), dao, mockk(), TestDispatchersProvider, mockk())
        assertNull(repo.findFirstSettledTime(emptyList()))
        coVerify(exactly = 0) { dao.findFirstSettledTime(any()) }
    }

    @Test fun `large account sets use bounded queries and the earliest returned instant`() = runTest {
        val dao = mockk<TransactionDao>()
        val repo = TransactionRepository(mockk(), dao, mockk(), TestDispatchersProvider, mockk())
        val accounts = List(901) { AccountId(UUID.randomUUID()) }
        val first = Instant.parse("2026-09-01T09:00:00Z")
        coEvery { dao.findFirstSettledTime(any()) } answers {
            if (firstArg<List<UUID>>().size == 900) first.plusSeconds(86400) else first
        }
        assertEquals(first, repo.findFirstSettledTime(accounts + accounts.first()))
        coVerify(exactly = 1) { dao.findFirstSettledTime(match { it.size == 900 }) }
        coVerify(exactly = 1) { dao.findFirstSettledTime(match { it.size == 1 }) }
    }
}
