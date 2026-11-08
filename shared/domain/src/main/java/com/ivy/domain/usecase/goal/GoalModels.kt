package com.ivy.domain.usecase.goal

import java.math.BigDecimal

/** The user intent behind a savings goal. */
enum class GoalType {
    EMERGENCY_FUND,
    PURCHASE,
    SINKING_FUND,
    CUSTOM,
}

/** Lifecycle state for a goal or saving pool. */
enum class GoalStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED,
}

/**
 * A goal definition independent of persistence and presentation.
 *
 * A goal's target is a desired amount. It does not imply that any money has
 * been reserved until one or more [GoalAccountAllocation]s exist.
 */
data class Goal(
    val id: String,
    val name: String,
    val type: GoalType,
    val targetAmount: BigDecimal,
    val status: GoalStatus = GoalStatus.ACTIVE,
)

/**
 * Money reserved for [goalId] from [accountId].
 *
 * Allocations are reservations, not transactions: creating one must not
 * change the account balance or create a ledger entry.
 */
data class GoalAccountAllocation(
    val goalId: String,
    val accountId: String,
    val amount: BigDecimal,
    val isActive: Boolean = true,
)

/** A calculated, read-only view of a goal's funding progress. */
data class GoalProgressSnapshot(
    val goalId: String,
    val targetAmount: BigDecimal,
    val reservedAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val progressPercentage: BigDecimal,
    val isComplete: Boolean,
)

/** A balance supplied by the account layer without importing account models. */
data class GoalAccountBalance(
    val accountId: String,
    val balance: BigDecimal,
)
