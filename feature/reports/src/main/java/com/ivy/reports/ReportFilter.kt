package com.ivy.reports

import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.TagId
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import java.util.UUID

data class ReportFilter(
    val id: UUID = UUID.randomUUID(),
    val trnTypes: List<TransactionType>,
    val period: TimePeriod?,
    val accounts: List<Account>,
    val categories: List<Category>,
    val currency: String,
    val minAmount: Double?,
    val maxAmount: Double?,
    val includeKeywords: List<String>,
    val excludeKeywords: List<String>,
    val includedTags: List<TagId>,
    val excludedTags: List<TagId>,

) {
    companion object {
        fun allTransactionTypes(): List<TransactionType> = listOf(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.TRANSFER
        )

        fun emptyFilter(
            baseCurrency: String
        ) = ReportFilter(
            trnTypes = emptyList(),
            period = null,
            accounts = emptyList(),
            categories = emptyList(),
            currency = baseCurrency,
            includeKeywords = emptyList(),
            excludeKeywords = emptyList(),
            minAmount = null,
            maxAmount = null,
            includedTags = emptyList(),
            excludedTags = emptyList()
        )
    }

    /**
     * Reports are useful when a person chooses one filter, not only when every
     * filter section has been configured. Empty selections therefore mean
     * "all" for that dimension and a missing period means the current period.
     */
    fun normalized(
        defaultPeriod: com.ivy.legacy.data.model.TimePeriod,
        allAccounts: List<Account>,
        allCategories: List<Category>,
    ): ReportFilter = copy(
        trnTypes = trnTypes.ifEmpty { allTransactionTypes() },
        period = period ?: defaultPeriod,
        accounts = accounts.ifEmpty { allAccounts },
        categories = categories.ifEmpty { allCategories },
    )

    fun validate(): Boolean {
        if (minAmount != null && maxAmount != null) {
            if (minAmount > maxAmount) return false
            if (maxAmount < minAmount) return false
        }

        return true
    }
}
