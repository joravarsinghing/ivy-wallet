package com.ivy.goals

import com.ivy.data.db.entity.GoalAllocationEntity
import com.ivy.data.db.entity.GoalEntity
import com.ivy.data.db.entity.GoalKind
import com.ivy.domain.usecase.goal.Goal as DomainGoal
import com.ivy.domain.usecase.goal.GoalAccountAllocation as DomainGoalAccountAllocation
import com.ivy.domain.usecase.goal.GoalFundingGuidance
import com.ivy.domain.usecase.goal.GoalFundingStatus
import com.ivy.domain.usecase.goal.GoalStatus as DomainGoalStatus
import com.ivy.domain.usecase.goal.GoalType as DomainGoalType
import com.ivy.domain.usecase.goal.deriveGoalFundingGuidance
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun GoalEntity.toUiGoal(
    allocations: List<GoalAllocationEntity>,
    baseCurrency: String,
    asOfDate: LocalDate,
): Goal {
    val domainGoal = DomainGoal(
        id = id.toString(),
        name = name,
        type = kind.toDomainType(),
        targetAmount = BigDecimal.valueOf(targetAmount),
        status = if (isActive) DomainGoalStatus.ACTIVE else DomainGoalStatus.PAUSED,
    )
    val domainAllocations = allocations.map { it.toDomainAllocation() }
    val guidance = deriveGoalFundingGuidance(
        goal = domainGoal,
        dueDate = deadline?.atZone(ZoneOffset.UTC)?.toLocalDate(),
        asOfDate = asOfDate,
        allocations = domainAllocations,
    )

    return Goal(
        id = id.toString(),
        name = name,
        type = kind.toUiType(),
        targetAmount = targetAmount,
        currency = baseCurrency,
        deadlineLabel = deadline?.atZone(ZoneOffset.UTC)?.let {
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US).format(it)
        },
        status = if (isActive) GoalStatus.ACTIVE else GoalStatus.PAUSED,
        allocations = allocations.map {
            GoalAllocation(
                accountId = it.accountId.toString(),
                amount = it.reservedAmount,
            )
        },
        fundingGuidance = guidance,
    )
}

internal fun Goal.toFundingGuidance(asOfDate: LocalDate): GoalFundingGuidance =
    deriveGoalFundingGuidance(
        goal = DomainGoal(
            id = id,
            name = name,
            type = type.toDomainType(),
            targetAmount = BigDecimal.valueOf(targetAmount),
            status = status.toDomainStatus(),
        ),
        dueDate = deadlineLabel.toDeadlineLocalDateOrNull(),
        asOfDate = asOfDate,
        allocations = allocations.map { it.toDomainAllocation(id) },
    )

internal fun GoalFundingGuidance.displayLabel(currency: String): String = when (status) {
    GoalFundingStatus.COMPLETE -> "Complete"
    GoalFundingStatus.OVERDUE -> "Past due"
    GoalFundingStatus.NO_DEADLINE -> "No deadline"
    GoalFundingStatus.NEEDS_FUNDING,
    GoalFundingStatus.ON_TRACK -> requiredMonthlyContribution
        ?.let { "Needs $currency ${groupedAmount(it)}/month" }
        ?: "No deadline"
}

private fun GoalAllocationEntity.toDomainAllocation() = DomainGoalAccountAllocation(
    goalId = goalId.toString(),
    accountId = accountId.toString(),
    amount = BigDecimal.valueOf(reservedAmount),
)

private fun GoalAllocation.toDomainAllocation(goalId: String) = DomainGoalAccountAllocation(
    goalId = goalId,
    accountId = accountId,
    amount = BigDecimal.valueOf(amount),
)

private fun GoalKind.toDomainType(): DomainGoalType = when (this) {
    GoalKind.EMERGENCY_FUND -> DomainGoalType.EMERGENCY_FUND
    GoalKind.PURCHASE -> DomainGoalType.PURCHASE
    GoalKind.SINKING_FUND -> DomainGoalType.SINKING_FUND
    GoalKind.CUSTOM -> DomainGoalType.CUSTOM
}

private fun GoalKind.toUiType(): GoalType = when (this) {
    GoalKind.EMERGENCY_FUND -> GoalType.EMERGENCY_FUND
    GoalKind.PURCHASE -> GoalType.PURCHASE
    GoalKind.SINKING_FUND -> GoalType.SINKING_FUND
    GoalKind.CUSTOM -> GoalType.CUSTOM
}

private fun GoalType.toDomainType(): DomainGoalType = when (this) {
    GoalType.EMERGENCY_FUND -> DomainGoalType.EMERGENCY_FUND
    GoalType.PURCHASE -> DomainGoalType.PURCHASE
    GoalType.SINKING_FUND -> DomainGoalType.SINKING_FUND
    GoalType.CUSTOM -> DomainGoalType.CUSTOM
}

private fun GoalStatus.toDomainStatus(): DomainGoalStatus = when (this) {
    GoalStatus.ACTIVE -> DomainGoalStatus.ACTIVE
    GoalStatus.PAUSED -> DomainGoalStatus.PAUSED
    GoalStatus.COMPLETED -> DomainGoalStatus.COMPLETED
}

private fun String?.toDeadlineLocalDateOrNull(): LocalDate? {
    val value = this?.trim().orEmpty()
    if (value.isBlank()) return null
    return runCatching {
        YearMonth.parse(value, DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)).atDay(1)
    }.getOrNull()
}

private fun groupedAmount(amount: BigDecimal): String =
    String.format(Locale.US, "%,.2f", amount.toDouble())
