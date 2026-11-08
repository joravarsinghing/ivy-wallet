package com.ivy.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.*
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@RunWith(TestParameterInjector::class)
class HomeCoherencePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun state(): HomeState {
        val due = LegacyDueSection(trns = persistentListOf(), stats = IncomeExpensePair.zero(), expanded = false)
        val from = LocalDate.parse("2026-10-10")
        var balance = BigDecimal("8299.20")
        val points = (0..29).map { i -> HomeTrendPoint("Day ${i + 1}", BigDecimal((i % 5) * 100),
            BigDecimal((i % 4) * 35), (balance + BigDecimal((i % 5) * 100 - (i % 4) * 35)).also { balance = it },
            periodIdentity = HomeTrendPeriodIdentity.Day(from.plusDays(i.toLong()))) }
        return HomeState(Theme.AUTO, "Joravar", TimePeriod(), AppBaseData("AED", persistentListOf(), persistentListOf()),
            persistentListOf(), IncomeExpensePair(points.sumOf { it.income }, points.sumOf { it.expenses }), points.last().balance,
            BufferInfo(BigDecimal("2500.00"), BigDecimal.ZERO), due, due, persistentListOf(), false, false, true, false,
            periodSelection = HomePeriodSelection.Rolling(30, com.ivy.data.model.IntervalType.DAY),
            appliedPeriodSelection = HomePeriodSelection.Rolling(30, com.ivy.data.model.IntervalType.DAY),
            trend = HomeTrendState(points = points.toImmutableList(), window = HomeDateWindow(from, from.plusDays(29))))
    }
    @Test fun compactHomeAndColoredSummary() = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) { HomeUi(state(), {}) }
    }
    @Test fun safeToSpendCanLeadTheDashboard() = render {
        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
            HomeUi(state().copy(dashboardOrder = normalizedHomeCardOrder(listOf("safe_to_spend"))), {})
        }
    }
}
