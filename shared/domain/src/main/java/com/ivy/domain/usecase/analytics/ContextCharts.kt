package com.ivy.domain.usecase.analytics

import com.ivy.base.analytics.*
import java.math.BigDecimal
import java.time.LocalDate

data class MoneyPosition(val label: String, val currency: String, val value: BigDecimal?, val reference: BigDecimal? = null)
data class PrincipalEvent(val date: LocalDate, val amount: BigDecimal?, val increase: Boolean, val interest: Boolean)

/** Presentation of existing, scoped values only. No queries, rates, forecasts or financial writes. */
object ContextCharts {
    fun amount(value: Double): BigDecimal? = value.takeIf { it.isFinite() }?.let(BigDecimal::valueOf)
    fun percentage(value: BigDecimal?, target: BigDecimal?): BigDecimal? =
        if (value == null || target == null || target.signum() <= 0) null
        else value.multiply(BigDecimal(100)).divide(target, java.math.MathContext.DECIMAL128)
    fun positions(id: String, title: String, explanation: String, rows: List<MoneyPosition>,
                  first: String = "Balance", second: String? = null, ink: Ink = Ink.BALANCE): List<AnalyticsCard> =
        rows.groupBy { it.currency }.toSortedMap().map { (currency, positions) ->
            AnalyticsCard("$id-$currency", title, explanation, ChartKind.BARS,
                listOf(ChartSeries(first, ink, positions.map { ChartValue(it.label, it.value) })) +
                    if (second == null) emptyList() else listOf(ChartSeries(second, Ink.BALANCE,
                        positions.map { ChartValue(it.label, it.reference) })), currency)
        }

    fun bridge(id: String, title: String, explanation: String, currency: String,
               opening: BigDecimal?, change: BigDecimal?, closing: BigDecimal?): AnalyticsCard =
        AnalyticsCard(id, title, explanation, ChartKind.WATERFALL,
            listOf(ChartSeries("Balance", Ink.BALANCE, listOf(ChartValue("Current balance", opening),
                ChartValue("Planned change", change), ChartValue("Projected balance", closing)).let { values ->
                if (opening == null || change == null || closing == null) values.map { it.copy(value = null) } else values
            })), currency,
            lastIsTotal = true, note = if (opening == null || change == null || closing == null) "Some forecast values are unavailable." else null)

    fun activity(id: String, rows: List<Pair<AnalyticsEntry, String>>): List<AnalyticsCard> =
        rows.groupBy { it.second }.toSortedMap().map { (currency, matches) ->
            val entries = matches.map { it.first }
            val from = entries.minOf { it.date }
            val to = entries.maxOf { it.date }
            val daily = entries.groupBy { it.date }
            val intervals = AnalyticsEngine.buckets(from, to)
            fun series(income: Boolean) = intervals.map { (start, end) ->
                val amount = daily.filterKeys { it >= start && it <= end }.values.flatten()
                    .filter { it.income == income }.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount }
                ChartValue("$start – $end", amount, date = start, end = end)
            }
            AnalyticsCard("$id-$currency", "Activity trend", "Matching settled activity · $from – $to. Native $currency amounts; transfers and balance adjustments excluded. This is cash flow, not account balance.",
                ChartKind.LINE, listOf(ChartSeries("Income", Ink.INCOME, series(true)), ChartSeries("Spent", Ink.SPENDING, series(false))), currency)
        }

    fun principal(id: String, title: String, currency: String, opening: BigDecimal?, start: LocalDate?,
                  today: LocalDate, events: List<PrincipalEvent>): AnalyticsCard {
        val elapsed = events.filter { it.date <= today }
        val review = opening == null || elapsed.any { it.amount == null }
        val points = if (review || start == null || start > today) emptyList() else AnalyticsEngine.buckets(start, today).map { (from, end) ->
            val remaining = elapsed.filter { !it.interest && it.date <= end }.fold(opening!!) { sum, event ->
                if (event.increase) sum + event.amount!! else sum - event.amount!!
            }
            ChartValue("$from – $end", remaining, date = from, end = end)
        }
        return AnalyticsCard(id, title, "Amount left over time in $currency. Principal increases add to the loan; repayments reduce it. Interest does not reduce principal. A negative remainder means repayments exceed recorded principal.",
            ChartKind.LINE, listOf(ChartSeries("Principal remaining", Ink.BALANCE, points)), currency,
            note = if (review) "Review unavailable amounts or currency conversions before drawing this history." else if (start == null) "Add the loan's start date to see its history." else null)
    }
}
