package com.ivy.design.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

enum class HeaderWindow(val width: Int, val height: Int, val font: Float = 1f,
                        val dark: Boolean = false, val amoled: Boolean = false) {
    Small(320, 640), Phone(360, 800), Fold(720, 720, dark = true), DeX(1280, 800),
    LargeText(360, 800, 2f), FoldLargeText(720, 720, 2f, true),
    Landscape(640, 360), Amoled(360, 800, dark = true, amoled = true)
}

/** Candidate renders only: do not record goldens before owner visual approval. */
@RunWith(TestParameterInjector::class)
class StickyHeaderPaparazziTest(@TestParameter private val window: HeaderWindow) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(
        screenWidth = window.width, screenHeight = window.height,
        density = com.android.resources.Density.MEDIUM,
        orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT
    ), showSystemUi = false)

    @Test fun headerOccludesContentAcrossItsEntireBounds() = paparazzi.snapshot {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
            IvyMaterial3Theme(dark = window.dark, isTrueBlack = window.amoled) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Simulate list content passing beneath a pinned header, including its gutters.
                    Column {
                        repeat(30) { Text("Scrolled content must stay behind the title bar", color = MaterialTheme.colorScheme.error) }
                    }
                    JvyScreenHeader("Reports", {}, "Current period") { Text("Actions") }
                }
            }
        }
    }
}
