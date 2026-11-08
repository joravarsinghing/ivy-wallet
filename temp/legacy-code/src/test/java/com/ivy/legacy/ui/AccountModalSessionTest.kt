package com.ivy.legacy.ui

import com.ivy.data.repository.AccountWriteConflict
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.accountSaveErrorMessage
import org.junit.Assert.*
import org.junit.Test

class AccountModalSessionTest {
    @Test fun `retained modal shares pending submission without duplicating writes`() {
        val original = AccountModalData(null, "AED", 0.0)
        var count = 0
        original.submission.submit({ count++ }, {})
        val retained = original.copy(autoFocusKeyboard = false)
        retained.submission.submit({ count++ }, {})
        assertEquals(original.id, retained.id); assertSame(original.submission, retained.submission)
        assertTrue(retained.submission.saving); assertEquals(1, count)
    }
    @Test fun `new modal cannot inherit another pending write`() {
        val old = AccountModalData(null, "AED", 0.0)
        old.submission.submit({}, {})
        val next = AccountModalData(null, "AED", 0.0)
        assertNotEquals(old.id, next.id); assertFalse(next.submission.saving)
    }
    @Test fun `conflicts provide review recovery without exposing exception details`() {
        assertTrue(accountSaveErrorMessage(AccountWriteConflict.AlreadySaved()).contains("already saved"))
        assertTrue(accountSaveErrorMessage(AccountWriteConflict.MissingAccount()).contains("no longer exists"))
        assertFalse(accountSaveErrorMessage(IllegalStateException("Private account name")).contains("Private"))
    }
}
