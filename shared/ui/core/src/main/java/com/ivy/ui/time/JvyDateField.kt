package com.ivy.ui.time

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.ivy.design.system.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** A calendar selection, never a free-text date masquerading as a valid value. */
@Composable
fun JvyDateField(label: String, value: LocalDate?, change: (LocalDate?) -> Unit,
                 modifier: Modifier = Modifier.fillMaxWidth(), optional: Boolean = false,
                 latestDate: LocalDate? = null, initialDate: LocalDate = LocalDate.now()) {
    var open by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        JvyOutlinedButton({ open = true }, Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.DateRange, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(value?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                ?: if (optional) "No deadline · choose date" else "Choose date")
        }
        if (optional && value != null) JvyTextButton({ change(null) }) { Text("Clear deadline") }
    }
    if (open) {
        JvyDatePickerDialog(value, onDismiss = { open = false }, onDatePicked = {
            change(it)
            open = false
        }, label = label, bounds = CalendarDateBounds(latest = latestDate), today = initialDate)
    }
}
