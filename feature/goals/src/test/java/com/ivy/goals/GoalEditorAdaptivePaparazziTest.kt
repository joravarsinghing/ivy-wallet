package com.ivy.goals

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(TestParameterInjector::class)
class GoalEditorAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val today = LocalDate.of(2026, 10, 26)
    private val goal get() = GoalScreenState.preview().goals.first().copy(deadlineLabel = null, deadline = LocalDate.of(2027, 4, 23))
    @Test fun create() = render { GoalEditorContent("USD", today, onClose = {}, onSave = {}) }
    @Test fun editWithActualDate() = render { GoalEditorContent("AED", today, existing = goal, onClose = {}, onSave = {}) }
    @Test fun details() = render { GoalDetailContent(goal, GoalScreenState.preview().accounts, today, {}, {}, {}) }
    @Test fun reservations() = render { ReserveMoneyContent(goal, GoalScreenState.preview().accounts, emptyMap(), onClose = {}, onSave = {}) }
    @Test fun noAccounts() = render { ReserveMoneyContent(goal, emptyList(), emptyMap(), onClose = {}, onSave = {}) }
    @Test fun saveFailureKeepsDraft() = render { GoalEditorContent("AED", today, existing = goal, onClose = {},
        saveError = "Couldn't save the goal. Please retry.", onSave = {}) }
    @Test fun foreignReservationsNeedReview() = render {
        GoalDetailContent(goal.copy(allocations = goal.allocations + GoalAllocation("foreign", 50.0)),
            GoalScreenState.preview().accounts + GoalAccount("foreign", "Euro account", 250.0, "EUR"), today, {}, {}, {})
    }
}
