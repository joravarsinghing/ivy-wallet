package com.ivy.budgets

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Budget
import com.ivy.budgets.model.DisplayBudget
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class BudgetsAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(empty: Boolean, exceeded: Boolean = false, mixed: Boolean = false) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            val categoryBudgets = if (empty) persistentListOf() else persistentListOf(
                DisplayBudget(Budget(name = "Eating outside with friends and family", amount = 550.0, categoryIdsSerialized = "00000000-0000-0000-0000-000000000001", accountIdsSerialized = null, orderId = 0.0), if (exceeded) 650.25 else 218.75),
                DisplayBudget(Budget(name = "Transport", amount = 350.0, categoryIdsSerialized = "00000000-0000-0000-0000-000000000002", accountIdsSerialized = null, orderId = 1.0), 125.50))
            val budgets = if (mixed) categoryBudgets.add(DisplayBudget(Budget(name = "Overall spending", amount = 1200.0,
                categoryIdsSerialized = null, accountIdsSerialized = null, orderId = 2.0), 344.25)) else categoryBudgets
            BudgetScreenContent(BudgetScreenState("AED", budgets, persistentListOf(), persistentListOf(), if (empty) 0.0 else 900.0,
                if (mixed) 1200.0 else 0.0, if (mixed) "Remaining: 555.75 AED" else null, null, false, null, calculateBudgetInsight(budgets)))
        }
    }
    @Test fun empty() = snapshot(true)
    @Test fun populated() = snapshot(false)
    @Test fun exceeded() = snapshot(false, true)
    @Test fun separateOverallAndCategoryLimits() = snapshot(false, mixed = true)
}
