package com.ivy.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.design.system.JvyLogo
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ui.component.IncomeExpensesCards
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.ui.theme.findContrastTextColor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
@RunWith(TestParameterInjector::class)
class BrandAndAccountContrastPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private fun nodes(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::nodes)
    @Test fun coloredCardsUseTheirOwnReadableActionInkAndLogosShareRoundedBacking() {
        val papers = listOf(Color(0xFF00E676), Color(0xFFFFFF00), Color(0xFF311B92))
        var inks = emptyList<Color>()
        render {
            IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
                val view = LocalView.current
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).drawWithContent {
                    drawContent()
                    inks = nodes((view as RootForTest).semanticsOwner.unmergedRootSemanticsNode).filter { node ->
                        node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text.equals("Add income", true) }
                    }.map { node ->
                        val layouts = mutableListOf<TextLayoutResult>()
                        check(node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts) == true)
                        layouts.single().layoutInput.style.color
                    }
                }, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth().background(Color(0xFF28282F)).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf(44.dp, 56.dp, 88.dp).forEach { JvyLogo(Modifier.size(it)) }
                    }
                    papers.forEach { paper ->
                        Column(Modifier.fillMaxWidth().background(paper)) {
                            IncomeExpensesCards(emptyList(), "AED", 1234.0, 234.0, true, paper)
                        }
                    }
                }
            }
        }
        assertEquals(papers.map(::findContrastTextColor), inks)
    }
}
