package com.ivy.wallet.domain.pure.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType

/** Existing Home trend policy; adjustments change ledger balance, not earned income. */
fun isWalletCashFlowTransaction(transaction: Transaction): Boolean =
    transaction.type != TransactionType.TRANSFER && !(transaction.type == TransactionType.INCOME &&
        transaction.title?.trim()?.equals("Adjust balance", ignoreCase = true) == true)
