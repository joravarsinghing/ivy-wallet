package com.ivy.loans

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.loans.loan.*
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Loan
import com.ivy.data.model.LoanType
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime

@RunWith(TestParameterInjector::class)
class LoansAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(empty: Boolean, completed: Boolean = false) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val loans = if (empty) persistentListOf() else persistentListOf(DisplayLoan(
                loan = Loan(name = "A longer family loan to repay", icon = "bank", color = 0xFF176C50.toInt(), amount = 7000.0,
                    type = LoanType.BORROW, dateTime = LocalDateTime.of(2026, 10, 1, 0, 0)),
                loanTotalAmount = 7000.0, amountPaid = 2500.0, percentPaid = 2500.0 / 7000.0))
            LoanPlanningContent(LoanScreenState("AED", if (completed) loans else persistentListOf(), if (completed) persistentListOf() else loans,
                persistentListOf(), null, null, false, "", "", true, Instant.parse("2026-10-25T00:00:00Z"), if (completed) LoanTab.COMPLETED else LoanTab.PENDING))
        }
    }
    @Test fun empty() = snapshot(true)
    @Test fun populated() = snapshot(false)
    @Test fun completed() = snapshot(false, true)
}
