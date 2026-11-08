package com.ivy.exchangerates

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

fun rateFreshnessLabel(raw: String?, today: LocalDate = LocalDate.now(ZoneOffset.UTC)): String {
    val date = raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return "Provider date unknown. Refresh to verify cached rates."
    if (date > today) return "Provider date needs review. Refresh before relying on these rates."
    val age = ChronoUnit.DAYS.between(date, today)
    val status = when { age == 0L -> "Today's reference rates"; age == 1L -> "Yesterday's reference rates"; else -> "$age days old${if (age > 3) " · may be stale" else ""}" }
    return "Provider date: ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} · $status"
}
