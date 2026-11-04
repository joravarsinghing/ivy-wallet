package com.ivy.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.system.FinancialVisibility
import com.ivy.design.system.LocalFinancialVisibility
import com.ivy.design.l0_system.UI
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.component.IncomeExpensesCards
import com.ivy.legacy.ui.component.transaction.HistoryDateDivider
import com.ivy.legacy.ui.component.transaction.SectionDivider
import com.ivy.legacy.ui.component.transaction.TransactionCard
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.Locale

/** Actual composed text/accessible descriptions, not just a formatter or source check. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
@RunWith(TestParameterInjector::class)
class ReadOnlyFinancialPrivacyPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    private var previousLocale = Locale.getDefault()
    @Before fun setLocale() { previousLocale = Locale.getDefault(); Locale.setDefault(Locale.US) }
    @After fun restoreLocale() { Locale.setDefault(previousLocale) }
    private fun collectText(node: SemanticsNode): List<String> = buildList {
        if (node.config.getOrNull(SemanticsProperties.TestTag) == "transaction_card") {
            assertTrue("Transfer header expanded beyond a usable card: ${node.size.height}px",
                node.size.height < window.height * 0.9f)
        }
        addAll(node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text })
        addAll(node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty())
        node.config.getOrNull(SemanticsProperties.StateDescription)?.let(::add)
        node.children.forEach { addAll(collectText(it)) }
    }

    private fun checkedRender(hidden: Boolean, amounts: List<String>, content: @Composable () -> Unit): String {
        var captured: String? = null
        render {
            IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
                CompositionLocalProvider(LocalFinancialVisibility provides FinancialVisibility(hidden)) {
                    val view = LocalView.current
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).drawWithContent {
                        drawContent()
                        // BoxWithConstraints subcomposes during measurement; inspecting in
                        // SideEffect would miss its actual text before the first layout.
                        val root = checkNotNull(view as? RootForTest) { "Expected the actual Compose semantics root" }
                        captured = collectText(root.semanticsOwner.unmergedRootSemanticsNode).joinToString("\n")
                    }) { content() }
                }
            }
        }
        val text = checkNotNull(captured) { "Semantics were not inspected" }
        amounts.forEach { amount ->
            if (hidden) assertFalse("Private value exposed: $amount", text.contains(amount))
            else assertTrue("Visible value missing: $amount", text.contains(amount))
        }
        if (hidden) assertTrue(text.contains(FinancialVisibility.REDACTED))
        return text
    }

    private fun summaries(hidden: Boolean) {
        val text = checkedRender(hidden, listOf("98,765.43", "12,345.67", "8,764.12", "4,567.89", "1,234.56")) {
            IncomeExpensesCards(persistentListOf(), "AED", 98765.43, 12345.67, false, UI.colors.pure)
            HistoryDateDivider(LocalDate.of(2026, 10, 31), 16.dp, "AED", 8890.12, 126.0)
            SectionDivider(true, "Upcoming", Gray, "AED", 4567.89, 1234.56) {}
            SectionDivider(false, "Overdue", Gray, "AED", 4567.89, 1234.56) {}
        }
        assertTrue(text.contains("Expanded"))
        assertTrue(text.contains("Collapsed"))
    }

    private fun transfer(hidden: Boolean, crypto: Boolean = false, colored: Boolean = false) {
        val source = Account(name = "Fictional AED wallet", currency = "AED", color = 0xfff5cb45.toInt())
        val destination = Account(name = "Fictional receiving wallet", currency = if (crypto) "BTC" else "USD", color = 0xff527fe4.toInt())
        val receiving = if (crypto) "0.000000000001" else "43210.98"
        checkedRender(hidden, listOf("54,321.09", if (crypto) "0.000000000001" else "43,210.98")) {
            TransactionCard(AppBaseData("AED", persistentListOf(source, destination), persistentListOf()),
                Transaction(source.id, TransactionType.TRANSFER, BigDecimal("54321.09"),
                    toAccountId = destination.id, toAmount = BigDecimal(receiving),
                    dateTime = Instant.parse("2026-10-31T09:00:00Z")),
                colored, {}, onClick = {})
        }
    }

    private fun category(hidden: Boolean) {
        val category = Category(CategoryId(UUID.randomUUID()),
            NotBlankTrimmedString.unsafe("Fictional family transport and everyday travel"),
            ColorInt(0xfff5cb45.toInt()), null, 0.0)
        checkedRender(hidden, listOf("87,654.32")) {
            CompactCategoryCard(CategoryData(category, 87654.32, 123.0, 87777.32),
                findContrastTextColor(category.color.value.toComposeColor()), "AED") {}
        }
    }

    @Test fun summariesVisible() = summaries(false)
    @Test fun summariesHidden() = summaries(true)
    @Test fun transferVisible() = transfer(false)
    @Test fun transferHidden() = transfer(true)
    @Test fun tinyCryptoTransferVisible() = transfer(false, crypto = true)
    @Test fun tinyCryptoTransferHidden() = transfer(true, crypto = true)
    @Test fun coloredTransferVisible() = transfer(false, colored = true)
    @Test fun coloredTransferHidden() = transfer(true, colored = true)
    @Test fun categoryVisible() = category(false)
    @Test fun categoryHidden() = category(true)
}
