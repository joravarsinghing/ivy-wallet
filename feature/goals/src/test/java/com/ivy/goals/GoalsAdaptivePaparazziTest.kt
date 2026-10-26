package com.ivy.goals

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class GoalsAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(empty: Boolean, long: Boolean = false) = render {
            val sample = GoalScreenState.preview()
            GoalsScreenContent(sample.copy(goals = if (empty) emptyList() else sample.goals.map { if (long) it.copy(name = "A longer emergency fund goal for my whole family", targetAmount = 12500000.50) else it }), {})
    }
    @Test fun empty() = snapshot(true)
    @Test fun populated() = snapshot(false)
    @Test fun longNamesAndAmounts() = snapshot(false, true)
    @Test fun loading() = render { GoalsScreenContent(GoalScreenState.empty().copy(loading = true), {}) }
    @Test fun loadError() = render {
        GoalsScreenContent(GoalScreenState.empty().copy(error = "Couldn't load goals. Please retry."), {})
    }
}
