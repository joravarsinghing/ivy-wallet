package com.ivy.networth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.design.system.JvyButton
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.domain.usecase.networth.WealthCandidate
import com.ivy.data.model.wealth.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class WealthReviewPaparazziTest(@TestParameter window: JvyPreviewWindow) : JvyAdaptivePreviewTest(window) {
    @Test fun reviewKeepsSaveAboveTheWindowEdge() {
        var bounds: Rect? = null
        render {
            WealthDialogFrame({}, footer = {
                JvyButton({}, Modifier.fillMaxWidth().onGloballyPositioned { bounds = it.boundsInRoot() }) { Text("Save review") }
            }) {
                val sources = listOf("Everyday banking", "Cash", "Emergency fund", "A longer investment account name").mapIndexed { i, name ->
                    WealthCandidate("account-$i", name, "AED", WealthSource.ACCOUNT, WealthKind.CASH, null, BalanceConvention.ASSET)
                }
                WealthReviewBody(sources, LocalDate.of(2026, 10, 25), false, false, {}, {}, {})
            }
        }
        val button = requireNotNull(bounds)
        assertTrue("Save must have a 48dp target", button.height >= 48f)
        assertTrue("Save must not be below the viewport", button.bottom <= window.height + 0.5f)
        assertTrue("Save must be visible", button.top >= 0f)
    }
}
