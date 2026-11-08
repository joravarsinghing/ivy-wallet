package com.ivy.legacy.ui.component.tags

import androidx.compose.runtime.*
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.design.system.EditorSubmission
import com.ivy.wallet.domain.deprecated.logic.TagWriteConflict
import java.util.UUID

fun tagEditorSubmission() = EditorSubmission(terminalFailure = { it is TagWriteConflict }, failureMessage = {
    if (it is TagWriteConflict) "This tag has changed or was removed. Close this form and review the current tags."
    else "Could not save. Your changes are still here. Try again."
})

class TagEditorDraft(val original: Tag?, val id: TagId = original?.id ?: TagId(UUID.randomUUID()),
                     val submission: EditorSubmission = tagEditorSubmission())

@Stable
class TagEditorHost {
    val selection = EditorSubmission()
    var draft: TagEditorDraft? by mutableStateOf(null)
    fun open(tag: Tag? = null) { if (draft?.submission?.saving != true) draft = TagEditorDraft(tag) }
    fun close(expected: TagId) { if (draft?.id == expected && draft?.submission?.saving != true) draft = null }
}
