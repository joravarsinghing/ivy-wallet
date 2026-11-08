package com.ivy.wallet.domain.deprecated.logic.loantrasactions

import androidx.compose.ui.graphics.Color
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.LoanRecordType
import com.ivy.base.model.TransactionType
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.data.model.LoanType
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.domain.deprecated.logic.loantrasactions.LoanTransactionsCore
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanData
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

class LoanTransactionMapperTest {
    private val core = mockk<LoanTransactionsCore>(relaxed = true)
    private val mapper = LTLoanMapper(core, TestTimeConverter())
    private val account = Account("Fictional bank", color = 0, currency = "AED")
    private val local = LocalDateTime.of(2026, 10, 31, 12, 0)
    private val loan = Loan("Fictional loan", 75.0, LoanType.BORROW, accountId = account.id, dateTime = local)
    private val current = Transaction(account.id, TransactionType.INCOME, BigDecimal("75.0"),
        dateTime = Instant.parse("2026-10-31T04:00:00Z"), loanId = loan.id)

    init {
        coEvery { core.fetchLoanTransaction(loan.id) } returns current
        coEvery { core.fetchLoan(loan.id) } returns loan.toEntity()
    }
    @Test fun `creation uses the confirmed principal date rather than a new current time`() = runTest {
        val data = CreateLoanData(loan.name, loan.amount, loan.type, Color(0), null, account,
            note = null, createLoanTransaction = true, dateTime = local, id = loan.id)
        mapper.createAssociatedLoanTransaction(data, loan.id)
        coVerify { core.updateAssociatedTransaction(createTransaction = true, loanId = loan.id,
            amount = loan.amount, loanType = loan.type, selectedAccountId = account.id, title = loan.name,
            time = Instant.parse("2026-10-31T12:00:00Z"), isLoanRecord = false, loanRecordType = LoanRecordType.DECREASE) }
    }
    @Test fun `stale null UI snapshot cannot create a duplicate principal transaction`() = runTest {
        mapper.editAssociatedLoanTransaction(loan, true, null)
        coVerify { core.updateAssociatedTransaction(createTransaction = true, loanId = loan.id,
            amount = loan.amount, loanType = loan.type, selectedAccountId = account.id, title = loan.name,
            time = current.dateTime, isLoanRecord = false, transaction = current, loanRecordType = LoanRecordType.DECREASE) }
    }
    @Test fun `editing metadata preserves the original instant across timezone changes`() = runTest {
        mapper.editAssociatedLoanTransaction(loan, true, current.copy(id = java.util.UUID.randomUUID()))
        coVerify { core.updateAssociatedTransaction(createTransaction = true, loanId = loan.id,
            amount = loan.amount, loanType = loan.type, selectedAccountId = account.id, title = loan.name,
            time = current.dateTime, isLoanRecord = false, transaction = current, loanRecordType = LoanRecordType.DECREASE) }
    }
    @Test fun `explicitly changing the loan date updates the linked transaction date`() = runTest {
        val changed = loan.copy(dateTime = local.plusDays(1))
        mapper.editAssociatedLoanTransaction(changed, true, current)
        coVerify { core.updateAssociatedTransaction(createTransaction = true, loanId = loan.id,
            amount = loan.amount, loanType = loan.type, selectedAccountId = account.id, title = loan.name,
            time = Instant.parse("2026-11-01T12:00:00Z"), isLoanRecord = false, transaction = current, loanRecordType = LoanRecordType.DECREASE) }
    }
}
