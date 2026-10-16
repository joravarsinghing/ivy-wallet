package com.ivy.reports

import com.ivy.base.model.TransactionType
import com.ivy.legacy.data.model.TimePeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class ReportFilterTest {
    @Test
    fun `empty filter gets current period and all transaction types`() {
        val currentPeriod = TimePeriod()

        val normalized = ReportFilter.emptyFilter(baseCurrency = "AED").normalized(
            defaultPeriod = currentPeriod,
            allAccounts = emptyList(),
            allCategories = emptyList()
        )

        assertNotNull(normalized.period)
        assertSame(currentPeriod, normalized.period)
        assertEquals(ReportFilter.allTransactionTypes(), normalized.trnTypes)
    }

    @Test
    fun `one selected type remains narrow while other dimensions get defaults`() {
        val currentPeriod = TimePeriod()
        val partial = ReportFilter.emptyFilter(baseCurrency = "AED").copy(
            trnTypes = listOf(TransactionType.EXPENSE)
        )

        val normalized = partial.normalized(
            defaultPeriod = currentPeriod,
            allAccounts = emptyList(),
            allCategories = emptyList()
        )

        assertEquals(listOf(TransactionType.EXPENSE), normalized.trnTypes)
        assertNotNull(normalized.period)
        assertSame(currentPeriod, normalized.period)
    }

    @Test
    fun `invalid amount range is still rejected after defaults`() {
        val partial = ReportFilter.emptyFilter(baseCurrency = "AED").copy(
            minAmount = 100.0,
            maxAmount = 10.0
        )

        val normalized = partial.normalized(
            defaultPeriod = TimePeriod(),
            allAccounts = emptyList(),
            allCategories = emptyList()
        )

        assertFalse(normalized.validate())
    }
}
