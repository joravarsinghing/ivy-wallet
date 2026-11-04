package com.ivy.networth

import java.time.LocalDate

/** A single immutable export intent, kept in memory only while Android chooses a destination. */
internal data class WealthCsvExport(val fileName: String, val content: String) {
    companion object {
        fun create(date: LocalDate, history: Boolean, content: String): WealthCsvExport {
            require(content.isNotBlank())
            val prefix = if (history) "Jvy_wealth_history" else "Jvy_wealth"
            return WealthCsvExport("${prefix}_$date.csv", content)
        }
    }
}
