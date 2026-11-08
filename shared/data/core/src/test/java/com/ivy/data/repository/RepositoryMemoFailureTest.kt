package com.ivy.data.repository

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Failed writes must not make a fully memoized wallet disagree with persisted data. */
class RepositoryMemoFailureTest {
    private val original = Category(
        id = CategoryId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        name = NotBlankTrimmedString.from("Household").getOrNull()!!,
        color = ColorInt(0), icon = null, orderNum = 1.0,
    )
    private fun memo() = RepositoryMemoFactory(DataObserver(), TestDispatchersProvider)
        .createMemo(DataWriteEvent::SaveCategories, DataWriteEvent::DeleteCategories)

    @Test fun `failed single delete retains an already loaded wallet item`() = runTest {
        val memo = memo()
        memo.findAll({ listOf(original) }, { toList() })
        try {
            memo.deleteById(original.id) { throw IllegalStateException("Storage unavailable") }
            fail("Expected failure")
        } catch (_: IllegalStateException) { }
        assertEquals(listOf(original), memo.findAll({ fail("Cache was loaded"); emptyList() }, { toList() }))
    }

    @Test fun `failed delete all does not clear the in-memory wallet`() = runTest {
        val memo = memo()
        memo.findAll({ listOf(original) }, { toList() })
        try {
            memo.deleteAll { throw IllegalStateException("Storage unavailable") }
            fail("Expected failure")
        } catch (_: IllegalStateException) { }
        assertEquals(original, memo.items[original.id])
        assertTrue(memo.findAllMemoized)
    }

    @Test fun `cancelling before delete commits leaves the item available`() = runTest {
        val memo = memo()
        memo.findAll({ listOf(original) }, { toList() })
        try {
            memo.deleteById(original.id) { throw CancellationException() }
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        assertEquals(original, memo.items[original.id])
    }

    @Test fun `successful delete changes the memo only after the write`() = runTest {
        val memo = memo()
        memo.findAll({ listOf(original) }, { toList() })
        memo.deleteById(original.id) { assertEquals(original, memo.items[original.id]) }
        assertTrue(memo.items.isEmpty())
    }

    @Test fun `successful delete all changes the memo only after the write`() = runTest {
        val memo = memo()
        memo.findAll({ listOf(original) }, { toList() })
        memo.deleteAll { assertEquals(original, memo.items[original.id]) }
        assertTrue(memo.items.isEmpty())
    }

    @Test fun `failed update keeps the previous version instead of the unsaved draft`() = runTest {
        val memo = memo()
        memo.findAll({ listOf(original) }, { toList() })
        val changed = original.copy(orderNum = 2.0)
        try {
            memo.save(changed) { throw IllegalStateException() }
            fail("Expected failure")
        } catch (_: IllegalStateException) { }
        assertEquals(original, memo.items[original.id])
    }

    @Test fun `failed batch update keeps the previous loaded wallet`() = runTest {
        val memo = memo()
        memo.findAll({ listOf(original) }, { toList() })
        try {
            memo.saveMany(listOf(original.copy(orderNum = 2.0))) { throw IllegalStateException() }
            fail("Expected failure")
        } catch (_: IllegalStateException) { }
        assertEquals(listOf(original), memo.items.values.toList())
    }
}
