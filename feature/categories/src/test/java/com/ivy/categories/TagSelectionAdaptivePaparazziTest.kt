package com.ivy.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.remember
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ui.component.tags.ShowTagModal
import com.ivy.legacy.ui.component.tags.TagEditorHost
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class TagSelectionAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val first = Tag(TagId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        NotBlankTrimmedString.unsafe("Fictional household spending with a long name"), null, ColorInt(0), null,
        0.0, Instant.parse("2026-10-31T12:00:00Z"))
    private val second = first.copy(id = TagId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        name = NotBlankTrimmedString.unsafe("Fictional travel"))

    private fun snapshot(empty: Boolean = false, pending: Boolean = false) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val host = remember { TagEditorHost().apply { if (pending) selection.submit({}, {}) } }
            ShowTagModal(onDismiss = {}, allTagList = if (empty) persistentListOf() else persistentListOf(first, second),
                selectedTagList = if (empty) persistentListOf() else persistentListOf(first.id),
                onTagSelected = { _, complete -> complete(Result.success(Unit)) },
                onTagDeSelected = { _, complete -> complete(Result.success(Unit)) },
                visible = true, editorHost = host, onTagSearch = {})
        }
    }
    @Test fun emptySelection() = snapshot(empty = true)
    @Test fun selectedTagsAndExplicitEdit() = snapshot()
    @Test fun pendingSelectionDisablesActions() = snapshot(pending = true)
}
