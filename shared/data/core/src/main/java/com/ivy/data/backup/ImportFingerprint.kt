package com.ivy.data.backup

import com.ivy.data.db.entity.TransactionEntity
import java.math.BigDecimal

/**
 * Stable identity used only while importing external CSV files.
 * External apps do not share Ivy's UUIDs, so matching the user-visible
 * transaction fields is safer than creating a second copy on every retry.
 */
data class ImportFingerprint(
    val accountId: String,
    val type: String,
    val amount: String,
    val toAccountId: String?,
    val toAmount: String?,
    val dateTime: String?,
    val title: String?,
    val description: String?,
)

fun TransactionEntity.importFingerprint(): ImportFingerprint = ImportFingerprint(
    accountId = accountId.toString(),
    type = type.name,
    amount = amount.normalizedImportAmount(),
    toAccountId = toAccountId?.toString(),
    toAmount = toAmount?.normalizedImportAmount(),
    dateTime = dateTime?.toString(),
    title = title.normalizedImportText(),
    description = description.normalizedImportText(),
)

private fun Double.normalizedImportAmount(): String =
    BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

private fun String?.normalizedImportText(): String? =
    this?.trim()?.replace(Regex("\\s+"), " ")?.lowercase()?.takeIf { it.isNotBlank() }
