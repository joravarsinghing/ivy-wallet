package com.ivy.legacy.ui.chart

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.legacy.datamodel.Account
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

class ContextualActivityChartTest {
    private val account = Account("Bank", 0, "AED")
    private val now = Instant.parse("2026-11-07T12:00:00Z")
    private val zone = ZoneId.of("America/New_York")
    @Test fun `only settled actual cash flows enter the scoped chart`() {
        fun entry(type: TransactionType, amount: String, title: String? = null) = Transaction(account.id, type, BigDecimal(amount), title = title, dateTime = now.minusSeconds(86400))
        val rows = listOf(entry(TransactionType.INCOME, "100"), entry(TransactionType.EXPENSE, "25"), entry(TransactionType.EXPENSE, "-5"),
            entry(TransactionType.TRANSFER, "1000"), entry(TransactionType.INCOME, "2000", "Adjust balance"),
            entry(TransactionType.EXPENSE, "400").copy(dateTime = null, dueDate = now),
            entry(TransactionType.EXPENSE, "500").copy(dateTime = now.plusSeconds(100)))
        val series = activityChartSnapshot("search", rows, listOf(account), zone, now).single().series
        assertEquals(BigDecimal(100), series.first().values.single().value)
        assertEquals(BigDecimal(20), series.last().values.single().value)
    }
    @Test fun `missing account currency produces review not partial totals`() {
        val trn = Transaction(account.id, TransactionType.EXPENSE, BigDecimal.TEN, dateTime = now)
        val card = activityChartSnapshot("transaction", listOf(trn), emptyList(), zone, now).single()
        assertTrue(card.series.isEmpty())
        assertNotNull(card.note)
    }
    @Test fun `empty activity remains an explicit card rather than disappearing`() {
        val card = activityChartSnapshot("account", emptyList(), listOf(account), zone, now, account.id).single()
        assertEquals("Activity trend", card.title)
        assertTrue(card.note!!.contains("No activity"))
    }
    @Test fun `transfer preference uses native receiving and sending legs without inventing FX`() {
        val receiving = account.copy(id = java.util.UUID.randomUUID(), currency = "USD")
        val transfer = Transaction(account.id, TransactionType.TRANSFER, BigDecimal("367.25"),
            toAccountId = receiving.id, toAmount = BigDecimal("100.00"), dateTime = now)
        val accounts = listOf(account, receiving)
        val incoming = activityChartSnapshot("account", listOf(transfer), accounts, zone, now, receiving.id, true).single()
        assertEquals("USD", incoming.unit)
        assertEquals(0, incoming.series.first().values.single().value!!.compareTo(BigDecimal(100)))
        assertEquals(0, incoming.series.last().values.single().value!!.signum())
        val outgoing = activityChartSnapshot("account", listOf(transfer), accounts, zone, now, account.id, true).single()
        assertEquals(0, outgoing.series.last().values.single().value!!.compareTo(BigDecimal("367.25")))
        assertTrue(activityChartSnapshot("account", listOf(transfer), accounts, zone, now, receiving.id, false).single().series.isEmpty())
        assertTrue(activityChartSnapshot("wallet", listOf(transfer), accounts, zone, now).single().series.isEmpty())
        val own = transfer.copy(toAccountId = account.id, toAmount = transfer.amount)
        val legs = com.ivy.wallet.domain.pure.transaction.accountCashFlow(own, account.id, true)
        assertEquals(0, (legs.first - legs.second).signum())
    }
}
