package com.ivy.report

import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.reports.ReportScreenState
import com.ivy.reports.UI
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class ReportNavigationAdaptivePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    @Test fun oneHeaderAndReadableTotals() = render {
        val theme = if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT
        IvyWalletPreview(theme) {
            UI(ReportScreenState(baseCurrency = "AED", balance = -6405.66,
                income = 2000.0, expenses = 8405.66))
        }
    }
}
