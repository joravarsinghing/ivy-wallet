package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ivy.data.model.IntervalType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.system.JvyButton
import com.ivy.design.system.JvyFormDialogFrame
import com.ivy.design.system.JvyTypography
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.LastNTimeRange
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.ui.R
import com.ivy.ui.time.JvyDateField
import com.ivy.wallet.ui.theme.components.IntervalPickerRow
import java.util.UUID

data class ChoosePeriodModalData(val id: UUID = UUID.randomUUID(), val period: TimePeriod)

/** A real window keeps confirmation above root navigation/FABs and nested calendars above this chooser. */
@Composable
fun BoxWithConstraintsScope.ChoosePeriodModal(
    modal: ChoosePeriodModalData?,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    updateGlobalPeriod: Boolean = true,
    onPeriodSelected: (TimePeriod) -> Unit,
) {
    if (modal == null) return
    val context = ivyWalletCtx()
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        key(modal.id) {
            PeriodChooserContent(modal.period, dismiss, onApply = { period ->
                if (updateGlobalPeriod) context.updateSelectedPeriodInMemory(period)
                onPeriodSelected(period)
                dismiss()
            }, modifier = modifier)
        }
    }
}

/** The actual dialog content is also used by adaptive interaction/render checks. */
@Composable
fun PeriodChooserContent(initialPeriod: TimePeriod, onClose: () -> Unit, onApply: (TimePeriod) -> Unit,
                         modifier: Modifier = Modifier) {
    var period by rememberSaveable(stateSaver = PeriodDraftSaver) { mutableStateOf<TimePeriod?>(initialPeriod) }
    val provider = LocalTimeProvider.current
    val converter = LocalTimeConverter.current
    val today = provider.localDateNow()
    val months = Month.monthsList()
    val years = (listOf(today.year - 1, today.year, today.year + 1) + listOfNotNull(initialPeriod.year)).distinct().sorted()
    val choices = years.flatMap { year -> months.map { MonthYear(it, year) } }
    val initialMonth = initialPeriod.month?.monthValue ?: today.monthValue
    val initialYear = initialPeriod.year ?: today.year
    val initialIndex = choices.indexOfFirst { it.month.monthValue == initialMonth && it.year == initialYear }.coerceAtLeast(0)
    val monthScroll = rememberLazyListState(initialFirstVisibleItemIndex = (initialIndex - 1).coerceAtLeast(0))
    val range = period?.fromToRange
    val from = with(converter) { range?.from?.toLocalDate() }
    val to = with(converter) { range?.to?.toLocalDate() }
    val allTime = range != null && range.from == null && (range.to == null || range.to!! > provider.utcNow())

    JvyFormDialogFrame(stringResource(R.string.choose_month), onClose, footer = {
        JvyButton(onClick = {
            period?.takeIf(::canApplyPeriod)?.let { selected ->
                // Saved primitives intentionally omit translated month names.
                onApply(selected.copy(month = selected.month?.let { months.first { month -> month.monthValue == it.monthValue } }))
            }
        }, modifier = Modifier.fillMaxWidth().testTag("period_apply"), enabled = canApplyPeriod(period)) {
            Text(stringResource(R.string.apply_filter))
        }
    }, modifier = modifier, bodyPadding = PaddingValues(vertical = 8.dp)) {
        Text(stringResource(R.string.jvy_period_apply_help), Modifier.padding(horizontal = 24.dp),
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(state = monthScroll, contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(choices, key = { "${it.year}-${it.month.monthValue}" }) { choice ->
                FilterChip(selected = period?.month?.monthValue == choice.month.monthValue &&
                    (period?.year ?: today.year) == choice.year,
                    onClick = { period = TimePeriod(month = choice.month, year = choice.year) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("period_month_${choice.year}_${choice.month.monthValue}"),
                    label = { Text(choice.forDisplay(today.year)) })
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 24.dp))
        Text(stringResource(R.string.or_custom_range), Modifier.padding(horizontal = 24.dp), style = JvyTypography.SectionTitle)
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JvyDateField(stringResource(R.string.from), from, change = { value ->
                period = TimePeriod(fromToRange = rangeWithCalendarDate(range, value, true, converter))
            }, optional = true, latestDate = to, initialDate = today,
                emptyLabel = stringResource(R.string.add_date), clearLabel = stringResource(R.string.jvy_clear_date))
            JvyDateField(stringResource(R.string.to), to, change = { value ->
                period = TimePeriod(fromToRange = rangeWithCalendarDate(range, value, false, converter))
            }, optional = true, earliestDate = from, initialDate = today,
                emptyLabel = stringResource(R.string.add_date), clearLabel = stringResource(R.string.jvy_clear_date))
        }
        FilterChip(selected = period?.lastNRange != null, onClick = {
            period = TimePeriod(lastNRange = period?.lastNRange ?: LastNTimeRange(1, IntervalType.WEEK))
        }, modifier = Modifier.padding(horizontal = 24.dp).heightIn(min = 48.dp).testTag("period_relative_choice"),
            label = { Text(stringResource(R.string.or_in_the_last), style = JvyTypography.SectionTitle) })
        IntervalPickerRow(intervalN = period?.lastNRange?.periodN ?: 1,
            intervalType = period?.lastNRange?.periodType ?: IntervalType.WEEK,
            onSetIntervalN = { count -> period = TimePeriod(lastNRange = LastNTimeRange(count, period?.lastNRange?.periodType ?: IntervalType.WEEK)) },
            onSetIntervalType = { type -> period = TimePeriod(lastNRange = LastNTimeRange(period?.lastNRange?.periodN ?: 1, type)) })
        Text(stringResource(R.string.or_all_time), Modifier.padding(horizontal = 24.dp), style = JvyTypography.SectionTitle)
        FilterChip(selected = allTime, onClick = { period = TimePeriod(fromToRange = FromToTimeRange(null, null)) },
            modifier = Modifier.padding(horizontal = 24.dp).heightIn(min = 48.dp).testTag("period_all_time"),
            label = { Text(stringResource(R.string.select_all_time)) })
    }
}

data class MonthYear(val month: Month, val year: Int) {
    fun forDisplay(currentYear: Int): String = if (year == currentYear) month.name else "${month.name}, $year"
}
