package com.ivy.legacy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.ivy.data.DataObserver
import com.ivy.data.db.dao.WealthDao
import com.ivy.data.db.dao.write.WriteCategoryRuleDao
import com.ivy.data.db.dao.write.WriteGoalAllocationDao
import com.ivy.data.db.dao.write.WriteGoalDao
import com.ivy.data.db.dao.write.WriteSavedAnalyticsDao
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.navigation.Navigation
import com.ivy.navigation.OnboardingScreen
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LogoutLogicTest {
    @Test
    fun `delete data clears planning and net worth in the wallet transaction`() = runBlocking {
        val allocations = mockk<WriteGoalAllocationDao>(relaxed = true)
        val goals = mockk<WriteGoalDao>(relaxed = true)
        val rules = mockk<WriteCategoryRuleDao>(relaxed = true)
        val analytics = mockk<WriteSavedAnalyticsDao>(relaxed = true)
        val wealth = mockk<WealthDao>(relaxed = true)
        val dataStore = mockk<DataStore<Preferences>>()
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()
        val atomicWrite = mockk<AtomicWriteRepository>()
        coEvery { atomicWrite.run<Unit>(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
            coVerifyAll {
                allocations.deleteAll()
                goals.deleteAll()
                rules.deleteAll()
                analytics.deleteAll()
                wealth.deleteAll()
            }
        }
        val navigation = Navigation()
        val logout = LogoutLogic(
            sharedPrefs = mockk(relaxed = true),
            navigation = navigation,
            dataObserver = DataObserver(),
            dataStore = dataStore,
            accountRepository = mockk(relaxed = true),
            transactionRepository = mockk(relaxed = true),
            categoryRepository = mockk(relaxed = true),
            tagRepository = mockk(relaxed = true),
            userDao = mockk(relaxed = true),
            writeSettingsDao = mockk(relaxed = true),
            writePlannedPaymentRuleDao = mockk(relaxed = true),
            writeBudgetDao = mockk(relaxed = true),
            writeLoanDao = mockk(relaxed = true),
            writeLoanRecordDao = mockk(relaxed = true),
            exchangeRatesRepository = mockk(relaxed = true),
            writeGoalAllocationDao = allocations,
            writeGoalDao = goals,
            writeCategoryRuleDao = rules,
            writeSavedAnalyticsDao = analytics,
            wealthDao = wealth,
            atomicWrite = atomicWrite,
        )

        logout.logout()

        coVerifyAll { atomicWrite.run<Unit>(any()) }
        assertEquals(OnboardingScreen, navigation.currentScreen)
    }
}
