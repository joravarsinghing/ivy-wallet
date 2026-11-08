package com.ivy.legacy.ui.component.tags

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.design.system.*
import com.ivy.legacy.ui.SearchInput
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.modal.IvyModal
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxWithConstraintsScope.ShowTagModal(
    onDismiss: () -> Unit, allTagList: ImmutableList<Tag>, selectedTagList: ImmutableList<TagId>,
    onTagSelected: (Tag, (Result<Unit>) -> Unit) -> Unit, onTagDeSelected: (Tag, (Result<Unit>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    id: UUID = UUID.fromString(rememberSaveable { UUID.randomUUID().toString() }),
    visible: Boolean = false, selectOnlyMode: Boolean = false,
    editorHost: TagEditorHost = remember { TagEditorHost() },
    onTagAdd: (TagId, String, (Result<Unit>) -> Unit) -> Unit = { _, _, complete -> complete(Result.failure(IllegalStateException())) },
    onTagEdit: (Tag, Tag, (Result<Unit>) -> Unit) -> Unit = { _, _, complete -> complete(Result.failure(IllegalStateException())) },
    onTagDelete: (Tag, (Result<Unit>) -> Unit) -> Unit = { _, complete -> complete(Result.failure(IllegalStateException())) },
    onTagSearch: (String) -> Unit,
) {
    var query by rememberSaveable(id.toString(), stateSaver = androidx.compose.ui.text.input.TextFieldValue.Saver) {
        mutableStateOf(selectEndTextFieldValue(""))
    }
    val draft = editorHost.draft
    val selection = editorHost.selection
    val pending = draft?.submission?.saving == true || selection.saving
    IvyModal(id, visible, onDismiss, title = stringResource(R.string.tags), dismissEnabled = !pending, PrimaryAction = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selection.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            selection.error?.let {
                Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error, style = JvyTypography.Supporting)
            }
            JvyButton(onDismiss, Modifier.fillMaxWidth(), enabled = !pending) { Text(stringResource(R.string.done)) }
        }
    }) {
        SearchInput(searchQueryTextFieldValue = query, hint = stringResource(R.string.search_tags),
            horizontalPadding = JvySpacing.PageGutter,
            onSetSearchQueryTextField = { query = it; onTagSearch(it.text) })
        Spacer(Modifier.height(JvySpacing.Group))
        Column(modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!selectOnlyMode) JvyOutlinedButton({ editorHost.open() }, enabled = !pending) { Text(stringResource(R.string.add_new)) }
            if (allTagList.isEmpty()) Text("No matching tags", style = JvyTypography.Supporting)
            allTagList.forEach { tag ->
                val selected = tag.id in selectedTagList
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    JvyOutlinedButton({
                        selection.submit({ complete ->
                            if (selected) onTagDeSelected(tag, complete) else onTagSelected(tag, complete)
                        }, {})
                    }, Modifier.weight(1f).semantics { this.selected = selected }, enabled = !pending,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)) {
                        Text("#"+tag.name.value, style = JvyTypography.Body)
                    }
                    if (!selectOnlyMode) JvyTextButton({ editorHost.open(tag) }, enabled = !pending) {
                        Text(stringResource(R.string.edit))
                    }
                }
            }
        }
    }
    if (draft != null && !selectOnlyMode) AddOrEditTagModal(id = draft.id, visible = visible,
        initialTag = draft.original, submission = draft.submission, onTagAdd = onTagAdd,
        onTagEdit = onTagEdit, onTagDelete = onTagDelete, onDismiss = { editorHost.close(draft.id) })
}
