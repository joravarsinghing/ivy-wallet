package com.ivy.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
@RunWith(TestParameterInjector::class)
class HomeTrendFaceliftPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun find(node: SemanticsNode, tag: String): SemanticsNode? =
        if (node.config.getOrNull(SemanticsProperties.TestTag) == tag) node
        else node.children.firstNotNullOfOrNull { find(it, tag) }
    private fun preview(negative: Boolean = false, gaps: Boolean = false, hidden: Boolean = false) {
        val start = LocalDate.parse("2026-10-01")
        val balances = listOf(1200, 1550, 1480, 1700, 1610, 2100, 2350, 2250, 2400, 1980,
            2160, 2680, 2500, 2380, 2450, 2600, 2750, 2490, 2640, 2410, 2590, 2800, 2510, 2670,
            2560, 2730, 2620, 2800, 2690, 2880)
        val points = balances.mapIndexed { i, balance ->
            val date = start.plusDays(i.toLong())
            HomeTrendPoint(date.toString(), BigDecimal(listOf(40, 240, 760, 360, 120, 540, 100)[i % 7]),
                BigDecimal(listOf(80, 180, 260, 140, 590, 300, 60)[i % 7]) * if (negative) BigDecimal.ONE.negate() else BigDecimal.ONE,
                BigDecimal(if (negative) balance - 2200 else balance), isObserved = !gaps || i !in 10..13,
                periodIdentity = HomeTrendPeriodIdentity.Day(date))
        }
        var inspected = false
        render {
            IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
                val view = LocalView.current
                HomeTrendCard(HomeTrendState(points = points.toImmutableList(),
                    window = HomeDateWindow(start, start.plusDays(29))), "AED", hidden, {}, {}, {}, {},
                    Modifier.drawWithContent {
                        drawContent()
                        val root = (view as RootForTest).semanticsOwner.unmergedRootSemanticsNode
                        val axis = find(root, "home_trend_axis")
                        if (hidden) {
                            assertNull(axis)
                            assertNull(find(root, "home_trend_plot"))
                        } else {
                            val ticks = checkNotNull(axis).boundsInRoot
                            val plot = checkNotNull(find(root, "home_trend_plot")).boundsInRoot
                            val dates = checkNotNull(find(root, "home_trend_dates")).boundsInRoot
                            assertTrue("Right-hand axis must not cover the plot", ticks.left >= plot.right)
                            assertTrue("Axis must leave most of the width for the actual graph", ticks.width < plot.width)
                            assertTrue("Dates must align with the plotted domain", dates.right <= plot.right)
                            assertEquals(plot.top, ticks.top, 0.5f)
                            assertEquals(plot.bottom, ticks.bottom, 0.5f)
                        }
                        inspected = true
                    })
            }
        }
        assertTrue("Inspect real laid-out content, not a preview-only alternate renderer", inspected)
    }
    @Test fun nativeMarketStyleOverview() = preview()
    @Test fun negativeBalancesAndRefundsKeepTheSharedZero() = preview(negative = true)
    @Test fun unknownHistoryRemainsDisconnected() = preview(gaps = true)
    @Test fun hiddenValuesRemoveThePlotAndItsScale() = preview(hidden = true)
}
