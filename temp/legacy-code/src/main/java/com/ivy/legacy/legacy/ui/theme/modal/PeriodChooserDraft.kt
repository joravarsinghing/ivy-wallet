package com.ivy.wallet.ui.theme.modal

import androidx.compose.runtime.saveable.Saver
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.IntervalType
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.LastNTimeRange
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.data.model.TimePeriodSavedState
import java.time.LocalDate
import java.util.UUID

/** UI draft primitives only; invalid relative counts remain editable after recreation. */
internal fun encodePeriodDraft(period: TimePeriod?): List<String> = when {
    period == null -> listOf("none")
    period.lastNRange != null -> listOf("relative-draft", period.lastNRange.periodN.toString(), period.lastNRange.periodType.name)
    else -> TimePeriodSavedState.encode(period) ?: listOf("none")
}

internal fun decodePeriodDraft(values: List<String>): TimePeriod? = runCatching {
    if (values.firstOrNull() == "relative-draft") {
        require(values.size == 3)
        TimePeriod(lastNRange = LastNTimeRange(values[1].toInt().also { require(it in 0..99999) }, IntervalType.valueOf(values[2])))
    } else TimePeriodSavedState.decode(values)
}.getOrNull()

val PeriodDraftSaver = Saver<TimePeriod?, List<String>>(save = { encodePeriodDraft(it) }, restore = ::decodePeriodDraft)
val ChoosePeriodModalDataSaver = Saver<ChoosePeriodModalData?, List<String>>(
    save = { data -> data?.let { listOf(it.id.toString()) + encodePeriodDraft(it.period) } ?: emptyList() },
    restore = { data -> runCatching {
        val period = decodePeriodDraft(data.drop(1)) ?: return@runCatching null
        ChoosePeriodModalData(UUID.fromString(data.first()), period)
    }.getOrNull() },
)

fun canApplyPeriod(period: TimePeriod?): Boolean = when {
    period == null -> false
    period.month != null -> period.month.monthValue in 1..12 && (period.year == null || period.year in 1..9999)
    period.fromToRange != null -> period.fromToRange.let { it.from == null || it.to == null || it.from <= it.to }
    else -> (period.lastNRange?.periodN ?: 0) > 0
}

/** Inclusive local days, including one-day ranges and DST transitions; never next midnight. */
fun rangeWithCalendarDate(range: FromToTimeRange?, date: LocalDate?, from: Boolean, converter: TimeConverter): FromToTimeRange? = with(converter) {
    val start = if (from) date?.atStartOfDay()?.toUTC() else range?.from
    val end = if (!from) date?.plusDays(1)?.atStartOfDay()?.toUTC()?.minusNanos(1) else range?.to
    if (start == null && end == null) null else FromToTimeRange(start, end)
}
