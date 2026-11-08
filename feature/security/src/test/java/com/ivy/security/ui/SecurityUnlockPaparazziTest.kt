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
import io.mockk.every
import io.mockk.mockk
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@RunWith(TestParameterInjector::class)
class SecurityUnlockPaparazziTest(@TestParameter private val window: Window) {
    enum class Window(val width: Int, val height: Int, val font: Float = 1f) { SmallPhone(320, 640), Phone(360, 800), Fold(720, 720), DeX(1280, 800), LargeText(360, 800, 2f), FoldLargeText(720, 720, 2f), Landscape(640, 360) }
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(screenWidth = window.width, screenHeight = window.height,
        density = ScreenDensity.MEDIUM, orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT), showSystemUi = false)
    private fun snapshot(mode: UnlockMode, recovering: Boolean = false) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(dark = window == Window.Fold || window == Window.FoldLargeText, isTrueBlack = false) {
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
    @Test fun pinOnly() = snapshot(UnlockMode.PIN)
    @Test fun legacySettings() = settings(UnlockMode.LEGACY_SYSTEM, verified = false)
    @Test fun backupCodeReady() = settings(UnlockMode.LEGACY_SYSTEM, verified = true)
    @Test fun pinSettingsEditor() = settings(UnlockMode.PIN, verified = true, changing = true)
    @Test fun securityControlShapes() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(dark = window == Window.Fold || window == Window.FoldLargeText, isTrueBlack = false) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            androidx.compose.material3.Text("Your Jvy PIN", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                            SecretField("New six-digit PIN", "123456", {})
                            SecretField("Confirm PIN", "123456", {})
                            com.ivy.design.system.JvyButton({}, Modifier.fillMaxWidth()) { androidx.compose.material3.Text("Save PIN") }
                            com.ivy.design.system.JvyOutlinedButton({}, Modifier.fillMaxWidth()) { androidx.compose.material3.Text("Keep current method") }
                        }
                    }
                }
            }
        }
    }
    private fun settings(mode: UnlockMode, verified: Boolean, changing: Boolean = false) {
        val vm = mockk<SecurityViewModel>(relaxed = true)
        every { vm.verifiedForSettings } returns verified
        every { vm.busy } returns false
        every { vm.recoveryVerified } returns false
        every { vm.recovering } returns false
        every { vm.message } returns ""
        paparazzi.snapshot {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(dark = window == Window.Fold || window == Window.FoldLargeText, isTrueBlack = false) {
                    SecuritySettingsContent(vm, SecuritySettings(mode, mode == UnlockMode.PIN, mode == UnlockMode.PIN), false, {}, {}, false, changing)
                }
            }
        }
    }
    @Test fun capturePrivacySetting() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(dark = window == Window.Fold || window == Window.FoldLargeText, isTrueBlack = false) {
                    Surface(Modifier.fillMaxSize()) { Box(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) { ScreenCaptureSetting(false, {}) } }
                }
            }
        }
    }
}
