package com.ivy.home

import androidx.compose.runtime.Immutable
import com.ivy.domain.usecase.cashflow.PlannedCashflowType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.time.LocalDate

enum class MoneyComingUpStatus {
    LOADING,
    CONTENT,
    EMPTY,
    ERROR,
}

@Immutable
data class MoneyComingUpEvent(
    val date: LocalDate,
    val type: PlannedCashflowType,
    val amount: BigDecimal,
)

@Immutable
data class MoneyComingUpState(
    val status: MoneyComingUpStatus = MoneyComingUpStatus.LOADING,
    val currentAvailable: BigDecimal = BigDecimal.ZERO,
    val upcomingIncome: BigDecimal = BigDecimal.ZERO,
    val upcomingExpenses: BigDecimal = BigDecimal.ZERO,
    val lowestProjectedAvailable: BigDecimal = BigDecimal.ZERO,
    val shortfall: BigDecimal = BigDecimal.ZERO,
    val safeToSpend: BigDecimal = BigDecimal.ZERO,
    val events: ImmutableList<MoneyComingUpEvent> = persistentListOf(),
    val errorMessage: String? = null,
)
