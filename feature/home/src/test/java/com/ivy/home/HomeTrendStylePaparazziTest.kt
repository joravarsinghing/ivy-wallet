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
    private fun snapshot(style: HomeTrendPlotStyle, hidden: Boolean = false, gap: Boolean = false, single: Boolean = false,
                         negative: Boolean = false, loading: Boolean = false,
                         metrics: Set<HomeTrendMetric> = HomeTrendMetric.entries.toSet()) = render {
        var runningBalance = BigDecimal("6200")
        val points = (0 until 30).map { day ->
            val income = if (day == 2) BigDecimal("3100.75") else BigDecimal.ZERO
            val spent = BigDecimal(if (day % 6 == 0) "65.25" else "0")
            runningBalance += income - spent
            HomeTrendPoint("Oct ${day + 1}", income, spent, runningBalance.let { if (negative) it.negate() else it },
                isObserved = if (single) day == 29 else !gap || day !in 10..14, periodIdentity = HomeTrendPeriodIdentity.day(LocalDate.of(2026, 10, day + 1)))
        }
        HomeTrendCard(HomeTrendState(points = points.toImmutableList(), loading = loading, visibleMetrics = metrics),
            "AED", hidden, {}, {}, {}, {}, plotStyle = style)
    }
    @Test fun cleanLine() = snapshot(HomeTrendPlotStyle.Line)
    @Test fun shadedLine() = snapshot(HomeTrendPlotStyle.ShadedLine)
    @Test fun missingHistory() = snapshot(HomeTrendPlotStyle.ShadedLine, gap = true)
    @Test fun hiddenBalances() = snapshot(HomeTrendPlotStyle.ShadedLine, hidden = true)
    @Test fun singleObservation() = snapshot(HomeTrendPlotStyle.ShadedLine, single = true)
    @Test fun negativeBalance() = snapshot(HomeTrendPlotStyle.ShadedLine, negative = true)
    @Test fun updating() = snapshot(HomeTrendPlotStyle.ShadedLine, loading = true)
    @Test fun balanceOnly() = snapshot(HomeTrendPlotStyle.ShadedLine, metrics = setOf(HomeTrendMetric.BALANCE))
    @Test fun spendingOnly() = snapshot(HomeTrendPlotStyle.ShadedLine, metrics = setOf(HomeTrendMetric.EXPENSES))

    @Test fun referenceComposition() = render {
        // Fictional ledger-consistent samples with comparable magnitudes reveal the composition.
        // The production chart never substitutes these values or inflates small real flows.
        val incomes = listOf(180, 760, 320, 980, 420, 650, 120, 1100, 480, 900)
        val expenses = listOf(90, 220, 460, 200, 620, 440, 730, 300, 680, 1050)
        var balance = 1100
        val points = incomes.indices.map { index ->
            balance += incomes[index] - expenses[index]
            HomeTrendPoint("Oct ${index + 1}", BigDecimal(incomes[index]), BigDecimal(expenses[index]),
                BigDecimal(balance), periodIdentity = HomeTrendPeriodIdentity.day(LocalDate.of(2026, 10, index + 1)))
        }
        HomeTrendCard(HomeTrendState(points = points.toImmutableList()), "AED", false, {}, {}, {}, {})
    }

    @Test fun flowsCrossBalance() = render {
        val points = (1..7).map { day ->
            HomeTrendPoint("Oct $day", BigDecimal(day * 160), BigDecimal((8 - day) * 120),
                BigDecimal(350 + day * 20), periodIdentity = HomeTrendPeriodIdentity.day(LocalDate.of(2026, 10, day)))
        }
        HomeTrendCard(HomeTrendState(points = points.toImmutableList()), "AED", false, {}, {}, {}, {})
    }
}
