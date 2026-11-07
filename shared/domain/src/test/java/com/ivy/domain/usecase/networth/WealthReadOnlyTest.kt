package com.ivy.domain.usecase.networth

import com.ivy.base.legacy.SharedPrefs
import com.ivy.data.db.dao.read.*
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.model.wealth.WealthBackup
import com.ivy.data.repository.WealthRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WealthReadOnlyTest {
    @Test fun `read only load cannot trigger legacy migration or capture rates`() = runTest {
        val account = AccountEntity(name = "Legacy savings", currency = "AED", color = 0)
        val repository = mockk<WealthRepository>()
        val accounts = mockk<AccountDao>()
        val transactions = mockk<TransactionDao>()
        val loans = mockk<LoanDao>()
        val records = mockk<LoanRecordDao>()
        val settings = mockk<SettingsDao>()
        val preferences = mockk<SharedPrefs>(relaxed = true)
        val rates = mockk<ExchangeRatesDao>()
        coEvery { repository.read() } returns WealthBackup()
        coEvery { accounts.findAll() } returns listOf(account)
        coEvery { transactions.findAll() } returns emptyList()
        coEvery { loans.findAll() } returns emptyList()
        coEvery { records.findAll() } returns emptyList()
        coEvery { settings.findFirstOrNull() } returns null
        every { preferences.getString(SharedPrefs.NET_WORTH_ASSET_ACCOUNT_IDS, any()) } returns account.id.toString()
        val reader = WealthWorkspaceUseCase(repository, accounts, transactions, loans, records, settings, preferences, rates)
        val result = reader.load(LocalDate.parse("2026-11-07"), "AED", 12, allowLegacyMigration = false)
        assertTrue(result.data.items.isEmpty())
        assertFalse(result.current.complete)
        assertEquals(12, result.history.size)
        coVerify(exactly = 0) { repository.merge(any()) }
        verify(exactly = 0) { preferences.getString(SharedPrefs.NET_WORTH_ASSET_ACCOUNT_IDS, any()) }
        coVerify(exactly = 0) { rates.findAll() }
    }
}
