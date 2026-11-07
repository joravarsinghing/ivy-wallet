package com.ivy.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.*
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.data.model.IntervalType
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.*
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
@RunWith(TestParameterInjector::class)
class HomeTimelineInteractionPaparazziTest(@TestParameter private val window: JvyPreviewWindow) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(screenWidth = window.width,
        screenHeight = window.height, density = com.android.resources.Density.MEDIUM,
        orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT),
        showSystemUi = false, snapshotHandler = HomeFinalFrameReport())
    private fun find(root: SemanticsNode, tag: String): SemanticsNode? =
        if (root.config.getOrNull(SemanticsProperties.TestTag) == tag) root else root.children.firstNotNullOfOrNull { find(it, tag) }

    @Test fun sharedPeriodControlsDispatchWithoutAnyOptionalCards() {
        var actions = 0
        var exercised = false
        val bounds = HomeDateWindow(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-04"))
        val due = LegacyDueSection(trns = persistentListOf(), stats = IncomeExpensePair.zero(), expanded = false)
        val original = HomeState(Theme.AUTO, "Fictional user", TimePeriod(), AppBaseData("AED", persistentListOf(), persistentListOf()),
            persistentListOf(), IncomeExpensePair(BigDecimal("100.00"), BigDecimal("20.00")), BigDecimal("12844.20"),
            BufferInfo(BigDecimal.ZERO, BigDecimal.ZERO), due, due, persistentListOf(), false, false, true, false,
            trend = HomeTrendState(window = bounds), visibleDashboardCards = emptySet())
        val view = ComposeView(paparazzi.context).apply {
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, window.font)) {
                    IvyMaterial3Theme(dark = window.dark, isTrueBlack = window.amoled) {
                        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
                            val actualView = LocalView.current
                            var state by remember { mutableStateOf(original) }
                            HomeUi(state, { event ->
                                if (event is HomeEvent.SetHomePeriod) {
                                    actions++
                                    assertEquals(HomePeriodSelection.Rolling(30, IntervalType.DAY), event.selection)
                                    state = state.copy(periodSelection = event.selection, trend = state.trend.copy(loading = true))
                                }
                            })
                            LaunchedEffect(Unit) {
                                delay(100)
                                fun root() = (actualView as RootForTest).semanticsOwner.unmergedRootSemanticsNode
                                assertNull(find(root(), "home_trend_card"))
                                val control = checkNotNull(find(root(), "home_period_rolling|30|DAY"))
                                assertNull(control.config.getOrNull(SemanticsProperties.Disabled))
                                assertTrue(control.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke() == true)
                                delay(100)
                                assertEquals(bounds, state.trend.window) // pending request must not relabel previous result
                                assertEquals(IncomeExpensePair(BigDecimal("100.00"), BigDecimal("20.00")), state.stats)
                                assertNotNull(find(root(), "home_period_chooser"))
                                assertNull(find(root(), "home_trend_card"))
                                exercised = true
                            }
                        }
                    }
                }
            }
        }
        try { paparazzi.gif(view, end = 400, fps = 10) } finally { view.disposeComposition() }
        assertTrue(exercised)
        assertEquals(1, actions)
    }
}

/** Exercise every frame without the odd-height DeX YUV video limitation. */
private class HomeFinalFrameReport : SnapshotHandler {
    private val report = HtmlReportWriter()
    override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int): SnapshotHandler.FrameHandler {
        val last = report.newFrameHandler(snapshot, 1, fps)
        var index = 0
        return java.lang.reflect.Proxy.newProxyInstance(SnapshotHandler.FrameHandler::class.java.classLoader,
            arrayOf(SnapshotHandler.FrameHandler::class.java)) { _, method, arguments ->
            when (method.name) {
                "handle" -> if (++index == frameCount) method.invoke(last, *(arguments ?: emptyArray())) else null
                "close" -> last.close()
                else -> method.invoke(last, *(arguments ?: emptyArray()))
            }
        } as SnapshotHandler.FrameHandler
    }
    override fun close() = report.close()
}
