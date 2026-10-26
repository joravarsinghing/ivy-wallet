package com.ivy.design.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

enum class ForegroundTheme { Light, Dark, Amoled }

/** Production non-legacy routes have no enclosing Material Surface. Do not mask that here. */
@RunWith(TestParameterInjector::class)
class ThemeForegroundPaparazziTest(@TestParameter private val theme: ForegroundTheme) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO, showSystemUi = false)
    @Test fun uncontainedTextUsesTheThemeForeground() = paparazzi.snapshot {
        CompositionLocalProvider(LocalContentColor provides Color.Magenta) {
            IvyMaterial3Theme(isTrueBlack = theme == ForegroundTheme.Amoled, dark = theme != ForegroundTheme.Light) {
                assertEquals(MaterialTheme.colorScheme.onBackground, LocalContentColor.current)
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    JvyScreenHeader("Goals", {}, "Plan for what matters to you.")
                    Text("Your goals · 1", style = JvyTypography.SectionTitle)
                    JvyEmptyState("What would you like to plan for?", "Choose a target and reserve money from your accounts.", Icons.Outlined.Flag)
                    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                        assertEquals(MaterialTheme.colorScheme.onPrimary, LocalContentColor.current)
                        Text("Nested surfaces still own their foreground")
                    }
                }
            }
        }
    }
}
