package com.ivy.loans

import androidx.compose.runtime.remember
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.model.LoanRecordType
import com.ivy.data.model.LoanType
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.datamodel.LoanRecord
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.domain.deprecated.logic.LoanWriteConflict
import com.ivy.wallet.ui.theme.modal.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@RunWith(TestParameterInjector::class)
class LoanEditorAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val account = Account("Fictional household account with a long name", color = 0xfffaca32.toInt(), currency = "AED")
    private val date = Instant.parse("2026-10-31T12:30:00Z")
    private fun snapshot(record: Boolean, state: Int) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val submission = remember {
                loanEditorSubmission().apply {
                    if (state == 0) submit({}, {})
                    else submit({ it(Result.failure(if (state == 2) LoanWriteConflict()
                        else if (state == 3) com.ivy.wallet.domain.deprecated.logic.loantrasactions.LoanConversionUnavailable()
                        else IllegalStateException("Fictional failure"))) }, {})
                }
            }
            if (record) LoanRecordModal(LoanRecordModalData(LoanRecord(UUID.randomUUID(), 12345.75,
                note = "Fictional repayment", dateTime = date, accountId = account.id, loanRecordType = LoanRecordType.DECREASE),
                "AED", selectedAccount = account, submission = submission), date, {}, {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, listOf(account))
            else LoanModal(LoanModalData(Loan("Fictional family loan", 123456.75, LoanType.BORROW, color = 0xff18ba92.toInt(),
                accountId = account.id, dateTime = LocalDateTime.of(2026, 10, 31, 12, 30)), "AED", account,
                autoFocusKeyboard = false, submission = submission), date, {}, {}, { _, _ -> }, { _, _, _ -> }, listOf(account), dismiss = {})
        }
    }
    @Test fun pendingLoan() = snapshot(false, 0)
    @Test fun failedLoan() = snapshot(false, 1)
    @Test fun reviewLoan() = snapshot(false, 2)
    @Test fun pendingRepayment() = snapshot(true, 0)
    @Test fun failedRepayment() = snapshot(true, 1)
    @Test fun reviewRepayment() = snapshot(true, 2)
    @Test fun missingRateRepayment() = snapshot(true, 3)
}
