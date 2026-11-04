package com.ivy.loans

import com.ivy.data.model.LoanType
import com.ivy.legacy.datamodel.Loan
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.loans.loan.outstandingLoanSummary
import org.junit.Assert.*
import org.junit.Test

class LoanSummaryTest {
    private fun loan(amount: Double, paid: Double, currency: String?, type: LoanType = LoanType.BORROW) =
        DisplayLoan(Loan("Fictional loan", amount, type), amount, paid, currency)

    @Test fun `different currencies are never added as the same unit`() {
        val summary = outstandingLoanSummary(listOf(loan(200.0, 0.0, "AED"), loan(100.0, 0.0, "USD")), LoanType.BORROW, "AED")
        assertEquals("200.00 AED · 100.00 USD", summary)
        assertFalse(summary.contains("300.00"))
    }
    @Test fun `same currency combines remaining principal and retains grouped display`() {
        assertEquals("1,700.00 AED", outstandingLoanSummary(listOf(loan(1000.0, 100.0, "AED"), loan(900.0, 100.0, "AED")), LoanType.BORROW, "AED"))
    }
    @Test fun `borrowed and lent totals remain separate`() {
        val loans = listOf(loan(100.0, 0.0, "AED"), loan(250.0, 25.0, "AED", LoanType.LEND))
        assertEquals("100.00 AED", outstandingLoanSummary(loans, LoanType.BORROW, "AED"))
        assertEquals("225.00 AED", outstandingLoanSummary(loans, LoanType.LEND, "AED"))
    }
    @Test fun `legacy implicit currency keeps wallet unit and zero totals stay absent`() {
        assertEquals("50.00 AED", outstandingLoanSummary(listOf(loan(75.0, 25.0, null)), LoanType.BORROW, "AED"))
        assertEquals("", outstandingLoanSummary(listOf(loan(25.0, 25.0, "AED")), LoanType.BORROW, "AED"))
    }
}
