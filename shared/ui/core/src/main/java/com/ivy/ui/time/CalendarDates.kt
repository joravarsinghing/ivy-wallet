package com.ivy.ui.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material date pickers use UTC calendar dates, not midnight in the device time zone. */
fun calendarDateMillis(date: LocalDate): Long = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

fun calendarDateFromMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
