package com.ivy.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import java.time.Instant
import java.util.UUID

/** A superseded write must not acknowledge, clear, close, or report failure for a newer draft. */
internal fun isCurrentTransactionWrite(owner: UUID?, revision: Long, current: UUID?, newestRevision: Long): Boolean =
    owner != null && owner == current && revision == newestRevision

/** Freeze the submitted draft before any dispatcher switch or queued database work. */
internal fun transactionWriteSnapshot(
    source: Transaction, accountId: UUID?, toAccountId: UUID?, categoryId: UUID?,
    type: TransactionType, amount: Double, convertedAmount: Double?, title: String?, description: String?,
    dueDate: Instant?, paidFor: Instant?, now: Instant,
): Transaction {
    require(amount.isFinite())
    require(convertedAmount == null || convertedAmount.isFinite())
    val value = amount.toBigDecimal()
    return source.copy(accountId = requireNotNull(accountId), toAccountId = toAccountId,
        categoryId = categoryId, type = type, amount = value, toAmount = convertedAmount?.toBigDecimal() ?: value,
        title = title?.trim(), description = description?.trim(), dueDate = dueDate, paidFor = paidFor,
        dateTime = if (source.dateTime == null && dueDate == null) now else source.dateTime,
        isSynced = false)
}
