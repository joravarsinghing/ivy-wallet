package com.ivy.legacy.data.model

import com.ivy.data.model.IntervalType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimePeriodValidationTest {
    @Test fun `last N periods require a positive count for every unit`() {
        IntervalType.entries.forEach { unit ->
            assertTrue(TimePeriod(lastNRange = LastNTimeRange(1, unit)).isValid())
            assertTrue(TimePeriod(lastNRange = LastNTimeRange(99999, unit)).isValid())
            assertFalse(TimePeriod(lastNRange = LastNTimeRange(0, unit)).isValid())
            assertFalse(TimePeriod(lastNRange = LastNTimeRange(-1, unit)).isValid())
        }
    }
    @Test fun `explicit calendar periods remain valid`() {
        assertTrue(TimePeriod(month = Month(10, "October")).isValid())
    }
    @Test fun `open ended all time ranges remain supported`() {
        assertTrue(TimePeriod(fromToRange = FromToTimeRange(null, null)).isValid())
    }
    @Test fun `missing period is not a selected period`() {
        assertFalse(TimePeriod().isValid())
    }
}
