package com.ivy.design.system

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Vivid plot ink, not text/container roles. Outlines meet 3:1 on the neutral insight card. */
data class JvyChartPalette(val balance: Color, val income: Color, val spending: Color)

object JvyChartColors {
    val Light = JvyChartPalette(Color(0xFF5C3DF5), Color(0xFF008663), Color(0xFFE0264B))
    val Dark = JvyChartPalette(Color(0xFF9265FF), Color(0xFF14CC9E), Color(0xFFFF4060))

    @Composable
    fun palette(): JvyChartPalette =
        if (MaterialTheme.colorScheme.surfaceVariant.luminance() < 0.5f) Dark else Light
}
