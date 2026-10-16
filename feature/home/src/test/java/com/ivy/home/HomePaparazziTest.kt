package com.ivy.home

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class HomePaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot Home Screen`() {
        snapshot(theme) {
            HomeUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Money trends with a missing month`() {
        snapshot(theme) {
            IvyWalletPreview(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                HomeTrendCard(
                    state = HomeTrendState(
                        range = HomeTrendRange.THREE_MONTHS,
                        points = persistentListOf(
                            HomeTrendPoint("Aug", BigDecimal.ZERO, BigDecimal.ZERO, isObserved = false),
                            HomeTrendPoint("Sep", BigDecimal("3850.00"), BigDecimal("335.40")),
                            HomeTrendPoint("Oct", BigDecimal("4200.00"), BigDecimal("510.00")),
                        ),
                    ),
                    currency = "AED",
                    hideBalance = false,
                    onRangeSelected = {},
                    onMetricToggle = {},
                    onOpenEditor = {},
                    onCloseEditor = {},
                )
            }
        }
    }
}
