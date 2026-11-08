package com.ivy.planned.list

import androidx.compose.runtime.Immutable

@Immutable
data class CashFlowForecast(
    val currentBalance: Double = 0.0,
    val recurringIncome: Double = 0.0,
    val recurringExpenses: Double = 0.0,
    val oneTimeIncome: Double = 0.0,
    val oneTimeExpenses: Double = 0.0,
) {
    val recurringNet: Double get() = recurringIncome - recurringExpenses
    val nextPeriodNet: Double get() = recurringNet + oneTimeIncome - oneTimeExpenses
    val projectedBalance: Double get() = currentBalance + nextPeriodNet
}

fun calculateCashFlowForecast(
    currentBalance: Double = 0.0,
    recurringIncome: Double,
    recurringExpenses: Double,
    oneTimeIncome: Double,
    oneTimeExpenses: Double,
): CashFlowForecast = CashFlowForecast(
    currentBalance = currentBalance,
    recurringIncome = recurringIncome,
    recurringExpenses = recurringExpenses,
    oneTimeIncome = oneTimeIncome,
    oneTimeExpenses = oneTimeExpenses,
)
