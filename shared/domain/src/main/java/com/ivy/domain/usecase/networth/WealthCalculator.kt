package com.ivy.domain.usecase.networth

import com.ivy.data.model.wealth.*
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WealthSourceValue(val itemId: String, val amount: BigDecimal?, val issue: String? = null)
data class WealthLine(
    val item: WealthItem,
    val nativeAmount: BigDecimal?,
    val reportingAmount: BigDecimal?,
    val valuationDate: String?,
    val rateId: String?,
    val issue: String? = null,
    val warning: String? = null,
    val included: Boolean = true,
    val exchangeRate: BigDecimal? = null,
)
data class WealthResult(
    val asOf: LocalDate,
    val currency: String,
    val lines: List<WealthLine>,
    val assets: BigDecimal,
    val liabilities: BigDecimal,
    val issues: List<String>,
) {
    val netWorth get() = assets - liabilities
    val complete get() = issues.isEmpty() && lines.any { it.included }
}
data class WealthChange(val label: String, val amount: BigDecimal)

/** Immutable, exact, as-of valuation. Positive signed positions are assets; negative are debt. */
object WealthCalculator {
    private val mc = MathContext.DECIMAL128
    fun calculate(data: WealthBackup, sources: List<WealthSourceValue>, date: LocalDate, currency: String): WealthResult {
        val asOf = date.toString()
        val policies = data.policies.filter { it.effectiveDate <= asOf }
            .groupBy { it.itemId }.mapValues { (_, values) -> values.maxWith(compareBy<WealthPolicy> { it.effectiveDate }.thenBy { java.time.Instant.parse(it.recordedAt) }.thenBy { it.id }) }
        val sourceMap = sources.associateBy { it.itemId }
        val issues = mutableListOf<String>()
        val active = data.items.filter { it.createdDate <= asOf }
        val duplicateAccounts = active.filter { it.source == WealthSource.LOAN && policies[it.id]?.let { p -> p.included && !p.archived } == true }.mapNotNull { it.duplicateAccountId }.toSet()
        val lines = mutableMapOf<String, WealthLine>()
        val visiting = mutableSetOf<String>()
        fun value(item: WealthItem): WealthLine {
            lines[item.id]?.let { return it }
            val policy = policies[item.id]
            if (policy == null) return WealthLine(item, null, null, null, null, "Inclusion has not been reviewed").also { lines[item.id] = it }
            if (!policy.included || policy.archived || item.source == WealthSource.ACCOUNT && item.sourceId in duplicateAccounts) {
                return WealthLine(item, null, null, null, null, included = false).also { lines[item.id] = it }
            }
            val manualConvention = if (item.kind.isDebt) BalanceConvention.POSITIVE_OWED else BalanceConvention.ASSET
            if (item.source == WealthSource.MANUAL && item.convention != manualConvention) {
                return WealthLine(item, null, null, null, null,
                    "Review this item's type and value to confirm whether you own it or owe it.").also { lines[item.id] = it }
            }
            if (!visiting.add(item.id)) return WealthLine(item, null, null, null, null, "Circular portfolio relationship")
            val superseded = data.valuations.filter { it.effectiveDate <= asOf }.mapNotNull { it.supersedesId }.toSet()
            val valuation = data.valuations.filter { it.itemId == item.id && it.effectiveDate <= asOf && it.id !in superseded }
                .maxWithOrNull(compareBy<WealthValuation> { it.effectiveDate }.thenBy { java.time.Instant.parse(it.recordedAt) }.thenBy { it.id })
            var issue: String? = null
            var valuationDate: String? = valuation?.effectiveDate
            var native: BigDecimal? = when (item.source) {
                WealthSource.ACCOUNT, WealthSource.LOAN -> {
                    valuationDate = asOf
                    val source = sourceMap[item.id]
                    issue = source?.issue
                    source?.amount.also { if (it == null && issue == null) issue = "Linked source is unavailable" }
                }
                WealthSource.MANUAL -> valuation?.let {
                    if (it.quantity != null && it.unitPrice != null) BigDecimal(it.quantity).multiply(BigDecimal(it.unitPrice), mc) else BigDecimal(it.amount)
                }.also { if (it == null) issue = "No valuation on or before this date" }
                WealthSource.HOLDINGS -> {
                    val children = active.filter { it.parentId == item.id }
                    if (children.isEmpty()) { issue = "Add holdings or switch to a portfolio total"; null }
                    else {
                        val childrenResult = calculateChildren(children.map(::value), item.currency, data, asOf)
                        issue = childrenResult.second
                        valuationDate = children.mapNotNull { lines[it.id]?.valuationDate }.minOrNull()
                        childrenResult.first
                    }
                }
            }
            if (native != null) {
                native = when (item.convention) {
                    BalanceConvention.ASSET -> native
                    BalanceConvention.POSITIVE_OWED -> native.negate()
                    BalanceConvention.NEGATIVE_OWED -> native
                }.multiply(BigDecimal(policy.ownership), mc).divide(BigDecimal("100"), mc)
            }
            val fx = rate(data.rates, item.currency, currency, asOf)
            if (native != null && fx == null) issue = "Missing ${item.currency}/$currency exchange rate"
            val warning = when {
                fx?.second?.source?.contains("date unknown") == true -> "Cached rate: original date unknown"
                valuationDate != null && ChronoUnit.DAYS.between(LocalDate.parse(valuationDate), date) > item.reviewDays -> "Valuation needs review ($valuationDate)"
                fx?.second != null && ChronoUnit.DAYS.between(LocalDate.parse(fx.second!!.effectiveDate), date) > 30 -> "Exchange rate is older than 30 days"
                else -> null
            }
            visiting.remove(item.id)
            return WealthLine(item, native, if (issue == null) native?.multiply(fx!!.first, mc) else null, valuationDate, fx?.second?.id, issue, warning, exchangeRate = fx?.first).also { lines[item.id] = it }
        }
        active.forEach(::value)
        val top = active.filter { it.parentId == null }.map { lines.getValue(it.id) }
        top.filter { it.included }.forEach { if (it.issue != null) issues += "${it.item.name}: ${it.issue}" }
        // A child can never also contribute its value as a standalone portfolio total.
        val amounts = top.filter { it.included }.mapNotNull { it.reportingAmount }
        return WealthResult(date, currency, lines.values.toList(), amounts.filter { it.signum() >= 0 }.fold(BigDecimal.ZERO, BigDecimal::add),
            amounts.filter { it.signum() < 0 }.fold(BigDecimal.ZERO) { sum, value -> sum + value.abs() }, issues)
    }
    private fun calculateChildren(lines: List<WealthLine>, currency: String, data: WealthBackup, date: String): Pair<BigDecimal?, String?> {
        val included = lines.filter { it.included }
        if (included.any { it.issue != null }) return null to "A holding needs a valuation or exchange rate"
        var total = BigDecimal.ZERO
        included.forEach {
            val conversion = rate(data.rates, it.item.currency, currency, date) ?: return null to "A holding needs an exchange rate"
            total += (it.nativeAmount ?: return null to "A holding needs a valuation").multiply(conversion.first, mc)
        }
        return total to null
    }
    fun rate(rates: List<WealthFx>, from: String, to: String, date: String): Pair<BigDecimal, WealthFx?>? {
        if (from == to) return BigDecimal.ONE to null
        directRate(rates, from, to, date)?.let { return it }
        val currencies = rates.flatMap { listOf(it.from, it.to) }.distinct().sorted().filter { it != from && it != to }
        for (via in currencies) {
            val a = directRate(rates, from, via, date) ?: continue
            val b = directRate(rates, via, to, date) ?: continue
            val combined = a.first.multiply(b.first, mc)
            val first = a.second!!; val second = b.second!!
            val provenance = WealthFx(first.id + "+" + second.id, from, to, combined.toPlainString(), minOf(first.effectiveDate, second.effectiveDate),
                "Via $via: ${first.source}; ${second.source}", maxOf(java.time.Instant.parse(first.recordedAt), java.time.Instant.parse(second.recordedAt)).toString())
            return combined to provenance
        }
        return null
    }
    private fun directRate(rates: List<WealthFx>, from: String, to: String, date: String): Pair<BigDecimal, WealthFx?>? {
        val latest = rates.filter { (it.from == from && it.to == to || it.from == to && it.to == from) && it.effectiveDate <= date }
            .maxWithOrNull(compareBy<WealthFx> { it.effectiveDate }.thenBy { java.time.Instant.parse(it.recordedAt) }.thenBy { it.id })
        return latest?.let { (if (it.from == from) BigDecimal(it.rate) else BigDecimal.ONE.divide(BigDecimal(it.rate), mc)) to it }
    }
    /** Reconciliation, not an assertion of investment returns. Every penny has a bucket. */
    fun changes(before: WealthResult, after: WealthResult): List<WealthChange> {
        require(before.currency == after.currency)
        val old = before.lines.filter { it.item.parentId == null }.associateBy { it.item.id }
        val current = after.lines.filter { it.item.parentId == null }.associateBy { it.item.id }
        val sums = linkedMapOf<String, BigDecimal>()
        (old.keys + current.keys).forEach { id ->
            val a = old[id]; val b = current[id]
            val av = a?.takeIf { it.included }?.reportingAmount ?: BigDecimal.ZERO
            val bv = b?.takeIf { it.included }?.reportingAmount ?: BigDecimal.ZERO
            val label = when {
                a == null || b == null || a.included != b.included -> "Added, archived or inclusion changes"
                b.item.source == WealthSource.ACCOUNT -> "Cash and account movements"
                b.item.source == WealthSource.LOAN || b.item.convention != BalanceConvention.ASSET -> "Debt and receivable movements"
                else -> "Manual valuations and investment flows"
            }
            val fxMovement = if (a?.nativeAmount != null && a.exchangeRate != null && b?.exchangeRate != null && a.included && b.included)
                a.nativeAmount.multiply(b.exchangeRate - a.exchangeRate, mc) else BigDecimal.ZERO
            sums[label] = (sums[label] ?: BigDecimal.ZERO) + bv - av - fxMovement
            if (fxMovement.signum() != 0) sums["FX revaluation"] = (sums["FX revaluation"] ?: BigDecimal.ZERO) + fxMovement
        }
        return sums.map { WealthChange(it.key, it.value) }
    }
}
