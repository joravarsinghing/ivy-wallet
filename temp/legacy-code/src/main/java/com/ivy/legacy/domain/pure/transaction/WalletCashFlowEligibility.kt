package com.ivy.wallet.domain.pure.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import java.math.BigDecimal
import java.util.UUID

/** Existing Home trend policy; adjustments change ledger balance, not earned income. */
fun isWalletCashFlowTransaction(transaction: Transaction): Boolean =
    transaction.type != TransactionType.TRANSFER && !(transaction.type == TransactionType.INCOME &&
        transaction.title?.trim()?.equals("Adjust balance", ignoreCase = true) == true)

/** Native account legs, never wallet income. Receiving transfers use stored toAmount. */
fun accountCashFlow(transaction: Transaction, accountId: UUID, includeTransfers: Boolean,
                    excludeAdjustments: Boolean = true): Pair<BigDecimal, BigDecimal> {
    if (transaction.isDeleted || excludeAdjustments && transaction.title?.trim()?.equals("Adjust balance", true) == true)
        return BigDecimal.ZERO to BigDecimal.ZERO
    return when (transaction.type) {
        TransactionType.INCOME -> (if (transaction.accountId == accountId) transaction.amount else BigDecimal.ZERO) to BigDecimal.ZERO
        TransactionType.EXPENSE -> BigDecimal.ZERO to (if (transaction.accountId == accountId) transaction.amount else BigDecimal.ZERO)
        TransactionType.TRANSFER -> if (includeTransfers)
            (if (transaction.toAccountId == accountId) transaction.toAmount else BigDecimal.ZERO) to
                (if (transaction.accountId == accountId) transaction.amount else BigDecimal.ZERO)
            else BigDecimal.ZERO to BigDecimal.ZERO
    }
}
