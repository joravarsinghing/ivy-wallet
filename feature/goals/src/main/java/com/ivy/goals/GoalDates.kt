package com.ivy.goals

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Compatibility for older presentation callers; stored GoalEntity.deadline is still an Instant. */
internal fun Goal.resolvedDeadline(): LocalDate? {
    deadline?.let { return it }
    val label = deadlineLabel?.trim().orEmpty()
    if (label.isBlank() || label.equals("No deadline", ignoreCase = true)) return null
    return runCatching { LocalDate.parse(label) }.getOrNull()
        ?: runCatching { YearMonth.parse(label, DateTimeFormatter.ofPattern("MMMM uuuu", Locale.US)).atDay(1) }.getOrNull()
        ?: throw IllegalArgumentException("Choose a valid deadline from the calendar, or clear it.")
}

internal fun Goal.deadlineInstant() = resolvedDeadline()?.atStartOfDay()?.toInstant(ZoneOffset.UTC)

internal fun Goal.displayDeadline(locale: Locale = Locale.getDefault()): String? =
    resolvedDeadline()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

internal fun goalInputError(name: String, target: String): String? = when {
    name.isBlank() -> "Enter a name for your goal."
    target.toBigDecimalOrNull()?.signum() != 1 || target.toDoubleOrNull()?.let { it.isFinite() && it > 0.0 } != true -> "Enter a target amount greater than zero."
    else -> null
}
