package com.ivy.search

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SearchAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(failed: Boolean) = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            SearchUi(SearchState("Fictional public transport", persistentListOf(), "AED", persistentListOf(), persistentListOf(),
                false, loading = !failed, failed = failed)) {}
        }
    }
    @Test fun loadingRetainsQuery() = snapshot(false)
    @Test fun retryRetainsQuery() = snapshot(true)
}
