package com.ivy.reports.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.design.system.FinancialVisibility
import com.ivy.design.system.LocalFinancialVisibility
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
@RunWith(TestParameterInjector::class)
class AnalyticsPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val start = LocalDate.parse("2026-10-01")
    private val rows = (0..29).map { i -> AnalyticsEntry(UUID.randomUUID(), start.plusDays(i.toLong()), i % 5 == 0,
        BigDecimal(if (i % 5 == 0) 1500 else i * 5 + 10), UUID(0, (i % 3).toLong()), "Category ${i % 3}", "Fictional title") }
    private fun cards() = AnalyticsEngine.cashFlow(rows, start, start.plusDays(29), "AED")
    private fun labels(node: SemanticsNode): List<String> = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } + node.children.flatMap(::labels)

    @Test fun cashFlowBars() = render { AnalyticsChartCard(cards().first()) }
    @Test fun spendingDonut() = render {
        AnalyticsChartCard(AnalyticsEngine.spending(rows, start, start.plusDays(29), "AED").first { it.kind == ChartKind.DONUT })
    }
    @Test fun cumulativeLine() = render {
        AnalyticsChartCard(AnalyticsEngine.spending(rows, start, start.plusDays(29), "AED").first { it.kind == ChartKind.LINE })
    }
    @Test fun waterfallKeepsItsZeroAndExactValues() = render { AnalyticsChartCard(cards().first { it.kind == ChartKind.WATERFALL }) }
    @Test fun calendarIncludesUnknownAndRefundStates() = render { AnalyticsChartCard(cards().first { it.kind == ChartKind.HEATMAP }) }
    @Test fun scatterHasReadableUnitsAndLowHistoryWarning() = render { AnalyticsChartCard(cards().first { it.kind == ChartKind.SCATTER }) }
    @Test fun purchaseDistributionShowsQuartileExplanation() = render {
        val purchases = (1..20).map { i -> rows.first().copy(id = UUID.randomUUID(), income = false, amount = BigDecimal(i)) }
        AnalyticsChartCard(AnalyticsEngine.spending(purchases, start, start.plusDays(29), "AED").first { it.kind == ChartKind.BOX })
    }
    @Test fun disconnectedObservationsExplainWhyThereIsNoLine() {
        var inspected = false
        render {
            val view = LocalView.current
            val sparse = ChartSeries("Net worth", Ink.BALANCE, listOf(ChartValue("First", BigDecimal.TEN),
                ChartValue("Missing", null), ChartValue("Latest", BigDecimal(20))))
            Box(Modifier.drawWithContent {
                drawContent()
                val texts = labels((view as RootForTest).semanticsOwner.unmergedRootSemanticsNode)
                assertTrue(texts.any { it.startsWith("Not enough consecutive observations") })
                inspected = true
            }) { AnalyticsChartCard(AnalyticsCard("sparse", "Sparse history", "Unknown dates stay disconnected.", ChartKind.LINE, listOf(sparse), "AED")) }
        }
        assertTrue(inspected)
    }
    @Test fun controlsWrapAndRetainFiveGroups() = render {
        Column {
            ReportModeControls(true, {})
            ExploreControls(ExploreSection.SPENDING, {}, false, {})
        }
    }
    @Test fun hiddenAmountsRemovePlotAndValueDisclosure() {
        var inspected = false
        render {
            val view = LocalView.current
            CompositionLocalProvider(LocalFinancialVisibility provides FinancialVisibility(true)) {
                Box(Modifier.drawWithContent {
                    drawContent()
                    val texts = labels((view as RootForTest).semanticsOwner.unmergedRootSemanticsNode)
                    assertTrue(texts.contains(FinancialVisibility.REDACTED))
                    assertFalse(texts.any { it == "View values" || it.contains("1,500") || it.contains("9,000") })
                    inspected = true
                }) { AnalyticsChartCard(cards().first()) }
            }
        }
        assertTrue(inspected)
    }
}
