package com.ivy.exchangerates

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.exchangerates.data.RateUi
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class RatesCoherencePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    @Test fun providerAndRefreshAreVisibleWithoutLosingManualRates() = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            ExchangeRatesContent(RatesState("AED", persistentListOf(RateUi("AED", "EUR", .25)),
                persistentListOf(RateUi("AED", "USD", .2723)), providerDate = "2026-11-07", checkedAt = "2026-11-07T12:00:00Z"), {})
        }
    }
    @Test fun staleOfflineCacheIsNotPresentedAsFresh() = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            ExchangeRatesContent(RatesState("AED", persistentListOf(), persistentListOf(RateUi("AED", "USD", .2723)),
                providerDate = "2026-10-01", error = "Could not refresh. Previously saved rates remain available."), {})
        }
    }
}
