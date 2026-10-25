package com.ivy.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.ui.testing.JvyAdaptivePreviewTest
import com.ivy.ui.testing.JvyPreviewWindow
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** Candidate animation renders, not device frame-time benchmarks or approved screenshot goldens. */
class HomeTrendMotionPaparazziTest : JvyAdaptivePreviewTest(JvyPreviewWindow.Phone) {
    private fun state(monthly: Boolean): HomeTrendState {
        val count = if (monthly) 3 else 7
        return HomeTrendState(range = if (monthly) HomeTrendRange.THREE_MONTHS else HomeTrendRange.SEVEN_DAYS,
            points = (1..count).map { index ->
                HomeTrendPoint(if (monthly) "Month $index" else "Oct $index", BigDecimal(index * 100),
                    BigDecimal(index * 40), BigDecimal(5000 + index * 60), periodIdentity =
                    if (monthly) HomeTrendPeriodIdentity.month(YearMonth.of(2026, 7 + index))
                    else HomeTrendPeriodIdentity.day(LocalDate.of(2026, 10, index)))
            }.toImmutableList())
    }

    private fun motion(hideDuringUpdate: Boolean, rapid: Boolean) {
        val daily = state(false)
        val monthly = state(true)
        var target by mutableStateOf(daily)
        var hidden by mutableStateOf(false)
        val view = ComposeView(paparazzi.context).apply {
            setContent {
                IvyMaterial3Theme(isTrueBlack = true, dark = true) {
                    Surface(Modifier.fillMaxSize()) {
                        HomeTrendCard(target, "AED", hidden, {}, {}, {}, {})
                    }
                }
                LaunchedEffect(Unit) {
                    delay(100)
                    target = monthly
                    if (hideDuringUpdate || rapid) {
                        delay(60)
                        if (hideDuringUpdate) hidden = true else target = daily
                    }
                }
            }
        }
        try {
            paparazzi.gif(view, end = 700, fps = 20)
            assertEquals(if (rapid) daily else monthly, target)
            if (hideDuringUpdate) assertTrue(hidden)
        } finally {
            view.disposeComposition()
        }
    }

    @Test fun dailyToMonthly() = motion(hideDuringUpdate = false, rapid = false)
    @Test fun latestTargetWins() = motion(hideDuringUpdate = false, rapid = true)
    @Test fun hideDuringTransition() = motion(hideDuringUpdate = true, rapid = false)
}
