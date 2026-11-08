package com.ivy.data.repository

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RepositoryMemoInvalidationTest {
    private val original = Category(CategoryId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        NotBlankTrimmedString.unsafe("Fictional"), ColorInt(0), null, 1.0)
    private val changed = original.copy(orderNum = 2.0)
    private fun memo() = RepositoryMemoFactory(DataObserver(), TestDispatchersProvider)
        .createMemo(DataWriteEvent::SaveCategories, DataWriteEvent::DeleteCategories)

    @Test fun `invalidation discards loaded state without deleting durable data`() = runTest {
        val memo = memo()
        memo.findAll({ listOf(original) }, { toList() })
        memo.invalidate()
        assertFalse(memo.findAllMemoized); assertTrue(memo.items.isEmpty())
        assertEquals(listOf(changed), memo.findAll({ listOf(changed) }, { toList() }))
    }

    @Test fun `late list read cannot repopulate cache after invalidation`() = runTest {
        val memo = memo()
        val started = CompletableDeferred<Unit>(); val resume = CompletableDeferred<Unit>()
        val old = async { memo.findAll({ started.complete(Unit); resume.await(); listOf(original) }, { toList() }) }
        started.await(); memo.invalidate(); resume.complete(Unit); old.await()
        assertFalse(memo.findAllMemoized); assertTrue(memo.items.isEmpty())
        assertEquals(listOf(changed), memo.findAll({ listOf(changed) }, { toList() }))
    }

    @Test fun `late single read cannot repopulate cache after invalidation`() = runTest {
        val memo = memo()
        val started = CompletableDeferred<Unit>(); val resume = CompletableDeferred<Unit>()
        val old = async { memo.findById(original.id) { started.complete(Unit); resume.await(); original } }
        started.await(); memo.invalidate(); resume.complete(Unit); old.await()
        assertTrue(memo.items.isEmpty())
        assertEquals(changed, memo.findById(original.id) { changed })
    }

    @Test fun `late list read cannot overwrite a committed update`() = runTest {
        val memo = memo()
        val started = CompletableDeferred<Unit>(); val resume = CompletableDeferred<Unit>()
        val old = async { memo.findAll({ started.complete(Unit); resume.await(); listOf(original) }, { toList() }) }
        started.await(); memo.save(changed) { }; resume.complete(Unit); old.await()
        assertEquals(changed, memo.items[original.id]); assertFalse(memo.findAllMemoized)
    }

    @Test fun `late single read cannot resurrect a committed deletion`() = runTest {
        val memo = memo()
        val started = CompletableDeferred<Unit>(); val resume = CompletableDeferred<Unit>()
        val old = async { memo.findById(original.id) { started.complete(Unit); resume.await(); original } }
        started.await(); memo.deleteById(original.id) { }; resume.complete(Unit); old.await()
        assertTrue(memo.items.isEmpty())
    }
}
