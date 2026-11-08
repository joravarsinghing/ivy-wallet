package com.ivy.data.backup

import com.ivy.data.db.entity.AccountEntity
import org.junit.Assert.*
import org.junit.Test

class AccountRestoreMatchingTest {
    private fun account(currency: String?) = AccountEntity("Cash", currency, 0)
    @Test fun `same name with different currencies never merges`() {
        assertTrue(accountRestoreReplacements(listOf(account("USD")), listOf(account("EUR")), "USD", "EUR").isEmpty())
    }
    @Test fun `unique same name and currency retain legacy remapping`() {
        val old = account("USD"); val incoming = account("USD")
        assertEquals(listOf(incoming.id to old.id), accountRestoreReplacements(listOf(old), listOf(incoming), null, null))
    }
    @Test fun `implicit account currencies use their own backup base`() {
        assertTrue(accountRestoreReplacements(listOf(account(null)), listOf(account(null)), "USD", "EUR").isEmpty())
    }
    @Test fun `same ID cannot overwrite the existing currency`() {
        val old = account("USD")
        assertThrows(IllegalArgumentException::class.java) {
            accountRestoreReplacements(listOf(old), listOf(old.copy(currency = "EUR")), "USD", "EUR")
        }
    }
    @Test fun `currency comparison matches mapper trimming and blank fallback`() {
        val old = account(" usd ")
        val incoming = account("  ")
        assertEquals(listOf(incoming.id to old.id), accountRestoreReplacements(listOf(old), listOf(incoming), "USD", "usd"))
    }
    @Test fun `ambiguous incoming and existing names remain separate`() {
        val old = account("USD")
        assertTrue(accountRestoreReplacements(listOf(old), listOf(account("USD"), account("USD")), null, null).isEmpty())
        assertTrue(accountRestoreReplacements(listOf(old, account("USD")), listOf(account("USD")), null, null).isEmpty())
    }
    @Test fun `accounts already explicitly represented in the backup cannot absorb another account`() {
        val old = account("USD")
        assertTrue(accountRestoreReplacements(listOf(old), listOf(old, account("USD")), null, null).isEmpty())
    }
}
