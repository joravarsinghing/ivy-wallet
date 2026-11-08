package com.ivy.legacy.data.model

import com.ivy.data.model.IntervalType
import java.time.Instant

/** Primitive context, not a financial backup. No credentials, file contents or DB records. */
object TimePeriodSavedState {
    fun encode(period: TimePeriod): ArrayList<String>? = when {
        period.month != null -> arrayListOf("month", period.month.monthValue.toString(), period.year?.toString().orEmpty())
        period.fromToRange != null -> arrayListOf("range", period.fromToRange.from?.toString().orEmpty(), period.fromToRange.to?.toString().orEmpty())
        period.lastNRange != null && period.lastNRange.periodN > 0 -> arrayListOf("last", period.lastNRange.periodN.toString(), period.lastNRange.periodType.name)
        else -> null
    }
    fun decode(saved: List<String>): TimePeriod? = runCatching {
        require(saved.size == 3 && saved.all { it.length <= 64 })
        when (saved[0]) {
            "month" -> TimePeriod(Month(saved[1].toInt().also { require(it in 1..12) }, ""),
                saved[2].takeIf { it.isNotEmpty() }?.toInt()?.also { require(it in -999999999..999999999) })
            "range" -> {
                val from = saved[1].takeIf { it.isNotEmpty() }?.let(Instant::parse)
                val to = saved[2].takeIf { it.isNotEmpty() }?.let(Instant::parse)
                require(from == null || to == null || from <= to)
                TimePeriod(fromToRange = FromToTimeRange(from, to))
            }
            "last" -> TimePeriod(lastNRange = LastNTimeRange(saved[1].toInt().also { require(it > 0) }, IntervalType.valueOf(saved[2])))
            else -> error("Unknown period")
        }
    }.getOrNull()
}
