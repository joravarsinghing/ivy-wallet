package com.ivy.goals

import androidx.compose.runtime.Immutable
import com.ivy.domain.usecase.goal.GoalFundingGuidance
import com.ivy.navigation.GoalsScreen
import java.time.LocalDate
import java.util.UUID

enum class GoalType(val label: String) {
    EMERGENCY_FUND("Emergency fund"),
    PURCHASE("Purchase"),
    SINKING_FUND("Planned expense"),
    CUSTOM("Custom")
}

enum class GoalStatus(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed")
}

internal const val reservationReviewMessage = "Some reservations use other currencies or unavailable accounts. Review this total; no conversion is applied."

internal fun Goal.reservationsNeedReview(accounts: List<GoalAccount>): Boolean = allocations.any { allocation ->
    accounts.firstOrNull { it.id == allocation.accountId }?.currency != currency
}

@Immutable
data class GoalAllocation(
    val accountId: String,
    val amount: Double
)

@Immutable
data class GoalAccount(
    val id: String,
    val name: String,
    val balance: Double,
    val currency: String
)

@Immutable
data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: GoalType,
    val targetAmount: Double,
    val currency: String,
    val deadlineLabel: String? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val allocations: List<GoalAllocation> = emptyList(),
    val categoryLabel: String? = null,
    val fundingGuidance: GoalFundingGuidance? = null,
    /** The selected calendar day. Labels are presentation/legacy compatibility only. */
    val deadline: LocalDate? = null,
) {
    val savedAmount: Double
        get() = allocations.sumOf { it.amount.coerceAtLeast(0.0) }

    val remainingAmount: Double
        get() = (targetAmount - savedAmount).coerceAtLeast(0.0)

    val progress: Float
        get() = if (targetAmount <= 0.0) 0f else (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)
}

@Immutable
data class GoalScreenState(
    val baseCurrency: String,
    val goals: List<Goal>,
    val accounts: List<GoalAccount>,
    val asOfDate: LocalDate = LocalDate.now(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val saveRevision: Long = 0,
) {
    val reservedAmount: Double
        get() = goals.sumOf { it.savedAmount }

    companion object {
        fun preview(): GoalScreenState {
            val asOfDate = LocalDate.of(2026, 10, 14)
            val goals = listOf(
                Goal(
                    name = "New laptop",
                    type = GoalType.PURCHASE,
                    targetAmount = 5000.0,
                    currency = "AED",
                    deadlineLabel = "April 2027",
                    allocations = listOf(GoalAllocation("adcb", 1250.0))
                ),
                Goal(
                    name = "Emergency fund",
                    type = GoalType.EMERGENCY_FUND,
                    targetAmount = 15000.0,
                    currency = "AED",
                    deadlineLabel = "No deadline",
                    allocations = listOf(
                        GoalAllocation("emergency", 3500.0),
                        GoalAllocation("adcb", 500.0)
                    )
                )
            )
            return GoalScreenState(
                baseCurrency = "AED",
                accounts = listOf(
                    GoalAccount("adcb", "Bank - ADCB", 11257.83, "AED"),
                    GoalAccount("cash", "Cash wallet", 420.00, "AED"),
                    GoalAccount("emergency", "Emergency fund", 3500.00, "AED")
                ),
                goals = goals.map { it.copy(fundingGuidance = it.toFundingGuidance(asOfDate)) },
                asOfDate = asOfDate,
            )
        }

        fun empty(currency: String = "AED") = GoalScreenState(
            baseCurrency = currency,
            goals = emptyList(),
            accounts = emptyList()
        )
    }
}

sealed interface GoalsScreenEvent {
    data object Back : GoalsScreenEvent
    data object Retry : GoalsScreenEvent
    data class GoalCreated(val goal: Goal) : GoalsScreenEvent
    data class GoalUpdated(val goal: Goal) : GoalsScreenEvent
}

/**
 * Navigation owns this screen identity. This import is intentionally kept in the
 * model file so callers can use the feature without a second route abstraction.
 */
typealias GoalsRoute = GoalsScreen
