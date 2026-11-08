package com.ivy.wallet.domain.deprecated.logic.loantrasactions

import com.ivy.wallet.ui.theme.modal.loanEditorSubmission
import org.junit.Assert.*
import org.junit.Test

class LoanConversionTest {
    @Test fun `same currency uses the source amount without storing a conversion`() {
        assertNull(validatedLoanConversion("AED", "AED", 75.0, null))
    }
    @Test fun `valid foreign conversion retains the actual quote`() {
        assertEquals(18.25, validatedLoanConversion("AED", "USD", 75.0, 18.25)!!, 0.0)
    }
    @Test fun `missing foreign quote cannot masquerade as same-currency null`() {
        try { validatedLoanConversion("AED", "USD", 75.0, null); fail() } catch (_: LoanConversionUnavailable) { }
    }
    @Test fun `invalid foreign quotes are rejected instead of saved as source units`() {
        for (quote in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            try { validatedLoanConversion("AED", "USD", 75.0, quote); fail() } catch (_: LoanConversionUnavailable) { }
        }
    }
    @Test fun `invalid original amounts are not converted`() {
        for (amount in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            try { validatedLoanConversion("AED", "AED", amount, null); fail() } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun `missing quote leaves an editable draft with a safe recovery instruction`() {
        val submission = loanEditorSubmission()
        submission.submit({ it(Result.failure(LoanConversionUnavailable())) }, { fail() })
        assertFalse(submission.saving)
        assertFalse(submission.requiresReview)
        assertTrue(submission.error!!.contains("exchange rate"))
        assertTrue(submission.error!!.contains("loan's currency"))
    }
}
