package com.ivy.security.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density as ScreenDensity
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.security.SecuritySettings
import com.ivy.security.UnlockMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SecurityUnlockPaparazziTest(@TestParameter private val window: Window) {
    enum class Window(val width: Int, val height: Int, val font: Float = 1f) { SmallPhone(320, 640), Phone(360, 800), Fold(720, 720), DeX(1280, 800), LargeText(360, 800, 2f) }
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(screenWidth = window.width, screenHeight = window.height,
        density = ScreenDensity.MEDIUM, orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT), showSystemUi = false)
    private fun snapshot(mode: UnlockMode, recovering: Boolean = false) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(dark = window == Window.Fold, isTrueBlack = false) {
                    SecurityUnlockContent(SecuritySettings(mode, mode == UnlockMode.EITHER, mode != UnlockMode.LEGACY_SYSTEM), false, "", null,
                        recovering, false, {}, {}, {}, { _, _ -> })
                }
            }
        }
    }
    @Test fun fingerprint() = snapshot(UnlockMode.BIOMETRIC)
    @Test fun fingerprintAndPin() = snapshot(UnlockMode.EITHER)
    @Test fun recovery() = snapshot(UnlockMode.EITHER, recovering = true)
    @Test fun legacyDeviceLock() = snapshot(UnlockMode.LEGACY_SYSTEM)
    @Test fun capturePrivacySetting() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(dark = window == Window.Fold, isTrueBlack = false) {
                    Surface(Modifier.fillMaxSize()) { Box(Modifier.padding(24.dp)) { ScreenCaptureSetting(false, {}) } }
                }
            }
        }
    }
}
