package com.ivy.data.repository

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Fake rollback verifies orchestration. SQLite execution has its own device gate. */
class AtomicWriteRepositoryTest {
    private val row = Category(CategoryId(UUID.randomUUID()), NotBlankTrimmedString.unsafe("Fictional loans"), ColorInt(0), null, 0.0)
    private val changed = row.copy(name = NotBlankTrimmedString.unsafe("Updated"))
    private val rows = mutableMapOf(row.id to row)
    private val events = mutableListOf<DataWriteEvent>()
    private var cancelAfterCommit = false
    private var transactionCount = 0
    private val observer = mockk<DataObserver>()
    private val memo = RepositoryMemoFactory(observer, TestDispatchersProvider)
        .createMemo(DataWriteEvent::SaveCategories, DataWriteEvent::DeleteCategories)
    private val atomic = AtomicWriteRepository({ block ->
        transactionCount++
        val before = rows.toMap()
        try { block() } catch (failure: Throwable) { rows.clear(); rows.putAll(before); throw failure }
        if (cancelAfterCommit) throw CancellationException()
    }, TestDispatchersProvider)

    init { coEvery { observer.post(any()) } answers { events += firstArg<DataWriteEvent>(); Unit } }
    private suspend fun read() = memo.findAll({ rows.values.toList() }, { toList() })
    private suspend fun write(value: Category) = memo.save(value) { rows[it.id] = it }

    @Test fun `staged writes are visible internally but publish only after commit`() = runTest {
        read()
        atomic.run {
            write(changed)
            assertTrue(events.isEmpty())
            assertEquals(row, memo.items[row.id])
            assertEquals(listOf(changed), read())
            assertEquals(changed, memo.findById(row.id) { rows[it] })
        }
        assertEquals(listOf(changed), read())
        assertEquals(listOf(DataWriteEvent.SaveCategories(listOf(changed))), events)
    }

    @Test fun `related failure rolls back and never caches or announces the draft`() = runTest {
        read()
        try { atomic.run { write(changed); error("Fictional related failure") }; fail() } catch (_: IllegalStateException) { }
        assertEquals(listOf(row), read())
        assertEquals(row, rows[row.id])
        assertTrue(events.isEmpty())
    }

    @Test fun `cancelled commit acknowledgement discards stale memo without inventing rollback`() = runTest {
        read()
        cancelAfterCommit = true
        try { atomic.run { write(changed) }; fail() } catch (_: CancellationException) { }
        assertEquals(changed, rows[row.id])
        assertEquals(listOf(changed), read())
        assertTrue(events.isEmpty())
    }

    @Test fun `nested related operations share one transaction and notification boundary`() = runTest {
        atomic.run { atomic.run { write(changed) }; assertTrue(events.isEmpty()) }
        assertEquals(1, transactionCount)
        assertEquals(1, events.size)
    }

    @Test fun `concurrent operations cannot overlap their transaction body`() = runTest {
        var inFlight = 0
        var peak = 0
        List(3) { async { atomic.run { inFlight++; peak = maxOf(peak, inFlight); delay(10); inFlight-- } } }.awaitAll()
        assertEquals(1, peak)
        assertEquals(3, transactionCount)
    }

    @Test fun `rolled back delete retains associations owner and memoized row`() = runTest {
        read()
        try {
            atomic.run {
                memo.deleteById(row.id) { rows.remove(it); Unit }
                assertTrue(events.isEmpty())
                error("Fictional second delete failure")
            }
            fail()
        } catch (_: IllegalStateException) { }
        assertEquals(listOf(row), read())
        assertTrue(events.isEmpty())
    }

    @Test fun `batch and delete all events wait for durable transaction`() = runTest {
        atomic.run {
            memo.saveMany(listOf(changed)) { it.forEach { rows[it.id] = it } }
            memo.deleteAll { rows.clear() }
            assertTrue(events.isEmpty())
        }
        assertTrue(read().isEmpty())
        assertEquals(2, events.size)
    }
}
