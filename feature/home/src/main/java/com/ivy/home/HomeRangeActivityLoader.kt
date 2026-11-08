package com.ivy.home

import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.Category
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.action.category.LegacyCategoryIncomeWithAccountFiltersAct
import com.ivy.wallet.domain.action.transaction.HistoryWithDateDivsAct
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

data class HomeRangeActivity(val history: ImmutableList<TransactionHistoryItem>, val spending: HomeSpendingBreakdownState)

/** Computes a local result; the owner publishes it with the plot after its generation check. */
class HomeRangeActivityLoader @Inject constructor(
    private val history: HistoryWithDateDivsAct,
    private val categoryAmounts: LegacyCategoryIncomeWithAccountFiltersAct,
) {
    suspend fun load(window: HomeDateWindow, converter: TimeConverter, currency: String,
                     accounts: List<Account>, categories: List<Category>, filterExcluded: Boolean = false): HomeRangeActivity {
        val items = history(HistoryWithDateDivsAct.Input(window.toUtcRange(converter), currency))
        val allowedAccounts = accounts.filter { !filterExcluded || it.includeInBalance }.map { it.id }.toSet()
        val transactions = items.filterIsInstance<Transaction>().filter {
            !filterExcluded || (it.accountId in allowedAccounts && isHomeCashFlow(it))
        }
        val rows = (categories + listOf(null)).mapNotNull { category ->
            val amount = categoryAmounts(LegacyCategoryIncomeWithAccountFiltersAct.Input(
                transactions, accounts, category, currency)).expense
            if (amount.signum() <= 0) null else HomeSpendingBreakdownRow(
                category?.id?.value?.toString(), category?.name?.value ?: "Uncategorized",
                category?.color?.value ?: 0xFF777777.toInt(), amount, BigDecimal.ZERO)
        }
        return HomeRangeActivity(items.toImmutableList(), buildHomeSpendingBreakdown(rows))
    }
}
