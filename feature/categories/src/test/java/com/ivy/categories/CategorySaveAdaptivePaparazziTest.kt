package com.ivy.categories

import androidx.compose.runtime.remember
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.system.EditorSubmission
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(TestParameterInjector::class)
class CategorySaveAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(pending: Boolean) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val submission = remember {
                EditorSubmission().apply {
                    if (pending) submit({}, {})
                    else submit({ it(Result.failure(IllegalStateException("Fictional storage error"))) }, {})
                }
            }
            val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
            val category = Category(CategoryId(id),
                NotBlankTrimmedString.from("Household and everyday essentials").getOrNull()!!,
                ColorInt(0xffffcc3d.toInt()), null, 1.0)
            CategoryModal(CategoryModalData(category, id, autoFocusKeyboard = false),
                onCreateCategory = { _, _ -> }, onEditCategory = { _, _ -> },
                dismiss = {}, submission = submission)
        }
    }

    @Test fun pendingWriteKeepsDraftAndDisablesClose() = snapshot(pending = true)
    @Test fun failedWriteKeepsRetryVisible() = snapshot(pending = false)
}
