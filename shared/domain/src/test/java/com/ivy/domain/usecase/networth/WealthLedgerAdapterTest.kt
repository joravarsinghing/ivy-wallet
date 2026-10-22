package com.ivy.domain.usecase.networth

import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.TransactionEntity
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.*
import java.util.UUID

class WealthLedgerAdapterTest {
    private val a = UUID.randomUUID()
    private val b = UUID.randomUUID()
    private val date = LocalDate.parse("2026-10-21")
    private val posted = Instant.parse("2026-10-21T12:00:00Z")
    private fun amount(rows: List<TransactionEntity>, id: UUID) = WealthLedgerAdapter.balances(rows, date, ZoneOffset.UTC)[id.toString()] ?: BigDecimal.ZERO
    @Test fun `transfer preserves total value and posted-only semantics`() {
        val income = TransactionEntity(a, TransactionType.INCOME, 1000.0, dateTime = posted)
        val transfer = TransactionEntity(a, TransactionType.TRANSFER, 250.0, b, 250.0, dateTime = posted)
        val planned = TransactionEntity(a, TransactionType.EXPENSE, 9000.0, dueDate = posted)
        val future = TransactionEntity(a, TransactionType.INCOME, 5000.0, dateTime = posted.plusSeconds(86400))
        val rows = listOf(income, transfer, planned, future)
        assertEquals(0, BigDecimal("750").compareTo(amount(rows, a)))
        assertEquals(0, BigDecimal("250").compareTo(amount(rows, b)))
    }
    @Test fun `cross currency transfer retains each native amount`() {
        val transfer = TransactionEntity(a, TransactionType.TRANSFER, 100.0, b, 367.0, dateTime = posted)
        assertEquals(0, BigDecimal("-100").compareTo(amount(listOf(transfer), a)))
        assertEquals(0, BigDecimal("367").compareTo(amount(listOf(transfer), b)))
    }
    @Test fun `day boundary uses selected accounting zone`() {
        val row = TransactionEntity(a, TransactionType.INCOME, 10.0, dateTime = Instant.parse("2026-10-22T00:30:00Z"))
        assertTrue(WealthLedgerAdapter.balances(listOf(row), date, ZoneOffset.UTC).isEmpty())
        assertEquals(0, BigDecimal("10").compareTo(WealthLedgerAdapter.balances(listOf(row), date, ZoneOffset.ofHours(-4)).getValue(a.toString())))
    }
}
