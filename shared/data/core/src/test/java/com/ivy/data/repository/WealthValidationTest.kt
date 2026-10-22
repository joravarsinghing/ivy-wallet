package com.ivy.data.repository

import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.model.wealth.*
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class WealthValidationTest {
    private val repository = WealthRepository(mockk<IvyRoomDatabase>())
    private val item = WealthItem("asset", "Home", WealthKind.PROPERTY, "AED", createdDate = "2026-10-21")
    @Test fun `full wealth backup round trips exact decimals and optional old fields`() {
        val data = WealthBackup(items = listOf(item), valuations = listOf(WealthValuation("valuation", "asset", "2026-10-21", "123456789.123456789", recordedAt = "2026-10-21T12:00:00Z")))
        assertEquals(data, Json.decodeFromString<WealthBackup>(Json.encodeToString(data)))
        assertEquals(WealthBackup(), Json.decodeFromString<WealthBackup>("{}"))
    }
    @Test fun `duplicate source is rejected before restore`() {
        val a = item.copy(source = WealthSource.ACCOUNT, sourceId = "bank")
        assertThrows(IllegalArgumentException::class.java) { repository.validate(WealthBackup(items = listOf(a, a.copy(id = "duplicate")))) }
    }
    @Test fun `orphan valuation is rejected`() {
        val value = WealthValuation("v", "missing", "2026-10-21", "1", recordedAt = "2026-10-21T12:00:00Z")
        assertThrows(IllegalArgumentException::class.java) { repository.validateReferences(WealthBackup(items = listOf(item), valuations = listOf(value))) }
    }
    @Test fun `nonpositive rates and invalid ownership are rejected`() {
        val rate = WealthFx("r", "USD", "AED", "0", "2026-10-21", recordedAt = "2026-10-21T12:00:00Z")
        assertThrows(IllegalArgumentException::class.java) { repository.validate(WealthBackup(rates = listOf(rate))) }
        val policy = WealthPolicy("p", item.id, "2026-10-21", ownership = "101", recordedAt = "2026-10-21T12:00:00Z")
        assertThrows(IllegalArgumentException::class.java) { repository.validate(WealthBackup(items = listOf(item), policies = listOf(policy))) }
    }
    @Test fun `self referencing portfolio is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { repository.validateReferences(WealthBackup(items = listOf(item.copy(source = WealthSource.HOLDINGS, parentId = item.id)))) }
    }
    @Test fun `old Ivy backup shape has no wealth credentials`() {
        val legacy = Json.decodeFromString<com.ivy.data.backup.IvyWalletCompleteData>("{}")
        assertNull(legacy.wealth)
        assertFalse(Json.encodeToString(WealthBackup()).contains("pin", ignoreCase = true))
    }
}
