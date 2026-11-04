package com.ivy.loans.loandetails.events

import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData

sealed interface LoanDetailsScreenEvent {
    data object Retry : LoanDetailsScreenEvent
    data object OnEditLoanClick : LoanDetailsScreenEvent
    data object OnAmountClick : LoanDetailsScreenEvent
    data object OnAddRecord : LoanDetailsScreenEvent
    data class OnCreateAccount(val data: CreateAccountData, val onResult: (Result<Unit>) -> Unit) : LoanDetailsScreenEvent
}
