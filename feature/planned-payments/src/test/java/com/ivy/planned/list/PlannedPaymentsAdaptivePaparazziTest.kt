package com.ivy.planned.list

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(TestParameterInjector::class)
class PlannedPaymentsAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(populated: Boolean, large: Boolean = false) = render {
        val theme = if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT
        IvyWalletPreview(theme) {
            val account = Account(name = "Everyday bank", color = 0xff168059.toInt())
            val amount = if (large) 12500000.75 else 500.0
            val rule = PlannedPaymentRule(accountId = account.id,
                title = if (large) "A longer planned payment for the entire family's travel and accommodation" else "Rent",
                amount = amount, startDate = Instant.parse("2026-10-29T12:00:00Z"), oneTime = true,
                intervalN = null, intervalType = null, type = TransactionType.EXPENSE)
            UI(PlannedPaymentsScreenState("AED", persistentListOf(), persistentListOf(account),
                if (populated) persistentListOf(rule) else persistentListOf(), 0.0, if (populated) amount else 0.0,
                persistentListOf(), 0.0, 0.0, true, true,
                forecast = CashFlowForecast(currentBalance = 10392.5, oneTimeExpenses = if (populated) amount else 0.0)))
        }
    }
    @Test fun emptyHasOneContextualAddAction() = snapshot(false)
    @Test fun populatedHasOneFooterAboveTheListEdge() = snapshot(true)
    @Test fun longNamesAndAmounts() = snapshot(true, true)
}
