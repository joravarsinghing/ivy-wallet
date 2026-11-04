package com.ivy.wallet.domain.deprecated.logic

import androidx.compose.ui.graphics.Color
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.model.LoanRecordType
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.dao.read.LoanRecordDao
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.db.entity.LoanEntity
import com.ivy.data.db.entity.LoanRecordEntity
import com.ivy.data.model.LoanType
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanRecordData
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/** Creator contracts; actual transaction/memo boundary is covered in data-core. */
class LoanPersistenceTest {
    private val rows = mutableMapOf<UUID, LoanEntity>()
    private val records = mutableMapOf<UUID, LoanRecordEntity>()
    private val loanDao = mockk<LoanDao>()
    private val recordDao = mockk<LoanRecordDao>()
    private val loanWriter = mockk<WriteLoanDao>()
    private val recordWriter = mockk<WriteLoanRecordDao>()
    private val atomic = mockk<AtomicWriteRepository>()
    private val creator = LoanCreator(loanDao, loanWriter, recordDao, recordWriter, atomic, TestDispatchersProvider)
    private val recordCreator = LoanRecordCreator(recordWriter, recordDao, loanDao, atomic, TestDispatchersProvider)
    private val data = CreateLoanData("Fictional family loan", 500.0, LoanType.BORROW, Color(0xff18ba92), null,
        note = null, dateTime = LocalDateTime.of(2026, 10, 31, 9, 15))
    private val record = CreateLoanRecordData(null, 75.0, Instant.parse("2026-11-01T12:00:00Z"), loanRecordType = LoanRecordType.DECREASE)

    init {
        coEvery { atomic.run<Any?>(any()) } coAnswers {
            val beforeRows = rows.toMap(); val beforeRecords = records.toMap()
            try { firstArg<suspend () -> Any?>().invoke() } catch (failure: Throwable) {
                rows.clear(); rows.putAll(beforeRows); records.clear(); records.putAll(beforeRecords); throw failure
            }
        }
        coEvery { loanDao.findById(any()) } answers { rows[firstArg()] }
        coEvery { loanDao.findMaxOrderNum() } returns 4.0
        coEvery { recordDao.findById(any()) } answers { records[firstArg()] }
        coEvery { recordDao.findAllByLoanId(any()) } answers { records.values.filter { it.loanId == firstArg<UUID>() } }
        coEvery { loanWriter.save(any()) } answers { firstArg<LoanEntity>().let { rows[it.id] = it } }
        coEvery { recordWriter.save(any()) } answers { firstArg<LoanRecordEntity>().let { records[it.id] = it } }
        coEvery { loanWriter.deleteById(any()) } answers { rows.remove(firstArg<UUID>()); Unit }
        coEvery { recordWriter.deleteById(any()) } answers { records.remove(firstArg<UUID>()); Unit }
    }

    @Test fun `loan success waits for related write then refresh`() = runTest {
        var related = false; var refreshed = false; var result: Result<Unit>? = null
        val id = creator.create(data, { assertTrue(related); assertTrue(refreshed); result = it },
            relatedWrite = { assertNull(result); assertEquals(data.id, it.id); related = true }) { refreshed = true }
        assertEquals(data.id, id); assertTrue(result!!.isSuccess)
    }
    @Test fun `related transaction failure is not a successful partial loan`() = runTest {
        var result: Result<Unit>? = null
        assertNull(creator.create(data, { result = it }, relatedWrite = { error("Fictional transaction failure") }) { fail() })
        assertTrue(result!!.isFailure); assertTrue(rows.isEmpty())
    }
    @Test fun `creation retry has one identity order and linked transaction`() = runTest {
        var related = 0
        repeat(2) { creator.create(data, { it.getOrThrow() }, relatedWrite = { related++ }) {} }
        assertEquals(1, rows.size); assertEquals(1, related)
        coVerify(exactly = 1) { loanDao.findMaxOrderNum() }
    }
    @Test fun `changed committed creation is review not overwrite`() = runTest {
        creator.create(data) {}
        var result: Result<Unit>? = null
        creator.create(data.copy(amount = 900.0), { result = it }) { fail() }
        assertTrue(result!!.exceptionOrNull() is LoanWriteConflict)
        assertEquals(500.0, rows.values.single().amount, 0.0)
    }
    @Test fun `missing loan edit cannot resurrect deleted data`() = runTest {
        creator.create(data) {}
        val loan = rows.values.single().toLegacyDomain(); rows.clear()
        var result: Result<Unit>? = null
        creator.edit(loan, { result = it }, relatedWrite = { fail() }) { fail() }
        assertTrue(result!!.exceptionOrNull() is LoanWriteConflict); assertTrue(rows.isEmpty())
    }
    @Test fun `nonfinite loan amounts cannot reach storage`() = runTest {
        for (amount in listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -1.0)) {
            var result: Result<Unit>? = null
            creator.create(data.copy(amount = amount), { result = it }) { fail() }
            assertTrue(result!!.isFailure)
        }
        assertTrue(rows.isEmpty())
    }
    @Test fun `record cannot be saved without its parent`() = runTest {
        var result: Result<Unit>? = null
        recordCreator.create(data.id, record, { result = it }) { fail() }
        assertTrue(result!!.exceptionOrNull() is LoanWriteConflict); assertTrue(records.isEmpty())
    }
    @Test fun `record related failure rolls back and retry retains identity`() = runTest {
        creator.create(data) {}
        recordCreator.create(data.id, record, { assertTrue(it.isFailure) }, relatedWrite = { error("Fictional failure") }) { fail() }
        assertTrue(records.isEmpty())
        repeat(2) { recordCreator.create(data.id, record, { it.getOrThrow() }) {} }
        assertEquals(record.id, records.values.single().id)
    }
    @Test fun `record conversions must remain positive and finite`() = runTest {
        creator.create(data) {}
        for (amount in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0)) {
            recordCreator.create(data.id, record.copy(convertedAmount = amount), { assertTrue(it.isFailure) }) { fail() }
        }
        assertTrue(records.isEmpty())
    }
    @Test fun `replayed repayment does not fetch a new quote or rewrite conversion`() = runTest {
        creator.create(data) {}
        var quotes = 0
        repeat(2) {
            recordCreator.create(data.id, record, { it.getOrThrow() }, prepare = {
                quotes++
                it.copy(convertedAmount = 18.25)
            }) {}
        }
        assertEquals(1, quotes)
        assertEquals(18.25, records.values.single().convertedAmount!!, 0.0)
    }
    @Test fun `loan deletion removes its records only after related deletion succeeds`() = runTest {
        creator.create(data) {}; recordCreator.create(data.id, record) {}
        val loan = rows.values.single().toLegacyDomain()
        creator.delete(loan, { assertTrue(it.isFailure) }, relatedWrite = { error("Fictional failure") }) { fail() }
        assertEquals(1, rows.size); assertEquals(1, records.size)
        creator.delete(loan, { it.getOrThrow() }) {}
        assertTrue(rows.isEmpty()); assertTrue(records.isEmpty())
    }
    @Test fun `refresh failure after commit cannot cause duplicate retry`() = runTest {
        creator.create(data, { it.getOrThrow() }) { error("Fictional refresh failure") }
        assertEquals(1, rows.size)
    }
    @Test fun `cancellation is rethrown rather than printed or falsely acknowledged`() = runTest {
        try {
            creator.create(data, { fail() }, relatedWrite = { throw CancellationException() }) { fail() }
            fail()
        } catch (_: CancellationException) { }
        assertTrue(rows.isEmpty())
    }
}
