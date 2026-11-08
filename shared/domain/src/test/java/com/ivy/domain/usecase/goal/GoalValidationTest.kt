package com.ivy.domain.usecase.goal

import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import org.junit.Test

class GoalValidationTest {

    @Test
    fun `accepts positive target and zero reservation`() {
        validateGoalTarget(BigDecimal("50000.00")).isValid shouldBe true
        validateReservedAmount(BigDecimal.ZERO).isValid shouldBe true
    }

    @Test
    fun `rejects zero or negative target`() {
        validateGoalTarget(BigDecimal.ZERO) shouldBe GoalValidationResult(
            isValid = false,
            error = GoalValidationError.TARGET_MUST_BE_POSITIVE,
        )
        validateGoalTarget(BigDecimal("-1.00")).error shouldBe
            GoalValidationError.TARGET_MUST_BE_POSITIVE
    }

    @Test
    fun `rejects negative reservation and expense rate`() {
        validateReservedAmount(BigDecimal("-0.01")).error shouldBe
            GoalValidationError.RESERVED_AMOUNT_MUST_BE_NON_NEGATIVE
        validateMonthlyEssentialExpenses(BigDecimal("-1.00")).error shouldBe
            GoalValidationError.MONTHLY_EXPENSES_MUST_BE_NON_NEGATIVE
    }

    @Test
    fun `requires valid identity and money invariants`() {
        requireValidGoal(
            Goal(
                id = "laptop",
                name = "New laptop",
                type = GoalType.PURCHASE,
                targetAmount = BigDecimal("5000.00"),
            ),
        )
        requireValidAllocation(
            GoalAccountAllocation(
                goalId = "laptop",
                accountId = "adcb",
                amount = BigDecimal("1000.00"),
            ),
        )
    }
}
