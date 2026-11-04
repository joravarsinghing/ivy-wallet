package com.ivy.legacy.ui

import com.ivy.legacy.ui.component.tags.TagEditorHost
import com.ivy.wallet.ui.theme.modal.LoanModalData
import com.ivy.wallet.ui.theme.modal.LoanRecordModalData
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import org.junit.Assert.*
import org.junit.Test

class EditorModalHostTest {
    @Test fun `stale dismissal cannot remove a newer account or category form`() {
        val host = EditorModalHost()
        val first = AccountModalData(null, "AED", 0.0)
        host.account = first
        val next = AccountModalData(null, "AED", 0.0)
        host.account = next
        host.closeAccount(first.id)
        assertSame(next, host.account)
        val category = CategoryModalData(null)
        host.category = CategoryModalData(null)
        host.closeCategory(category.id)
        assertNotNull(host.category)
    }
    @Test fun `loan date copies keep nested account and pending submission ownership`() {
        val first = LoanModalData(null, "AED")
        first.editors.account = AccountModalData(null, "AED", 0.0)
        first.submission.submit({}, {})
        val next = first.copy(baseCurrency = "USD")
        assertSame(first.editors, next.editors)
        assertSame(first.editors.account, next.editors.account)
        assertSame(first.submission, next.submission)
        assertTrue(next.submission.saving)
    }
    @Test fun `repayment date copies keep errors and nested editors`() {
        val first = LoanRecordModalData(null, "AED")
        first.submission.submit({ it(Result.failure(IllegalStateException())) }, {})
        val next = first.copy(baseCurrency = "USD")
        assertSame(first.editors, next.editors)
        assertSame(first.submission, next.submission)
        assertNotNull(next.submission.error)
    }
    @Test fun `tag host rejects replacement or dismissal while writing`() {
        val host = TagEditorHost()
        host.open()
        val first = host.draft!!
        var complete: ((Result<Unit>) -> Unit)? = null
        first.submission.submit({ complete = it }, {})
        host.open(); host.close(first.id)
        assertSame(first, host.draft)
        complete!!(Result.failure(IllegalStateException()))
        assertNotNull(host.draft!!.submission.error)
        host.close(first.id)
        host.open()
        assertNotEquals(first.id, host.draft!!.id)
    }
}
