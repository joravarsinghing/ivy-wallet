package com.ivy.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.math.BigDecimal

@RunWith(TestParameterInjector::class)
class HomeTimelinePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun preview(loading: Boolean = false, hidden: Boolean = false, error: Boolean = false) = render {
        val bounds = HomeDateWindow(LocalDate.parse("2026-10-06"), LocalDate.parse("2026-11-04"))
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeCurrentHeader("Fictional user", "AED", BigDecimal("12844.20"), hidden, {})
                HomeTimelineControls(HomePeriodSelection.CurrentCycle,
                    if (loading || error) HomePeriodSelection.Rolling(30, com.ivy.data.model.IntervalType.DAY) else HomePeriodSelection.CurrentCycle,
                    bounds, loading, if (error) "Couldn't update this period. Previous results remain shown." else null, false, {}, {}, {})
                HomePeriodSummary(IncomeExpensePair(BigDecimal("6350.27"), BigDecimal("1296.45")), "AED", hidden, bounds)
            }
        }
    }
    @Test fun controlsRemainAvailableWithoutChart() = preview()
    @Test fun pendingSelectionKeepsPreviousDateCaption() = preview(loading = true)
    @Test fun hiddenSummaryDoesNotExposeAmounts() = preview(hidden = true)
    @Test fun retryRetainsPreviousResultDates() = preview(error = true)
}
