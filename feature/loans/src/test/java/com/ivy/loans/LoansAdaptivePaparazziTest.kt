package com.ivy.loans

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.loans.loan.*
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.utils.format
import com.ivy.ui.money.formatDecimalMoney
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
    private fun loan(name: String, type: LoanType, amount: Double, paid: Double, currency: String) = DisplayLoan(
        loan = Loan(name, amount, type, icon = "bank", color = 0xFF176C50.toInt(),
            dateTime = LocalDateTime.of(2026, 10, 1, 0, 0)),
        loanTotalAmount = amount, amountPaid = paid, currencyCode = currency,
        formattedDisplayText = "${formatDecimalMoney(paid.toBigDecimal(), currency)} / ${formatDecimalMoney(amount.toBigDecimal(), currency)} (${(paid / amount * 100).format(2)}%)",
        percentPaid = paid / amount,
    )

    private fun snapshot(empty: Boolean, completed: Boolean = false, hidden: Boolean = false) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            androidx.compose.runtime.CompositionLocalProvider(com.ivy.design.system.LocalFinancialVisibility provides com.ivy.design.system.FinancialVisibility(hidden)) {
            val loans = if (empty) persistentListOf() else persistentListOf(
                loan("Fictional family loan to repay", LoanType.BORROW, 7000.0, if (completed) 7000.0 else 2500.0, "AED"),
                loan("Fictional travel loan", LoanType.BORROW, 1000.0, if (completed) 1000.0 else 250.0, "USD"),
                loan("Fictional money lent", LoanType.LEND, 2500.0, if (completed) 2500.0 else 0.0, "AED"),
            )
            LoanPlanningContent(LoanScreenState("AED", if (completed) loans else persistentListOf(), if (completed) persistentListOf() else loans,
                persistentListOf(), null, null, false, outstandingLoanSummary(loans, LoanType.BORROW, "AED"),
                outstandingLoanSummary(loans, LoanType.LEND, "AED"), true, Instant.parse("2026-10-25T00:00:00Z"),
                if (completed) LoanTab.COMPLETED else LoanTab.PENDING))
            }
        }
    }
    @Test fun empty() = snapshot(true)
    @Test fun populated() = snapshot(false)
    @Test fun completed() = snapshot(false, true)
    @Test fun hiddenSummariesAndProgress() = snapshot(false, hidden = true)
}
