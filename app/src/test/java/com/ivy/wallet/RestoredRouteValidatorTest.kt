package com.ivy.wallet

import com.ivy.base.model.TransactionType
import com.ivy.data.db.dao.read.*
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.CategoryEntity
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.navigation.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RestoredRouteValidatorTest {
    private val accounts = mockk<AccountDao>()
    private val categories = mockk<CategoryDao>()
    private val transactions = mockk<TransactionDao>()
    private val planned = mockk<PlannedPaymentRuleDao>()
    private val validator = RestoredRouteValidator(accounts, categories, transactions, planned)
    private val account = AccountEntity("Fictional bank", color = 0)
    private val category = CategoryEntity("Fictional category", color = 0)
    init {
        coEvery { accounts.findById(any()) } returns null
        coEvery { categories.findById(any()) } returns null
        coEvery { transactions.findById(any()) } returns null
        coEvery { planned.findById(any()) } returns null
    }
    @Test fun `explicit empty selections still require the named category or account to exist`() = runTest {
        val categoryRoute = TransactionsScreen(categoryId = category.id, restoredTransactionIds = emptyList())
        val accountRoute = TransactionsScreen(accountId = account.id, restoredTransactionIds = emptyList())
        assertFalse(validator.usable(categoryRoute)); assertFalse(validator.usable(accountRoute))
        coEvery { accounts.findById(account.id) } returns account
        coEvery { categories.findById(category.id) } returns category
        assertTrue(validator.usable(categoryRoute)); assertTrue(validator.usable(accountRoute))
        assertTrue(validator.usable(TransactionsScreen(unspecifiedCategory = true, restoredTransactionIds = emptyList())))
    }
    @Test fun `existing transaction edit requires a live source and destination`() = runTest {
        val destination = UUID.randomUUID()
        val row = TransactionEntity(account.id, TransactionType.TRANSFER, 100.0, destination, 27.25)
        coEvery { transactions.findById(row.id) } returns row
        coEvery { accounts.findById(account.id) } returns account
        val route = EditTransactionScreen(row.id, TransactionType.TRANSFER)
        assertFalse(validator.usable(route))
        coEvery { accounts.findById(destination) } returns account.copy(id = destination)
        assertTrue(validator.usable(route))
        coEvery { transactions.findById(row.id) } returns row.copy(isDeleted = true)
        assertFalse(validator.usable(route))
    }
    @Test fun `invalid stored amounts and missing existing payment cannot reopen editing`() = runTest {
        val row = TransactionEntity(account.id, TransactionType.EXPENSE, Double.NaN)
        coEvery { accounts.findById(account.id) } returns account
        coEvery { transactions.findById(row.id) } returns row
        assertFalse(validator.usable(EditTransactionScreen(row.id, TransactionType.EXPENSE)))
        assertFalse(validator.usable(EditPlannedScreen(UUID.randomUUID(), TransactionType.EXPENSE)))
    }
}
