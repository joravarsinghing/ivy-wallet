package com.ivy.ui.time

import java.time.LocalDate

/** Inclusive local calendar limits; never compare them as device-zone instants. */
data class CalendarDateBounds(val earliest: LocalDate? = null, val latest: LocalDate? = null) {
    init {
        require(earliest == null || latest == null || earliest <= latest) { "The earliest date must not follow the latest date." }
    }

    operator fun contains(date: LocalDate): Boolean =
        (earliest == null || date >= earliest) && (latest == null || date <= latest)

    fun displayDate(candidate: LocalDate): LocalDate = when {
        earliest != null && candidate < earliest -> earliest
        latest != null && candidate > latest -> latest
        else -> candidate
    }

    fun years(displayed: LocalDate): IntRange =
        (earliest?.year ?: minOf(1900, displayed.year))..(latest?.year ?: maxOf(2100, displayed.year))
}
