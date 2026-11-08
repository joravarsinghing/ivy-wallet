package com.ivy.ui.time.impl

import com.ivy.ui.time.CalendarDateBounds
import java.time.LocalDate
import java.time.LocalTime

/** Callbacks are in-memory only; saved picker UI state never serializes consumers. */
internal sealed interface PickerRequest {
    val id: Long

    data class Date(override val id: Long, val initial: LocalDate?, val today: LocalDate,
                    val bounds: CalendarDateBounds, val picked: (LocalDate) -> Unit) : PickerRequest
    data class Time(override val id: Long, val initial: LocalTime, val picked: (LocalTime) -> Unit) : PickerRequest
}
