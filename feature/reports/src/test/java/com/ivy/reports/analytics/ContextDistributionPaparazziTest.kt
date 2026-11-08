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
import com.ivy.base.analytics.Ink
import com.ivy.design.system.FinancialVisibility
import com.ivy.design.system.LocalFinancialVisibility
import com.ivy.design.system.chart.JvyContextCharts
import com.ivy.design.system.chart.JvyAnalyticsCard
import com.ivy.domain.usecase.analytics.ContextCharts
import com.ivy.domain.usecase.analytics.MoneyPosition
import com.ivy.domain.usecase.analytics.PrincipalEvent
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
@RunWith(TestParameterInjector::class)
class ContextDistributionPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val positions = listOf(MoneyPosition("Everyday bank", "AED", BigDecimal("10426.50")),
        MoneyPosition("Credit card", "AED", BigDecimal("-1200.00")), MoneyPosition("Travel cash", "USD", BigDecimal("280.25")))
    @Test fun accountBalancesHaveSignedValuesAndSeparateCurrencies() = render {
        JvyContextCharts(ContextCharts.positions("accounts", "Where your everyday money is", "Current balances; currencies stay separate, never added together.", positions))
    }
    @Test fun budgetAndGoalPercentagesKeepOverfundingAndUnknownValues() = render {
        JvyContextCharts(ContextCharts.positions("progress", "Progress compared", "Each target separately. Unknown targets are not zero.",
            listOf(MoneyPosition("Emergency fund", "%", BigDecimal(150)), MoneyPosition("Travel", "%", BigDecimal(25)),
                MoneyPosition("Needs review", "%", null)), first = "Progress", ink = Ink.INCOME))
    }
    @Test fun scheduledCashFlowIsLabelledAsProjection() = render {
        JvyAnalyticsCard(ContextCharts.bridge("forecast", "Current to projected balance", "Scheduled payments, not recorded history or guaranteed money.", "AED",
            BigDecimal("1000"), BigDecimal("-1250"), BigDecimal("-250")))
    }
    @Test fun principalHistoryHasNoDotsAndDoesNotCountInterestAsPrincipal() = render {
        val day = LocalDate.parse("2026-11-07")
        JvyAnalyticsCard(ContextCharts.principal("loan", "Principal remaining over time", "AED", BigDecimal(5000), day.minusDays(90), day,
            listOf(PrincipalEvent(day.minusDays(30), BigDecimal(1000), false, false), PrincipalEvent(day, BigDecimal(100), false, true))))
    }
    private fun text(node: SemanticsNode): List<String> = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } + node.children.flatMap(::text)
    @Test fun hiddenContextRemovesAmountsPlotsAndDisclosure() {
        var checked = false
        render {
            val view = LocalView.current
            CompositionLocalProvider(LocalFinancialVisibility provides FinancialVisibility(true)) {
                Box(Modifier.drawWithContent {
                    drawContent()
                    val labels = text((view as RootForTest).semanticsOwner.unmergedRootSemanticsNode)
                    assertFalse(labels.any { it.contains("10,426") || it.contains("1,200") || it == "View values" })
                    assertTrue(labels.contains(FinancialVisibility.REDACTED))
                    checked = true
                }) { JvyContextCharts(ContextCharts.positions("accounts", "Accounts", "Native balances", positions)) }
            }
        }
        assertTrue(checked)
    }
}
