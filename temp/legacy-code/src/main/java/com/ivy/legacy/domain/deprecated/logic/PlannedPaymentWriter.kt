package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.model.TransactionType
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.CategoryDao
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import java.util.UUID
import javax.inject.Inject

class PlannedPaymentConflict : IllegalStateException("Planned payment changed or is no longer available")

/** The rule and its pending ledger entries are one acknowledged, replay-safe write. */
class PlannedPaymentWriter @Inject constructor(
    private val atomic: AtomicWriteRepository,
    private val reader: PlannedPaymentRuleDao,
    private val writer: WritePlannedPaymentRuleDao,
    private val accounts: AccountDao,
    private val categories: CategoryDao,
    private val generator: PlannedPaymentsGenerator,
    private val transactions: TransactionRepository,
) {
    suspend fun save(draft: PlannedPaymentRule, creating: Boolean, expected: PlannedPaymentRule?): PlannedPaymentRule = atomic.run {
        require(draft.type != TransactionType.TRANSFER && draft.amount.isFinite() && draft.amount > 0)
        require(!draft.isDeleted && draft.startDate != null)
        require(draft.oneTime || (draft.intervalN != null && draft.intervalN > 0 && draft.intervalType != null))
        if (accounts.findById(draft.accountId)?.isDeleted != false) throw PlannedPaymentConflict()
        if (draft.categoryId != null && categories.findById(draft.categoryId)?.isDeleted != false) throw PlannedPaymentConflict()
        val submitted = draft.copy(title = draft.title?.trim(), description = draft.description?.trim(), isSynced = false)
        val current = reader.findById(draft.id)?.toLegacyDomain()?.copy(isSynced = false)
        // A commit may finish before Android delivers its acknowledgement. It must not
        // regenerate entries, move due dates or duplicate a completed one-time payment.
        if (current == submitted) return@run submitted
        if (creating) {
            if (current != null) throw PlannedPaymentConflict()
        } else if (current == null || expected == null || current != expected.copy(isSynced = false)) {
            throw PlannedPaymentConflict()
        }
        writer.save(submitted.toEntity())
        generator.generate(submitted, requireComplete = true)
        submitted
    }

    suspend fun delete(id: UUID) = atomic.run {
        transactions.deletedByRecurringRuleIdAndNoDateTime(id)
        writer.deleteById(id)
    }
}
