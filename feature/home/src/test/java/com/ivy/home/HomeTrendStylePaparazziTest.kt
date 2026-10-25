package com.ivy.home

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class HomeTrendStylePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun snapshot(style: HomeTrendPlotStyle, hidden: Boolean = false, gap: Boolean = false, single: Boolean = false, negative: Boolean = false) = render {
        var runningBalance = BigDecimal("6200")
        val points = (0 until 30).map { day ->
            val income = if (day == 2) BigDecimal("3100.75") else BigDecimal.ZERO
            val spent = BigDecimal(if (day % 6 == 0) "65.25" else "0")
            runningBalance += income - spent
            HomeTrendPoint("Oct ${day + 1}", income, spent, runningBalance.let { if (negative) it.negate() else it },
                isObserved = if (single) day == 29 else !gap || day !in 10..14, periodIdentity = HomeTrendPeriodIdentity.day(LocalDate.of(2026, 10, day + 1)))
        }
        HomeTrendCard(HomeTrendState(points = points.toImmutableList()), "AED", hidden, {}, {}, {}, {}, plotStyle = style)
    }
    @Test fun cleanLine() = snapshot(HomeTrendPlotStyle.Line)
    @Test fun shadedLine() = snapshot(HomeTrendPlotStyle.ShadedLine)
    @Test fun missingHistory() = snapshot(HomeTrendPlotStyle.ShadedLine, gap = true)
    @Test fun hiddenBalances() = snapshot(HomeTrendPlotStyle.ShadedLine, hidden = true)
    @Test fun singleObservation() = snapshot(HomeTrendPlotStyle.ShadedLine, single = true)
    @Test fun negativeBalance() = snapshot(HomeTrendPlotStyle.ShadedLine, negative = true)
}
