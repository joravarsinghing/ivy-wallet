package com.ivy.categories

import androidx.compose.runtime.remember
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.data.repository.AccountWriteConflict
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.accountEditorSubmission
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class AccountSaveAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(state: Int) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val submission = remember {
                accountEditorSubmission().apply {
                    if (state == 0) submit({}, {})
                    else submit({ it(Result.failure(if (state == 2) AccountWriteConflict.AlreadySaved()
                        else IllegalStateException("Fictional storage failure"))) }, {})
                }
            }
            AccountModal(AccountModalData(Account("Everyday household spending account", color = 0xffffcc3d.toInt(),
                currency = "AED"), "AED", 123456.75, autoFocusKeyboard = false),
                onCreateAccount = { _, _ -> }, onEditAccount = { _, _, _ -> }, dismiss = {}, submission = submission)
        }
    }
    @Test fun pendingSaveDisablesEditingAndClose() = snapshot(0)
    @Test fun failedSaveRetainsDraftAndRetry() = snapshot(1)
    @Test fun committedConflictOffersCloseAndReview() = snapshot(2)
}
