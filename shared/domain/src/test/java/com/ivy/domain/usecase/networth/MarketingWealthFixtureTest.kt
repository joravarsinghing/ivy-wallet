package com.ivy.domain.usecase.networth

import com.ivy.base.model.TransactionType
import com.ivy.data.backup.IvyWalletCompleteData
import com.ivy.data.model.wealth.WealthSource
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

class MarketingWealthFixtureTest {
    @Test fun `fictional account balances and complete net worth agree with marketing manifest`() {
        val fixture = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "shared/data/core/src/test/resources/backups/jvy-marketing-2026-10.json") }.first { it.isFile }
        val data = Json.decodeFromString(IvyWalletCompleteData.serializer(), fixture.readText())
        val balances = data.accounts.associate { account -> account.id.toString() to data.transactions.fold(BigDecimal.ZERO) { sum, tx ->
            val debit = if (tx.accountId == account.id) BigDecimal.valueOf(tx.amount).let { if (tx.type == TransactionType.INCOME) it else it.negate() } else BigDecimal.ZERO
            val credit = if (tx.type == TransactionType.TRANSFER && tx.toAccountId == account.id) BigDecimal.valueOf(tx.toAmount!!) else BigDecimal.ZERO
            sum + debit + credit
        } }
        assertEquals(0, BigDecimal("92205").compareTo(balances.values.fold(BigDecimal.ZERO, BigDecimal::add)))
        val sources = data.wealth!!.items.mapNotNull { item -> when (item.source) {
            WealthSource.ACCOUNT -> WealthSourceValue(item.id, balances[item.sourceId])
            WealthSource.LOAN -> {
                val loan = data.loans.single { it.id.toString() == item.sourceId }
                val remainder = data.loanRecords.filter { it.loanId == loan.id && !it.interest }.fold(BigDecimal.valueOf(loan.amount)) { amount, record -> amount - BigDecimal.valueOf(record.amount) }
                WealthSourceValue(item.id, remainder)
            }
            else -> null
        } }
        val result = WealthCalculator.calculate(data.wealth!!, sources, LocalDate.parse("2026-10-25"), "AED")
        assertTrue(result.issues.toString(), result.complete)
        assertEquals(0, BigDecimal("209905").compareTo(result.netWorth))
        assertEquals(0, BigDecimal("15800").compareTo(result.liabilities))
        assertEquals(12, result.lines.size)
    }
}
