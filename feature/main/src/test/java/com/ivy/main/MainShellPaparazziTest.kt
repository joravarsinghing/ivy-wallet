package com.ivy.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density as ScreenDensity
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.legacy.data.model.MainTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class MainShellPaparazziTest(@TestParameter private val window: Window) {
    enum class Window(val width: Int, val height: Int, val font: Float = 1f) { SmallPhone(320, 640), Phone(360, 800), Fold(720, 720), DeX(1280, 800), LargeText(360, 800, 2f) }
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(screenWidth = window.width, screenHeight = window.height,
        density = ScreenDensity.MEDIUM, orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT), showSystemUi = false)
    private fun snapshot(planning: Boolean) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(dark = window == Window.Fold, isTrueBlack = false) {
                    Surface(Modifier.fillMaxSize()) {
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val rail = mainNavigationRailWidth(maxWidth)
                            Box(Modifier.fillMaxSize().padding(start = rail)) { MainToolsHubContent(planning, {}) }
                            MainNavigation(if (planning) MainTab.PLAN else MainTab.MORE, {}, rail,
                                Modifier.align(if (rail.value == 0f) Alignment.BottomCenter else Alignment.CenterStart))
                        }
                    }
                }
            }
        }
    }
    @Test fun planning() = snapshot(true)
    @Test fun allTools() = snapshot(false)
}
