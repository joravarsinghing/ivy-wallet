package com.ivy.loans

import androidx.compose.runtime.CompositionLocalProvider
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.model.LoanRecordType
import com.ivy.data.model.LoanType
import com.ivy.design.system.FinancialVisibility
import com.ivy.design.system.LocalFinancialVisibility
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.datamodel.LoanRecord
import com.ivy.loans.loan.data.DisplayLoanRecord
import com.ivy.loans.loandetails.LoanDetailsScreenContent
import com.ivy.loans.loandetails.LoanDetailsScreenState
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@RunWith(TestParameterInjector::class)
class LoanDetailsAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val loan = Loan("Fictional household loan", 7000.0, LoanType.BORROW, color = 0xff176c50.toInt(),
        dateTime = LocalDateTime.of(2026, 10, 1, 12, 0))
    private fun snapshot(hidden: Boolean = false, missing: Boolean = false) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            CompositionLocalProvider(LocalFinancialVisibility provides FinancialVisibility(hidden)) {
                LoanDetailsScreenContent(LoanDetailsScreenState("AED", if (missing) null else loan,
                    if (missing) persistentListOf() else persistentListOf(DisplayLoanRecord(LoanRecord(
                        loanId = loan.id, amount = 2500.0, dateTime = Instant.parse("2026-10-15T12:00:00Z"),
                        loanRecordType = LoanRecordType.DECREASE, id = UUID.fromString("00000000-0000-0000-0000-000000000004")),
                        loanRecordCurrencyCode = "AED", loanCurrencyCode = "AED")),
                    7000.0, 2500.0, 0.0, persistentListOf(), null, false, null, null, false, false,
                    Instant.parse("2026-10-31T12:00:00Z"), operationError = if (missing) "Could not load loan details. Try again." else null))
            }
        }
    }
    @Test fun populatedDetails() = snapshot()
    @Test fun hiddenDetails() = snapshot(hidden = true)
    @Test fun retryableMissingDetails() = snapshot(missing = true)
}
