package com.ivy.domain.usecase.goal

import java.math.BigDecimal

enum class GoalValidationError {
    GOAL_ID_REQUIRED,
    ACCOUNT_ID_REQUIRED,
    GOAL_NAME_REQUIRED,
    TARGET_MUST_BE_POSITIVE,
    RESERVED_AMOUNT_MUST_BE_NON_NEGATIVE,
    MONTHLY_EXPENSES_MUST_BE_NON_NEGATIVE,
}

data class GoalValidationResult(
    val isValid: Boolean,
    val error: GoalValidationError? = null,
)

fun validateGoalTarget(targetAmount: BigDecimal): GoalValidationResult =
    if (targetAmount.signum() > 0) {
        GoalValidationResult(isValid = true)
    } else {
        GoalValidationResult(
            isValid = false,
            error = GoalValidationError.TARGET_MUST_BE_POSITIVE,
        )
    }

fun validateReservedAmount(reservedAmount: BigDecimal): GoalValidationResult =
    if (reservedAmount.signum() >= 0) {
        GoalValidationResult(isValid = true)
    } else {
        GoalValidationResult(
            isValid = false,
            error = GoalValidationError.RESERVED_AMOUNT_MUST_BE_NON_NEGATIVE,
        )
    }

fun validateMonthlyEssentialExpenses(expenses: BigDecimal): GoalValidationResult =
    if (expenses.signum() >= 0) {
        GoalValidationResult(isValid = true)
    } else {
        GoalValidationResult(
            isValid = false,
            error = GoalValidationError.MONTHLY_EXPENSES_MUST_BE_NON_NEGATIVE,
        )
    }

fun requireValidGoal(goal: Goal) {
    require(goal.id.isNotBlank()) { GoalValidationError.GOAL_ID_REQUIRED }
    require(goal.name.isNotBlank()) { GoalValidationError.GOAL_NAME_REQUIRED }
    requireValidGoalTarget(goal.targetAmount)
}

fun requireValidAllocation(allocation: GoalAccountAllocation) {
    require(allocation.goalId.isNotBlank()) { GoalValidationError.GOAL_ID_REQUIRED }
    require(allocation.accountId.isNotBlank()) { GoalValidationError.ACCOUNT_ID_REQUIRED }
    requireValidReservedAmount(allocation.amount)
}

fun requireValidGoalTarget(targetAmount: BigDecimal) {
    val result = validateGoalTarget(targetAmount)
    require(result.isValid) { result.error ?: GoalValidationError.TARGET_MUST_BE_POSITIVE }
}

fun requireValidReservedAmount(reservedAmount: BigDecimal) {
    val result = validateReservedAmount(reservedAmount)
    require(result.isValid) { result.error ?: GoalValidationError.RESERVED_AMOUNT_MUST_BE_NON_NEGATIVE }
}

fun requireValidMonthlyEssentialExpenses(expenses: BigDecimal) {
    val result = validateMonthlyEssentialExpenses(expenses)
    require(result.isValid) { result.error ?: GoalValidationError.MONTHLY_EXPENSES_MUST_BE_NON_NEGATIVE }
}
