package com.ivy.data.backup

import androidx.annotation.Keep
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.BudgetEntity
import com.ivy.data.db.entity.CategoryEntity
import com.ivy.data.db.entity.LoanEntity
import com.ivy.data.db.entity.LoanRecordEntity
import com.ivy.data.db.entity.PlannedPaymentRuleEntity
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.data.db.entity.TagAssociationEntity
import com.ivy.data.db.entity.TagEntity
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.db.entity.SavedAnalyticsEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Suppress("DataClassDefaultValues")
data class IvyWalletCompleteData(
    @SerialName("accounts")
    val accounts: List<AccountEntity> = emptyList(),
    @SerialName("budgets")
    val budgets: List<BudgetEntity> = emptyList(),
    @SerialName("categories")
    val categories: List<CategoryEntity> = emptyList(),
    @SerialName("loanRecords")
    val loanRecords: List<LoanRecordEntity> = emptyList(),
    @SerialName("loans")
    val loans: List<LoanEntity> = emptyList(),
    @SerialName("plannedPaymentRules")
    val plannedPaymentRules: List<PlannedPaymentRuleEntity> = emptyList(),
    @SerialName("settings")
    val settings: List<SettingsEntity> = emptyList(),
    @SerialName("transactions")
    val transactions: List<TransactionEntity> = emptyList(),
    @SerialName("sharedPrefs")
    val sharedPrefs: HashMap<String, String> = HashMap(),
    @SerialName("tags")
    val tags: List<TagEntity> = emptyList(),
    @SerialName("tagAssociations")
    val tagAssociations: List<TagAssociationEntity> = emptyList(),
    @SerialName("savedAnalytics")
    val savedAnalytics: List<SavedAnalyticsEntity> = emptyList(),
    @SerialName("goals")
    val goals: List<com.ivy.data.db.entity.GoalEntity> = emptyList(),
    @SerialName("goalAllocations")
    val goalAllocations: List<com.ivy.data.db.entity.GoalAllocationEntity> = emptyList(),
    @SerialName("categoryRules")
    val categoryRules: List<com.ivy.data.db.entity.CategoryRuleEntity> = emptyList(),
    @SerialName("netWorthAssetAccountIds")
    val netWorthAssetAccountIds: List<String> = emptyList(),
    @SerialName("netWorthLiabilityAccountIds")
    val netWorthLiabilityAccountIds: List<String> = emptyList(),
    @SerialName("netWorthHistory")
    val netWorthHistory: List<NetWorthBackupSnapshot> = emptyList(),
    @SerialName("netWorthSettingsVersion")
    val netWorthSettingsVersion: Int = 0,
    @SerialName("wealth")
    val wealth: com.ivy.data.model.wealth.WealthBackup? = null
)

@Keep
@Serializable
data class NetWorthBackupSnapshot(
    @SerialName("period") val period: String,
    @SerialName("assets") val assets: Double,
    @SerialName("liabilities") val liabilities: Double,
)
