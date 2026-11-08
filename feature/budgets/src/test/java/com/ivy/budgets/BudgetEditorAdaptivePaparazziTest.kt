package com.ivy.budgets

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Budget
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(TestParameterInjector::class)
class BudgetEditorAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(failure: Boolean) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            BudgetModal(BudgetModalData(Budget("Food, household and everyday shopping", 15000.25, null, null, orderId = 0.0),
                "AED", emptyList(), emptyList(), UUID.fromString("00000000-0000-0000-0000-000000000003"), false),
                onCreate = {}, onEdit = {}, onDelete = {},
                saveError = if (failure) "Could not save. Your changes are still here. Try again." else null,
                dismiss = {})
        }
    }
    @Test fun editBudget() = snapshot(false)
    @Test fun failedSave() = snapshot(true)
}
