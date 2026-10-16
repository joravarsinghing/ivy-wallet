package com.ivy.domain.usecase.savings

import java.math.BigDecimal
import java.math.RoundingMode

/** Explains whether a savings rate can be calculated for the current period. */
enum class SavingsRateStatus(
    val explanation: String,
) {
    CALCULATED("Savings rate calculated from the period income and expenses."),
    ZERO_INCOME("Savings rate is unavailable because income is zero."),
    NEGATIVE_INCOME("Savings rate is unavailable because income is negative."),
}

data class SavingsRateResult(
    val savingsAmount: BigDecimal,
    val savingsRatePercentage: BigDecimal?,
    val status: SavingsRateStatus,
)

/**
 * Calculates the amount saved and savings rate for one period.
 *
 * The percentage is rounded to ten decimal places only when a finite decimal
 * representation is not possible. Monetary subtraction itself is never rounded.
 */
fun calculateSavingsRate(
    income: BigDecimal,
    expenses: BigDecimal,
): SavingsRateResult {
    val savingsAmount = income.subtract(expenses)

    return when {
        income.signum() > 0 -> SavingsRateResult(
            savingsAmount = savingsAmount,
            savingsRatePercentage = savingsAmount
                .multiply(BigDecimal("100"))
                .divide(income, RATE_SCALE, RoundingMode.HALF_UP),
            status = SavingsRateStatus.CALCULATED,
        )

        income.signum() == 0 -> SavingsRateResult(
            savingsAmount = savingsAmount,
            savingsRatePercentage = null,
            status = SavingsRateStatus.ZERO_INCOME,
        )

        else -> SavingsRateResult(
            savingsAmount = savingsAmount,
            savingsRatePercentage = null,
            status = SavingsRateStatus.NEGATIVE_INCOME,
        )
    }
}

private const val RATE_SCALE = 10
