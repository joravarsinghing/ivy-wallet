package com.ivy.home

import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.repository.GoalRepository
import com.ivy.domain.usecase.cashflow.CashflowForecastRequest
import com.ivy.domain.usecase.cashflow.ForecastReservation
import com.ivy.domain.usecase.cashflow.PlannedCashflowEntry
import com.ivy.domain.usecase.cashflow.PlannedCashflowType
import com.ivy.domain.usecase.cashflow.calculateCashflowForecast
import com.ivy.legacy.incrementDate
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.wallet.domain.deprecated.logic.PlannedPaymentsLogic
import com.ivy.wallet.domain.deprecated.logic.currency.ExchangeRatesLogic
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Read-only Home adapter for the Money Coming Up forecast.
 *
 * It deliberately reads existing rules and goal allocations only. It never generates,
 * settles, deletes, or saves transactions.
 */
class MoneyComingUpAdapter @Inject constructor(
    private val plannedPaymentsLogic: PlannedPaymentsLogic,
    private val goalRepository: GoalRepository,
    private val exchangeRatesLogic: ExchangeRatesLogic,
    private val timeProvider: TimeProvider,
) {
    suspend fun load(
        openingBalance: BigDecimal,
        baseCurrency: String,
        accounts: List<Account>,
    ): MoneyComingUpState {
        return try {
            val openingDate = timeProvider.localDateNow()
            val rules = plannedPaymentsLogic.oneTime() + plannedPaymentsLogic.recurring()
            val entries = rules
                .mapNotNull { rule ->
                    val amount = exchangeRatesLogic.amountBaseCurrency(
                        plannedPayment = rule,
                        baseCurrency = baseCurrency,
                        accounts = accounts,
                    )
                    if (amount.isFinite() && amount > 0.0) {
                        rule to BigDecimal.valueOf(amount)
                    } else {
                        null
                    }
                }
                .let { rulesWithAmounts ->
                    plannedCashflowEntries(
                        rulesWithAmounts = rulesWithAmounts,
                        openingDate = openingDate,
                        zoneId = timeProvider.getZoneId(),
                    )
                }

            val reservedAmount = goalRepository.findActiveGoals()
                .flatMap { goalRepository.findAllocations(it.id) }
                .fold(BigDecimal.ZERO) { total, allocation ->
                    total.add(BigDecimal.valueOf(allocation.reservedAmount))
                }

            summarizeMoneyComingUp(
                openingDate = openingDate,
                openingBalance = openingBalance,
                reservations = reservedAmount,
                entries = entries,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            MoneyComingUpState(
                status = MoneyComingUpStatus.ERROR,
                errorMessage = error.message ?: "The forecast could not be loaded.",
            )
        }
    }
}

internal fun plannedCashflowEntries(
    rulesWithAmounts: List<Pair<PlannedPaymentRule, BigDecimal>>,
    openingDate: LocalDate,
    zoneId: ZoneId,
): List<PlannedCashflowEntry> {
    val closingDateExclusive = openingDate.plusDays(30)

    return rulesWithAmounts.flatMap { (rule, amount) ->
        occurrenceDates(
            rule = rule,
            openingDate = openingDate,
            closingDateExclusive = closingDateExclusive,
            zoneId = zoneId,
        ).map { date ->
            PlannedCashflowEntry(
                id = "${rule.id}@$date",
                date = date,
                type = rule.type.toPlannedCashflowType() ?: return@map null,
                amount = amount,
            )
        }.filterNotNull()
    }.sortedWith(compareBy<PlannedCashflowEntry>({ it.date }, { it.id }))
}

private fun occurrenceDates(
    rule: PlannedPaymentRule,
    openingDate: LocalDate,
    closingDateExclusive: LocalDate,
    zoneId: ZoneId,
): List<LocalDate> {
    val start = rule.startDate ?: return emptyList()
    val startDate = start.atZone(zoneId).toLocalDate()

    if (rule.oneTime) {
        return if (!startDate.isBefore(openingDate) && startDate.isBefore(closingDateExclusive)) {
            listOf(startDate)
        } else {
            emptyList()
        }
    }

    val intervalN = rule.intervalN ?: return emptyList()
    val intervalType = rule.intervalType ?: return emptyList()
    if (intervalN <= 0) return emptyList()

    val dates = mutableListOf<LocalDate>()
    var occurrence = start
    var date = occurrence.atZone(zoneId).toLocalDate()
    var guard = 0

    while (date.isBefore(openingDate)) {
        val next = intervalType.incrementDate(occurrence, intervalN.toLong())
        if (!next.isAfter(occurrence)) return emptyList()
        occurrence = next
        date = occurrence.atZone(zoneId).toLocalDate()
        if (++guard > MAX_OCCURRENCE_ADVANCE) return emptyList()
    }

    while (date.isBefore(closingDateExclusive)) {
        dates += date
        val next = intervalType.incrementDate(occurrence, intervalN.toLong())
        if (!next.isAfter(occurrence)) break
        occurrence = next
        date = occurrence.atZone(zoneId).toLocalDate()
        if (++guard > MAX_OCCURRENCE_ADVANCE) break
    }

    return dates
}

internal fun summarizeMoneyComingUp(
    openingDate: LocalDate,
    openingBalance: BigDecimal,
    reservations: BigDecimal,
    entries: List<PlannedCashflowEntry>,
): MoneyComingUpState {
    val forecast = calculateCashflowForecast(
        CashflowForecastRequest(
            openingDate = openingDate,
            openingBalance = openingBalance,
            reservations = listOf(ForecastReservation(reservations)),
            plannedEntries = entries,
        )
    )
    val lowestProjectedAvailable = minOf(
        forecast.openingBalanceAfterReservations,
        forecast.days.minOf { it.closingBalance },
    )
    val shortfall = lowestProjectedAvailable.negate().max(BigDecimal.ZERO)

    return MoneyComingUpState(
        status = if (entries.isEmpty()) MoneyComingUpStatus.EMPTY else MoneyComingUpStatus.CONTENT,
        currentAvailable = forecast.openingBalanceAfterReservations,
        upcomingIncome = forecast.plannedIncome,
        upcomingExpenses = forecast.plannedExpenses,
        lowestProjectedAvailable = lowestProjectedAvailable,
        shortfall = shortfall,
        safeToSpend = lowestProjectedAvailable.max(BigDecimal.ZERO),
        events = entries.map { entry ->
            MoneyComingUpEvent(
                date = entry.date,
                type = entry.type,
                amount = entry.amount,
            )
        }.toImmutableList(),
    )
}

private fun TransactionType.toPlannedCashflowType(): PlannedCashflowType? = when (this) {
    TransactionType.INCOME -> PlannedCashflowType.INCOME
    TransactionType.EXPENSE -> PlannedCashflowType.EXPENSE
    TransactionType.TRANSFER -> null
}

private const val MAX_OCCURRENCE_ADVANCE = 100_000
