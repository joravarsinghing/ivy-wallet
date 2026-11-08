package com.ivy.data.model.wealth

import kotlinx.serialization.Serializable

@Serializable
enum class WealthKind { CASH, SAVINGS, INVESTMENT, RETIREMENT, PROPERTY, VEHICLE, GOLD, CRYPTO, BUSINESS, RECEIVABLE, OTHER_ASSET, CREDIT_CARD, MORTGAGE, PERSONAL_LOAN, VEHICLE_LOAN, STUDENT_LOAN, BORROWED, OTHER_DEBT }
@Serializable
enum class WealthSource { MANUAL, ACCOUNT, LOAN, HOLDINGS }
@Serializable
enum class BalanceConvention { ASSET, POSITIVE_OWED, NEGATIVE_OWED }

val WealthKind.isDebt: Boolean get() = this in setOf(WealthKind.CREDIT_CARD, WealthKind.MORTGAGE,
    WealthKind.PERSONAL_LOAN, WealthKind.VEHICLE_LOAN, WealthKind.STUDENT_LOAN, WealthKind.BORROWED, WealthKind.OTHER_DEBT)

/** Exact amounts are serialized as decimal strings, never floating point. */
@Serializable
data class WealthItem(
    val id: String,
    val name: String,
    val kind: WealthKind,
    val currency: String,
    val source: WealthSource = WealthSource.MANUAL,
    val sourceId: String? = null,
    val parentId: String? = null,
    val convention: BalanceConvention = BalanceConvention.ASSET,
    val createdDate: String,
    val note: String = "",
    val reviewDays: Int = 90,
    val apr: String? = null,
    val minimumPayment: String? = null,
    val dueDate: String? = null,
    val unit: String = "units",
    val securedById: String? = null,
    /** When an account is a duplicate representation of this loan, exclude that source. */
    val duplicateAccountId: String? = null,
)

@Serializable
data class WealthPolicy(
    val id: String,
    val itemId: String,
    val effectiveDate: String,
    val included: Boolean = true,
    val ownership: String = "100",
    val archived: Boolean = false,
    val recordedAt: String,
)

@Serializable
data class WealthValuation(
    val id: String,
    val itemId: String,
    val effectiveDate: String,
    val amount: String,
    val quantity: String? = null,
    val unitPrice: String? = null,
    val contribution: String? = null,
    val withdrawal: String? = null,
    val note: String = "",
    val recordedAt: String,
    val supersedesId: String? = null,
)

@Serializable
data class WealthFx(
    val id: String,
    val from: String,
    val to: String,
    val rate: String,
    val effectiveDate: String,
    val source: String = "Manual",
    val recordedAt: String,
)

@Serializable
data class WealthSnapshotRecord(
    val id: String,
    val asOf: String,
    val currency: String,
    val assets: String,
    val liabilities: String,
    val complete: Boolean,
    val inputFingerprint: String,
    val inputs: String,
    val recordedAt: String,
    val supersedesId: String? = null,
)

@Serializable
data class WealthReview(
    val id: String,
    val effectiveDate: String,
    val assetsReviewed: Boolean,
    val debtsReviewed: Boolean,
    val recordedAt: String,
)

@Serializable
data class WealthBackup(
    val version: Int = 1,
    val items: List<WealthItem> = emptyList(),
    val policies: List<WealthPolicy> = emptyList(),
    val valuations: List<WealthValuation> = emptyList(),
    val rates: List<WealthFx> = emptyList(),
    val snapshots: List<WealthSnapshotRecord> = emptyList(),
    val reviews: List<WealthReview> = emptyList(),
)
