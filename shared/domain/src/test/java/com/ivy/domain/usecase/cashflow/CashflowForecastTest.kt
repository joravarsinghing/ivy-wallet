package com.ivy.domain.usecase.cashflow

import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Test

class CashflowForecastTest {

    @Test
    fun `forecasts planned income and expenses for exactly 30 days`() {
        val result = calculateCashflowForecast(
            CashflowForecastRequest(
                openingDate = LocalDate.of(2026, 10, 1),
                openingBalance = BigDecimal("5000.00"),
                plannedEntries = listOf(
                    PlannedCashflowEntry(
                        id = "salary",
                        date = LocalDate.of(2026, 10, 3),
                        type = PlannedCashflowType.INCOME,
                        amount = BigDecimal("2500.10"),
                    ),
                    PlannedCashflowEntry(
                        id = "rent",
                        date = LocalDate.of(2026, 10, 5),
                        type = PlannedCashflowType.EXPENSE,
                        amount = BigDecimal("1800.25"),
                    ),
                ),
            ),
        )

        result.days.size shouldBe 30
        result.closingDate shouldBe LocalDate.of(2026, 10, 30)
        result.plannedIncome shouldBe BigDecimal("2500.10")
        result.plannedExpenses shouldBe BigDecimal("1800.25")
        result.endingBalance shouldBe BigDecimal("5699.85")
        result.days[4].closingBalance shouldBe BigDecimal("5699.85")
    }

    @Test
    fun `deducts opening reservations once and ignores entries outside the window`() {
        val result = calculateCashflowForecast(
            CashflowForecastRequest(
                openingDate = LocalDate.of(2026, 10, 1),
                openingBalance = BigDecimal("1000.00"),
                reservations = listOf(ForecastReservation(BigDecimal("200.00"))),
                plannedEntries = listOf(
                    PlannedCashflowEntry(
                        id = "inside",
                        date = LocalDate.of(2026, 10, 30),
                        type = PlannedCashflowType.EXPENSE,
                        amount = BigDecimal("100.00"),
                    ),
                    PlannedCashflowEntry(
                        id = "outside",
                        date = LocalDate.of(2026, 10, 31),
                        type = PlannedCashflowType.EXPENSE,
                        amount = BigDecimal("900.00"),
                    ),
                ),
            ),
        )

        result.reservationsDeductedAtOpening shouldBe BigDecimal("200.00")
        result.openingBalanceAfterReservations shouldBe BigDecimal("800.00")
        result.endingBalance shouldBe BigDecimal("700.00")
        result.days.first().closingBalance shouldBe BigDecimal("800.00")
        result.days.last().closingBalance shouldBe BigDecimal("700.00")
    }

    @Test
    fun `does not represent ledger writes or historical entries in the contract`() {
        val result = forecastMoneyComingUp(
            CashflowForecastRequest(
                openingDate = LocalDate.of(2026, 10, 14),
                openingBalance = BigDecimal("100.00"),
                plannedEntries = listOf(
                    PlannedCashflowEntry(
                        id = "historical",
                        date = LocalDate.of(2026, 10, 13),
                        type = PlannedCashflowType.INCOME,
                        amount = BigDecimal("999.00"),
                    ),
                ),
            ),
        )

        result.plannedIncome shouldBe BigDecimal.ZERO
        result.endingBalance shouldBe BigDecimal("100.00")
    }
}
