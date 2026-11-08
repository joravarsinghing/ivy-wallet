package com.ivy.wallet.ui.theme.modal

import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.legacy.data.model.*
import com.ivy.data.model.IntervalType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PeriodChooserDraftTest {
    @Test fun `open chooser identity and month draft survive save restore without objects or records`() {
        val original = ChoosePeriodModalData(period = TimePeriod(Month(10, "October"), 2026))
        val scope = androidx.compose.runtime.saveable.SaverScope { true }
        val saved = with(ChoosePeriodModalDataSaver) { scope.save(original) }!!
        val restored = ChoosePeriodModalDataSaver.restore(saved)!!
        assertEquals(original.id, restored.id)
        assertEquals(10, restored.period.month!!.monthValue)
        assertEquals(2026, restored.period.year)
        assertTrue(saved.all { it.length < 64 })
        assertNull(ChoosePeriodModalDataSaver.restore(emptyList()))
    }
    @Test fun `custom date boundaries follow a twenty five hour DST day`() {
        val provider = io.mockk.mockk<com.ivy.base.time.TimeProvider>()
        io.mockk.every { provider.getZoneId() } returns java.time.ZoneId.of("America/New_York")
        val converter = com.ivy.base.time.impl.StandardTimeConverter(provider)
        val day = LocalDate.parse("2026-11-01")
        val range = rangeWithCalendarDate(rangeWithCalendarDate(null, day, true, converter), day, false, converter)!!
        assertEquals(Instant.parse("2026-11-01T04:00:00Z"), range.from)
        assertEquals(Instant.parse("2026-11-02T05:00:00Z").minusNanos(1), range.to)
    }
    @Test fun `month custom relative and all-time selections validate independently`() {
        assertTrue(canApplyPeriod(TimePeriod(Month(10, "October"), 2026)))
        assertTrue(canApplyPeriod(TimePeriod(fromToRange = FromToTimeRange(null, null))))
        assertTrue(canApplyPeriod(TimePeriod(lastNRange = LastNTimeRange(2, IntervalType.WEEK))))
        assertFalse(canApplyPeriod(TimePeriod(lastNRange = LastNTimeRange(0, IntervalType.WEEK))))
        assertFalse(canApplyPeriod(null))
        assertFalse(canApplyPeriod(TimePeriod(fromToRange = FromToTimeRange(Instant.MAX, Instant.MIN))))
    }
    @Test fun `primitive drafts preserve invalid relative input without silently reusing valid values`() {
        val invalid = TimePeriod(lastNRange = LastNTimeRange(0, IntervalType.MONTH))
        assertEquals(invalid, decodePeriodDraft(encodePeriodDraft(invalid)))
        assertFalse(canApplyPeriod(decodePeriodDraft(encodePeriodDraft(invalid))))
        val month = decodePeriodDraft(encodePeriodDraft(TimePeriod(Month(10, "October"), 2026)))!!
        assertEquals(10, month.month!!.monthValue)
        assertEquals(2026, month.year)
        assertTrue(canApplyPeriod(month))
        assertNull(decodePeriodDraft(listOf("relative-draft", "nonsense", "MONTH")))
    }
    @Test fun `custom range includes both full days and permits the same from and to date`() {
        val converter = TestTimeConverter()
        val day = LocalDate.parse("2026-10-31")
        val start = rangeWithCalendarDate(null, day, true, converter)
        val range = rangeWithCalendarDate(start, day, false, converter)!!
        assertEquals(Instant.parse("2026-10-31T00:00:00Z"), range.from)
        assertEquals(Instant.parse("2026-11-01T00:00:00Z").minusNanos(1), range.to)
        assertTrue(canApplyPeriod(TimePeriod(fromToRange = range)))
        assertNull(rangeWithCalendarDate(rangeWithCalendarDate(range, null, true, converter), null, false, converter))
    }
}
