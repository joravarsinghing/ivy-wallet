package com.ivy.planned.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.IntervalType
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.navigation.EditPlannedScreen
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class PlannedEditorAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(error: Boolean) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
            val account = Account("Everyday account with a longer name", color = 0xff18ba92.toInt(), currency = "AED", id = id)
            UI(EditPlannedScreen(id, TransactionType.EXPENSE), EditPlannedScreenState(
                currency = "AED", transactionType = TransactionType.EXPENSE,
                startDate = LocalDateTime.of(2026, 11, 1, 12, 0), intervalN = 1,
                intervalType = IntervalType.MONTH, oneTime = false,
                initialTitle = "Monthly home rent and household planning", description = "A recurring payment with a long explanation.",
                categories = persistentListOf(), accounts = persistentListOf(account), account = account,
                category = null, amount = 125000.25, categoryModalVisible = false,
                descriptionModalVisible = false, deleteTransactionModalVisible = false,
                categoryModalData = null, accountModalData = null, recurringRuleModalData = null,
                transactionTypeModalVisible = false, amountModalVisible = false,
                saveError = if (error) "Could not save. Your changes are still here. Try again." else null,
            )) {}
        }
    }
    @Test fun populated() = snapshot(false)
    @Test fun failedSaveKeepsTheEditor() = snapshot(true)
    @Test fun openingEntryHasOneBackAndNoActiveFields() = render {
        com.ivy.design.system.JvyEditorLoading("Planned payment", false, {}, {})
    }
    @Test fun failedOpeningOffersRetryAndBack() = render {
        com.ivy.design.system.JvyEditorLoading("Planned payment", true, {}, {})
    }
}
