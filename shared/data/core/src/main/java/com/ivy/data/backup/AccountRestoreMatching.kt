package com.ivy.data.backup

import com.ivy.data.db.entity.AccountEntity
import java.util.Locale
import java.util.UUID

internal fun restoreCurrency(currency: String?, base: String?): String? =
    (currency?.trim()?.takeIf(String::isNotBlank) ?: base?.trim()?.takeIf(String::isNotBlank))?.uppercase(Locale.ROOT)

/** Names are only a convenience match, never permission to reinterpret an existing ledger. */
internal fun accountRestoreReplacements(
    existing: List<AccountEntity>, incoming: List<AccountEntity>,
    existingBase: String?, incomingBase: String?,
): List<Pair<UUID, UUID>> {
    fun currency(account: AccountEntity, base: String?) = restoreCurrency(account.currency, base)
    val byId = existing.associateBy { it.id }
    incoming.forEach { account ->
        byId[account.id]?.let { previous ->
            require(currency(previous, existingBase) == currency(account, incomingBase)) {
                "Account '${account.name}' has the same ID but a different currency. Reconcile its currency before restoring; existing money has not been converted."
            }
        }
    }
    val existingByName = existing.filterNot { it.isDeleted }.groupBy { it.name to currency(it, existingBase) }
    val incomingByName = incoming.filterNot { it.isDeleted }.groupBy { it.name to currency(it, incomingBase) }
    val incomingIds = incoming.map { it.id }.toSet()
    return incoming.filterNot { it.isDeleted || it.id in byId }.mapNotNull { account ->
        val key = account.name to currency(account, incomingBase)
        // Unknown currencies, ambiguous names and two incoming accounts targeting one row
        // are kept separate rather than silently combined.
        val target = existingByName[key]?.singleOrNull()
        if (key.second != null && target != null && target.id !in incomingIds && incomingByName[key]?.size == 1)
            account.id to target.id else null
    }
}
