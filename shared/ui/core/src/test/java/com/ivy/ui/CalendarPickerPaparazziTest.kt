package com.ivy.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.ui.time.JvyCalendarPickerContent
import com.ivy.ui.time.CalendarDateBounds
import com.ivy.ui.time.rememberJvyCalendarState
import com.ivy.ui.time.calendarDateFromMillis
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

enum class CalendarWindow(val width: Int, val height: Int, val font: Float = 1f, val dark: Boolean = false, val amoled: Boolean = false) {
    SmallPhone(320, 640), PhoneDark(360, 800, dark = true), Amoled(360, 800, dark = true, amoled = true),
    LargeText(360, 800, 2f), Landscape(640, 360), Fold(720, 720, dark = true), DeX(1280, 800),
    FoldLargeText(720, 720, 2f, dark = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(TestParameterInjector::class)
class CalendarPickerPaparazziTest(@TestParameter private val window: CalendarWindow) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(
        screenWidth = window.width, screenHeight = window.height, density = com.android.resources.Density.MEDIUM,
        orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT), showSystemUi = false)
    @Test fun selectedDayAndConfirmationRemainReachable() = paparazzi.snapshot {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
            IvyMaterial3Theme(isTrueBlack = window.amoled, dark = window.dark) {
                val state = rememberJvyCalendarState(LocalDate.of(2027, 4, 23), CalendarDateBounds(), LocalDate.of(2026, 10, 26))
                JvyCalendarPickerContent("Deadline (optional)", state, {}, {})
            }
        }
    }

    @Test fun legacyRangeLimitsUseTheSameCalendar() = paparazzi.snapshot {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
            IvyMaterial3Theme(isTrueBlack = window.amoled, dark = window.dark) {
                val initial = LocalDate.of(2027, 4, 23)
                val bounds = CalendarDateBounds(initial.minusDays(2), initial.plusDays(2))
                val state = rememberJvyCalendarState(initial, bounds, initial)
                assertFalse(state.selectableDates.isSelectableDate(com.ivy.ui.time.calendarDateMillis(initial.minusDays(3))))
                assertTrue(state.selectableDates.isSelectableDate(com.ivy.ui.time.calendarDateMillis(initial)))
                JvyCalendarPickerContent("Select date", state, {}, {})
            }
        }
    }

    @Test fun invalidExistingDateNeedsAnExplicitNewSelection() = paparazzi.snapshot {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
            IvyMaterial3Theme(isTrueBlack = window.amoled, dark = window.dark) {
                val latest = LocalDate.of(2027, 4, 23)
                val state = rememberJvyCalendarState(latest.plusYears(1), CalendarDateBounds(latest = latest), latest)
                assertNull(state.selectedDateMillis)
                assertEquals(2027, calendarDateFromMillis(state.displayedMonthMillis).year)
                JvyCalendarPickerContent("Value date", state, {}, {})
            }
        }
    }

    @Test fun existingDateBeyond2100RemainsEditable() = paparazzi.snapshot {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
            IvyMaterial3Theme(isTrueBlack = window.amoled, dark = window.dark) {
                val date = LocalDate.of(2150, 4, 23)
                val state = rememberJvyCalendarState(date, CalendarDateBounds(), date)
                assertEquals(date, calendarDateFromMillis(state.selectedDateMillis!!))
                JvyCalendarPickerContent("Deadline (optional)", state, {}, {})
            }
        }
    }
}
