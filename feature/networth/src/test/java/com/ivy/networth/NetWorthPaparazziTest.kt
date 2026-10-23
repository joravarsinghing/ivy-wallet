package com.ivy.networth

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density as ComposeDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityOptionsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.data.model.wealth.*
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.domain.usecase.networth.*
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

@RunWith(TestParameterInjector::class)
class NetWorthPaparazziTest(@TestParameter private val window: Window) {
    enum class Window(val width: Int, val height: Int, val font: Float = 1f) { SmallPhone(320, 640), Phone(360, 800), Fold(720, 720), DeX(1280, 800), LargeText(360, 800, 2f) }
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(screenWidth = window.width, screenHeight = window.height, density = Density.MEDIUM,
        orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT), showSystemUi = false)

    private fun snapshot(tab: Int, hidden: Boolean = false, empty: Boolean = false) {
        Locale.setDefault(Locale.US)
        val date = LocalDate.parse("2026-10-21")
        val now = "2026-10-21T12:00:00Z"
        val item = WealthItem("home", "Family home · a longer property name", WealthKind.PROPERTY, "AED", createdDate = "2026-01-01")
        val data = if (empty) WealthBackup() else WealthBackup(items = listOf(item),
            policies = listOf(WealthPolicy("policy", item.id, "2026-01-01", recordedAt = now)),
            valuations = listOf(WealthValuation("value", item.id, "2026-01-01", "1250000.50", recordedAt = now)))
        val result = WealthCalculator.calculate(data, emptyList(), date, "AED")
        val history = (5 downTo 0).map { i -> val d = date.minusMonths(i.toLong()); WealthHistoryPoint(d, WealthCalculator.calculate(data, emptyList(), d, "AED")) }
        val workspace = WealthWorkspace(data, result, emptyList(), history, emptyList(), null)
        val vm = mockk<NetWorthViewModel>(relaxed = true)
        every { vm.workspace } returns workspace
        every { vm.date } returns date
        every { vm.hidden } returns hidden
        every { vm.loading } returns false
        every { vm.error } returns null
        every { vm.months } returns 6
        val registry = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) = Unit
            }
        }
        paparazzi.snapshot {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry, LocalDensity provides ComposeDensity(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(dark = window == Window.Fold, isTrueBlack = false) {
                    Surface { NetWorthScreenContent(vm, {}, initialTab = tab) }
                }
            }
        }
    }
    @Test fun overview() = snapshot(0)
    @Test fun history() = snapshot(3)
    @Test fun hiddenBalances() = snapshot(0, hidden = true)
    @Test fun emptyAssets() = snapshot(1, empty = true)
    @Test fun firstVisitOverview() = snapshot(0, empty = true)
    @Test fun guideStart() = guide(0)
    @Test fun guideFinish() = guide(3)
    private fun guide(step: Int) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalDensity provides ComposeDensity(LocalDensity.current.density, window.font)) {
                IvyMaterial3Theme(dark = window == Window.Fold, isTrueBlack = false) {
                    Surface(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Surface(Modifier.widthIn(max = 640.dp).fillMaxWidth(), shape = androidx.compose.material3.MaterialTheme.shapes.large) {
                                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    WealthGuidePage(step, {}, {}, {}, {})
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
