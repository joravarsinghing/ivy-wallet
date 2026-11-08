package com.ivy.domain.usecase.exchange

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.ivy.data.model.ExchangeRate
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.ExchangeRatesRepository
import timber.log.Timber
import javax.inject.Inject
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ivy.data.datastore.IvyDataStore

object ExchangeRateFreshness {
    val base = stringPreferencesKey("exchange_rates_last_base")
    val providerDate = stringPreferencesKey("exchange_rates_provider_date")
    val checkedAt = stringPreferencesKey("exchange_rates_checked_at")
    const val SOURCE = "Fawaz Ahmed Currency API · daily reference rates"
}

class SyncExchangeRatesUseCase @Inject constructor(
    private val repository: ExchangeRatesRepository,
    private val dataStore: IvyDataStore,
) {
    suspend fun sync(baseCurrency: AssetCode): Either<String, Unit> = either {
        val snapshot = repository.fetchDatedEurRates().bind()
        val eurRates = snapshot.rates
            .associateBy(ExchangeRate::currency)
            .mapValues { it.value.rate }

        /* At this point we must have non-empty EUR rates
           Now we must convert them to base currency

        "eur": {
            "bgn": 1.955902,
            "usd": 1.062366,
        }
         */
        val eurToBaseRate = eurRates[baseCurrency]
        ensure(eurToBaseRate != null) { "EUR to ${baseCurrency.code} rate not found." }

        val baseCurrencyRates = eurRates.mapNotNull { (target, rate) ->
            either {
                val baseTargetRate = rate.value / eurToBaseRate.value
                ExchangeRate(
                    baseCurrency = baseCurrency,
                    currency = target,
                    rate = PositiveDouble.from(baseTargetRate).bind(),
                    manualOverride = false
                )
            }.getOrNull()
        }.toList()
        Timber.d("Updating exchange rates: $baseCurrencyRates")
        val manuallyOverridden = repository.findAllManuallyOverridden()
            .map { it.identifier() }
            .toSet()
        val newRatesToSave = baseCurrencyRates.mapNotNull { newRate ->
            val hasManualOverride = newRate.identifier() in manuallyOverridden
            // Only save exchange rates that are not overridden
            newRate.takeIf { !hasManualOverride }
        }
        repository.saveManyRates(newRatesToSave)
        // Only mark fresh after validated rates have been stored; manual overrides remain untouched.
        dataStore.edit {
            it[ExchangeRateFreshness.base] = baseCurrency.code
            it[ExchangeRateFreshness.providerDate] = snapshot.date.toString()
            it[ExchangeRateFreshness.checkedAt] = java.time.Instant.now().toString()
        }
    }

    private fun ExchangeRate.identifier(): AssetCodeId = AssetCodeId(
        baseCurrency = baseCurrency,
        currency = currency
    )

    data class AssetCodeId(
        val baseCurrency: AssetCode,
        val currency: AssetCode,
    )
}
