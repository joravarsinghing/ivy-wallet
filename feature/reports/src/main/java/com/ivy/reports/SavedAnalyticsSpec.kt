package com.ivy.reports

import com.ivy.base.model.TransactionType
import com.ivy.data.model.IntervalType
import com.ivy.data.model.Category
import com.ivy.data.model.TagId
import com.ivy.data.model.Tag
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.LastNTimeRange
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Versioned, feature-owned representation of a saved report definition.
 *
 * The database intentionally stores this as JSON so the reports feature can evolve its
 * query and visualization model without leaking feature concepts into the data layer.
 */
@Serializable
data class SavedAnalyticsDefinition(
    val schemaVersion: Int = 1,
    val filter: SavedFilterDefinition,
    val visualization: String = Visualization.DONUT.name,
    val grouping: String = Grouping.CATEGORY.name,
    val interval: String = Interval.MONTH.name,
)

@Serializable
data class SavedFilterDefinition(
    val transactionTypes: List<String> = emptyList(),
    val period: SavedPeriodDefinition? = null,
    val accountIds: List<String> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val currency: String = "",
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val includeKeywords: List<String> = emptyList(),
    val excludeKeywords: List<String> = emptyList(),
    val includedTagIds: List<String> = emptyList(),
    val excludedTagIds: List<String> = emptyList(),
)

@Serializable
data class SavedPeriodDefinition(
    val month: Int? = null,
    val year: Int? = null,
    val fromEpochMillis: Long? = null,
    val toEpochMillis: Long? = null,
    val lastN: Int? = null,
    val lastNType: String? = null,
)

enum class Visualization {
    TABLE,
    BAR,
    LINE,
    AREA,
    DONUT,
}
enum class Grouping {
    CATEGORY,
    ACCOUNT,
    PAYEE,
    TAG,
    PERIOD,
}

enum class Interval {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

fun ReportFilter.toSavedDefinition(): SavedAnalyticsDefinition =
    SavedAnalyticsDefinition(
        filter = SavedFilterDefinition(
            transactionTypes = trnTypes.map(TransactionType::name),
            period = period?.toSavedDefinition(),
            accountIds = accounts.map { it.id.toString() },
            categoryIds = categories.map { it.id.value.toString() },
            currency = currency,
            minAmount = minAmount,
            maxAmount = maxAmount,
            includeKeywords = includeKeywords,
            excludeKeywords = excludeKeywords,
            includedTagIds = includedTags.map { it.value.toString() },
            excludedTagIds = excludedTags.map { it.value.toString() },
        )
    )

fun TimePeriod.toSavedDefinition(): SavedPeriodDefinition = SavedPeriodDefinition(
    month = month?.monthValue,
    year = year,
    fromEpochMillis = fromToRange?.from?.toEpochMilli(),
    toEpochMillis = fromToRange?.to?.toEpochMilli(),
    lastN = lastNRange?.periodN,
    lastNType = lastNRange?.periodType?.name,
)

fun SavedAnalyticsDefinition.toReportFilter(
    accounts: List<Account>,
    categories: List<Category>,
    baseCurrency: String,
    tags: List<Tag>? = null,
    validateBounds: Boolean = true,
): ReportFilter? = runCatching {
    require(schemaVersion == 1)
    val parsedTypes = filter.transactionTypes.map(TransactionType::valueOf)
    fun ids(values: List<String>) = values.map { value -> UUID.fromString(value).also { require(it.toString() == value.lowercase()) } }
    val accountIds = ids(filter.accountIds).toSet()
    val categoryIds = ids(filter.categoryIds).toSet()
    val includedTagIds = ids(filter.includedTagIds).map(::TagId)
    val excludedTagIds = ids(filter.excludedTagIds).map(::TagId)
    require(accounts.map { it.id }.containsAll(accountIds))
    require(categories.map { it.id.value }.containsAll(categoryIds))
    if (tags != null) require(tags.map { it.id }.containsAll(includedTagIds + excludedTagIds))

    ReportFilter(
        trnTypes = parsedTypes,
        period = filter.period?.toTimePeriod(),
        accounts = accounts.filter { it.id in accountIds },
        categories = categories.filter { it.id.value in categoryIds },
        currency = filter.currency.ifBlank { baseCurrency },
        minAmount = filter.minAmount,
        maxAmount = filter.maxAmount,
        includeKeywords = filter.includeKeywords,
        excludeKeywords = filter.excludeKeywords,
        includedTags = includedTagIds,
        excludedTags = excludedTagIds,
    ).also { if (validateBounds) require(it.validate()) }
}.getOrNull()

private fun SavedPeriodDefinition.toTimePeriod(): TimePeriod? {
    // An explicitly present empty period is the existing encoding of all-time.
    // A null period still means the app's default period; never conflate them.
    if (month == null && year == null && fromEpochMillis == null && toEpochMillis == null && lastN == null && lastNType == null)
        return TimePeriod(fromToRange = FromToTimeRange(null, null))
    require(year == null || year in -999999999..999999999)
    require(month == null || month in 1..12)
    require(year == null || month != null)
    require(listOf(month != null, fromEpochMillis != null || toEpochMillis != null, lastN != null || lastNType != null).count { it } == 1)
    require(fromEpochMillis == null || toEpochMillis == null || fromEpochMillis <= toEpochMillis)
    val range = if (fromEpochMillis != null || toEpochMillis != null) {
        FromToTimeRange(
            from = fromEpochMillis?.let(Instant::ofEpochMilli),
            to = toEpochMillis?.let(Instant::ofEpochMilli),
        )
    } else {
        null
    }

    val lastNValue = lastN
    val lastNTypeValue = lastNType
    val lastNRange = if (lastNValue != null && lastNTypeValue != null) {
        require(lastNValue > 0)
        LastNTimeRange(lastNValue, IntervalType.valueOf(lastNTypeValue))
    } else {
        require(lastNValue == null && lastNTypeValue == null)
        null
    }

    return if (month != null || year != null || range != null || lastNRange != null) {
        TimePeriod(
            month = month?.let(Month::fromMonthValue),
            year = year,
            fromToRange = range,
            lastNRange = lastNRange,
        )
    } else {
        null
    }
}
