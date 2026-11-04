package com.ivy.home

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class HomeEditorNavigationPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val theme get() = if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT

    @Test fun homeEditorKeepsCloseAndActionsVisible() = render {
        IvyWalletPreview(theme) {
            HomeDashboardEditorContent(defaultHomeDashboardCards.map { it.id }.toSet(), {}, {}, {})
        }
    }

    @Test fun trendEditorKeepsCloseAndActionsVisible() = render {
        IvyWalletPreview(theme) {
            HomeTrendEditorContent(HomeTrendMetric.values().toSet(), {}, {})
        }
    }
}
