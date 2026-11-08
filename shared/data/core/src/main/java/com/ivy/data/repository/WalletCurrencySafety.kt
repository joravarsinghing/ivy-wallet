package com.ivy.data.repository

/** Typed failure: no conversion has occurred and existing amounts are unchanged. */
class WalletCurrencyChangeBlocked : IllegalArgumentException(
    "Your goals, budgets, loans or older accounts use the wallet currency. " +
        "Changing it would relabel their amounts without converting them. " +
        "Keep this currency for now; account currencies and net-worth reporting currency can be set separately."
)

fun requireSafeWalletCurrencyChange(current: String?, requested: String, hasImplicitCurrencyData: Boolean) {
    require(requested.isNotBlank()) { "Choose a currency." }
    if (current != null && !current.equals(requested, ignoreCase = true) && hasImplicitCurrencyData) {
        throw WalletCurrencyChangeBlocked()
    }
}
