package com.ivy.domain.usecase.emergencyfund

import java.math.BigDecimal
import java.math.MathContext

/** Describes how many months of current expenses the protected buffer covers. */
enum class EmergencyFundCoverageStatus(
    val explanation: String,
) {
    NO_EXPENSES("Emergency-fund coverage is unbounded because current-period expenses are zero."),
    BELOW_ONE_MONTH("The protected buffer covers less than one month of current expenses."),
    ONE_TO_THREE_MONTHS("The protected buffer covers between one and three months of current expenses."),
    THREE_PLUS_MONTHS("The protected buffer covers at least three months of current expenses."),
}

data class EmergencyFundCoverageResult(
    val monthsCovered: BigDecimal?,
    val status: EmergencyFundCoverageStatus,
)

/**
 * Calculates how many months the protected buffer covers at the current period's expense rate.
 *
 * When expenses are zero, coverage has no finite number of months and is represented by null.
 * Terminating decimal ratios remain exact; non-terminating ratios use BigDecimal's
 * DECIMAL128 context rather than binary floating-point arithmetic.
 */
fun calculateEmergencyFundCoverage(
    protectedBuffer: BigDecimal,
    currentPeriodExpenses: BigDecimal,
): EmergencyFundCoverageResult {
    if (currentPeriodExpenses.signum() == 0) {
        return EmergencyFundCoverageResult(
            monthsCovered = null,
            status = EmergencyFundCoverageStatus.NO_EXPENSES,
        )
    }

    val monthsCovered = try {
        protectedBuffer.divide(currentPeriodExpenses)
    } catch (_: ArithmeticException) {
        protectedBuffer.divide(currentPeriodExpenses, MathContext.DECIMAL128)
    }
    val status = when {
        monthsCovered < BigDecimal.ONE -> EmergencyFundCoverageStatus.BELOW_ONE_MONTH
        monthsCovered < THREE_MONTHS -> EmergencyFundCoverageStatus.ONE_TO_THREE_MONTHS
        else -> EmergencyFundCoverageStatus.THREE_PLUS_MONTHS
    }

    return EmergencyFundCoverageResult(
        monthsCovered = monthsCovered,
        status = status,
    )
}

private val THREE_MONTHS = BigDecimal("3")
