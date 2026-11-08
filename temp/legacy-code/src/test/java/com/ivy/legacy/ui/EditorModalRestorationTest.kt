package com.ivy.legacy.ui

import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class EditorModalRestorationTest {
    private val account = Account("Fictional private bank", color = 0, currency = "AED")
    private val category = Category(CategoryId(UUID.randomUUID()), NotBlankTrimmedString.unsafe("Fictional household"), ColorInt(0), null, 1.0)

    @Test fun `creation modal retains its session identity without serializing pending work`() {
        val host = EditorModalHost().apply { this.account = AccountModalData(null, "AED", 0.0); this.category = CategoryModalData(null) }
        host.account!!.submission.submit({}, {})
        val restored = EditorModalHost()
        assertTrue(restored.restoreState(host.saveState()!!, "AED", emptyList(), emptyList()))
        assertEquals(host.account!!.id, restored.account!!.id)
        assertEquals(host.category!!.id, restored.category!!.id)
        assertFalse(restored.account!!.submission.saving)
        assertNull(restored.account!!.submission.error)
    }

    @Test fun `editing rehydrates fresh records without saving their names or object graphs`() {
        val host = EditorModalHost().apply {
            this.account = AccountModalData(this@EditorModalRestorationTest.account, "AED", 120.0)
            this.category = CategoryModalData(this@EditorModalRestorationTest.category)
        }
        val payload = host.saveState()!!
        assertFalse(payload.joinToString().contains(account.name))
        assertFalse(payload.joinToString().contains(category.name.value))
        val fresh = EditorModalHost()
        assertTrue(fresh.restoreState(payload, "AED", listOf(account), listOf(category), balances = mapOf(account.id to 120.0)))
        assertSame(account, fresh.account!!.account)
        assertSame(category, fresh.category!!.category)
    }

    @Test fun `removed records cannot turn editing into creation`() {
        val host = EditorModalHost().apply { this.account = AccountModalData(this@EditorModalRestorationTest.account, "AED", 10.0); this.category = CategoryModalData(this@EditorModalRestorationTest.category) }
        val fresh = EditorModalHost()
        assertFalse(fresh.restoreState(host.saveState()!!, "AED", emptyList(), emptyList()))
        assertNull(fresh.account); assertNull(fresh.category)
    }

    @Test fun `changed metadata balance or currency refuses the stale modal`() {
        val payload = EditorModalHost().apply { this.account = AccountModalData(this@EditorModalRestorationTest.account, "AED", 120.0) }.saveState()!!
        val fresh = EditorModalHost()
        assertFalse(fresh.restoreState(payload, "USD", listOf(account), emptyList(), balances = mapOf(account.id to 120.0)))
        assertFalse(fresh.restoreState(payload, "AED", listOf(account), emptyList(), balances = mapOf(account.id to 121.0)))
        assertFalse(fresh.restoreState(payload, "AED", listOf(account.copy(name = "Renamed")), emptyList(), balances = mapOf(account.id to 120.0)))
        assertNull(fresh.account)
    }

    @Test fun `retained live session wins over a saved descriptor`() {
        val old = EditorModalHost().apply { this.account = AccountModalData(null, "AED", 0.0) }.saveState()!!
        val live = EditorModalHost().apply { this.account = AccountModalData(null, "AED", 0.0) }
        val expected = live.account
        assertTrue(live.restoreState(old, "AED", emptyList(), emptyList()))
        assertSame(expected, live.account)
    }
    @Test fun `malformed descriptor fails without reopening a form`() {
        val fresh = EditorModalHost()
        assertFalse(fresh.restoreState(listOf("future"), "AED", emptyList(), emptyList()))
        assertNull(fresh.account)
    }
}
