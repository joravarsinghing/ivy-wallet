package com.ivy.ui.time.impl

import androidx.compose.runtime.*
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.ui.time.CalendarDateBounds
import com.ivy.ui.time.JvyDatePickerDialog
import com.ivy.ui.time.JvyTimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Stable
@Singleton
class AndroidDateTimePicker @Inject constructor(
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : DateTimePicker {
    internal var pending: PickerRequest? by mutableStateOf(null)
        private set
    private var nextId = 0L

    @Composable
    override fun Content() {
        when (val request = pending) {
            is PickerRequest.Date -> key(request.id) {
                JvyDatePickerDialog(request.initial, onDismiss = { dismiss(request.id) },
                    onDatePicked = { confirmDate(request.id, it) }, bounds = request.bounds, today = request.today)
            }
            is PickerRequest.Time -> key(request.id) {
                JvyTimePickerDialog(request.initial, onDismiss = { dismiss(request.id) },
                    onTimePicked = { confirmTime(request.id, it) })
            }
            null -> Unit
        }
    }

    override fun pickDate(initialDate: Instant?, onDatePick: (LocalDate) -> Unit) {
        // Transaction timestamps keep the device-local day; calendar-only values skip this conversion.
        pickCalendarDate(initialDate?.let { with(timeConverter) { it.toLocalDate() } }, onDatePick = onDatePick)
    }

    override fun pickCalendarDate(initialDate: LocalDate?, minDate: LocalDate?, maxDate: LocalDate?, onDatePick: (LocalDate) -> Unit) {
        val bounds = CalendarDateBounds(minDate, maxDate)
        pending = PickerRequest.Date(++nextId, initialDate, timeProvider.localDateNow(), bounds, onDatePick)
    }

    override fun pickTime(initialTime: LocalTime?, onTimePick: (LocalTime) -> Unit) {
        pending = PickerRequest.Time(++nextId, initialTime ?: timeProvider.localTimeNow(), onTimePick)
    }

    internal fun dismiss(id: Long) {
        if (pending?.id == id) pending = null
    }

    internal fun confirmDate(id: Long, date: LocalDate) {
        val request = pending as? PickerRequest.Date ?: return
        if (request.id != id || date !in request.bounds) return
        pending = null // Clear first: a consumer may open the next picker from its callback.
        request.picked(date)
    }

    internal fun confirmTime(id: Long, time: LocalTime) {
        val request = pending as? PickerRequest.Time ?: return
        if (request.id != id) return
        pending = null
        request.picked(time.withSecond(0).withNano(0))
    }
}
