package com.ivy.reports.analytics
enum class ExploreSection { SPENDING, CASH_FLOW, WEALTH, DEBT, GOALS }
typealias ChartKind = com.ivy.base.analytics.ChartKind
typealias Ink = com.ivy.base.analytics.Ink
typealias ChartValue = com.ivy.base.analytics.ChartValue
typealias ChartSeries = com.ivy.base.analytics.ChartSeries
typealias AnalyticsCard = com.ivy.base.analytics.AnalyticsCard
typealias AnalyticsEntry = com.ivy.domain.usecase.analytics.AnalyticsEntry
typealias AnalyticsBoard = com.ivy.domain.usecase.analytics.AnalyticsBoard
/** Compatibility facade; all financial reducers now live in shared/domain. */
object AnalyticsEngine {
    private val engine = com.ivy.domain.usecase.analytics.AnalyticsEngine
    fun spending(entries: List<AnalyticsEntry>, from: java.time.LocalDate, to: java.time.LocalDate, currency: String) = engine.spending(entries, from, to, currency)
    fun cashFlow(entries: List<AnalyticsEntry>, from: java.time.LocalDate, to: java.time.LocalDate, currency: String) = engine.cashFlow(entries, from, to, currency)
    fun savingsRate(income: java.math.BigDecimal, spent: java.math.BigDecimal) = engine.savingsRate(income, spent)
    fun waterfallSteps(card: AnalyticsCard) = engine.waterfallSteps(card)
    fun quantile(sorted: List<java.math.BigDecimal>, fraction: Double) = engine.quantile(sorted, fraction)
    fun histogram(amounts: List<java.math.BigDecimal>, currency: String) = engine.histogram(amounts, currency)
    fun buckets(from: java.time.LocalDate, to: java.time.LocalDate, limit: Int = 30) = engine.buckets(from, to, limit)
}
