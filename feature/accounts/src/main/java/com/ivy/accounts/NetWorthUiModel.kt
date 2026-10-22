package com.ivy.accounts

import androidx.compose.runtime.Immutable
import com.ivy.domain.usecase.networth.NetWorthAccountRole
import java.math.BigDecimal
import java.util.UUID
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class NetWorthAccountUi(
    val id: UUID,
    val name: String,
    val balance: BigDecimal,
    val role: NetWorthAccountRole?,
)

@Immutable
data class NetWorthUiState(
    val assets: BigDecimal = BigDecimal.ZERO,
    val liabilities: BigDecimal = BigDecimal.ZERO,
    val accounts: ImmutableList<NetWorthAccountUi>,
    val history: ImmutableList<NetWorthHistoryPoint> = kotlinx.collections.immutable.persistentListOf(),
    val setupIssues: Int = 0,
    val hasWealthItems: Boolean = false,
) {
    val netWorth: BigDecimal get() = assets.subtract(liabilities)
    val unclassifiedCount: Int get() = setupIssues + accounts.count { it.role == null }
}

@Immutable
data class NetWorthHistoryPoint(
    val period: String,
    val netWorth: Double,
)
