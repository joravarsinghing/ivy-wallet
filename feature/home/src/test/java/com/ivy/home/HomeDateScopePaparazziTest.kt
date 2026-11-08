package com.ivy.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
@RunWith(TestParameterInjector::class)
class HomeDateScopePaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private val scope = HomeDateWindow.ending(HomeTrendRange.THIRTY_DAYS, LocalDate.of(2026, 11, 3))
    private val account = Account(name = "Fictional bank", color = 0, currency = "AED")
    private val emptyDue = LegacyDueSection(persistentListOf(), true, IncomeExpensePair.zero())

    private fun text(node: SemanticsNode): List<String> = buildList {
        addAll(node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text })
        node.children.forEach { addAll(text(it)) }
    }

    @Test fun `the same explicit date scope is composed on all three cards`() {
        var labels = emptyList<String>()
        render {
            IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
                val view = LocalView.current
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).drawWithContent {
                    drawContent()
                    labels = text((view as RootForTest).semanticsOwner.unmergedRootSemanticsNode)
                }, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeTrendCard(HomeTrendState(window = scope), "AED", false, {}, {}, {}, {})
                    HomeSpendingBreakdownCard(HomeSpendingBreakdownState(), "AED", false, dateWindow = scope)
                    HomeCompactActivity(AppBaseData("AED", persistentListOf(account), persistentListOf()),
                        emptyDue, emptyDue, emptyList(), false, {}, {}, {}, dateWindow = scope)
                }
            }
        }
        assertEquals(3, labels.count { it.contains(scope.displayLabel()) })
        assertTrue(labels.any { it.contains("No activity in this period") })
        assertFalse(labels.contains("Planned payments"))
    }

    @Test fun `upcoming actions remain a separate panel even with empty selected history`() {
        var labels = emptyList<String>()
        val upcoming = emptyDue.copy(trns = persistentListOf(Transaction(account.id, TransactionType.EXPENSE,
            BigDecimal("42.25"), title = "Fictional upcoming payment", dueDate = Instant.parse("2026-11-10T12:00:00Z"))))
        render {
            IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
                val view = LocalView.current
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).drawWithContent {
                    drawContent()
                    labels = text((view as RootForTest).semanticsOwner.unmergedRootSemanticsNode)
                }) {
                    HomeCompactActivity(AppBaseData("AED", persistentListOf(account), persistentListOf()),
                        upcoming, emptyDue, emptyList(), false, {}, {}, {}, dateWindow = scope)
                }
            }
        }
        assertTrue(labels.indexOf("Planned payments") in 0 until labels.indexOf("Recent activity"))
        assertTrue(labels.contains("Fictional upcoming payment"))
        assertTrue(labels.contains("Pay"))
        assertTrue(labels.any { it.contains("No activity in this period") })
    }
}
