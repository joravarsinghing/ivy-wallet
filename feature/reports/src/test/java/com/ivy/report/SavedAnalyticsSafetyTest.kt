package com.ivy.reports

import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class SavedAnalyticsSafetyTest {
    private val account = Account("Fictional account", 0, "AED")
    private fun decode(spec: SavedFilterDefinition) = SavedAnalyticsDefinition(filter = spec).toReportFilter(listOf(account), emptyList(), "AED")
    @Test fun `missing selected account cannot broaden a saved report to every account`() {
        assertNull(decode(SavedFilterDefinition(accountIds = listOf(UUID.randomUUID().toString()))))
        assertNull(decode(SavedFilterDefinition(accountIds = listOf("broken-id"))))
        assertEquals(listOf(account), decode(SavedFilterDefinition(accountIds = listOf(account.id.toString())))!!.accounts)
        assertTrue(decode(SavedFilterDefinition())!!.accounts.isEmpty()) // Deliberately empty means all.
    }
    @Test fun `explicit all time and default period remain distinct without changing saved schema`() {
        val all = ReportFilter.emptyFilter("AED").copy(period = TimePeriod(fromToRange = FromToTimeRange(null, null)))
        val restored = all.toSavedDefinition().toReportFilter(emptyList(), emptyList(), "AED")!!
        assertNotNull(restored.period!!.fromToRange)
        assertNull(restored.period!!.fromToRange!!.from)
        assertNull(restored.period!!.fromToRange!!.to)
        assertNull(decode(SavedFilterDefinition())!!.period)
    }
    @Test fun `reversed partial unknown and invalid period specifications fail closed`() {
        val start = Instant.parse("2026-11-01T12:00:00Z").toEpochMilli()
        for (period in listOf(SavedPeriodDefinition(fromEpochMillis = start, toEpochMillis = start - 1),
            SavedPeriodDefinition(lastN = 0, lastNType = "MONTH"), SavedPeriodDefinition(lastN = 1),
            SavedPeriodDefinition(lastN = 1, lastNType = "FUTURE"), SavedPeriodDefinition(month = 13), SavedPeriodDefinition(year = 2026))) {
            assertNull(decode(SavedFilterDefinition(period = period)))
        }
    }
    @Test fun `invalid enums ids and nonfinite or reversed amount ranges cannot become broad reports`() {
        for (spec in listOf(SavedFilterDefinition(transactionTypes = listOf("UNKNOWN")),
            SavedFilterDefinition(categoryIds = listOf(UUID.randomUUID().toString())),
            SavedFilterDefinition(includedTagIds = listOf("bad")), SavedFilterDefinition(minAmount = Double.NaN),
            SavedFilterDefinition(maxAmount = Double.POSITIVE_INFINITY), SavedFilterDefinition(minAmount = -1.0),
            SavedFilterDefinition(minAmount = 50.0, maxAmount = 10.0))) assertNull(decode(spec))
    }
}
