package com.ivy.domain.usecase.cashflow

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/** The only entry kinds that participate in a Money Coming Up forecast. */
enum class PlannedCashflowType {
    INCOME,
    EXPENSE,
}

/** A future, planned cashflow entry. Ledger transactions are intentionally not accepted here. */
data class PlannedCashflowEntry(
    val id: String,
    val date: LocalDate,
    val type: PlannedCashflowType,
    val amount: BigDecimal,
)

/** Money already reserved at forecast opening. It is deducted once, before daily movements. */
data class ForecastReservation(
    val amount: BigDecimal,
)

data class CashflowForecastRequest(
    val openingDate: LocalDate,
    val openingBalance: BigDecimal,
    val reservations: List<ForecastReservation> = emptyList(),
    val plannedEntries: List<PlannedCashflowEntry> = emptyList(),
)

data class CashflowForecastDay(
    val date: LocalDate,
    val plannedIncome: BigDecimal,
    val plannedExpenses: BigDecimal,
    val netChange: BigDecimal,
    val closingBalance: BigDecimal,
)

/** A read-only projection. Building it never writes to the ledger or any other store. */
data class CashflowForecast(
    val openingDate: LocalDate,
    val closingDate: LocalDate,
    val openingBalance: BigDecimal,
    val reservationsDeductedAtOpening: BigDecimal,
    val openingBalanceAfterReservations: BigDecimal,
    val plannedIncome: BigDecimal,
    val plannedExpenses: BigDecimal,
    val endingBalance: BigDecimal,
    val days: List<CashflowForecastDay>,
)

typealias MoneyComingUpForecastRequest = CashflowForecastRequest
typealias MoneyComingUpForecast = CashflowForecast

const val CASHFLOW_FORECAST_DAYS: Int = 30

/**
 * Builds the fixed 30-day forecast window [openingDate, openingDate + 29 days].
 *
 * Only explicitly supplied planned income and planned expenses are included. The
 * opening reservation total is subtracted once; it is not subtracted again from
 * each daily balance.
 */
fun calculateCashflowForecast(request: CashflowForecastRequest): CashflowForecast {
    require(request.plannedEntries.map { it.id }.toSet().size == request.plannedEntries.size) {
        "Planned cashflow entry IDs must be unique"
    }
    request.plannedEntries.forEach(::requireValidPlannedEntry)
    request.reservations.forEach(::requireValidReservation)

    val windowEndExclusive = request.openingDate.plusDays(CASHFLOW_FORECAST_DAYS.toLong())
    val entriesByDate = request.plannedEntries
        .filter { entry ->
            !entry.date.isBefore(request.openingDate) && entry.date.isBefore(windowEndExclusive)
        }
        .sortedWith(compareBy<PlannedCashflowEntry>({ it.date }, { it.id }))
        .groupBy { it.date }

    val reservations = request.reservations.fold(BigDecimal.ZERO, BigDecimal::add)
    var closingBalance = request.openingBalance.subtract(reservations)
    val days = (0 until CASHFLOW_FORECAST_DAYS).map { offset ->
        val date = request.openingDate.plusDays(offset.toLong())
        val entries = entriesByDate[date].orEmpty()
        val income = entries
            .filter { it.type == PlannedCashflowType.INCOME }
            .fold(BigDecimal.ZERO) { total, entry -> total.add(entry.amount) }
        val expenses = entries
            .filter { it.type == PlannedCashflowType.EXPENSE }
            .fold(BigDecimal.ZERO) { total, entry -> total.add(entry.amount) }
        val netChange = income.subtract(expenses)
        closingBalance = closingBalance.add(netChange)

        CashflowForecastDay(
            date = date,
            plannedIncome = income,
            plannedExpenses = expenses,
            netChange = netChange,
            closingBalance = closingBalance,
        )
    }

    return CashflowForecast(
        openingDate = request.openingDate,
        closingDate = request.openingDate.plusDays((CASHFLOW_FORECAST_DAYS - 1).toLong()),
        openingBalance = request.openingBalance,
        reservationsDeductedAtOpening = reservations,
        openingBalanceAfterReservations = request.openingBalance.subtract(reservations),
        plannedIncome = days.fold(BigDecimal.ZERO) { total, day -> total.add(day.plannedIncome) },
        plannedExpenses = days.fold(BigDecimal.ZERO) { total, day -> total.add(day.plannedExpenses) },
        endingBalance = closingBalance,
        days = days,
    )
}

fun forecastMoneyComingUp(request: MoneyComingUpForecastRequest): MoneyComingUpForecast =
    calculateCashflowForecast(request)

private fun requireValidPlannedEntry(entry: PlannedCashflowEntry) {
    require(entry.id.isNotBlank()) { "Planned cashflow entry ID is required" }
    require(entry.amount.signum() > 0) { "Planned cashflow amount must be positive" }
}

private fun requireValidReservation(reservation: ForecastReservation) {
    require(reservation.amount.signum() >= 0) { "Forecast reservations cannot be negative" }
}

/** Boundary helper for callers that still hold a legacy Double amount. */
fun legacyForecastAmount(amount: Double): BigDecimal =
    BigDecimal.valueOf(amount).round(MathContext.DECIMAL128)
