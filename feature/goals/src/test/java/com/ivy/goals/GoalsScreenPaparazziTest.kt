package com.ivy.goals

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class GoalsScreenPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot goals guidance`() {
        snapshot(theme) {
            IvyWalletPreview(
                theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT,
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    GoalsScreenContent(
                        state = GoalScreenState.preview(),
                        onEvent = {},
                    )
                }
            }
        }
    }
}
