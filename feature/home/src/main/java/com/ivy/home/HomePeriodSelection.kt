package com.ivy.home

import com.ivy.data.model.IntervalType
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.LastNTimeRange
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.base.time.TimeConverter
import java.time.LocalDate
import java.time.YearMonth

/** Home's preference is a mode, not a chart zoom or a cached resolved date. */
sealed interface HomePeriodSelection {
    data object CurrentCycle : HomePeriodSelection
    data class BudgetMonth(val month: YearMonth) : HomePeriodSelection
    data class Rolling(val count: Int, val unit: IntervalType) : HomePeriodSelection {
        init { require(count in 1..10000) }
    }
    data class Custom(val from: LocalDate?, val through: LocalDate?) : HomePeriodSelection {
        init { require(from == null || through == null || from <= through) }
    }
    data object AllTime : HomePeriodSelection
}

data class HomePeriodResolution(val window: HomeDateWindow?, val cappedAtToday: Boolean = false)

fun HomePeriodSelection.resolve(today: LocalDate, startDay: Int, firstActivity: LocalDate?): HomePeriodResolution {
    fun cycle(month: YearMonth): HomeDateWindow {
        fun start(value: YearMonth) = value.atDay(startDay.coerceIn(1, value.lengthOfMonth()))
        return HomeDateWindow(start(month), start(month.plusMonths(1)).minusDays(1))
    }
    val requested = when (this) {
        HomePeriodSelection.CurrentCycle -> {
            val month = YearMonth.from(today)
            cycle(if (today < cycle(month).from) month.minusMonths(1) else month)
        }
        is HomePeriodSelection.BudgetMonth -> cycle(month)
        is HomePeriodSelection.Rolling -> HomeDateWindow(when (unit) {
            IntervalType.DAY -> today.minusDays(count.toLong()).plusDays(1)
            IntervalType.WEEK -> today.minusWeeks(count.toLong()).plusDays(1)
            IntervalType.MONTH -> today.minusMonths(count.toLong()).plusDays(1)
            IntervalType.YEAR -> today.minusYears(count.toLong()).plusDays(1)
        }, today)
        is HomePeriodSelection.Custom -> {
            val start = from ?: minOf(firstActivity ?: today, through ?: today)
            HomeDateWindow(start, through ?: today)
        }
        HomePeriodSelection.AllTime -> return HomePeriodResolution(firstActivity?.takeIf { it <= today }?.let { HomeDateWindow(it, today) })
    }
    require(requested.from <= today) { "Choose a period that starts today or earlier." }
    return HomePeriodResolution(HomeDateWindow(requested.from, minOf(requested.through, today)), requested.through > today)
}

fun HomePeriodSelection.asTimePeriod(converter: TimeConverter): TimePeriod = when (this) {
    HomePeriodSelection.CurrentCycle -> TimePeriod()
    is HomePeriodSelection.BudgetMonth -> TimePeriod(Month(month.monthValue,
        month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())), month.year)
    is HomePeriodSelection.Rolling -> TimePeriod(lastNRange = LastNTimeRange(count, unit))
    is HomePeriodSelection.Custom -> with(converter) { TimePeriod(fromToRange = FromToTimeRange(
        from?.atStartOfDay()?.toUTC(), through?.plusDays(1)?.atStartOfDay()?.toUTC()?.minusNanos(1))) }
    HomePeriodSelection.AllTime -> TimePeriod(fromToRange = FromToTimeRange(null, null))
}

fun homePeriodFromLegacy(period: TimePeriod, today: LocalDate, converter: TimeConverter): HomePeriodSelection = with(converter) {
    val month = period.month
    val relative = period.lastNRange
    val range = period.fromToRange
    when {
        month != null -> HomePeriodSelection.BudgetMonth(YearMonth.of(period.year ?: today.year, month.monthValue))
        relative != null -> HomePeriodSelection.Rolling(relative.periodN, relative.periodType)
        range != null -> {
            if (range.from == null && range.to == null) HomePeriodSelection.AllTime
            else HomePeriodSelection.Custom(range.from?.toLocalDate(), range.to?.toLocalDate())
        }
        else -> HomePeriodSelection.CurrentCycle
    }
}

/** Only nonsensitive query primitives; old chart-range preferences are intentionally ignored. */
fun HomePeriodSelection.encode(): String = when (this) {
    HomePeriodSelection.CurrentCycle -> "current"
    is HomePeriodSelection.BudgetMonth -> "month|$month"
    is HomePeriodSelection.Rolling -> "rolling|$count|${unit.name}"
    is HomePeriodSelection.Custom -> "custom|${from ?: ""}|${through ?: ""}"
    HomePeriodSelection.AllTime -> "all"
}

fun decodeHomePeriod(value: String?): HomePeriodSelection = runCatching {
    val parts = value?.split('|') ?: return HomePeriodSelection.CurrentCycle
    when {
        parts == listOf("current") -> HomePeriodSelection.CurrentCycle
        parts == listOf("all") -> HomePeriodSelection.AllTime
        parts.size == 2 && parts[0] == "month" -> HomePeriodSelection.BudgetMonth(YearMonth.parse(parts[1]))
        parts.size == 3 && parts[0] == "rolling" -> HomePeriodSelection.Rolling(parts[1].toInt(), IntervalType.valueOf(parts[2]))
        parts.size == 3 && parts[0] == "custom" -> HomePeriodSelection.Custom(
            parts[1].takeIf { it.isNotEmpty() }?.let(LocalDate::parse), parts[2].takeIf { it.isNotEmpty() }?.let(LocalDate::parse))
        else -> HomePeriodSelection.CurrentCycle
    }
}.getOrDefault(HomePeriodSelection.CurrentCycle)
