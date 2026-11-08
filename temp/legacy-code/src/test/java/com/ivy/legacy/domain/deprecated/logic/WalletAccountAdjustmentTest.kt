package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account as DomainAccount
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.action.viewmodel.account.AccountDataAct
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class WalletAccountAdjustmentTest {
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val account = Account("Fictional account", color = 0, currency = "AED", id = id)
    private val store = mockk<TransactionRepository>()
    private val accounts = mockk<AccountRepository>()
    private val writes = mutableListOf<Transaction>()
    private val now = Instant.parse("2026-10-30T12:00:00Z")
    private val clock = mockk<TimeProvider> { every { utcNow() } returns now }
    private val logic = WalletAccountLogic(store, TransactionMapper(accounts),
        mockk<AccountDataAct>(), mockk<SharedPrefs>(), mockk<CurrencyRepository>(), clock)

    init {
        coEvery { accounts.findById(any()) } returns DomainAccount(AccountId(id),
            NotBlankTrimmedString.from(account.name).getOrNull()!!, AssetCode.from("AED").getOrNull()!!,
            ColorInt(0), null, true, 0.0)
        coEvery { store.save(any()) } answers { writes += firstArg<Transaction>(); Unit }
    }

    @Test fun `raising balance writes one income with unchanged amount currency account and time`() = runTest {
        logic.adjustBalance(account, actualBalance = 125.0, newBalance = 150.5)
        val income = writes.single() as Income
        assertEquals(25.5, income.value.amount.value, 0.0)
        assertEquals("AED", income.value.asset.code)
        assertEquals(AccountId(id), income.account)
        assertEquals(now, income.time)
        assertTrue(income.settled)
    }

    @Test fun `lowering balance including debt writes one expense`() = runTest {
        logic.adjustBalance(account, actualBalance = 125.0, newBalance = -25.0)
        val expense = writes.single() as Expense
        assertEquals(150.0, expense.value.amount.value, 0.0)
        assertEquals(AccountId(id), expense.account)
    }

    @Test fun `unchanged balance and existing fiat tolerance do not create transactions`() = runTest {
        logic.adjustBalance(account, actualBalance = 125.0, newBalance = 125.0)
        logic.adjustBalance(account, actualBalance = 0.0, newBalance = 0.001, isFiat = true)
        assertTrue(writes.isEmpty())
        coVerify(exactly = 0) { store.save(any()) }
    }

    @Test fun `small non fiat adjustment is preserved instead of rounded away`() = runTest {
        logic.adjustBalance(account, actualBalance = 0.0, newBalance = 0.001)
        assertEquals(0.001, (writes.single() as Income).value.amount.value, 0.0)
    }

    @Test fun `nonfinite balances and overflowing differences are rejected without writes`() = runTest {
        val pairs = listOf(0.0 to Double.NaN, 0.0 to Double.POSITIVE_INFINITY,
            0.0 to Double.NEGATIVE_INFINITY, Double.NaN to 1.0,
            Double.POSITIVE_INFINITY to 1.0, Double.NEGATIVE_INFINITY to 1.0,
            Double.MAX_VALUE to -Double.MAX_VALUE)
        for ((actual, target) in pairs) {
            try {
                logic.adjustBalance(account, actualBalance = actual, newBalance = target)
                fail("Must reject invalid balance")
            } catch (_: IllegalArgumentException) { }
        }
        assertTrue(writes.isEmpty())
    }

    @Test fun `missing account mapping is a failure not a successful no op`() = runTest {
        coEvery { accounts.findById(any()) } returns null
        for (target in listOf(25.0, -25.0)) {
            try {
                logic.adjustBalance(account, actualBalance = 0.0, newBalance = target)
                fail("Must report unavailable adjustment")
            } catch (_: IllegalStateException) { }
        }
        assertTrue(writes.isEmpty())
    }

    @Test fun `storage failure and cancellation both propagate`() = runTest {
        val failures = listOf(IllegalStateException("Fictional write failure"), CancellationException("Fictional cancellation"))
        for (failure in failures) {
            coEvery { store.save(any()) } throws failure
            try {
                logic.adjustBalance(account, actualBalance = 0.0, newBalance = 25.0)
                fail("Must propagate write failure")
            } catch (actual: Exception) { assertSame(failure, actual) }
        }
    }

    @Test fun `account opening uses its supplied stable transaction identity`() = runTest {
        val openingId = UUID.fromString("00000000-0000-0000-0000-000000000003")
        logic.initializeBalance(account, 25.0, openingId)
        assertEquals(openingId, writes.single().id.value)
        assertEquals(25.0, (writes.single() as Income).value.amount.value, 0.0)
    }
}
