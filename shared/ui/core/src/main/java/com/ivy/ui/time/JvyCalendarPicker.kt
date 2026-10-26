package com.ivy.ui.time

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ivy.design.system.*
import com.ivy.ui.R
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Seven proportional columns: Material 1.2's fixed day grid clips below 360dp. */
internal fun calendarMonthRows(month: YearMonth, firstDay: DayOfWeek): List<List<LocalDate?>> {
    val offset = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val cells = (0 until ((offset + month.lengthOfMonth() + 6) / 7) * 7).map { index ->
        (index - offset + 1).takeIf { it in 1..month.lengthOfMonth() }?.let(month::atDay)
    }
    return cells.chunked(7)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JvyCalendarPickerContent(label: String, state: DatePickerState, onClose: () -> Unit, onConfirm: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val month = YearMonth.from(calendarDateFromMillis(state.displayedMonthMillis))
    val chosen = state.selectedDateMillis?.let(::calendarDateFromMillis)
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val rows = remember(month, firstDay) { calendarMonthRows(month, firstDay) }
    var chooseYear by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val yearItemHeight = maxOf(56.dp, (24f * density.fontScale + 16f).dp)
    val yearOffset = with(density) { yearItemHeight.roundToPx() } * (month.year - state.yearRange.first)
    val yearScroll = rememberScrollState(initial = yearOffset)
    LaunchedEffect(chooseYear) { if (chooseYear) yearScroll.scrollTo(yearOffset) }
    val colors = MaterialTheme.colorScheme
    val numbers = remember(locale) { NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false } }
    JvyFormDialogFrame(label, onClose, footer = {
        JvyButton(onConfirm, Modifier.fillMaxWidth(), enabled = chosen != null &&
            chosen.year in state.yearRange && state.selectableDates.isSelectableYear(chosen.year) &&
            state.selectableDates.isSelectableDate(calendarDateMillis(chosen))) { Text(stringResource(R.string.done)) }
    }, bodyPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
        Text(chosen?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) ?: stringResource(R.string.jvy_picker_date_title),
            Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ state.displayedMonthMillis = calendarDateMillis(month.minusMonths(1).atDay(1)) },
                enabled = month.minusMonths(1).year in state.yearRange) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.jvy_picker_previous_month))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                JvyTextButton({ chooseYear = true }) {
                    Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)))
                }
                DropdownMenu(chooseYear, { chooseYear = false }, Modifier.heightIn(max = 240.dp), scrollState = yearScroll) {
                    state.yearRange.forEach { year ->
                        DropdownMenuItem(text = { Text(numbers.format(year)) }, modifier = Modifier.height(yearItemHeight),
                            enabled = state.selectableDates.isSelectableYear(year), onClick = {
                                state.displayedMonthMillis = calendarDateMillis(month.withYear(year).atDay(1))
                                chooseYear = false
                            })
                    }
                }
            }
            IconButton({ state.displayedMonthMillis = calendarDateMillis(month.plusMonths(1).atDay(1)) },
                enabled = month.plusMonths(1).year in state.yearRange) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, stringResource(R.string.jvy_picker_next_month))
            }
        }
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                repeat(7) { index ->
                    val day = firstDay.plus(index.toLong())
                    Box(Modifier.weight(1f).heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                        Text(day.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                            Modifier.semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) },
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            rows.forEach { week ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    week.forEach { day ->
                        if (day == null) Spacer(Modifier.weight(1f).height(48.dp)) else {
                            val isChosen = chosen == day
                            val enabled = state.selectableDates.isSelectableYear(day.year) &&
                                state.selectableDates.isSelectableDate(calendarDateMillis(day))
                            Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape)
                                .then(if (isChosen) Modifier.background(colors.primary) else Modifier)
                                .semantics(mergeDescendants = true) {
                                    selected = isChosen
                                    contentDescription = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
                                }.clickable(enabled = enabled, role = Role.Button) { state.selectedDateMillis = calendarDateMillis(day) }
                                .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(numbers.format(day.dayOfMonth), style = MaterialTheme.typography.bodyLarge,
                                    color = if (isChosen) colors.onPrimary else if (enabled) colors.onSurface else colors.onSurface.copy(alpha = .38f))
                            }
                        }
                    }
                }
            }
        }
    }
}
