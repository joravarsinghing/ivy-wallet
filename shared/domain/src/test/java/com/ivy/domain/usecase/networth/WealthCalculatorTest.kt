package com.ivy.domain.usecase.networth

import com.ivy.data.model.wealth.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class WealthCalculatorTest {
    private val day = LocalDate.parse("2026-10-21")
    private val stamp = "2026-10-21T12:00:00Z"
    private fun item(id: String, currency: String = "AED", source: WealthSource = WealthSource.MANUAL,
        convention: BalanceConvention = BalanceConvention.ASSET, parent: String? = null) =
        WealthItem(id, id, WealthKind.OTHER_ASSET, currency, source, if (source == WealthSource.ACCOUNT || source == WealthSource.LOAN) id else null, parent, convention, "2026-01-01")
    private fun policy(id: String, included: Boolean = true, ownership: String = "100", date: String = "2026-01-01") =
        WealthPolicy("p-$id-$date-$included", id, date, included, ownership, recordedAt = stamp)
    private fun value(id: String, amount: String, date: String = "2026-01-01", quantity: String? = null, price: String? = null) =
        WealthValuation("v-$id-$date", id, date, amount, quantity, price, recordedAt = stamp)
    private fun calculate(data: WealthBackup, sources: List<WealthSourceValue> = emptyList(), date: LocalDate = day) = WealthCalculator.calculate(data, sources, date, "AED")
    private fun equal(expected: String, actual: BigDecimal) { assertEquals(0, BigDecimal(expected).compareTo(actual)) }

    @Test fun `property ownership and mortgage produce equity without double subtraction`() {
        val data = WealthBackup(items = listOf(item("home"), item("mortgage", convention = BalanceConvention.POSITIVE_OWED)),
            policies = listOf(policy("home", ownership = "50"), policy("mortgage")),
            valuations = listOf(value("home", "1000000"), value("mortgage", "200000")))
        val result = calculate(data)
        equal("500000", result.assets); equal("200000", result.liabilities); equal("300000", result.netWorth)
    }
    @Test fun `repaying principal from cash does not create wealth`() {
        val data = WealthBackup(items = listOf(item("bank", source = WealthSource.ACCOUNT), item("debt", source = WealthSource.LOAN, convention = BalanceConvention.POSITIVE_OWED)), policies = listOf(policy("bank"), policy("debt")))
        val before = calculate(data, listOf(WealthSourceValue("bank", BigDecimal("10000")), WealthSourceValue("debt", BigDecimal("5000"))))
        val after = calculate(data, listOf(WealthSourceValue("bank", BigDecimal("9000")), WealthSourceValue("debt", BigDecimal("4000"))))
        equal("5000", before.netWorth); equal("5000", after.netWorth)
        equal("0", WealthCalculator.changes(before, after).fold(BigDecimal.ZERO) { s, c -> s + c.amount })
    }
    @Test fun `interest payment reduces wealth but principal component cancels`() {
        val data = WealthBackup(items = listOf(item("bank", source = WealthSource.ACCOUNT), item("debt", source = WealthSource.LOAN, convention = BalanceConvention.POSITIVE_OWED)), policies = listOf(policy("bank"), policy("debt")))
        val before = calculate(data, listOf(WealthSourceValue("bank", BigDecimal("10000")), WealthSourceValue("debt", BigDecimal("5000"))))
        val after = calculate(data, listOf(WealthSourceValue("bank", BigDecimal("8800")), WealthSourceValue("debt", BigDecimal("4000"))))
        equal("-200", after.netWorth - before.netWorth)
    }
    @Test fun `overdrafts and credit card overpayments cross sides correctly`() {
        val data = WealthBackup(items = listOf(item("bank", source = WealthSource.ACCOUNT), item("card", source = WealthSource.ACCOUNT, convention = BalanceConvention.NEGATIVE_OWED)), policies = listOf(policy("bank"), policy("card")))
        val result = calculate(data, listOf(WealthSourceValue("bank", BigDecimal("-30")), WealthSourceValue("card", BigDecimal("20"))))
        equal("20", result.assets); equal("30", result.liabilities); equal("-10", result.netWorth)
    }
    @Test fun `missing foreign exchange is incomplete rather than zero valuation`() {
        val result = calculate(WealthBackup(items = listOf(item("usd", "USD")), policies = listOf(policy("usd")), valuations = listOf(value("usd", "100"))))
        assertFalse(result.complete); assertNull(result.lines.single().reportingAmount); assertTrue(result.issues.single().contains("exchange rate"))
    }
    @Test fun `future rates and valuations never leak into past dates`() {
        val rate = WealthFx("rate", "USD", "AED", "3.67", "2026-11-01", recordedAt = stamp)
        val data = WealthBackup(items = listOf(item("usd", "USD")), policies = listOf(policy("usd")), valuations = listOf(value("usd", "100", "2026-11-01")), rates = listOf(rate))
        assertNull(calculate(data).lines.single().nativeAmount)
        assertNull(WealthCalculator.rate(listOf(rate), "USD", "AED", day.toString()))
    }
    @Test fun `portfolio counts constituent holdings exactly once`() {
        val data = WealthBackup(items = listOf(item("portfolio", source = WealthSource.HOLDINGS), item("stock", parent = "portfolio")),
            policies = listOf(policy("portfolio"), policy("stock")), valuations = listOf(value("stock", "999999", quantity = "1.25", price = "100")))
        equal("125", calculate(data).assets)
    }
    @Test fun `switching to total valuation excludes old constituent totals`() {
        val data = WealthBackup(items = listOf(item("portfolio"), item("stock", parent = "portfolio")), policies = listOf(policy("portfolio"), policy("stock")), valuations = listOf(value("portfolio", "500"), value("stock", "100")))
        equal("500", calculate(data).assets)
    }
    @Test fun `archival is effective dated and keeps earlier value`() {
        val archived = policy("home", false, date = "2026-10-01").copy(archived = true)
        val data = WealthBackup(items = listOf(item("home")), policies = listOf(policy("home"), archived), valuations = listOf(value("home", "1000")))
        equal("1000", calculate(data, date = LocalDate.parse("2026-09-30")).assets)
        equal("0", calculate(data).assets)
    }
    @Test fun `same date correction supersedes original without erasing audit trail`() {
        val original = value("home", "1000")
        val correction = original.copy(id = "correction", amount = "1200", supersedesId = original.id, recordedAt = "2026-10-21T13:00:00Z")
        val data = WealthBackup(items = listOf(item("home")), policies = listOf(policy("home")), valuations = listOf(original, correction))
        equal("1200", calculate(data).assets); assertEquals(2, data.valuations.size)
    }
    @Test fun `explicit duplicate debt source contributes once`() {
        val account = item("card", source = WealthSource.ACCOUNT, convention = BalanceConvention.NEGATIVE_OWED)
        val loan = item("loan", source = WealthSource.LOAN, convention = BalanceConvention.POSITIVE_OWED).copy(duplicateAccountId = "card")
        val data = WealthBackup(items = listOf(account, loan), policies = listOf(policy("card"), policy("loan")))
        equal("100", calculate(data, listOf(WealthSourceValue("card", BigDecimal("-100")), WealthSourceValue("loan", BigDecimal("100")))).liabilities)
    }
    @Test fun `currency movements are separately reconciled`() {
        val firstRate = WealthFx("r1", "USD", "AED", "3", "2026-01-01", recordedAt = stamp)
        val secondRate = firstRate.copy(id = "r2", rate = "4", effectiveDate = "2026-10-01")
        val data = WealthBackup(items = listOf(item("usd", "USD")), policies = listOf(policy("usd")), valuations = listOf(value("usd", "100")), rates = listOf(firstRate, secondRate))
        val before = calculate(data, date = LocalDate.parse("2026-09-30")); val after = calculate(data)
        val changes = WealthCalculator.changes(before, after)
        equal("100", changes.first { it.label == "FX revaluation" }.amount)
        equal("100", changes.fold(BigDecimal.ZERO) { s, c -> s + c.amount })
    }
    @Test fun `excluded source with missing value does not block known totals`() {
        val data = WealthBackup(items = listOf(item("excluded", "USD"), item("cash")), policies = listOf(policy("excluded", false), policy("cash")), valuations = listOf(value("cash", "0")))
        val result = calculate(data)
        assertTrue(result.complete); equal("0", result.netWorth)
    }
    @Test fun `ownership precision is retained before display rounding`() {
        val data = WealthBackup(items = listOf(item("asset")), policies = listOf(policy("asset", ownership = "12.5")), valuations = listOf(value("asset", "0.10")))
        equal("0.0125", calculate(data).netWorth)
    }
    @Test fun `multi currency holdings use reproducible two leg rates`() {
        val rates = listOf(WealthFx("eur-usd", "EUR", "USD", "1.1", "2026-01-01", recordedAt = stamp), WealthFx("usd-aed", "USD", "AED", "3.67", "2026-01-01", recordedAt = stamp))
        val converted = WealthCalculator.rate(rates, "EUR", "AED", day.toString())!!
        equal("4.037", converted.first)
        assertEquals("eur-usd+usd-aed", converted.second?.id)
        val data = WealthBackup(items = listOf(item("portfolio", "USD", WealthSource.HOLDINGS), item("stock", "EUR", parent = "portfolio")), policies = listOf(policy("portfolio"), policy("stock")), valuations = listOf(value("stock", "100")), rates = rates)
        equal("403.7", calculate(data).assets)
    }
    @Test fun `newer inverse quote supersedes an older direct quote`() {
        val old = WealthFx("old", "USD", "AED", "3", "2026-01-01", recordedAt = stamp)
        val newer = WealthFx("new", "AED", "USD", "0.25", "2026-10-01", recordedAt = stamp)
        val rate = WealthCalculator.rate(listOf(old, newer), "USD", "AED", day.toString())!!
        equal("4", rate.first); assertEquals("new", rate.second?.id)
    }
    @Test fun `fractional second updates sort after a whole second ownership and FX record`() {
        val first = "2026-10-21T12:00:00Z"
        val second = "2026-10-21T12:00:00.100Z"
        val oldPolicy = policy("usd").copy(recordedAt = first)
        val newPolicy = oldPolicy.copy(id = "changed-policy", ownership = "50", recordedAt = second)
        val oldRate = WealthFx("old", "USD", "AED", "3", "2026-01-01", recordedAt = first)
        val newRate = oldRate.copy(id = "new", rate = "4", recordedAt = second)
        val data = WealthBackup(items = listOf(item("usd", "USD")), policies = listOf(oldPolicy, newPolicy), valuations = listOf(value("usd", "100")), rates = listOf(oldRate, newRate))
        equal("200", calculate(data).netWorth)
    }
}
