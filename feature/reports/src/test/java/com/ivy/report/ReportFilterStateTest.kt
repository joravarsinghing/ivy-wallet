package com.ivy.reports

import com.ivy.base.model.TransactionType
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import org.junit.Assert.*
import org.junit.Test

class ReportFilterStateTest {
    @Test fun `cleared and temporarily invalid drafts remain distinguishable from failed restoration`() {
        assertTrue(ReportFilterState.isCleared(ReportFilterState.save(null)))
        val invalid = filter.copy(minAmount = 50.0, maxAmount = 20.0)
        val saved = ReportFilterState.save(invalid)
        assertNull(ReportFilterState.restore(saved, listOf(account), emptyList(), "AED"))
        val draft = ReportFilterState.restore(saved, listOf(account), emptyList(), "AED", allowInvalidDraft = true)!!
        assertFalse(draft.validate())
        assertEquals(20.0, draft.maxAmount!!, 0.0)
        assertNull(ReportFilterState.save(filter.copy(includeKeywords = listOf("x".repeat(70000)))))
    }
    private val account = Account("Fictional private account name", 0, "AED")
    private val filter = ReportFilter.emptyFilter("AED").copy(accounts = listOf(account),
        trnTypes = listOf(TransactionType.EXPENSE), period = TimePeriod(fromToRange = FromToTimeRange(null, null)),
        minAmount = 10.5, includeKeywords = listOf("Fictional, train\n\"n|s"))
    @Test fun `query state retains identity all time keywords and exact selection without record graphs`() {
        val saved = ReportFilterState.save(filter)!!
        assertFalse(saved.joinToString().contains(account.name))
        assertEquals(filter, ReportFilterState.restore(saved, listOf(account), emptyList(), "AED"))
    }
    @Test fun `deleted selection or malformed payload cannot silently widen a restored draft`() {
        assertNull(ReportFilterState.restore(ReportFilterState.save(filter), emptyList(), emptyList(), "AED"))
        assertNull(ReportFilterState.restore(listOf("future"), listOf(account), emptyList(), "AED"))
    }
    @Test fun `draft changes and cancellation do not mutate the applied filter`() {
        val draft = filter.copy(maxAmount = 99.0)
        val restored = ReportFilterState.restore(ReportFilterState.save(draft), listOf(account), emptyList(), "AED")!!
        assertEquals(99.0, restored.maxAmount!!, 0.0)
        assertNull(filter.maxAmount)
        assertEquals(filter.id, restored.id)
    }
}
