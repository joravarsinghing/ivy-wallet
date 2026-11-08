package com.ivy.goals

import com.ivy.data.db.entity.GoalAllocationEntity
import com.ivy.data.db.entity.GoalEntity
import com.ivy.data.db.entity.GoalKind
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalFundingGuidanceAdapterTest {
    @Test
    fun `maps active persisted reservations into deadline guidance`() {
        val goalId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val goal = GoalEntity(
            id = goalId,
            name = "Laptop",
            kind = GoalKind.PURCHASE,
            targetAmount = 6000.0,
            deadline = LocalDate.of(2027, 4, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
        )

        val uiGoal = goal.toUiGoal(
            allocations = listOf(
                GoalAllocationEntity(
                    goalId = goalId,
                    accountId = accountId,
                    reservedAmount = 2000.0,
                )
            ),
            baseCurrency = "AED",
            asOfDate = LocalDate.of(2026, 10, 14),
        )

        val guidance = requireNotNull(uiGoal.fundingGuidance)
        assertEquals(BigDecimal("2000.0"), guidance.reservedAmount)
        assertEquals(BigDecimal("4000.0"), guidance.remainingAmount)
        assertEquals("Needs AED 666.67/month", guidance.displayLabel("AED"))
        assertEquals(1, uiGoal.allocations.size)
    }

    @Test
    fun `keeps no deadline and complete labels based on reserved amount`() {
        val complete = Goal(
            name = "Emergency fund",
            type = GoalType.EMERGENCY_FUND,
            targetAmount = 100.0,
            currency = "AED",
            allocations = listOf(GoalAllocation("cash", 100.0)),
            deadlineLabel = "October 2026",
        ).toFundingGuidance(LocalDate.of(2026, 10, 14))
        val openEnded = Goal(
            name = "Holiday",
            type = GoalType.SINKING_FUND,
            targetAmount = 3000.0,
            currency = "AED",
        ).toFundingGuidance(LocalDate.of(2026, 10, 14))
        val pastDue = Goal(
            name = "Late bill",
            type = GoalType.CUSTOM,
            targetAmount = 100.0,
            currency = "AED",
            deadlineLabel = "October 2026",
        ).toFundingGuidance(LocalDate.of(2026, 10, 14))

        assertEquals("Complete", complete.displayLabel("AED"))
        assertEquals("No deadline", openEnded.displayLabel("AED"))
        assertEquals("Past due", pastDue.displayLabel("AED"))
    }
}
