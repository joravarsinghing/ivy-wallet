package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.data.repository.TagRepository
import com.ivy.data.repository.mapper.TagMapper
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class TagCreatorTest {
    private val id = TagId(UUID.randomUUID())
    private var stored: Tag? = null
    private val repository = mockk<TagRepository>()
    private val mapper = mockk<TagMapper>()
    private val atomic = mockk<AtomicWriteRepository>()
    private val creator = TagCreator(repository, mapper, atomic, TestDispatchersProvider)
    init {
        coEvery { atomic.run<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coEvery { repository.findById(id) } answers { stored }
        coEvery { repository.save(any()) } answers { stored = firstArg() }
        coEvery { repository.deleteById(id) } answers { stored = null }
        every { with(mapper) { createNewTag(id, any()) } } answers {
            Tag(id, NotBlankTrimmedString.unsafe(secondArg<String>()), null, ColorInt(0), null, 0.0, Instant.parse("2026-10-31T12:00:00Z"))
        }
    }
    @Test fun `creation retries retain one tag identity and creation timestamp`() = runTest {
        creator.create(id, " Fictional TRIP ", { it.getOrThrow() }) {}
        val created = stored
        creator.create(id, "fictional trip", { it.getOrThrow() }) {}
        assertEquals(created, stored)
        assertEquals(id, stored!!.id)
        coVerify(exactly = 1) { repository.save(any()) }
    }
    @Test fun `failed create retains identity for successful retry`() = runTest {
        coEvery { repository.save(any()) } throws IllegalStateException("Fictional failure")
        creator.create(id, "trip", { assertTrue(it.isFailure) }) { fail() }
        assertNull(stored)
        coEvery { repository.save(any()) } answers { stored = firstArg() }
        creator.create(id, "trip", { it.getOrThrow() }) {}
        assertEquals(id, stored!!.id)
    }
    @Test fun `changed committed intent cannot rename a tag as creation retry`() = runTest {
        creator.create(id, "trip", { it.getOrThrow() }) {}
        creator.create(id, "other", { assertTrue(it.exceptionOrNull() is TagWriteConflict) }) { fail() }
        assertEquals("trip", stored!!.name.value)
    }
    @Test fun `edit is acknowledged and retry preserves metadata`() = runTest {
        creator.create(id, "trip", { it.getOrThrow() }) {}
        val old = stored!!
        val updated = old.copy(name = NotBlankTrimmedString.unsafe("New name"))
        repeat(2) { creator.edit(old, updated, { it.getOrThrow() }) {} }
        assertEquals(updated, stored)
    }
    @Test fun `missing tag edit cannot recreate it`() = runTest {
        creator.create(id, "trip", { it.getOrThrow() }) {}
        val original = stored!!; stored = null
        creator.edit(original, original, { assertTrue(it.exceptionOrNull() is TagWriteConflict) }) { fail() }
        assertNull(stored)
    }
    @Test fun `delete failure does not refresh or pretend success`() = runTest {
        creator.create(id, "trip", { it.getOrThrow() }) {}
        coEvery { repository.deleteById(id) } throws IllegalStateException("Fictional failure")
        creator.delete(stored!!, { assertTrue(it.isFailure) }) { fail() }
        assertNotNull(stored)
    }
    @Test fun `refresh failure after commit stays successful`() = runTest {
        creator.create(id, "trip", { it.getOrThrow() }) { error("Fictional refresh failure") }
        assertNotNull(stored)
    }
    @Test fun `cancelled write does not turn into a false acknowledgement`() = runTest {
        coEvery { repository.save(any()) } throws CancellationException()
        try { creator.create(id, "trip", { fail() }) { fail() }; fail() } catch (_: CancellationException) { }
    }
    @Test fun `selection failure is acknowledged without claiming a changed association`() = runTest {
        creator.create(id, "trip", { it.getOrThrow() }) {}
        coEvery { repository.associateTagToEntity(any(), id) } throws IllegalStateException("Fictional association failure")
        creator.select(stored!!, UUID.randomUUID(), true, { assertTrue(it.isFailure) }) { fail() }
    }
    @Test fun `removed tag cannot gain new associations`() = runTest {
        creator.create(id, "trip", { it.getOrThrow() }) {}
        val tag = stored!!; stored = null
        creator.select(tag, UUID.randomUUID(), true, { assertTrue(it.exceptionOrNull() is TagWriteConflict) }) { fail() }
        coVerify(exactly = 0) { repository.associateTagToEntity(any(), any()) }
    }
}
