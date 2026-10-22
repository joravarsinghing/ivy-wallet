package com.ivy.main

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Width-based layout classes used by the main shell.
 *
 * These describe the available window, not a device model, so the same rules
 * work for phones, square foldables, tablets, and resizable DeX windows.
 */
enum class AdaptiveWindowClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

fun adaptiveWindowClass(width: Dp): AdaptiveWindowClass = when {
    width < 600.dp -> AdaptiveWindowClass.COMPACT
    width < 840.dp -> AdaptiveWindowClass.MEDIUM
    else -> AdaptiveWindowClass.EXPANDED
}
