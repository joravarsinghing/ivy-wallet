package com.ivy.domain.usecase.goal

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import org.junit.Test

class GoalCalculatorsTest {

    @Test
    fun `reserved amount includes only active allocations for the requested goal`() {
        val allocations = listOf(
            GoalAccountAllocation("laptop", "adcb", BigDecimal("1200.10")),
            GoalAccountAllocation("laptop", "cash", BigDecimal("300.20"), isActive = false),
            GoalAccountAllocation("emergency", "adcb", BigDecimal("5000.00")),
        )

        calculateReservedAmount("laptop", allocations) shouldBe BigDecimal("1200.10")
    }

    @Test
    fun `remaining amount is exact and clamps overfunded goals to zero`() {
        calculateRemainingAmount(
            targetAmount = BigDecimal("5000.00"),
            reservedAmount = BigDecimal("1200.10"),
        ) shouldBe BigDecimal("3799.90")

        calculateRemainingAmount(
            targetAmount = BigDecimal("5000.00"),
            reservedAmount = BigDecimal("5000.01"),
        ) shouldBe BigDecimal.ZERO
    }

    @Test
    fun `available balance applies reservations to the owning account`() {
        val allocations = listOf(
            GoalAccountAllocation("laptop", "adcb", BigDecimal("2000.00")),
            GoalAccountAllocation("emergency", "adcb", BigDecimal("1000.00")),
            GoalAccountAllocation("laptop", "cash", BigDecimal("500.00")),
        )

        calculateAccountAvailableBalance(
            accountId = "adcb",
            accountBalance = BigDecimal("2500.00"),
            allocations = allocations,
        ) shouldBe BigDecimal.ZERO

        calculateAccountAvailableBalance(
            accountId = "cash",
            accountBalance = BigDecimal("1000.00"),
            allocations = allocations,
        ) shouldBe BigDecimal("500.00")
    }

    @Test
    fun `safe to spend sums available balances across accounts`() {
        val allocations = listOf(
            GoalAccountAllocation("laptop", "adcb", BigDecimal("2000.00")),
            GoalAccountAllocation("emergency", "adcb", BigDecimal("1000.00")),
            GoalAccountAllocation("laptop", "cash", BigDecimal("500.00")),
        )

        calculateSafeToSpend(
            accountBalances = listOf(
                GoalAccountBalance("adcb", BigDecimal("2500.00")),
                GoalAccountBalance("cash", BigDecimal("1000.00")),
            ),
            allocations = allocations,
        ) shouldBe BigDecimal("500.00")
    }

    @Test
    fun `goal progress reports target reserved remaining and capped percentage`() {
        val snapshot = calculateGoalProgress(
            goalId = "laptop",
            targetAmount = BigDecimal("5000.00"),
            allocations = listOf(
                GoalAccountAllocation("laptop", "adcb", BigDecimal("1200.00")),
                GoalAccountAllocation("laptop", "cash", BigDecimal("1300.00")),
            ),
        )

        snapshot.targetAmount shouldBe BigDecimal("5000.00")
        snapshot.reservedAmount shouldBe BigDecimal("2500.00")
        snapshot.remainingAmount shouldBe BigDecimal("2500.00")
        snapshot.progressPercentage.compareTo(BigDecimal("50.0")) shouldBe 0
        snapshot.isComplete shouldBe false

        calculateGoalProgress(
            goalId = "laptop",
            targetAmount = BigDecimal("5000.00"),
            allocations = listOf(
                GoalAccountAllocation("laptop", "adcb", BigDecimal("6000.00")),
            ),
        ).progressPercentage.compareTo(BigDecimal("100")) shouldBe 0
    }

    @Test
    fun `emergency months returns exact ratio or null for zero expenses`() {
        calculateEmergencyFundMonths(
            reservedEmergencyFund = BigDecimal("1500.00"),
            monthlyEssentialExpenses = BigDecimal("625.00"),
        ) shouldBe BigDecimal("2.4")

        calculateEmergencyFundMonths(
            reservedEmergencyFund = BigDecimal("2500.00"),
            monthlyEssentialExpenses = BigDecimal.ZERO,
        ).shouldBeNull()
    }
}
