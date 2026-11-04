package com.ivy.budgets

import com.ivy.design.system.ProcessDraftState
import com.ivy.design.system.draftUuid
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Budget
import com.ivy.data.model.Category
import java.security.MessageDigest

internal object BudgetEditorRestoration {
    private fun signature(budget: Budget): String = MessageDigest.getInstance("SHA-256")
        .digest(budget.copy(isSynced = false, orderId = 0.0).toString().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    fun save(modal: BudgetModalData?): List<String>? = modal?.let {
        ProcessDraftState.encode(mapOf("session" to it.id.toString(), "record" to it.budget?.id?.toString(),
            "baseline" to it.budget?.let(::signature), "currency" to it.baseCurrency,
            "focus" to it.autoFocusKeyboard.toString()))
    }
    fun restore(payload: List<String>, currency: String, budgets: List<Budget>, categories: List<Category>, accounts: List<Account>): BudgetModalData? = runCatching {
        val fields = requireNotNull(ProcessDraftState.decode(payload))
        val session = requireNotNull(fields.draftUuid("session"))
        val original = fields.draftUuid("record")?.let { id -> requireNotNull(budgets.firstOrNull { it.id == id }) }
        require(fields["currency"] == currency)
        require(original == null || fields["baseline"] == signature(original))
        // A new intent already present in storage is not another Add operation.
        require(original != null || budgets.none { it.id == session })
        BudgetModalData(original, currency, categories, accounts, session, fields["focus"] == "true")
    }.getOrNull()
}
