package com.ivy.home

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

enum class HomeTrendRange(val months: Int, val label: String) {
    THREE_MONTHS(3, "3m"),
    SIX_MONTHS(6, "6m"),
    TWELVE_MONTHS(12, "12m"),
}

enum class HomeTrendMetric(val label: String) {
    INCOME("Income"),
    EXPENSES("Spent"),
    SAVINGS("Saved"),
}

@Immutable
data class HomeTrendPoint(
    val periodLabel: String,
    val income: BigDecimal,
    val expenses: BigDecimal,
) {
    val savings: BigDecimal
        get() = income.subtract(expenses)
}

@Immutable
data class HomeTrendState(
    val range: HomeTrendRange = HomeTrendRange.THREE_MONTHS,
    val points: ImmutableList<HomeTrendPoint> = persistentListOf(),
    val visibleMetrics: Set<HomeTrendMetric> = setOf(
        HomeTrendMetric.INCOME,
        HomeTrendMetric.EXPENSES,
        HomeTrendMetric.SAVINGS,
    ),
    val editorVisible: Boolean = false,
    val loading: Boolean = false,
) {
    val hasData: Boolean
        get() = points.any { it.income.signum() != 0 || it.expenses.signum() != 0 }
}
