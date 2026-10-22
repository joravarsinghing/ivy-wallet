package com.ivy.planned.list

import com.ivy.data.model.IntervalType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PlannedPaymentScheduleTest {
    @Test
    fun `finds next monthly occurrence`() {
        val next = nextPlannedPaymentDate(
            startDate = LocalDateTime.of(2025, 1, 15, 10, 0),
            oneTime = false,
            intervalN = 1,
            intervalType = IntervalType.MONTH,
            now = LocalDateTime.of(2025, 3, 20, 10, 0),
        )

        assertEquals(LocalDateTime.of(2025, 4, 15, 10, 0), next)
    }
}
