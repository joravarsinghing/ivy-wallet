package com.ivy.goals

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class GoalInputValidationTest {
    private fun goal(label: String? = null, date: LocalDate? = null) = Goal(name = "Laptop", type = GoalType.PURCHASE,
        targetAmount = 9500.0, currency = "USD", deadlineLabel = label, deadline = date)

    @Test fun `selected mid-month day survives storage conversion`() {
        val date = LocalDate.of(2027, 4, 23)
        assertEquals(date.atStartOfDay().toInstant(ZoneOffset.UTC), goal(date = date).deadlineInstant())
    }
    @Test fun `typed date is authoritative over a legacy month label`() {
        assertEquals(LocalDate.of(2027, 4, 23), goal("April 2027", LocalDate.of(2027, 4, 23)).resolvedDeadline())
    }
    @Test fun `legacy month callers keep their original first-day meaning`() {
        assertEquals(LocalDate.of(2027, 4, 1), goal("April 2027").resolvedDeadline())
    }
    @Test fun `empty optional deadline remains absent`() {
        listOf(null, "", " ", "No deadline").forEach { assertNull(goal(it).resolvedDeadline()) }
    }
    @Test fun `malformed deadline never silently becomes no deadline`() {
        listOf("Apirhfifkf1f 27289", "2027-02-29", "2027-04-31", "01/02/03").forEach { label ->
            assertTrue(runCatching { goal(label).deadlineInstant() }.isFailure)
        }
    }
    @Test fun `valid leap-day date remains exact`() {
        assertEquals(LocalDate.of(2028, 2, 29), goal("2028-02-29").resolvedDeadline())
    }
    @Test fun `display date is localized rather than reparsed as a storage value`() {
        val value = goal(date = LocalDate.of(2027, 4, 23))
        assertNotEquals(value.displayDeadline(Locale.US), value.displayDeadline(Locale.FRANCE))
        assertEquals(LocalDate.of(2027, 4, 23), value.resolvedDeadline())
    }
    @Test fun `invalid target values and missing name are rejected`() {
        listOf("", "0", "-1", "NaN", "Infinity", "1e9999", "1e-9999", "not money").forEach {
            assertNotNull(goalInputError("Laptop", it))
        }
        assertNotNull(goalInputError(" ", "9500"))
        assertNull(goalInputError("Laptop", "9500.25"))
    }
    @Test fun `reservations above available balance are rejected not silently clamped`() {
        val accounts = listOf(GoalAccount("bank", "Bank", 100.0, "USD"))
        assertNotNull(reservationInputError(mapOf("bank" to "81"), accounts, mapOf("bank" to 20.0)))
        assertNull(reservationInputError(mapOf("bank" to "80"), accounts, mapOf("bank" to 20.0)))
    }
    @Test fun `blank and zero reservations release money while nonfinite values fail`() {
        val accounts = listOf(GoalAccount("bank", "Bank", 100.0, "USD"))
        listOf("", "0").forEach { assertNull(reservationInputError(mapOf("bank" to it), accounts, emptyMap())) }
        listOf("NaN", "Infinity", "-1", "wrong").forEach {
            assertNotNull(reservationInputError(mapOf("bank" to it), accounts, emptyMap()))
        }
    }
    @Test fun `decimal rounding cannot reject an exactly available reservation`() {
        val accounts = listOf(GoalAccount("bank", "Bank", 0.3, "USD"))
        assertNull(reservationInputError(mapOf("bank" to "0.20"), accounts, mapOf("bank" to 0.1)))
    }
    @Test fun `foreign and unavailable allocations are flagged instead of implying conversion`() {
        val accounts = listOf(GoalAccount("usd", "Bank", 100.0, "USD"), GoalAccount("eur", "Euro bank", 50.0, "EUR"))
        assertFalse(goal().copy(allocations = listOf(GoalAllocation("usd", 20.0))).reservationsNeedReview(accounts))
        assertTrue(goal().copy(allocations = listOf(GoalAllocation("eur", 20.0))).reservationsNeedReview(accounts))
        assertTrue(goal().copy(allocations = listOf(GoalAllocation("missing", 20.0))).reservationsNeedReview(accounts))
    }
}
