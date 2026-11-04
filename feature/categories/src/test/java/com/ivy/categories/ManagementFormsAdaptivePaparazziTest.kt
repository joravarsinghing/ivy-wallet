package com.ivy.categories

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Actual management forms, not a generic dialog-body proxy. */
@RunWith(TestParameterInjector::class)
class ManagementFormsAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val theme get() = if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT
    @Test fun editAccountWithLongNameAndLargeBalance() = render {
        IvyWalletPreview(theme) {
            AccountModal(AccountModalData(Account("Everyday spending and household account", color = 0xff18ba92.toInt(), currency = "AED"),
                "AED", 1234567.89, autoFocusKeyboard = false,
                id = UUID.fromString("00000000-0000-0000-0000-000000000001")),
                onCreateAccount = { _, _ -> }, onEditAccount = { _, _, _ -> }, dismiss = {})
        }
    }
    @Test fun newCategoryForm() = render {
        IvyWalletPreview(theme) {
            CategoryModal(CategoryModalData(null, UUID.fromString("00000000-0000-0000-0000-000000000002"), false),
                onCreateCategory = { _, _ -> }, onEditCategory = { _, _ -> }, dismiss = {})
        }
    }
}
