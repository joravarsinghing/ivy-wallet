package com.ivy.domain.usecase.exchange

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import arrow.core.Either
import com.ivy.data.model.ExchangeRate
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.ExchangeRatesRepository
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ExchangeRateFreshnessTest {
    private class Store : DataStore<Preferences> {
        override val data = MutableStateFlow<Preferences>(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }
    private fun rate(currency: AssetCode, amount: Double, manual: Boolean = false, base: AssetCode = AssetCode.EUR) =
        ExchangeRate(base, currency, PositiveDouble.from(amount).getOrNull()!!, manual)

    @Test fun `successful sync records provider date after storing and preserves manual overrides`() = runTest {
        val repository = mockk<ExchangeRatesRepository>()
        val store = Store()
        val date = LocalDate.now().minusDays(1)
        coEvery { repository.fetchDatedEurRates() } returns Either.Right(ExchangeRatesRepository.DatedRates(date,
            listOf(rate(AssetCode.EUR, 1.0), rate(AssetCode.USD, 1.2))))
        coEvery { repository.findAllManuallyOverridden() } returns listOf(rate(AssetCode.EUR, .9, true, AssetCode.USD))
        coEvery { repository.saveManyRates(any()) } coAnswers {
            assertNull(store.data.value[ExchangeRateFreshness.checkedAt])
            val rates = firstArg<List<ExchangeRate>>()
            assertFalse(rates.any { it.currency == AssetCode.EUR })
            assertTrue(rates.all { it.baseCurrency == AssetCode.USD })
        }
        assertTrue(SyncExchangeRatesUseCase(repository, store).sync(AssetCode.USD).isRight())
        assertEquals(date.toString(), store.data.value[ExchangeRateFreshness.providerDate])
        assertEquals("USD", store.data.value[ExchangeRateFreshness.base])
        assertNotNull(store.data.value[ExchangeRateFreshness.checkedAt])
    }
    @Test fun `failed refresh cannot mark cached quotes fresh or overwrite rates`() = runTest {
        val repository = mockk<ExchangeRatesRepository>()
        val store = Store()
        store.edit { it[ExchangeRateFreshness.checkedAt] = "previous success" }
        coEvery { repository.fetchDatedEurRates() } returns Either.Left("Offline")
        assertTrue(SyncExchangeRatesUseCase(repository, store).sync(AssetCode.USD).isLeft())
        assertEquals("previous success", store.data.value[ExchangeRateFreshness.checkedAt])
        coVerify(exactly = 0) { repository.saveManyRates(any()) }
    }
}
