package com.ivy.data.model.wealth

import java.time.Instant
import java.util.UUID

fun WealthBackup.policyOn(itemId: String, date: String): WealthPolicy? = policies
    .filter { it.itemId == itemId && it.effectiveDate <= date }
    .maxWithOrNull(compareBy<WealthPolicy> { it.effectiveDate }.thenBy { Instant.parse(it.recordedAt) }.thenBy { it.id })

fun List<WealthReview>.reviewOn(date: String): WealthReview? = filter { it.effectiveDate <= date }
    .maxWithOrNull(compareBy<WealthReview> { it.effectiveDate }.thenBy { Instant.parse(it.recordedAt) }.thenBy { it.id })

/** Financial definitions are dated positions; changing one must not rewrite earlier results. */
fun WealthItem.hasSameFinancialDefinition(other: WealthItem): Boolean =
    kind == other.kind && currency == other.currency && source == other.source && sourceId == other.sourceId &&
        parentId == other.parentId && convention == other.convention && unit == other.unit &&
        securedById == other.securedById && duplicateAccountId == other.duplicateAccountId

/** Uses existing item/policy/value records, keeping old backups readable without a schema migration. */
fun planWealthSave(
    existing: WealthBackup, proposed: WealthItem, policy: WealthPolicy, valuation: WealthValuation?,
    newId: () -> String = { UUID.randomUUID().toString() },
): WealthBackup {
    // Manual values are unsigned estimates. Their selected kind owns the sign, including
    // when saving a position previously affected by the old asset-to-debt editor bug.
    val item = if (proposed.source == WealthSource.MANUAL) proposed.copy(
        convention = if (proposed.kind.isDebt) BalanceConvention.POSITIVE_OWED else BalanceConvention.ASSET) else proposed
    val previous = existing.items.firstOrNull { it.id == item.id }
    if (previous == null || previous.hasSameFinancialDefinition(item)) {
        return WealthBackup(items = listOf(item), policies = listOf(policy), valuations = listOfNotNull(valuation))
    }
    val day = policy.effectiveDate
    val children = if (item.source == WealthSource.HOLDINGS) existing.items.filter { it.parentId == item.id } else emptyList()
    val replacedIds = (children.map { it.id } + item.id).toSet()
    val laterRecord = existing.policies.any { it.itemId in replacedIds && it.effectiveDate > day } ||
        existing.valuations.any { it.itemId in replacedIds && it.effectiveDate > day }
    require(!laterRecord) {
        "Change this setup on or after its latest recorded date. Earlier values can still be corrected without changing the setup."
    }
    val latestPriorTime = existing.policies.filter { it.itemId in replacedIds && it.effectiveDate == day }
        .maxOfOrNull { Instant.parse(it.recordedAt) }
    val requestedTime = Instant.parse(policy.recordedAt)
    val revisionTime = if (latestPriorTime != null && latestPriorTime >= requestedTime) latestPriorTime.plusNanos(1).toString() else policy.recordedAt
    val replacementId = newId()
    val replacements = mutableListOf(item.copy(id = replacementId, createdDate = day))
    val policies = mutableListOf(policy.copy(itemId = replacementId, recordedAt = revisionTime))
    val values = mutableListOf<WealthValuation>()
    valuation?.let { values += it.copy(itemId = replacementId, supersedesId = null) }
    // A group keeps its current holdings, but their former parent links remain intact in history.
    children.forEach { child ->
        val childId = newId()
        replacements += child.copy(id = childId, parentId = replacementId, createdDate = day)
        val childPolicy = existing.policyOn(child.id, day)
        if (childPolicy != null) policies += childPolicy.copy(id = newId(), itemId = childId, effectiveDate = day, recordedAt = revisionTime)
        val superseded = existing.valuations.mapNotNull { it.supersedesId }.toSet()
        existing.valuations.filter { it.itemId == child.id && it.effectiveDate <= day && it.id !in superseded }
            .maxWithOrNull(compareBy<WealthValuation> { it.effectiveDate }.thenBy { Instant.parse(it.recordedAt) }.thenBy { it.id })
            ?.let { values += it.copy(id = newId(), itemId = childId, supersedesId = null) }
    }
    replacedIds.forEach { id ->
        val old = existing.policyOn(id, day)
        policies += WealthPolicy(newId(), id, day, included = false, ownership = old?.ownership ?: "100", archived = true, recordedAt = revisionTime)
    }
    return WealthBackup(items = replacements, policies = policies, valuations = values)
}

/** Multiple generations of a linked account are valid only when their active dates do not overlap. */
fun validateWealthSourceLinks(data: WealthBackup) {
    data.items.filter { it.source == WealthSource.ACCOUNT || it.source == WealthSource.LOAN }
        .groupBy { it.source to it.sourceId }.values.filter { it.size > 1 }.forEach { positions ->
            val ids = positions.map { it.id }.toSet()
            val dates = positions.map { it.createdDate } + data.policies.filter { it.itemId in ids }.map { it.effectiveDate }
            dates.distinct().forEach { day ->
                require(positions.count { it.createdDate <= day && data.policyOn(it.id, day)?.archived != true } <= 1) {
                    "This account or loan already has an active setup on $day. Its earlier setup must be archived before another can be counted."
                }
            }
        }
}
