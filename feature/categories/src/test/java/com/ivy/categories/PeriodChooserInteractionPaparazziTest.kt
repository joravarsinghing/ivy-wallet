package com.ivy.categories

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.HtmlReportWriter
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.IntervalType
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.LastNTimeRange
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.ui.testing.JvyPreviewWindow
import com.ivy.wallet.ui.theme.modal.PeriodChooserContent
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import java.time.*

/** Invokes real Compose control actions and checks their laid-out bounds; not physical-device taps. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
@RunWith(TestParameterInjector::class)
class PeriodChooserInteractionPaparazziTest(@TestParameter private val window: JvyPreviewWindow) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(
        screenWidth = window.width, screenHeight = window.height, density = com.android.resources.Density.MEDIUM,
        orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT),
        showSystemUi = false, snapshotHandler = FinalFrameReport())
    private val time = object : TimeProvider {
        override fun utcNow() = Instant.parse("2026-11-04T12:00:00Z")
        override fun getZoneId() = ZoneId.of("UTC")
        override fun localNow() = LocalDateTime.ofInstant(utcNow(), getZoneId())
        override fun localDateNow() = localNow().toLocalDate()
        override fun localTimeNow() = localNow().toLocalTime()
    }
    private fun find(root: SemanticsNode, tag: String): SemanticsNode? =
        if (root.config.getOrNull(SemanticsProperties.TestTag) == tag) root
        else root.children.firstNotNullOfOrNull { find(it, tag) }
    private fun scrollContainer(root: SemanticsNode): SemanticsNode? =
        if (root.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) != null) root
        else root.children.firstNotNullOfOrNull(::scrollContainer)

    private fun exercise(close: Boolean, invalidRelative: Boolean = false, scrollToAllTime: Boolean = false,
                         selectRelative: Boolean = false) {
        var applied: TimePeriod? = null
        var applies = 0
        var closed = false
        var exercised = false
        val initial = if (invalidRelative) TimePeriod(lastNRange = LastNTimeRange(0, IntervalType.WEEK))
            else TimePeriod(Month(11, "November"), 2026)
        val view = ComposeView(paparazzi.context).apply {
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, window.font)) {
                    IvyMaterial3Theme(dark = window.dark, isTrueBlack = window.amoled) {
                        IvyWalletPreview(if (window.amoled) Theme.AMOLED_DARK else if (window.dark) Theme.DARK else Theme.LIGHT) {
                            val actualView = LocalView.current
                            CompositionLocalProvider(LocalTimeProvider provides time) {
                                PeriodChooserContent(initial, { closed = true }, { applies++; applied = it })
                                LaunchedEffect(Unit) {
                                    fun root() = (actualView as RootForTest).semanticsOwner.unmergedRootSemanticsNode
                                    fun node(tag: String) = checkNotNull(find(root(), tag)) { "Missing $tag" }
                                    fun click(tag: String) {
                                        val target = node(tag)
                                        assertNull(target.config.getOrNull(SemanticsProperties.Disabled))
                                        assertTrue(target.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke() == true)
                                    }
                                    delay(100)
                                    if (invalidRelative) assertNotNull(node("period_apply").config.getOrNull(SemanticsProperties.Disabled))
                                    click("period_month_2026_10")
                                    delay(100)
                                    assertEquals(0, applies) // selection is a draft, not an implicit apply
                                    if (selectRelative) {
                                        click("period_relative_choice")
                                        delay(100)
                                    }
                                    if (scrollToAllTime) {
                                        val body = checkNotNull(scrollContainer(root()))
                                        val maximum = body.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)!!.maxValue()
                                        if (window == JvyPreviewWindow.Landscape || window == JvyPreviewWindow.LargeText)
                                            assertTrue("Short or large-text body must be scrollable", maximum > 0)
                                        if (maximum > 0) {
                                            assertTrue(body.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f, maximum) == true)
                                            delay(1000)
                                        }
                                        val option = node("period_all_time").boundsInRoot
                                        assertTrue("All-time option must be reachable", option.height >= 48 && option.top >= 0)
                                        assertTrue(option.bottom <= node("period_apply").boundsInRoot.top)
                                        click("period_all_time")
                                        delay(100)
                                    }
                                    val footer = node("period_apply").boundsInRoot
                                    assertTrue("Apply must stay inside viewport: $footer", footer.top >= 0 && footer.bottom <= window.height)
                                    assertTrue(footer.height >= 48f)
                                    click(if (close) "modal_close_button" else "period_apply")
                                    exercised = true
                                }
                            }
                        }
                    }
                }
            }
        }
        try { paparazzi.gif(view, end = if (scrollToAllTime) 1500 else 400, fps = 10) } finally { view.disposeComposition() }
        assertTrue("The control interaction must actually run", exercised)
        if (close) {
            assertTrue(closed)
            assertEquals(0, applies)
            assertNull(applied)
        } else if (selectRelative) {
            assertEquals(1, applies)
            assertNull(applied!!.month)
            assertEquals(1, applied!!.lastNRange!!.periodN)
            assertEquals(IntervalType.WEEK, applied!!.lastNRange!!.periodType)
        } else if (scrollToAllTime) {
            assertEquals(1, applies)
            assertNull(applied!!.month)
            assertNotNull(applied!!.fromToRange)
            assertNull(applied!!.fromToRange!!.from)
            assertNull(applied!!.fromToRange!!.to)
        } else {
            assertEquals(1, applies)
            assertEquals(10, applied!!.month!!.monthValue)
            assertEquals(2026, applied!!.year)
            assertTrue(applied!!.month!!.name.isNotBlank())
        }
    }
    @Test fun octoberRequiresAndReceivesExplicitApply() = exercise(false)
    @Test fun closeCancelsOctoberDraft() = exercise(true)
    @Test fun choosingMonthOverridesInvalidUnrelatedRelativeCount() = exercise(false, true)
    @Test fun lowerOptionsScrollIntoViewWithoutMovingApply() = exercise(false, scrollToAllTime = true)
    @Test fun prefilledOneWeekCanBeSelectedWithoutEditingTheNumber() = exercise(false, selectRelative = true)
}

/** Advance every interaction frame, but write a PNG: DeX's 1000x625 thumbnail is invalid YUV video. */
private class FinalFrameReport : SnapshotHandler {
    private val report = HtmlReportWriter()
    override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int): SnapshotHandler.FrameHandler {
        val last = report.newFrameHandler(snapshot, 1, fps)
        var index = 0
        // Android's test compile classpath omits java.desktop, although Paparazzi's
        // JVM runtime supplies BufferedImage. Bridge its frame callback without
        // adding desktop/JDK dependencies to the Android module.
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
