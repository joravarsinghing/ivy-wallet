package com.ivy.loans.loan

import com.ivy.data.model.LoanType
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.ui.money.formatDecimalMoney
import java.math.BigDecimal

/** Presentation only: never add different units or invent an exchange rate. */
internal fun outstandingLoanSummary(loans: List<DisplayLoan>, type: LoanType, baseCurrency: String): String =
    loans.filter { it.loan.type == type }.groupBy { it.currencyCode ?: baseCurrency }
        .toSortedMap().mapNotNull { (currency, records) ->
            val amount = records.fold(BigDecimal.ZERO) { total, loan ->
                total + BigDecimal.valueOf(loan.loanTotalAmount) - BigDecimal.valueOf(loan.amountPaid)
            }
            if (amount.signum() == 0) null else formatDecimalMoney(amount, currency)
        }.joinToString(" · ")
