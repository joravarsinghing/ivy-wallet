package com.ivy.budgets

import androidx.compose.runtime.Immutable
import com.ivy.budgets.model.DisplayBudget

@Immutable
data class BudgetInsight(
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val overBudgetCount: Int = 0,
    val atRiskCount: Int = 0,
) {
    val remaining: Double get() = totalBudget - totalSpent
}

fun calculateBudgetInsight(budgets: List<DisplayBudget>): BudgetInsight {
    val totalBudget = budgets.sumOf { it.budget.amount }
    val totalSpent = budgets.sumOf { it.spentAmount }
    return BudgetInsight(
        totalBudget = totalBudget,
        totalSpent = totalSpent,
        overBudgetCount = budgets.count { it.spentAmount > it.budget.amount },
        atRiskCount = budgets.count {
            it.budget.amount > 0.0 &&
                it.spentAmount / it.budget.amount >= 0.8 &&
                it.spentAmount <= it.budget.amount
        },
    )
}
