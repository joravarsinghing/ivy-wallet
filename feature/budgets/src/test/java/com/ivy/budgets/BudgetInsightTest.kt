package com.ivy.budgets

import com.ivy.budgets.model.DisplayBudget
import com.ivy.legacy.datamodel.Budget
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetInsightTest {
    @Test
    fun `counts exceeded and at risk budgets`() {
        val budgets = listOf(
            DisplayBudget(Budget(name = "Food", amount = 100.0, categoryIdsSerialized = null, accountIdsSerialized = null, orderId = 0.0), spentAmount = 120.0),
            DisplayBudget(Budget(name = "Bills", amount = 100.0, categoryIdsSerialized = null, accountIdsSerialized = null, orderId = 1.0), spentAmount = 85.0),
            DisplayBudget(Budget(name = "Fun", amount = 100.0, categoryIdsSerialized = null, accountIdsSerialized = null, orderId = 2.0), spentAmount = 20.0),
        )

        val insight = calculateBudgetInsight(budgets)

        assertEquals(300.0, insight.totalBudget, 0.001)
        assertEquals(225.0, insight.totalSpent, 0.001)
        assertEquals(1, insight.overBudgetCount)
        assertEquals(1, insight.atRiskCount)
        assertEquals(75.0, insight.remaining, 0.001)
    }
}
