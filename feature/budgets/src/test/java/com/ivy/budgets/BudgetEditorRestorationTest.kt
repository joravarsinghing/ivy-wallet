package com.ivy.budgets

import com.ivy.legacy.datamodel.Budget
import org.junit.Assert.*
import org.junit.Test

class BudgetEditorRestorationTest {
    private val budget = Budget("Fictional groceries", 500.0, null, null, orderId = 1.0)
    @Test fun `creation retains its intent identity without replaying a submission`() {
        val modal = BudgetModalData(null, "AED", emptyList(), emptyList())
        val result = BudgetEditorRestoration.restore(BudgetEditorRestoration.save(modal)!!, "AED", emptyList(), emptyList(), emptyList())!!
        assertEquals(modal.id, result.id); assertNull(result.budget)
    }
    @Test fun `editing resolves the live record without serializing the financial graph`() {
        val modal = BudgetModalData(budget, "AED", emptyList(), emptyList())
        val payload = BudgetEditorRestoration.save(modal)!!
        assertFalse(payload.joinToString().contains(budget.name))
        val result = BudgetEditorRestoration.restore(payload, "AED", listOf(budget), emptyList(), emptyList())!!
        assertSame(budget, result.budget); assertEquals(modal.id, result.id)
    }
    @Test fun `missing changed and foreign currency contexts refuse the saved editor`() {
        val payload = BudgetEditorRestoration.save(BudgetModalData(budget, "AED", emptyList(), emptyList()))!!
        assertNull(BudgetEditorRestoration.restore(payload, "AED", emptyList(), emptyList(), emptyList()))
        assertNull(BudgetEditorRestoration.restore(payload, "AED", listOf(budget.copy(amount = 900.0)), emptyList(), emptyList()))
        assertNull(BudgetEditorRestoration.restore(payload, "USD", listOf(budget), emptyList(), emptyList()))
    }
    @Test fun `a completed creation is not reopened as another add`() {
        val payload = BudgetEditorRestoration.save(BudgetModalData(null, "AED", emptyList(), emptyList(), budget.id))!!
        assertNull(BudgetEditorRestoration.restore(payload, "AED", listOf(budget), emptyList(), emptyList()))
    }
}
