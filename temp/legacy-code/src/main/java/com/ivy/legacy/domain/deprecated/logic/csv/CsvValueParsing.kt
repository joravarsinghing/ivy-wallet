package com.ivy.legacy.domain.deprecated.logic.csv

import com.ivy.base.time.TimeConverter
import com.ivy.wallet.domain.deprecated.logic.csv.model.RowMapping
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField

/** Known legacy CSV sources use a dot decimal, optionally with valid grouped commas. */
fun parseImportedCsvAmount(amount: String?): Double? {
    val input = amount?.trim()?.takeIf(String::isNotEmpty) ?: return null
    // Legacy CSV uses a dot decimal; only correctly grouped commas may be removed.
    // Reject decimal-comma/ambiguous grouping instead of multiplying money silently.
    if (',' in input) {
        if (!groupedCsvAmount.matches(input)) return null
    } else if (!plainCsvAmount.matches(input)) return null
    return input.replace(",", "").toDoubleOrNull()?.takeIf(Double::isFinite)
}

private val groupedCsvAmount = Regex("""[+-]?\d{1,3}(?:,\d{3})+(?:\.\d+)?""")
private val plainCsvAmount = Regex("""[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?""")

/** An explicit offset is authoritative; local wall times are converted exactly once. */
internal fun parseImportedCsvTimestamp(
    rowMapping: RowMapping,
    value: String?,
    converter: TimeConverter,
): Instant? {
    val input = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (rowMapping.dateOnlyFormat == null && rowMapping.dateTimeFormat == null) {
        for (format in listOf(DateTimeFormatter.ISO_DATE_TIME, DateTimeFormatter.RFC_1123_DATE_TIME)) {
            try { return Instant.from(format.withResolverStyle(ResolverStyle.STRICT).parse(input)) }
            catch (_: java.time.DateTimeException) { }
        }
    }
    return parseImportedCsvDate(rowMapping, input)?.let { with(converter) { it.toUTC() } }
}

/** Parses wall time only. Zone conversion belongs to the injected TimeConverter. */
internal fun parseImportedCsvDate(
    rowMapping: RowMapping,
    dateString: String?
): LocalDateTime? {
    if (dateString == null || dateString.isBlank()) return null

    if (rowMapping.dateOnlyFormat != null || rowMapping.dateTimeFormat != null) {
        rowMapping.dateOnlyFormat?.let { format ->
            try { return dateString.parseDateOnly(format) } catch (_: Exception) { }
        }
        rowMapping.dateTimeFormat?.let { format ->
            try { return dateString.parseDateTime(format) } catch (_: Exception) { }
        }
        // A declared source format is a contract, not permission to guess another date.
        return null
    }

    val supportedPatterns = listOf(
        "dd/MM/yyyy HH:mm",
        "dd/MM/yyyy HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-dd-MM HH:mm:ss",
        "yyyy/MM/dd HH:mm:ss",
        "MM/dd/yyyy HH:mm:ss",
        "dd/MM/yyyy h:mm a",

        // Fortune City Date variations with 24-h
        "d/M/yyyy HH:mm",
        "d/MM/yyyy HH:mm",
        "dd/M/yyyy HH:mm",
        "dd/MM/yyyy HH:mm",
        "M/d/yyyy HH:mm",
        "M/dd/yyyy HH:mm",
        "MM/d/yyyy HH:mm",
        "MM/dd/yyyy HH:mm",

        // Fortune City Date variations with 12-h (am/pm)
        "d/M/yyyy h:mm a",
        "d/MM/yyyy h:mm a",
        "dd/M/yyyy h:mm a",
        "dd/MM/yyyy h:mm a",
        "M/d/yyyy h:mm a",
        "M/dd/yyyy h:mm a",
        "MM/d/yyyy h:mm a",
        "MM/dd/yyyy h:mm a",

        // Fortune City Date with "-" variations with 24-h
        "d-M-yyyy HH:mm",
        "d-MM-yyyy HH:mm",
        "dd-M-yyyy HH:mm",
        "dd-MM-yyyy HH:mm",
        "M-d-yyyy HH:mm",
        "M-dd-yyyy HH:mm",
        "MM-d-yyyy HH:mm",
        "MM-dd-yyyy HH:mm",

        // Fortune City Date with "-" variations with 12-h (am/pm)
        "d-M-yyyy h:mm a",
        "d-MM-yyyy h:mm a",
        "dd-M-yyyy h:mm a",
        "dd-MM-yyyy h:mm a",
        "M-d-yyyy h:mm a",
        "M-dd-yyyy h:mm a",
        "MM-d-yyyy h:mm a",
        "MM-dd-yyyy h:mm a",

        // More Fortune City Date formats
        "dd-MM-yy H:mm",
        "dd-MM-yy HH:mm",
        "MM-dd-yy H:mm",
        "MM-dd-yy HH:mm",
    )

    for (pattern in supportedPatterns) {
        try {
            return dateString.parseDateTime(dateTimeFormat = pattern)
        } catch (e: Exception) {
        }
    }

    val supportedDateOnlyPatterns = listOf(
        "MM/dd/yyyy",
        "dd.MM.yyyy",
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd/MM/yyyy"
    )

    for (pattern in supportedDateOnlyPatterns) {
        try {
            return dateString.parseDateOnly(dateFormat = pattern)
        } catch (e: Exception) {
        }
    }

    val isoFormats = listOf(
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_INSTANT,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.RFC_1123_DATE_TIME
    )

    for (format in isoFormats) {
        try {
            val parsedDate = LocalDateTime.parse(dateString, format.withResolverStyle(ResolverStyle.STRICT))
            if (parsedDate != null) {
                return parsedDate
            }
        } catch (e: Exception) {
        }
    }

    return null
}

private fun String.parseDateOnly(
    dateFormat: String
): LocalDateTime {
    return LocalDate.parse(
        this,
        strictCsvDateFormatter(dateFormat)
    ).atTime(12, 0)
}

private fun String.parseDateTime(
    dateTimeFormat: String
): LocalDateTime {
    return LocalDateTime.parse(
        this,
        strictCsvDateFormatter(dateTimeFormat)
    )
}


private fun strictCsvDateFormatter(pattern: String): DateTimeFormatter =
    DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern)
        .parseDefaulting(ChronoField.ERA, 1)
        .toFormatter().withResolverStyle(ResolverStyle.STRICT)
