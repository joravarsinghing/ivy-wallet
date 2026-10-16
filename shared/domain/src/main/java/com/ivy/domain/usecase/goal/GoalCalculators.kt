package com.ivy.domain.usecase.goal

import java.math.BigDecimal
import java.math.MathContext

private val ZERO = BigDecimal.ZERO
private val ONE_HUNDRED = BigDecimal("100")
private val MAX_PROGRESS_PERCENTAGE = ONE_HUNDRED

/** Sums active reservations for one goal without mutating the supplied list. */
fun calculateReservedAmount(
    goalId: String,
    allocations: Iterable<GoalAccountAllocation>,
): BigDecimal {
    require(goalId.isNotBlank()) { GoalValidationError.GOAL_ID_REQUIRED }

    return allocations
        .filter { it.isActive && it.goalId == goalId }
        .onEach(::requireValidAllocation)
        .fold(ZERO) { total, allocation -> total.add(allocation.amount) }
}

/** Returns the unfunded part of a goal, never below zero when it is overfunded. */
fun calculateRemainingAmount(
    targetAmount: BigDecimal,
    reservedAmount: BigDecimal,
): BigDecimal {
    requireValidGoalTarget(targetAmount)
    requireValidReservedAmount(reservedAmount)
    return targetAmount.subtract(reservedAmount).max(ZERO)
}

/** Returns spendable balance after active reservations for one account. */
fun calculateAccountAvailableBalance(
    accountId: String,
    accountBalance: BigDecimal,
    allocations: Iterable<GoalAccountAllocation>,
): BigDecimal {
    require(accountId.isNotBlank()) { GoalValidationError.ACCOUNT_ID_REQUIRED }

    val reservedAmount = allocations
        .filter { it.isActive && it.accountId == accountId }
        .onEach(::requireValidAllocation)
        .fold(ZERO) { total, allocation -> total.add(allocation.amount) }

    return accountBalance.subtract(reservedAmount).max(ZERO)
}

/**
 * Sums spendable balances across accounts. Reservations are applied per
 * account, so one over-reserved account cannot hide available money in another.
 */
fun calculateSafeToSpend(
    accountBalances: Iterable<GoalAccountBalance>,
    allocations: Iterable<GoalAccountAllocation>,
): BigDecimal = accountBalances.fold(ZERO) { total, account ->
    total.add(
        calculateAccountAvailableBalance(
            accountId = account.accountId,
            accountBalance = account.balance,
            allocations = allocations,
        ),
    )
}

/** Builds a goal progress snapshot from its target and active account reservations. */
fun calculateGoalProgress(
    goalId: String,
    targetAmount: BigDecimal,
    allocations: Iterable<GoalAccountAllocation>,
): GoalProgressSnapshot {
    requireValidGoalTarget(targetAmount)

    val reservedAmount = calculateReservedAmount(goalId, allocations)
    val remainingAmount = calculateRemainingAmount(targetAmount, reservedAmount)
    val progressPercentage = reservedAmount
        .multiply(ONE_HUNDRED)
        .divide(targetAmount, MathContext.DECIMAL128)
        .min(MAX_PROGRESS_PERCENTAGE)

    return GoalProgressSnapshot(
        goalId = goalId,
        targetAmount = targetAmount,
        reservedAmount = reservedAmount,
        remainingAmount = remainingAmount,
        progressPercentage = progressPercentage,
        isComplete = reservedAmount >= targetAmount,
    )
}

/**
 * Calculates emergency-fund coverage in months.
 *
 * A zero expense rate has no finite coverage value and returns null. A
 * non-terminating decimal uses DECIMAL128 only as the required representation
 * of the ratio; no binary floating point conversion is involved.
 */
fun calculateEmergencyFundMonths(
    reservedEmergencyFund: BigDecimal,
    monthlyEssentialExpenses: BigDecimal,
): BigDecimal? {
    requireValidReservedAmount(reservedEmergencyFund)
    requireValidMonthlyEssentialExpenses(monthlyEssentialExpenses)

    if (monthlyEssentialExpenses.signum() == 0) return null

    return try {
        reservedEmergencyFund.divide(monthlyEssentialExpenses)
    } catch (_: ArithmeticException) {
        reservedEmergencyFund.divide(monthlyEssentialExpenses, MathContext.DECIMAL128)
    }
}
