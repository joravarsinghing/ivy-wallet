package com.ivy.exchangerates

import com.ivy.exchangerates.data.RateUi

sealed interface RatesEvent {
    data object Refresh : RatesEvent
    data class Search(val query: String) : RatesEvent
    data class RemoveOverride(val rate: RateUi) : RatesEvent
    data class UpdateRate(val rate: RateUi, val newRate: Double) : RatesEvent
    data class AddRate(val rate: RateUi, val onResult: (Result<Unit>) -> Unit = {}) : RatesEvent
}
