package com.ivy.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.ui.theme.Black
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.findContrastTextColor
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.UUID

/** Actual Home rows, including the fallback glyph and uncategorized background. */
@RunWith(TestParameterInjector::class)
class HomeActivityIconContrastTest(
    @TestParameter window: JvyPreviewWindow,
) : JvyAdaptivePreviewTest(window) {
    @Test fun categoryIconsUseTheTransactionContrastPolicy() {
        val colors = listOf("Transport · yellow" to Color(0xFFFFF59A),
            "Shopping · light background" to Color(0xFFFAFAFA),
            "Entertainment · dark purple" to Color(0xFF352680),
            "Bills · dark background" to Color(0xFF111114))
        assertEquals(Black, findContrastTextColor(colors[0].second))
        assertEquals(Black, findContrastTextColor(colors[1].second))
        assertEquals(White, findContrastTextColor(colors[2].second))
        assertEquals(White, findContrastTextColor(colors[3].second))
        val categories = colors.mapIndexed { index, (name, color) -> Category(
            CategoryId(UUID(0, index.toLong() + 1)), NotBlankTrimmedString.from(name).getOrNull()!!,
            ColorInt(color.toArgb()), icon = if (index % 2 == 0) IconAsset.from("transport").getOrNull() else null,
            orderNum = index.toDouble()) }
        val base = AppBaseData("AED", persistentListOf(), categories.toImmutableList())
        render {
            IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    categories.forEach { category -> CompactActivityRow(
                        Transaction(UUID(0, 99), TransactionType.EXPENSE, BigDecimal("45.50"),
                            title = category.name.value, categoryId = category.id.value), base, false, onClick = {}) }
                    CompactActivityRow(Transaction(UUID(0, 99), TransactionType.INCOME, BigDecimal.TEN,
                        title = "Uncategorized · fallback"), base, false, onClick = {})
                }
            }
        }
    }
}
