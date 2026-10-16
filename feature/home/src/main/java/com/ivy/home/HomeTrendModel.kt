package com.ivy.home

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

enum class HomeTrendBucket {
    DAY,
    MONTH,
}

enum class HomeTrendRange(
    /** Number of calendar buckets represented by this range. */
    val periodCount: Int,
    val bucket: HomeTrendBucket,
    val label: String,
    /** Kept for source compatibility with the existing monthly Home loader. */
    val months: Int,
) {
    SEVEN_DAYS(7, HomeTrendBucket.DAY, "7d", 1),
    THIRTY_DAYS(30, HomeTrendBucket.DAY, "30d", 1),
    THREE_MONTHS(3, HomeTrendBucket.MONTH, "3m", 3),
    SIX_MONTHS(6, HomeTrendBucket.MONTH, "6m", 6),
    TWELVE_MONTHS(12, HomeTrendBucket.MONTH, "12m", 12),

    ;

    val isDaily: Boolean
        get() = bucket == HomeTrendBucket.DAY
}

/**
 * Stable identity for a plotted period. The display label is intentionally not part of the
 * ordering contract: it may be localized, shortened, or changed by the UI.
 */
sealed interface HomeTrendPeriodIdentity {
    val bucket: HomeTrendBucket
    val startDate: LocalDate?

    data class Day(val date: LocalDate) : HomeTrendPeriodIdentity {
        override val bucket: HomeTrendBucket = HomeTrendBucket.DAY
        override val startDate: LocalDate = date
    }

    data class Month(val yearMonth: YearMonth) : HomeTrendPeriodIdentity {
        override val bucket: HomeTrendBucket = HomeTrendBucket.MONTH
        override val startDate: LocalDate = yearMonth.atDay(1)
    }

    /** Compatibility identity for callers that only have the old display label. */
    data class Legacy(
        val label: String,
        override val startDate: LocalDate?,
    ) : HomeTrendPeriodIdentity {
        override val bucket: HomeTrendBucket = HomeTrendBucket.MONTH
    }

    companion object {
        fun day(date: LocalDate): HomeTrendPeriodIdentity = Day(date)

        fun month(yearMonth: YearMonth): HomeTrendPeriodIdentity = Month(yearMonth)

        internal fun legacy(label: String): HomeTrendPeriodIdentity =
            Legacy(label = label, startDate = parseLegacyStartDate(label))
    }
}

enum class HomeTrendMetric(val label: String) {
    INCOME("Income"),
    EXPENSES("Spent"),
    // Kept as SAVINGS for DataStore compatibility. This is income minus spending,
    // not money moved into a savings account.
    SAVINGS("Net change"),
}

@Immutable
data class HomeTrendPoint(
    val periodLabel: String,
    val income: BigDecimal,
    val expenses: BigDecimal,
    /** True when this period is inside observed coverage; false means it predates activity. */
    val isObserved: Boolean = true,
    /** Stable period identity used for ordering and bucketing. */
    val periodIdentity: HomeTrendPeriodIdentity = HomeTrendPeriodIdentity.legacy(periodLabel),
    /** Intra-month samples used to give monthly ranges a truthful daily shape. */
    val dailySamples: ImmutableList<HomeTrendSample> = persistentListOf(),
) {
    val savings: BigDecimal
        get() = income.subtract(expenses)

    /** A stable key suitable for persistence or chart selection callbacks. */
    val periodKey: String
        get() = when (val identity = periodIdentity) {
            is HomeTrendPeriodIdentity.Day -> "day:${identity.date}"
            is HomeTrendPeriodIdentity.Month -> "month:${identity.yearMonth}"
            is HomeTrendPeriodIdentity.Legacy -> "legacy:${identity.label}"
        }
}

@Immutable
data class HomeTrendSample(
    val date: LocalDate,
    val income: BigDecimal,
    val expenses: BigDecimal,
) {
    val savings: BigDecimal
        get() = income.subtract(expenses)
}

@Immutable
data class HomeTrendState(
    val range: HomeTrendRange = HomeTrendRange.THIRTY_DAYS,
    val points: ImmutableList<HomeTrendPoint> = persistentListOf(),
    val visibleMetrics: Set<HomeTrendMetric> = setOf(
        HomeTrendMetric.INCOME,
        HomeTrendMetric.EXPENSES,
        HomeTrendMetric.SAVINGS,
    ),
    val editorVisible: Boolean = false,
    val loading: Boolean = false,
) {
    val hasData: Boolean
        get() = points.any { it.isObserved }

    /** A single observed bucket can be shown as a point, but does not establish a trend line. */
    val hasComparableTrend: Boolean
        get() = points.count { it.isObserved } > 1
}

private fun parseLegacyStartDate(label: String): LocalDate? {
    val normalized = label.trim()
    val month = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE to { value: String ->
            YearMonth.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(value)).atDay(1)
        },
        DateTimeFormatter.ofPattern("yyyy-MM") to { value: String ->
            YearMonth.parse(value, DateTimeFormatter.ofPattern("yyyy-MM")).atDay(1)
        },
        DateTimeFormatter.ofPattern("yyyy/MM") to { value: String ->
            YearMonth.parse(value, DateTimeFormatter.ofPattern("yyyy/MM")).atDay(1)
        },
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH) to { value: String ->
            YearMonth.parse(value, DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)).atDay(1)
        },
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH) to { value: String ->
            YearMonth.parse(value, DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)).atDay(1)
        },
    )

    return month.firstNotNullOfOrNull { (formatter, parser) ->
        try {
            // Parsing with the formatter first keeps malformed labels on the compatibility path.
            formatter.parse(normalized)
            parser(normalized)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
