package com.ivy.categories

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.data.model.TagId
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ui.component.tags.AddOrEditTagModal
import com.ivy.legacy.ui.component.tags.tagEditorSubmission
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.domain.deprecated.logic.TagWriteConflict
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(TestParameterInjector::class)
class TagEditorAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(state: Int) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val submission = androidx.compose.runtime.remember {
                tagEditorSubmission().apply {
                    if (state == 0) submit({}, {})
                    else submit({ it(Result.failure(if (state == 2) TagWriteConflict() else IllegalStateException("Fictional failure"))) }, {})
                }
            }
            val tag = com.ivy.data.model.Tag(TagId(UUID.randomUUID()),
                com.ivy.data.model.primitive.NotBlankTrimmedString.unsafe("Fictional travel fund"), null,
                com.ivy.data.model.primitive.ColorInt(0), null, 0.0, java.time.Instant.parse("2026-10-31T12:00:00Z"))
            AddOrEditTagModal(tag.id, visible = true, initialTag = tag, submission = submission,
                onTagAdd = { _, _, _ -> }, onTagEdit = { _, _, _ -> }, onTagDelete = { _, _ -> }, onDismiss = {})
        }
    }
    @Test fun pendingTag() = snapshot(0)
    @Test fun failedTag() = snapshot(1)
    @Test fun reviewTag() = snapshot(2)
}
