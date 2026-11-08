package com.ivy.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.ui.time.JvyTimePickerContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(TestParameterInjector::class)
class TimePickerPaparazziTest(@TestParameter private val window: CalendarWindow) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(
        screenWidth = window.width, screenHeight = window.height, density = com.android.resources.Density.MEDIUM,
        orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT), showSystemUi = false)
    private fun time(hour: Int, minute: Int, is24Hour: Boolean) = paparazzi.snapshot {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
            IvyMaterial3Theme(isTrueBlack = window.amoled, dark = window.dark) {
                val state = rememberTimePickerState(hour, minute, is24Hour)
                JvyTimePickerContent(state, {}, {})
            }
        }
    }
    @Test fun twentyFourHourLateEvening() = time(23, 59, true)
    @Test fun twelveHourAfternoon() = time(13, 25, false)
    @Test fun twelveHourMidnight() = time(0, 0, false)
}
