package com.ivy.planned.list

import com.ivy.data.model.IntervalType
import java.time.LocalDateTime

/** Returns the first occurrence on or after [now], without mutating the stored rule. */
fun nextPlannedPaymentDate(
    startDate: LocalDateTime?,
    oneTime: Boolean,
    intervalN: Int?,
    intervalType: IntervalType?,
    now: LocalDateTime,
): LocalDateTime? {
    val firstOccurrence = startDate ?: return null
    if (oneTime || intervalType == null || intervalN == null || intervalN <= 0) return firstOccurrence

    var occurrence = firstOccurrence
    var guard = 0
    while (occurrence.isBefore(now) && guard++ < 1_000) {
        occurrence = when (intervalType) {
            IntervalType.DAY -> occurrence.plusDays(intervalN.toLong())
            IntervalType.WEEK -> occurrence.plusWeeks(intervalN.toLong())
            IntervalType.MONTH -> occurrence.plusMonths(intervalN.toLong())
            IntervalType.YEAR -> occurrence.plusYears(intervalN.toLong())
        }
    }
    return occurrence
}
