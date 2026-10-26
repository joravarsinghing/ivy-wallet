package com.ivy.ui.time.impl

import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.StandardTimeConverter
import com.ivy.ui.time.calendarDateFromMillis
import com.ivy.ui.time.calendarDateMillis
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class AndroidDateTimePickerTest {
    private val clock = mockk<TimeProvider> {
        every { getZoneId() } returns ZoneId.of("America/Los_Angeles")
        every { localDateNow() } returns LocalDate.of(2026, 10, 26)
        every { localTimeNow() } returns LocalTime.of(13, 25, 47)
    }
    private val converter = StandardTimeConverter(clock)
    private val picker = AndroidDateTimePicker(clock, converter)

    @Test fun `transaction instant opens on its device-local day not its UTC day`() {
        picker.pickDate(Instant.parse("2026-10-26T00:30:00Z")) {}
        assertEquals(LocalDate.of(2026, 10, 25), (picker.pending as PickerRequest.Date).initial)
    }

    @Test fun `calendar-only dates are never shifted through a time zone`() {
        val day = LocalDate.of(2026, 10, 26)
        picker.pickCalendarDate(day) {}
        assertEquals(day, (picker.pending as PickerRequest.Date).initial)
    }

    @Test fun `cancel never reports a selected date or time`() {
        var calls = 0
        picker.pickCalendarDate(null, LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1)) { calls++ }
        picker.dismiss(picker.pending!!.id)
        picker.pickTime(null) { calls++ }
        picker.dismiss(picker.pending!!.id)
        assertEquals(0, calls)
        assertNull(picker.pending)
    }

    @Test fun `bounds are inclusive and invalid confirmation leaves the request open`() {
        val day = LocalDate.of(2027, 4, 23)
        var result: LocalDate? = null
        picker.pickCalendarDate(day, day, day) { result = it }
        val id = picker.pending!!.id
        picker.confirmDate(id, day.minusDays(1))
        assertNotNull(picker.pending)
        assertNull(result)
        picker.confirmDate(id, day.plusDays(1))
        assertNull(result)
        picker.confirmDate(id, day)
        assertEquals(day, result)
        assertNull(picker.pending)
    }

    @Test fun `selection is delivered exactly once and stale dialog events cannot cancel a new request`() {
        var calls = 0
        picker.pickCalendarDate(null) { calls++ }
        val oldId = picker.pending!!.id
        picker.confirmDate(oldId, LocalDate.of(2027, 4, 23))
        picker.confirmDate(oldId, LocalDate.of(2027, 4, 24))
        picker.pickTime(LocalTime.NOON) { calls++ }
        val current = picker.pending
        picker.dismiss(oldId)
        picker.confirmDate(oldId, LocalDate.of(2027, 4, 25))
        assertSame(current, picker.pending)
        assertEquals(1, calls)
    }

    @Test fun `date and time requests replace each other rather than opening two dialogs`() {
        picker.pickTime(null) {}
        val oldId = picker.pending!!.id
        picker.pickCalendarDate(null) {}
        picker.confirmTime(oldId, LocalTime.NOON)
        assertTrue(picker.pending is PickerRequest.Date)
    }

    @Test fun `callbacks may chain the next picker without losing that request`() {
        picker.pickCalendarDate(null) { picker.pickTime(LocalTime.of(9, 30)) {} }
        picker.confirmDate(picker.pending!!.id, LocalDate.of(2027, 4, 23))
        assertEquals(LocalTime.of(9, 30), (picker.pending as PickerRequest.Time).initial)
    }

    @Test fun `new requests get fresh identities even when their initial values match`() {
        picker.pickTime(LocalTime.NOON) {}
        val old = picker.pending!!.id
        picker.pickTime(LocalTime.NOON) {}
        assertNotEquals(old, picker.pending!!.id)
    }

    @Test fun `time selection retains minute precision across midnight noon and late evening`() {
        listOf(LocalTime.MIDNIGHT, LocalTime.NOON, LocalTime.of(23, 59, 48, 9)).forEach { input ->
            var result: LocalTime? = null
            picker.pickTime(input) { result = it }
            picker.confirmTime(picker.pending!!.id, input)
            assertEquals(input.withSecond(0).withNano(0), result)
        }
    }

    @Test fun `null initial time uses the clock instead of stale previous input`() {
        picker.pickTime(LocalTime.of(22, 55)) {}
        picker.pickTime(null) {}
        assertEquals(LocalTime.of(13, 25, 47), (picker.pending as PickerRequest.Time).initial)
    }

    @Test fun `date editing callback preserves transaction time and converts only at the caller boundary`() {
        val original = Instant.parse("2026-10-26T00:30:00Z")
        var result: Instant? = null
        picker.pickDate(original) { day ->
            result = with(converter) { day.atTime(original.toLocalTime()).toUTC() }
        }
        val chosen = calendarDateFromMillis(calendarDateMillis(LocalDate.of(2026, 11, 2)))
        picker.confirmDate(picker.pending!!.id, chosen)
        assertEquals(Instant.parse("2026-11-03T01:30:00Z"), result) // DST changed; local 17:30 did not.
    }

    @Test fun `invalid limits fail before replacing the active request`() {
        picker.pickTime(LocalTime.NOON) {}
        val active = picker.pending
        assertTrue(runCatching { picker.pickCalendarDate(null, LocalDate.of(2030, 1, 1), LocalDate.of(2020, 1, 1)) {} }.isFailure)
        assertSame(active, picker.pending)
    }
}
