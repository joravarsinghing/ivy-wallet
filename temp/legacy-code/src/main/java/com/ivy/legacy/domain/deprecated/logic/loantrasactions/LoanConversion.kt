package com.ivy.wallet.domain.deprecated.logic.loantrasactions

class LoanConversionUnavailable : IllegalStateException()

/** Null is valid only for identical units, never an unavailable foreign quote. */
internal fun validatedLoanConversion(sourceCurrency: String, loanCurrency: String, amount: Double, converted: Double?): Double? {
    require(amount.isFinite() && amount > 0)
    if (sourceCurrency == loanCurrency) return null
    if (converted == null || !converted.isFinite() || converted <= 0) throw LoanConversionUnavailable()
    return converted
}
