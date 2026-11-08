package com.ivy.accounts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density as ScreenDensity
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class AccountsAdaptivePaparazziTest(@TestParameter private val window: Window) {
    enum class Window(val width: Int, val height: Int, val font: Float = 1f) {
        SmallPhone(320, 640), Phone(360, 800), Fold(720, 720), DeX(1280, 800),
        LargeText(360, 800, 2f), FoldLargeText(720, 720, 2f), Landscape(640, 360),
    }
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(
        screenWidth = window.width, screenHeight = window.height, density = ScreenDensity.MEDIUM,
        orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT), showSystemUi = false)
    private fun snapshot(hidden: Boolean = false, empty: Boolean = false, compact: Boolean = false) {
        val data = accountPreviewState(compact).let { sample ->
            sample.copy(hideBalances = hidden, accountsData = if (empty) persistentListOf() else sample.accountsData,
                totalBalanceWithExcluded = if (empty) "0" else sample.totalBalanceWithExcluded,
                totalBalanceWithoutExcluded = if (empty) "0" else sample.totalBalanceWithoutExcluded)
        }
        paparazzi.snapshot {
            IvyWalletPreview(if (window == Window.Fold || window == Window.FoldLargeText) Theme.DARK else Theme.LIGHT) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font)) { AccountsContent(data) }
            }
        }
    }
    @Test fun accounts() = snapshot()
    @Test fun hiddenBalances() = snapshot(hidden = true)
    @Test fun emptyAccounts() = snapshot(empty = true)
    @Test fun compactAccounts() = snapshot(compact = true)
}
