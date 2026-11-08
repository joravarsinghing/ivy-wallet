package com.ivy.legacy.domain.deprecated.logic.csv

import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.data.IvyCurrency
import java.util.Locale

/** Validate before creating either account; never reinterpret an explicit unit. */
fun resolveImportedCsvCurrency(
    name: String?,
    rawCurrency: String?,
    baseCurrency: String,
    accounts: List<Account>,
): String? {
    val accountName = name?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val existing = accounts.firstOrNull { it.name.equals(accountName, ignoreCase = true) }
    val existingCurrency = (existing?.currency ?: baseCurrency).uppercase(Locale.ROOT)
    val requested = rawCurrency?.trim()?.takeIf(String::isNotEmpty)?.uppercase(Locale.ROOT)
    if (existing != null) {
        return existingCurrency.takeIf { requested == null || requested == it }
    }
    if (requested == null || requested == baseCurrency.uppercase(Locale.ROOT)) {
        return baseCurrency.uppercase(Locale.ROOT)
    }
    if (!Regex("[A-Z][A-Z0-9]{1,11}").matches(requested)) return null
    return IvyCurrency.fromCode(requested)?.code
}
