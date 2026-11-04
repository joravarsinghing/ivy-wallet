package com.ivy.home

import com.ivy.base.time.TimeConverter
import com.ivy.navigation.TransactionDateRange
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.runtime.Immutable

/** One inclusive local-calendar window for the plot, spending and activity. */
@Immutable
data class HomeDateWindow(val from: LocalDate, val through: LocalDate) {
    init { require(from <= through) }

    fun toUtcRange(converter: TimeConverter): ClosedTimeRange = with(converter) {
        ClosedTimeRange(from.atStartOfDay().toUTC(), through.plusDays(1).atStartOfDay().toUTC().minusNanos(1))
    }

    fun navigationRange(converter: TimeConverter): TransactionDateRange = toUtcRange(converter).let {
        TransactionDateRange(it.from, it.to)
    }

    fun displayLabel(): String {
        val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
        return "${from.format(format)} – ${through.format(format)}"
    }

    companion object {
        fun ending(range: HomeTrendRange, end: LocalDate): HomeDateWindow = HomeDateWindow(
            if (range.isDaily) end.minusDays(range.periodCount - 1L)
            else end.withDayOfMonth(1).minusMonths(range.periodCount - 1L), end)
    }
}
