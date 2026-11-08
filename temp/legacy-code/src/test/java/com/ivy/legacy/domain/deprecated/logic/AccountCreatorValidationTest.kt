package com.ivy.legacy.domain.deprecated.logic

import androidx.compose.ui.graphics.Color
import com.ivy.base.TestDispatchersProvider
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.repository.AccountWriteRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.deprecated.logic.WalletAccountLogic
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AccountCreatorValidationTest {
    private val logic = mockk<WalletAccountLogic>(relaxed = true)
    private val dao = mockk<AccountDao>(relaxed = true)
    private val repository = mockk<AccountWriteRepository>()
    private val creator = AccountCreator(logic, dao, repository, mockk<CurrencyRepository>(), TestDispatchersProvider)
    private val valid = CreateAccountData("Fictional account", "AED", Color.Black, null, 0.0)

    init {
        coEvery { repository.edit(any(), any()) } coAnswers { secondArg<suspend () -> Unit>().invoke() }
        coEvery { repository.create(any(), any(), any()) } coAnswers { thirdArg<suspend () -> Unit>().invoke() }
    }

    private suspend fun rejects(block: suspend () -> Unit) {
        try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) { }
    }

    @Test fun `nonfinite opening balances fail before account or adjustment writes`() = runTest {
        for (balance in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            rejects { creator.createAccount(valid.copy(balance = balance)) { fail("No success refresh") } }
        }
        coVerify(exactly = 0) { repository.create(any(), any(), any()) }
        coVerify(exactly = 0) { logic.initializeBalance(any(), any(), any()) }
    }

    @Test fun `invalid new account cannot refresh as if saved`() = runTest {
        rejects { creator.createAccount(valid.copy(name = "  ")) { fail("No success refresh") } }
        rejects { creator.createAccount(valid.copy(currency = "")) { fail("No success refresh") } }
        coVerify(exactly = 0) { repository.create(any(), any(), any()) }
    }

    @Test fun `invalid edits do not save account details or acknowledge success`() = runTest {
        for (balance in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            rejects { creator.editAccount(Account("Fictional", color = 0, currency = "AED"), balance) { fail("No success refresh") } }
        }
        rejects { creator.editAccount(Account(" ", color = 0, currency = "AED"), 10.0) { fail("No success refresh") } }
        coVerify(exactly = 0) { repository.create(any(), any(), any()) }
        coVerify(exactly = 0) { repository.edit(any(), any()) }
    }

    @Test fun `valid zero opening balance refreshes only after both operations finish`() = runTest {
        var adjusted = false
        coEvery { logic.initializeBalance(any(), any(), any()) } answers { adjusted = true }
        var refreshed = false
        creator.createAccount(valid) { assertTrue(adjusted); refreshed = true }
        assertTrue(refreshed)
        coVerify(exactly = 1) { repository.create(any(), any(), any()) }
    }

    @Test fun `adjustment failure cannot invoke the account success refresh`() = runTest {
        val failure = IllegalStateException("Fictional adjustment write failure")
        coEvery { logic.initializeBalance(any(), any(), any()) } throws failure
        try {
            creator.createAccount(valid.copy(balance = -25.0)) { fail("No success refresh") }
            fail("Must propagate failure")
        } catch (actual: IllegalStateException) {
            // Coroutine stack recovery may copy the exception across Dispatchers.IO.
            assertEquals(failure.javaClass, actual.javaClass)
            assertEquals(failure.message, actual.message)
        }
        // The creator delegates the whole boundary; rollback is tested at the coordinator.
        coVerify(exactly = 1) { repository.create(any(), any(), any()) }
    }

    @Test fun `storage cancellation propagates without a success refresh`() = runTest {
        coEvery { repository.create(any(), any(), any()) } throws CancellationException("Fictional cancellation")
        try {
            creator.createAccount(valid) { fail("No success refresh") }
            fail("Must propagate cancellation")
        } catch (_: CancellationException) { }
        coVerify(exactly = 0) { logic.initializeBalance(any(), any(), any()) }
    }

    @Test fun `explicit result is success even when optional refresh fails after storage`() = runTest {
        var result: Result<Unit>? = null
        creator.createAccount(valid, { result = it }) { error("Fictional refresh failure") }
        assertTrue(result!!.isSuccess)
    }

    @Test fun `explicit result reports storage failure without a success refresh`() = runTest {
        coEvery { repository.create(any(), any(), any()) } throws IllegalStateException("Private account detail")
        var result: Result<Unit>? = null
        creator.createAccount(valid, { result = it }) { fail("Must not refresh") }
        assertTrue(result!!.isFailure)
    }
}
