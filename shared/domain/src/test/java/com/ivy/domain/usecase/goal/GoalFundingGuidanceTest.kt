package com.ivy.domain.usecase.goal

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import org.junit.Test

class GoalFundingGuidanceTest {

    @Test
    fun `derives remaining and required monthly contribution from reservations and deadline`() {
        val result = deriveGoalFundingGuidance(
            goal = Goal(
                id = "laptop",
                name = "Laptop",
                type = GoalType.PURCHASE,
                targetAmount = BigDecimal("6000.00"),
            ),
            dueDate = LocalDate.of(2027, 4, 14),
            asOfDate = LocalDate.of(2026, 10, 14),
            allocations = listOf(
                GoalAccountAllocation("laptop", "adcb", BigDecimal("2000.00")),
            ),
            plannedMonthlyContribution = BigDecimal("700.00"),
        )

        result.reservedAmount shouldBe BigDecimal("2000.00")
        result.remainingAmount shouldBe BigDecimal("4000.00")
        result.monthsRemaining shouldBe 6L
        result.requiredMonthlyContribution!!.compareTo(
            BigDecimal("4000.00").divide(BigDecimal("6"), MathContext.DECIMAL128),
        ) shouldBe 0
        result.projectedFinishDate shouldBe LocalDate.of(2027, 4, 14)
        result.status shouldBe GoalFundingStatus.ON_TRACK
    }

    @Test
    fun `does not treat target or planned contribution as reserved money`() {
        val result = deriveGoalFundingGuidance(
            goal = Goal(
                id = "holiday",
                name = "Holiday",
                type = GoalType.SINKING_FUND,
                targetAmount = BigDecimal("3000.00"),
            ),
            dueDate = null,
            asOfDate = LocalDate.of(2026, 10, 14),
            allocations = emptyList(),
            plannedMonthlyContribution = BigDecimal("300.00"),
        )

        result.reservedAmount shouldBe BigDecimal.ZERO
        result.remainingAmount shouldBe BigDecimal("3000.00")
        result.requiredMonthlyContribution.shouldBeNull()
        result.status shouldBe GoalFundingStatus.NO_DEADLINE
    }

    @Test
    fun `reports complete and overdue guidance from derived reservation state`() {
        val complete = deriveGoalFundingGuidance(
            goal = Goal("funded", "Funded", GoalType.CUSTOM, BigDecimal("100.00")),
            dueDate = LocalDate.of(2026, 10, 1),
            asOfDate = LocalDate.of(2026, 10, 14),
            allocations = listOf(GoalAccountAllocation("funded", "cash", BigDecimal("100.00"))),
        )
        val overdue = deriveGoalFundingGuidance(
            goal = Goal("late", "Late", GoalType.CUSTOM, BigDecimal("100.00")),
            dueDate = LocalDate.of(2026, 10, 1),
            asOfDate = LocalDate.of(2026, 10, 14),
            allocations = emptyList(),
        )

        complete.status shouldBe GoalFundingStatus.COMPLETE
        complete.projectedFinishDate shouldBe LocalDate.of(2026, 10, 14)
        overdue.status shouldBe GoalFundingStatus.OVERDUE
        overdue.requiredMonthlyContribution shouldBe BigDecimal("100.00")
    }
}
