package com.ivy.domain.usecase.analytics

import com.ivy.base.analytics.*


import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.Instant
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

data class AnalyticsEntry(
    val id: UUID, val date: LocalDate, val income: Boolean, val amount: BigDecimal,
    val categoryId: UUID?, val category: String, val title: String,
)
data class AnalyticsBoard(val caption: String, val cards: List<AnalyticsCard>, val notices: List<String> = emptyList(),
                          val from: Instant? = null, val to: Instant? = null)

/** Read-only, exact-money reducers. Signed refunds are never coerced into positive spending. */
object AnalyticsEngine {
    private val mc = MathContext.DECIMAL128
    private val zero = BigDecimal.ZERO
    private fun Iterable<AnalyticsEntry>.total() = fold(zero) { sum, row -> sum + row.amount }
    private fun series(name: String, ink: Ink, values: List<ChartValue>) = ChartSeries(name, ink, values)

    fun spending(entries: List<AnalyticsEntry>, from: LocalDate, to: LocalDate, currency: String): List<AnalyticsCard> {
        val expenses = entries.filterNot { it.income }
        if (expenses.isEmpty()) return emptyList()
        val categories = expenses.groupBy { it.categoryId }.map { (id, rows) ->
            ChartValue(rows.first().category, rows.total(), categoryId = id)
        }.sortedByDescending { it.value }
        val cards = mutableListOf(
            AnalyticsCard("categories", "Where your money went", "Net spending by category, including refunds. Open a category from View values.",
                ChartKind.BARS, listOf(series("Spent", Ink.SPENDING, categories)), currency),
        )
        if (categories.all { it.value!!.signum() >= 0 } && categories.any { it.value!!.signum() > 0 }) {
            val slices = if (categories.size <= 5) categories else categories.take(4) + ChartValue("Other", categories.drop(4).fold(zero) { a, b -> a + b.value!! })
            cards += AnalyticsCard("share", "Spending share", "Category shares, with smaller categories grouped as Other when needed. Exact amounts are below; slices are not separate accounts.",
                ChartKind.DONUT, listOf(series("Spent", Ink.SPENDING, slices)), currency)
        }
        val weekdays = (1..7).map { weekday ->
            ChartValue(java.time.DayOfWeek.of(weekday).name.lowercase().replaceFirstChar { it.uppercase() },
                expenses.filter { it.date.dayOfWeek.value == weekday }.total())
        }
        cards += AnalyticsCard("weekdays", "Which days cost more?", "Total net spending on each weekday in this report, not an average day.", ChartKind.BARS,
            listOf(series("Spent", Ink.SPENDING, weekdays)), currency)
        val days = buckets(from, to)
        val firstObserved = entries.minOf { it.date }
        val daily = expenses.groupBy { it.date }.mapValues { it.value.total() }
        var cumulative = zero
        cards += AnalyticsCard("pace", "Spending pace", "Cumulative net spending from the start of this report. Refunds can make the line fall.", ChartKind.LINE,
            listOf(series("Cumulative spent", Ink.SPENDING, days.map { (start, end) ->
                cumulative += daily.filterKeys { it >= start && it <= end }.values.fold(zero, BigDecimal::add)
                ChartValue(end.toString(), cumulative.takeUnless { end < firstObserved }, date = start, end = end)
            })), currency)
        val positive = expenses.filter { it.amount.signum() > 0 }
        if (positive.isNotEmpty()) {
            cards += AnalyticsCard("sizes", "Typical purchase sizes", "Equal-width amount bands count positive expense entries. Refunds are excluded from this distribution only.",
                ChartKind.BARS, listOf(series("Transactions", Ink.BALANCE, histogram(positive.map { it.amount }, currency))), "transactions", advanced = true)
        }
        val titles = expenses.groupBy { it.title.ifBlank { "Untitled expense" } }.map { (title, rows) -> ChartValue(title, rows.total()) }.sortedByDescending { it.value }
        cards += AnalyticsCard("titles", "Largest expense titles", "Top ten recorded titles. Titles are not verified merchants; spelling variants remain separate.",
            ChartKind.BARS, listOf(series("Net spent", Ink.SPENDING, titles.take(10))), currency, advanced = true)
        val months = monthBuckets(from, to)
        val top = categories.take(3)
        cards += AnalyticsCard("category-trends", "Category trends", "Up to three largest categories across calendar months. Partial months are labelled by their exact bounds.",
            ChartKind.LINE, top.mapIndexed { index, category ->
                series(category.label, Ink.entries[index], months.map { (start, end) ->
                    ChartValue("$start – $end", expenses.filter { it.categoryId == category.categoryId && it.date >= start && it.date <= end }.total().takeUnless { end < firstObserved }, category.categoryId, start, end)
                })
            }, currency, advanced = true)
        val boxes = positive.groupBy { it.categoryId }.values.filter { it.size >= 20 }.sortedByDescending { it.size }.take(5)
        if (boxes.isNotEmpty()) cards += AnalyticsCard("spread", "Purchase-size spread", "At least 20 positive expenses per category. Min, lower quartile, median, upper quartile and max; not outlier-adjusted whiskers.",
            ChartKind.BOX, boxes.map { rows ->
                val sorted = rows.map { it.amount }.sorted()
                series(rows.first().category, Ink.BALANCE, listOf(0.0, .25, .5, .75, 1.0).mapIndexed { i, q ->
                    ChartValue(listOf("Min", "Q1", "Median", "Q3", "Max")[i], quantile(sorted, q))
                })
            }, currency, advanced = true)
        return cards
    }

    fun cashFlow(entries: List<AnalyticsEntry>, from: LocalDate, to: LocalDate, currency: String): List<AnalyticsCard> {
        if (entries.isEmpty()) return emptyList()
        val months = monthBuckets(from, to)
        val firstObserved = entries.minOf { it.date }
        fun values(income: Boolean) = months.map { (start, end) ->
            ChartValue("$start – $end", entries.filter { it.income == income && it.date >= start && it.date <= end }.total().takeUnless { end < firstObserved }, date = start, end = end)
        }
        val incoming = values(true)
        val outgoing = values(false)
        val income = entries.filter { it.income }.total()
        val spent = entries.filterNot { it.income }.total()
        val heatStart = maxOf(from, to.minusDays(90))
        val daily = entries.filterNot { it.income }.groupBy { it.date }.mapValues { it.value.total() }
        val heat = generateSequence(heatStart) { it.plusDays(1) }.takeWhile { it <= to }.map { date ->
            ChartValue(date.toString(), if (date < firstObserved) null else daily[date] ?: zero, date = date)
        }.toList()
        return listOf(
            AnalyticsCard("monthly-flow", "Income and spending", "Calendar-month totals inside this exact report window; histories over 30 months use 30 exact date intervals. Groups run chronologically. Partial months are not full-month comparisons.", ChartKind.BARS,
                listOf(series("Income", Ink.INCOME, incoming), series("Spent", Ink.SPENDING, outgoing)), currency),
            AnalyticsCard("waterfall", "From income to surplus", "Income minus spending is a cash-flow surplus or deficit, not your balance or net worth. This view excludes transfers and balance adjustments.", ChartKind.WATERFALL,
                listOf(series("Cash flow", Ink.BALANCE, listOf(ChartValue("Income", income), ChartValue("Spent", -spent), ChartValue("Surplus / deficit", income - spent)))), currency),
            AnalyticsCard("calendar", "Spending calendar", "Daily net spending. Showing $heatStart – $to (at most 91 days). Before the first matching record is unknown, not zero; negative days are refunds.", ChartKind.HEATMAP,
                listOf(series("Daily spent", Ink.SPENDING, heat)), currency),
            AnalyticsCard("savings-rate", "Cash-flow savings rate", "(Income − spending) ÷ income × 100. Missing where income is zero or negative. This is not investment return; refunds can produce values above 100%.", ChartKind.LINE,
                listOf(series("Rate", Ink.BALANCE, incoming.indices.map { i ->
                    incoming[i].copy(value = incoming[i].value?.let { savingsRate(it, outgoing[i].value!!) })
                })), "%", advanced = true),
            AnalyticsCard("relationship", "Income versus spending", "Each point is one calendar-month bucket: income on the horizontal axis, spending on the vertical. A relationship does not establish a cause. Use longer history to judge patterns.", ChartKind.SCATTER,
                listOf(series("Income (horizontal)", Ink.INCOME, incoming), series("Spent (vertical)", Ink.SPENDING, outgoing)), currency, advanced = true,
                note = if (months.size < 20) "Fewer than 20 monthly observations: too little data to infer a relationship." else null),
        )
    }

    fun savingsRate(income: BigDecimal, spent: BigDecimal): BigDecimal? =
        if (income.signum() <= 0) null else (income - spent).divide(income, mc).multiply(BigDecimal(100))

    fun waterfallSteps(card: AnalyticsCard): List<Pair<BigDecimal, BigDecimal>> = ChartMath.waterfallSteps(card)

    fun quantile(sorted: List<BigDecimal>, fraction: Double): BigDecimal {
        require(sorted.isNotEmpty() && fraction in 0.0..1.0)
        val position = BigDecimal.valueOf(fraction).multiply(BigDecimal(sorted.lastIndex))
        val lower = position.toInt()
        val upper = (lower + 1).coerceAtMost(sorted.lastIndex)
        return sorted[lower] + (sorted[upper] - sorted[lower]).multiply(position - BigDecimal(lower))
    }

    fun histogram(amounts: List<BigDecimal>, currency: String): List<ChartValue> {
        if (amounts.isEmpty()) return emptyList()
        val max = amounts.maxOrNull()!!
        if (max.signum() <= 0) return emptyList()
        val width = max.divide(BigDecimal(5), mc)
        val counts = IntArray(5)
        amounts.forEach { counts[it.divide(width, mc).toInt().coerceIn(0, 4)]++ }
        return counts.indices.map { i ->
            val start = width.multiply(BigDecimal(i)).stripTrailingZeros().toPlainString()
            val end = width.multiply(BigDecimal(i + 1)).stripTrailingZeros().toPlainString()
            ChartValue("$start ≤ amount ${if (i == 4) "≤" else "<"} $end $currency", BigDecimal(counts[i]))
        }
    }

    /** Exact, disjoint calendar intervals. Bounded drawing cost even for all-history queries. */
    fun buckets(from: LocalDate, to: LocalDate, limit: Int = 30): List<Pair<LocalDate, LocalDate>> {
        if (to < from) return emptyList()
        val days = ChronoUnit.DAYS.between(from, to) + 1
        val count = minOf(days, limit.toLong()).toInt()
        return (0 until count).map { i ->
            from.plusDays(days * i / count) to from.plusDays(days * (i + 1) / count - 1)
        }
    }

    private fun monthBuckets(from: LocalDate, to: LocalDate): List<Pair<LocalDate, LocalDate>> {
        val count = ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) + 1
        // Long histories keep exact totals, grouping adjacent calendar months rather than dropping old rows.
        if (count > 30) return buckets(from, to)
        return (0 until count.toInt().coerceAtLeast(0)).map { offset ->
            val month = YearMonth.from(from).plusMonths(offset.toLong())
            maxOf(from, month.atDay(1)) to minOf(to, month.atEndOfMonth())
        }
    }
}
