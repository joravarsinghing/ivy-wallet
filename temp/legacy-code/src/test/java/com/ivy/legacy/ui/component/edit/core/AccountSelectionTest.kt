package com.ivy.wallet.ui.edit.core

import com.ivy.legacy.datamodel.Account
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

@Suppress("DEPRECATION")
class AccountSelectionTest {
    private val accounts = (1L..3L).map { Account("Account $it", color = 0, id = UUID(0, it)) }

    @Test fun `first middle and last accounts use actual row indices without spacer offsets`() {
        accounts.forEachIndexed { index, account ->
            assertEquals(index, selectedAccountIndex(accounts, account.id))
        }
    }
    @Test fun `a refreshed object with the same ID remains selected`() {
        val refreshed = accounts[1].copy(name = "Renamed account")
        assertNotEquals(accounts[1], refreshed)
        assertEquals(1, selectedAccountIndex(accounts, refreshed.id))
    }
    @Test fun `reordering retains selection by ID at its new index`() {
        assertEquals(0, selectedAccountIndex(accounts.reversed(), accounts.last().id))
    }
    @Test fun `a missing account does not scroll to an unrelated account`() {
        assertNull(selectedAccountIndex(accounts, UUID(0, 99)))
    }
    @Test fun `no selection and an empty wallet have no scroll target`() {
        assertNull(selectedAccountIndex(accounts, null))
        assertNull(selectedAccountIndex(emptyList(), accounts.first().id))
    }
}
