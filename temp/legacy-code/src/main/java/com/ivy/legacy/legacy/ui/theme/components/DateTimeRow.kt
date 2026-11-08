package com.ivy.legacy.legacy.ui.theme.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import com.ivy.design.system.JvyOutlinedButton
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.legacy.utils.convertLocalToUTC
import com.ivy.legacy.utils.convertUTCToLocal
import com.ivy.legacy.utils.formatNicely
import com.ivy.legacy.utils.timeNowUTC
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateTimeRow(
    dateTime: LocalDateTime,
    onEditDate: () -> Unit,
    onEditTime: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val timeFormatter = LocalTimeFormatter.current

    FlowRow(modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JvyOutlinedButton(onEditDate, enabled = enabled) { Text(dateTime.formatNicely()) }
        JvyOutlinedButton(onEditTime, enabled = enabled) { Text(with(timeFormatter) { dateTime.toLocalTime().format() }) }
    }
}

// The timepicker returns time in UTC, but the date picker returns date in LocalTimeZone
// hence use this method to get both date & time in UTC
@Deprecated("Rework this to use the TimeConverter API")
fun getTrueDate(
    date: LocalDate,
    time: LocalTime,
    convert: Boolean = true
): LocalDateTime {
    val timeLocal = if (convert) time.convertUTCToLocal() else time

    return timeNowUTC()
        .withYear(date.year)
        .withMonth(date.monthValue)
        .withDayOfMonth(date.dayOfMonth)
        .withHour(timeLocal.hour)
        .withMinute(timeLocal.minute)
        .withSecond(0)
        .withNano(0)
        .convertLocalToUTC()
}
