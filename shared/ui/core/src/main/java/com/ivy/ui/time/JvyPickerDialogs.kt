package com.ivy.ui.time

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ivy.design.system.JvyButton
import com.ivy.design.system.JvyFormDialogFrame
import com.ivy.design.system.JvyTextButton
import com.ivy.ui.R
import java.time.LocalDate
import java.time.LocalTime

/** One dialog host for fields, the injected picker API and the legacy bridge. */
@Composable
private fun PickerDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { keyboard?.hide() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberJvyCalendarState(initialDate: LocalDate?, bounds: CalendarDateBounds, today: LocalDate): DatePickerState {
    val candidate = initialDate ?: today
    val displayed = bounds.displayDate(candidate)
    return rememberDatePickerState(
        initialSelectedDateMillis = candidate.takeIf { it in bounds }?.let(::calendarDateMillis),
        initialDisplayedMonthMillis = calendarDateMillis(displayed),
        yearRange = bounds.years(displayed),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = calendarDateFromMillis(utcTimeMillis) in bounds
            override fun isSelectableYear(year: Int) = year in bounds.years(displayed)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JvyDatePickerDialog(initialDate: LocalDate?, onDismiss: () -> Unit, onDatePicked: (LocalDate) -> Unit,
                        label: String = stringResource(R.string.jvy_picker_date_title),
                        bounds: CalendarDateBounds = CalendarDateBounds(), today: LocalDate = LocalDate.now()) {
    val state = rememberJvyCalendarState(initialDate, bounds, today)
    PickerDialog(onDismiss) {
        JvyCalendarPickerContent(label, state, onDismiss) {
            state.selectedDateMillis?.let(::calendarDateFromMillis)?.takeIf { it in bounds }?.let(onDatePicked)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JvyTimePickerDialog(initialTime: LocalTime, onDismiss: () -> Unit, onTimePicked: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialTime.hour, initialTime.minute, DateFormat.is24HourFormat(LocalContext.current))
    PickerDialog(onDismiss) {
        JvyTimePickerContent(state, onDismiss) { onTimePicked(LocalTime.of(state.hour, state.minute)) }
    }
}

/** Native clock/keyboard editing inside the same bounded frame as calendar selection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JvyTimePickerContent(state: TimePickerState, onClose: () -> Unit, onConfirm: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    var keyboardMode by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val clockFits = maxWidth >= 360.dp && maxHeight >= 640.dp && fontScale <= 1.3f
        JvyFormDialogFrame(stringResource(R.string.jvy_picker_time_title), onClose, footer = {
            JvyButton(onConfirm, Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
        }, bodyPadding = PaddingValues(vertical = 8.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (clockFits && !keyboardMode) TimePicker(state = state, layoutType = TimePickerLayoutType.Vertical)
                else TimeInput(state = state)
                if (clockFits) JvyTextButton({ keyboardMode = !keyboardMode }) {
                    Text(stringResource(if (keyboardMode) R.string.jvy_picker_use_clock else R.string.jvy_picker_type_time))
                }
            }
        }
    }
}
