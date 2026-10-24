package com.ivy.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.core.app.ActivityOptionsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density as ScreenDensity
import com.android.resources.ScreenOrientation
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.accounts.AccountsContent
import com.ivy.accounts.accountPreviewState
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.MainTab
import com.ivy.networth.NetWorthScreenContent
import com.ivy.networth.NetWorthViewModel
import com.ivy.data.model.wealth.*
import com.ivy.domain.usecase.networth.*
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(TestParameterInjector::class)
class MainFeatureShellPaparazziTest(@TestParameter private val window: MainShellPaparazziTest.Window) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(
        screenWidth = window.width, screenHeight = window.height, density = ScreenDensity.MEDIUM,
        orientation = if (window.width > window.height) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT), showSystemUi = false)
    private fun snapshot(accounts: Boolean, hidden: Boolean = false, tab: Int = 0) {
        val date = LocalDate.parse("2026-10-21")
        val data = WealthBackup(items = listOf(WealthItem("home", "Family home", WealthKind.PROPERTY, "AED", createdDate = "2026-01-01", reviewDays = 365)),
            policies = listOf(WealthPolicy("p", "home", "2026-01-01", recordedAt = "2026-10-21T00:00:00Z")),
            valuations = listOf(WealthValuation("v", "home", "2026-01-01", "1250000.50", recordedAt = "2026-10-21T00:00:00Z")))
        val result = WealthCalculator.calculate(data, emptyList(), date, "AED")
        val vm = mockk<NetWorthViewModel>(relaxed = true)
        every { vm.workspace } returns WealthWorkspace(data, result, emptyList(), emptyList(), emptyList(), null)
        every { vm.date } returns date
        every { vm.hidden } returns hidden
        every { vm.error } returns null
        val registry = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) = Unit
            }
        }
        paparazzi.snapshot {
            IvyWalletPreview(if (window == MainShellPaparazziTest.Window.Fold || window == MainShellPaparazziTest.Window.FoldLargeText) Theme.DARK else Theme.LIGHT) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, window.font), LocalActivityResultRegistryOwner provides registry) {
                    val rail = mainNavigationRailWidth(maxWidth, LocalDensity.current.fontScale)
                    MainContentWindow(rail) {
                        check(maxWidth == window.width.dp - rail) { "Feature preview did not receive the production content width" }
                        if (accounts) AccountsContent(accountPreviewState().copy(hideBalances = hidden))
                        else NetWorthScreenContent(vm, {}, initialTab = tab, embedded = true)
                    }
                    MainNavigation(if (accounts) MainTab.ACCOUNTS else MainTab.NET_WORTH, {}, rail,
                        Modifier.align(if (rail == 0.dp) Alignment.BottomCenter else Alignment.CenterStart))
                }
            }
        }
    }
    @Test fun accountsInsideNavigationShell() = snapshot(accounts = true)
    @Test fun hiddenAccountsInsideNavigationShell() = snapshot(accounts = true, hidden = true)
    @Test fun netWorthInsideNavigationShell() = snapshot(accounts = false)
    @Test fun hiddenNetWorthInsideNavigationShell() = snapshot(accounts = false, hidden = true)
    @Test fun ownedItemsInsideNavigationShell() = snapshot(accounts = false, tab = 1)
}
