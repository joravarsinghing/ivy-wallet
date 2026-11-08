package com.ivy.wallet.domain.deprecated.logic

import androidx.compose.ui.graphics.Color
import com.ivy.base.TestDispatchersProvider
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.CategoryRepository
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class CategoryCreatorTest {
    private val store = mutableMapOf<CategoryId, Category>()
    private val repository = mockk<CategoryRepository>()
    private val creator = CategoryCreator(repository, TestDispatchersProvider)
    private val data = CreateCategoryData("Household", Color(0xff18ba92), null,
        UUID.fromString("00000000-0000-0000-0000-000000000001"))

    init {
        coEvery { repository.findById(any()) } answers { store[CategoryId(firstArg<UUID>())] }
        coEvery { repository.findMaxOrderNum() } returns 4.0
        coEvery { repository.save(any()) } answers { firstArg<Category>().let { store[it.id] = it } }
    }

    @Test fun `success is acknowledged once after storage and refresh`() = runTest {
        var refreshed = false
        val results = mutableListOf<Result<Unit>>()
        creator.createCategory(data, onResult = { it.getOrThrow(); assertTrue(refreshed); results += it }) {
            assertEquals(it, store[it.id]); refreshed = true
        }
        assertEquals(1, results.size)
        assertTrue(results.single().isSuccess)
        assertEquals(CategoryId(data.id), store.values.single().id)
    }

    @Test fun `storage failure is acknowledged without closing or refreshing as if successful`() = runTest {
        coEvery { repository.save(any()) } throws IllegalStateException("Fictional storage failure")
        val results = mutableListOf<Result<Unit>>()
        creator.createCategory(data, results::add) { fail("Must not refresh a failed write") }
        assertEquals(1, results.size)
        assertTrue(results.single().isFailure)
        assertTrue(store.isEmpty())
        coVerify(exactly = 1) { repository.save(any()) }
    }

    @Test fun `retry after an ambiguous storage acknowledgement retains one record and its order`() = runTest {
        var first = true
        coEvery { repository.save(any()) } answers {
            firstArg<Category>().let { store[it.id] = it }
            if (first) { first = false; throw IllegalStateException("Reply interrupted after commit") }
        }
        val results = mutableListOf<Result<Unit>>()
        creator.createCategory(data, results::add) {}
        val order = store.values.single().orderNum
        creator.createCategory(data, results::add) {}
        assertEquals(1, store.size)
        assertEquals(order, store.values.single().orderNum, 0.0)
        assertTrue(results[0].isFailure)
        assertTrue(results[1].isSuccess)
        coVerify(exactly = 1) { repository.findMaxOrderNum() }
    }

    @Test fun `a failed optional refresh cannot report an already committed write as failed`() = runTest {
        var result: Result<Unit>? = null
        creator.createCategory(data, { result = it }) { throw IllegalStateException("Refresh unavailable") }
        assertEquals(1, store.size)
        assertTrue(result!!.isSuccess)
    }

    @Test fun `blank names fail without writing or claiming success`() = runTest {
        var result: Result<Unit>? = null
        creator.createCategory(data.copy(name = "  "), { result = it }) { fail("Invalid data") }
        assertTrue(result!!.isFailure)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test fun `editing retains identity and only acknowledges the updated record`() = runTest {
        val category = Category(CategoryId(data.id), NotBlankTrimmedString.from("Renamed").getOrNull()!!,
            ColorInt(0), null, 7.0)
        var result: Result<Unit>? = null
        creator.editCategory(category, { result = it }) { assertEquals(category, it) }
        assertEquals(category, store.values.single())
        assertTrue(result!!.isSuccess)
    }

    @Test fun `cancellation is not swallowed or turned into a successful save`() = runTest {
        coEvery { repository.save(any()) } throws CancellationException()
        try {
            creator.createCategory(data, { fail("Cancelled owner must not get a false result") }) {}
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }

    @Test fun `concurrent attempts for one operation cannot allocate multiple records or order positions`() = runTest {
        coEvery { repository.save(any()) } coAnswers {
            delay(10)
            firstArg<Category>().let { store[it.id] = it }
        }
        listOf(async { creator.createCategory(data) {} }, async { creator.createCategory(data) {} }).awaitAll()
        assertEquals(1, store.size)
        coVerify(exactly = 1) { repository.findMaxOrderNum() }
    }
}
