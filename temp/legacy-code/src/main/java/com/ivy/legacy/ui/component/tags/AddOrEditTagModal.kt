package com.ivy.legacy.ui.component.tags

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.system.*
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.modal.*

@Composable
fun BoxWithConstraintsScope.AddOrEditTagModal(
    id: TagId,
    onTagAdd: (TagId, String, (Result<Unit>) -> Unit) -> Unit,
    onTagEdit: (Tag, Tag, (Result<Unit>) -> Unit) -> Unit,
    onTagDelete: (Tag, (Result<Unit>) -> Unit) -> Unit,
    modifier: Modifier = Modifier, visible: Boolean = false, initialTag: Tag? = null,
    submission: EditorSubmission = remember(id) { tagEditorSubmission() },
    onDismiss: () -> Unit,
) {
    val latestId by rememberUpdatedState(id)
    var name by rememberSaveable(id.value.toString()) { mutableStateOf(initialTag?.name?.value.orEmpty()) }
    var deleteVisible by rememberSaveable(id.value.toString()) { mutableStateOf(false) }
    val editable = !submission.saving && !submission.requiresReview
    fun editableNow() = !submission.saving && !submission.requiresReview
    val close = { if (latestId == id && !submission.saving) onDismiss() }
    fun save() {
        val valid = NotBlankTrimmedString.from(name).getOrNull() ?: return
        if (!editableNow() || latestId != id) return
        submission.submit({ complete ->
            if (initialTag == null) onTagAdd(id, valid.value, complete)
            else onTagEdit(initialTag, initialTag.copy(name = valid), complete)
        }, onSuccess = { if (latestId == id) onDismiss() })
    }
    IvyModal(id.value, visible, close, title = if (initialTag == null) "New tag" else "Edit tag", dismissEnabled = !submission.saving,
        hasUnsavedChanges = !submission.requiresReview && name != initialTag?.name?.value.orEmpty(),
        PrimaryAction = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (submission.saving) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.jvy_saving), style = JvyTypography.Supporting)
                }
                submission.error?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error, style = JvyTypography.Supporting) }
                JvyButton(if (submission.requiresReview) close else ::save, Modifier.fillMaxWidth(),
                    enabled = submission.requiresReview || (editable && name.isNotBlank())) { Text(stringResource(R.string.done)) }
            }
        }) {
        OutlinedTextField(name, { if (editableNow()) name = it }, modifier.fillMaxWidth().padding(horizontal = 24.dp),
            enabled = editable, label = { Text("Tag name") }, textStyle = JvyTypography.Body,
            shape = JvyShapes.Control, singleLine = true)
        if (initialTag != null) JvyTextButton({ deleteVisible = true }, Modifier.padding(horizontal = 24.dp), enabled = editable) {
            Text(stringResource(R.string.delete), color = if (editable) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        }
    }
    DeleteModal(visible = visible && deleteVisible, title = stringResource(R.string.confirm_deletion),
        description = "Delete this tag? It will be removed from transactions, but the transactions themselves will stay.",
        dismiss = { deleteVisible = false }) {
        deleteVisible = false
        if (initialTag != null && editableNow()) submission.submit({ complete -> onTagDelete(initialTag, complete) },
            onSuccess = { if (latestId == id) onDismiss() })
    }
}
