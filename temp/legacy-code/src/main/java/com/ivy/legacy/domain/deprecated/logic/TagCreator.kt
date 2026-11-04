package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.data.repository.TagRepository
import com.ivy.data.repository.mapper.TagMapper
import com.ivy.legacy.domain.creation.acknowledgedWrite
import java.util.Locale
import javax.inject.Inject

class TagWriteConflict : IllegalStateException()

class TagCreator @Inject constructor(
    private val repository: TagRepository,
    private val mapper: TagMapper,
    private val atomic: AtomicWriteRepository,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun create(id: TagId, text: String, onResult: (Result<Unit>) -> Unit, refresh: suspend () -> Unit) =
        acknowledgedWrite(dispatchers, onResult, {
            atomic.run {
                // Preserve the existing normalized tag contract and the original creation date.
                val name = NotBlankTrimmedString.from(text.lowercase(Locale.getDefault())).getOrNull()
                    ?: throw IllegalArgumentException()
                val existing = repository.findById(id)
                if (existing != null && existing.name != name) throw TagWriteConflict()
                if (existing == null) repository.save(with(mapper) { createNewTag(tagId = id, name = name) })
            }
        }, { refresh() })

    suspend fun edit(original: Tag, updated: Tag, onResult: (Result<Unit>) -> Unit, refresh: suspend () -> Unit) =
        acknowledgedWrite(dispatchers, onResult, {
            atomic.run {
                val existing = repository.findById(original.id) ?: throw TagWriteConflict()
                require(original.id == updated.id)
                // A retry of the committed edit is safe; an unrelated rename is not.
                if (existing != original && existing != updated) throw TagWriteConflict()
                repository.save(updated)
            }
        }, { refresh() })

    suspend fun delete(tag: Tag, onResult: (Result<Unit>) -> Unit, refresh: suspend () -> Unit) =
        acknowledgedWrite(dispatchers, onResult, { repository.deleteById(tag.id) }, { refresh() })

    suspend fun select(tag: Tag, entity: java.util.UUID, selected: Boolean, onResult: (Result<Unit>) -> Unit,
                       refresh: suspend () -> Unit) = acknowledgedWrite(dispatchers, onResult, {
        atomic.run {
            if (repository.findById(tag.id) == null) throw TagWriteConflict()
            val association = com.ivy.data.model.primitive.AssociationId(entity)
            if (selected) repository.associateTagToEntity(association, tag.id)
            else repository.removeTagAssociation(association, tag.id)
        }
    }, { refresh() })
}
