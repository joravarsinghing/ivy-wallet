package com.ivy.wallet

import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.CategoryDao
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.navigation.*
import java.util.UUID
import javax.inject.Inject

/** Validate identifiers before publishing restored routes that require a live record. */
class RestoredRouteValidator @Inject constructor(
    private val accounts: AccountDao,
    private val categories: CategoryDao,
    private val transactions: TransactionDao,
    private val planned: PlannedPaymentRuleDao,
) {
    private suspend fun account(id: UUID) = accounts.findById(id)?.isDeleted == false
    suspend fun usable(screen: Screen): Boolean = when (screen) {
        is EditTransactionScreen -> screen.initialTransactionId?.let { id ->
            transactions.findById(id)?.takeUnless { it.isDeleted }?.let { row ->
                row.amount.isFinite() && row.amount > 0 && account(row.accountId) &&
                    (row.toAccountId == null || account(row.toAccountId!!))
            } ?: false
        } ?: (screen.accountId?.let { account(it) } ?: true)
        is EditPlannedScreen -> screen.plannedPaymentRuleId?.let { id ->
            planned.findById(id)?.takeUnless { it.isDeleted }?.let { account(it.accountId) } ?: false
        } ?: (screen.accountId?.let { account(it) } ?: true)
        is TransactionsScreen -> when {
            screen.allTransactions -> true
            screen.accountId != null -> account(screen.accountId!!)
            screen.categoryId != null -> categories.findById(screen.categoryId!!)?.isDeleted == false
            else -> screen.unspecifiedCategory == true
        }
        else -> true // Other pages already have explicit missing-record/empty recovery states.
    }
}
