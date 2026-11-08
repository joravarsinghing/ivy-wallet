package com.ivy.home

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.math.BigDecimal

@RunWith(TestParameterInjector::class)
class HomeSampledTrendPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun preview(range: HomeTrendRange) = render {
        val end = LocalDate.parse("2026-11-04")
        val points = homeTrendSamplingWindows(range, end).mapIndexed { index, bucket ->
            HomeTrendPoint(bucket.displayLabel(), BigDecimal(if (index % 5 == 0) "600.00" else "50.00"),
                BigDecimal(if (index % 3 == 0) "250.00" else "30.00"), BigDecimal(10000 + index * 60),
                periodIdentity = HomeTrendPeriodIdentity.Interval(bucket.from, bucket.through))
        }
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            HomeTrendCard(HomeTrendState(range = range, points = points.toImmutableList(), window = HomeDateWindow.ending(range, end)),
                "AED", false, {}, {}, {}, {})
        }
    }
    @Test fun threeMonthsHaveThirtyRealSamples() = preview(HomeTrendRange.THREE_MONTHS)
    @Test fun sixMonthsHaveThirtyRealSamples() = preview(HomeTrendRange.SIX_MONTHS)
    @Test fun twelveMonthsHaveThirtyRealSamples() = preview(HomeTrendRange.TWELVE_MONTHS)
}
