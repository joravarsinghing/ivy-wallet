package com.ivy.loans

import com.ivy.base.model.LoanRecordType
import com.ivy.data.model.LoanType
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.datamodel.LoanRecord
import com.ivy.wallet.ui.theme.modal.LoanModalData
import com.ivy.wallet.ui.theme.modal.LoanRecordModalData
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class LoanEditorRestorationTest {
    private val account = Account("Fictional bank", 0, "AED")
    private val date = Instant.parse("2026-11-01T12:00:00Z")
    private val loan = Loan("Fictional education", 9000.0, LoanType.BORROW, accountId = account.id,
        dateTime = LocalDateTime.of(2026, 11, 1, 12, 0))
    private val record = LoanRecord(loan.id, 300.0, dateTime = date, accountId = account.id, loanRecordType = LoanRecordType.DECREASE)
    private fun restore(payload: List<String>, loans: List<Loan> = listOf(loan), records: List<LoanRecord> = listOf(record),
        accounts: List<Account> = listOf(account), currency: String = "AED") = LoanEditorRestoration.restore(payload, currency, loans, records, accounts)

    @Test fun `new loan retains creation identity selected account date and tab without pending work`() {
        val submission = com.ivy.design.system.EditorSubmission()
        val modal = LoanModalData(null, "AED", account, createLoanTransaction = true, submission = submission)
        submission.submit({}, {})
        val payload = LoanEditorRestoration.save(modal, null, null, null, date, "COMPLETED")!!
        val result = restore(payload)!!
        assertEquals(modal.id, result.loan!!.id); assertSame(account, result.loan.selectedAccount)
        assertEquals(date, result.date); assertEquals("COMPLETED", result.tab)
        assertTrue(result.loan.createLoanTransaction); assertNotSame(modal, result.loan)
        assertTrue(submission.saving)
    }
    @Test fun `edited calendar choice survives but does not replace the stored baseline`() {
        val modal = LoanModalData(loan.copy(dateTime = loan.dateTime!!.plusDays(1)), "AED", account)
        val payload = LoanEditorRestoration.save(modal, loan, null, null, date)!!
        assertFalse(payload.joinToString().contains(loan.name))
        assertEquals(modal.loan!!.dateTime, restore(payload)!!.loan!!.loan!!.dateTime)
    }
    @Test fun `repayment keeps identity edited instant interest and conversion provenance`() {
        val modal = LoanRecordModalData(record.copy(dateTime = date.plusSeconds(3600)), "AED", "AED", account,
            createLoanRecordTransaction = true, isLoanInterest = true)
        val payload = LoanEditorRestoration.save(null, loan, modal, record, date)!!
        val result = restore(payload)!!.record!!
        assertEquals(modal.id, result.id); assertEquals(modal.loanRecord!!.dateTime, result.loanRecord!!.dateTime)
        assertEquals(record.convertedAmount, result.loanRecord!!.convertedAmount)
        assertTrue(result.isLoanInterest); assertTrue(result.createLoanRecordTransaction)
    }
    @Test fun `missing and changed records do not become new loans or repayments`() {
        val loanPayload = LoanEditorRestoration.save(LoanModalData(loan, "AED", account), loan, null, null, date)!!
        assertNull(restore(loanPayload, loans = emptyList()))
        assertNull(restore(loanPayload, loans = listOf(loan.copy(amount = 8000.0))))
        val recordPayload = LoanEditorRestoration.save(null, loan, LoanRecordModalData(record, "AED", selectedAccount = account), record, date)!!
        assertNull(restore(recordPayload, records = emptyList()))
        assertNull(restore(recordPayload, records = listOf(record.copy(amount = 100.0))))
    }
    @Test fun `missing account and currency changes cannot reinterpret an old amount`() {
        val payload = LoanEditorRestoration.save(LoanModalData(null, "AED", account), null, null, null, date)!!
        assertNull(restore(payload, accounts = emptyList()))
        assertNull(restore(payload, accounts = listOf(account.copy(currency = "USD"))))
        assertNull(restore(payload, currency = "USD"))
    }
    @Test fun `completed loan and repayment creations require review not a duplicate add`() {
        val payload = LoanEditorRestoration.save(LoanModalData(null, "AED", account, id = loan.id), null, null, null, date)!!
        assertNull(restore(payload))
        val repayment = LoanEditorRestoration.save(null, loan, LoanRecordModalData(null, "AED", selectedAccount = account, id = record.id), null, date)!!
        assertNull(restore(repayment))
    }
}
