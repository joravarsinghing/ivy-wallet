package com.ivy.home

import com.ivy.base.model.TransactionType
import com.ivy.data.model.IntervalType
import com.ivy.domain.usecase.cashflow.PlannedCashflowEntry
import com.ivy.domain.usecase.cashflow.PlannedCashflowType
import com.ivy.legacy.datamodel.PlannedPaymentRule
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyComingUpAdapterTest {
    @Test
    fun `recurring entries cover exactly the next 30 calendar days`() {
        val rule = PlannedPaymentRule(
            startDate = Instant.parse("2026-10-14T00:00:00Z"),
            intervalN = 1,
            intervalType = IntervalType.DAY,
            oneTime = false,
            type = TransactionType.EXPENSE,
            accountId = UUID.randomUUID(),
        )

        val entries = plannedCashflowEntries(
            rulesWithAmounts = listOf(rule to BigDecimal("10.00")),
            openingDate = LocalDate.parse("2026-10-14"),
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(30, entries.size)
        assertEquals(LocalDate.parse("2026-10-14"), entries.first().date)
        assertEquals(LocalDate.parse("2026-11-12"), entries.last().date)
    }

    @Test
    fun `summary subtracts reservations once and uses lowest projected balance`() {
        val openingDate = LocalDate.parse("2026-10-14")
        val entries = listOf(
            PlannedCashflowEntry(
                id = "expense",
                date = openingDate.plusDays(1),
                type = PlannedCashflowType.EXPENSE,
                amount = BigDecimal("900.00"),
            ),
            PlannedCashflowEntry(
                id = "income",
                date = openingDate.plusDays(2),
                type = PlannedCashflowType.INCOME,
                amount = BigDecimal("500.00"),
            ),
        )

        val state = summarizeMoneyComingUp(
            openingDate = openingDate,
            openingBalance = BigDecimal("1000.00"),
            reservations = BigDecimal("200.00"),
            entries = entries,
        )

        assertEquals(BigDecimal("800.00"), state.currentAvailable)
        assertEquals(BigDecimal("500.00"), state.upcomingIncome)
        assertEquals(BigDecimal("900.00"), state.upcomingExpenses)
        assertEquals(BigDecimal("-100.00"), state.lowestProjectedAvailable)
        assertEquals(BigDecimal("100.00"), state.shortfall)
        assertEquals(BigDecimal.ZERO, state.safeToSpend)
        assertTrue(state.events.size == 2)
    }

    @Test
    fun `empty window exposes current available and no events`() {
        val state = summarizeMoneyComingUp(
            openingDate = LocalDate.parse("2026-10-14"),
            openingBalance = BigDecimal("1000.00"),
            reservations = BigDecimal("200.00"),
            entries = emptyList(),
        )

        assertEquals(MoneyComingUpStatus.EMPTY, state.status)
        assertEquals(BigDecimal("800.00"), state.currentAvailable)
        assertTrue(state.events.isEmpty())
    }
}
