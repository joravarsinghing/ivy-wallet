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
}
