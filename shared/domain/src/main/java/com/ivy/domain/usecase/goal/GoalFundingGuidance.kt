package com.ivy.domain.usecase.goal

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class GoalFundingStatus {
    COMPLETE,
    NO_DEADLINE,
    ON_TRACK,
    NEEDS_FUNDING,
    OVERDUE,
}

/** Derived guidance for a goal. It is not a contribution, transfer, or ledger write. */
data class GoalFundingGuidance(
    val goalId: String,
    val targetAmount: BigDecimal,
    val reservedAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val monthsRemaining: Long?,
    val requiredMonthlyContribution: BigDecimal?,
    val plannedMonthlyContribution: BigDecimal?,
    val projectedFinishDate: LocalDate?,
    val status: GoalFundingStatus,
)

/**
 * Derives funding guidance from a goal, its current reservations, and an
 * optional future contribution plan. No amount is persisted or counted as
 * reserved merely because guidance was calculated.
 */
fun deriveGoalFundingGuidance(
    goal: Goal,
    dueDate: LocalDate?,
    asOfDate: LocalDate,
    allocations: Iterable<GoalAccountAllocation>,
    plannedMonthlyContribution: BigDecimal? = null,
): GoalFundingGuidance {
    requireValidGoal(goal)
    require(plannedMonthlyContribution == null || plannedMonthlyContribution.signum() >= 0) {
        "Planned monthly contribution cannot be negative"
    }

    val reservedAmount = calculateReservedAmount(goal.id, allocations)
    val remainingAmount = calculateRemainingAmount(goal.targetAmount, reservedAmount)
    val monthsRemaining = dueDate?.let { due ->
        if (due.isBefore(asOfDate)) {
            0L
        } else {
            ChronoUnit.MONTHS.between(
                asOfDate.withDayOfMonth(1),
                due.withDayOfMonth(1),
            ).coerceAtLeast(1L)
        }
    }
    val requiredMonthlyContribution = when {
        remainingAmount.signum() == 0 -> BigDecimal.ZERO
        dueDate == null -> null
        monthsRemaining == 0L -> remainingAmount
        else -> remainingAmount.divide(
            BigDecimal.valueOf(monthsRemaining ?: error("Months are required for a deadline")),
            MathContext.DECIMAL128,
        )
    }
    val projectedFinishDate = when {
        remainingAmount.signum() == 0 -> asOfDate
        plannedMonthlyContribution == null || plannedMonthlyContribution.signum() == 0 -> null
        else -> {
            val monthsNeeded = remainingAmount
                .divide(plannedMonthlyContribution, 0, RoundingMode.CEILING)
                .longValueExact()
            asOfDate.plusMonths(monthsNeeded)
        }
    }
    val status = when {
        remainingAmount.signum() == 0 -> GoalFundingStatus.COMPLETE
        dueDate == null -> GoalFundingStatus.NO_DEADLINE
        dueDate.isBefore(asOfDate) -> GoalFundingStatus.OVERDUE
        plannedMonthlyContribution != null &&
            requiredMonthlyContribution != null &&
            plannedMonthlyContribution >= requiredMonthlyContribution -> GoalFundingStatus.ON_TRACK
        else -> GoalFundingStatus.NEEDS_FUNDING
    }

    return GoalFundingGuidance(
        goalId = goal.id,
        targetAmount = goal.targetAmount,
        reservedAmount = reservedAmount,
        remainingAmount = remainingAmount,
        monthsRemaining = monthsRemaining,
        requiredMonthlyContribution = requiredMonthlyContribution,
        plannedMonthlyContribution = plannedMonthlyContribution,
        projectedFinishDate = projectedFinishDate,
        status = status,
    )
}
