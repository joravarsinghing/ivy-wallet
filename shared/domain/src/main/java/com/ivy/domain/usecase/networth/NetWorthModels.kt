package com.ivy.domain.usecase.networth

import com.ivy.data.model.AccountId
import java.math.BigDecimal

/**
 * Explicit account classification for true net worth.
 *
 * This is deliberately separate from [includeInBalance]. A wallet account can be included in
 * cash balance while still needing an explicit asset/liability decision for net worth.
 */
enum class NetWorthAccountRole {
    ASSET,
    LIABILITY,
}

data class NetWorthAccountClassification(
    val accountId: AccountId,
    val role: NetWorthAccountRole,
)

data class NetWorthReadiness(
    val unclassifiedAccountIds: Set<AccountId>,
) {
    val isReady: Boolean get() = unclassifiedAccountIds.isEmpty()
}

data class NetWorthSnapshot(
    val assets: BigDecimal,
    val liabilities: BigDecimal,
) {
    val netWorth: BigDecimal get() = assets.subtract(liabilities)
}

/** Pure foundation for a future net-worth screen and historical net-worth series. */
object NetWorthCalculator {
    fun readiness(
        includedAccountIds: Set<AccountId>,
        classifications: List<NetWorthAccountClassification>,
    ): NetWorthReadiness {
        val classifiedIds = classifications.map { it.accountId }.toSet()
        return NetWorthReadiness(includedAccountIds - classifiedIds)
    }

    fun calculate(
        balances: Map<AccountId, BigDecimal>,
        classifications: List<NetWorthAccountClassification>,
    ): NetWorthSnapshot {
        val roleByAccount = classifications.associateBy { it.accountId }
        val assets = balances.entries
            .filter { roleByAccount[it.key]?.role == NetWorthAccountRole.ASSET }
            .fold(BigDecimal.ZERO) { total, entry -> total.add(entry.value) }
        val liabilities = balances.entries
            .filter { roleByAccount[it.key]?.role == NetWorthAccountRole.LIABILITY }
            .fold(BigDecimal.ZERO) { total, entry -> total.add(entry.value.abs()) }
        return NetWorthSnapshot(assets = assets, liabilities = liabilities)
    }
}
