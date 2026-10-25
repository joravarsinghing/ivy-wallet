package com.ivy.ui.testing

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import com.ivy.design.system.IvyMaterial3Theme
import org.junit.Rule

enum class JvyPreviewWindow(val width: Int, val height: Int, val font: Float = 1f, val dark: Boolean = false, val amoled: Boolean = false) {
    SmallPhone(320, 640), Phone(360, 800), Fold(720, 720, dark = true), DeX(1280, 800),
    LargeText(360, 800, 2f), FoldLargeText(720, 720, 2f, true), Landscape(640, 360), Amoled(360, 800, dark = true, amoled = true)
}

open class JvyAdaptivePreviewTest(protected val window: JvyPreviewWindow) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(screenWidth = window.width,
        screenHeight = window.height, density = com.android.resources.Density.MEDIUM,
        orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT), showSystemUi = false)
    protected fun render(content: @Composable BoxWithConstraintsScope.() -> Unit) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(isTrueBlack = window.amoled, dark = window.dark) {
                    Surface(Modifier.fillMaxSize()) { BoxWithConstraints(Modifier.fillMaxSize(), content = content) }
                }
            }
        }
    }
}
