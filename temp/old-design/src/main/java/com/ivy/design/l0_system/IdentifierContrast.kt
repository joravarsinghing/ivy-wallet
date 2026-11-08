package com.ivy.design.l0_system

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Foregrounds for opaque, user-coloured identifiers, not the page's semantic palette. */
fun identifierForeground(background: Color, light: Color = Color.White, dark: Color = Color.Black): Color {
    val luminance = background.copy(alpha = 1f).luminance()
    fun contrast(ink: Color): Float =
        (maxOf(ink.luminance(), luminance) + .05f) / (minOf(ink.luminance(), luminance) + .05f)
    val preferred = if (contrast(dark) >= contrast(light)) dark else light
    // Retain Ivy's preferred off-black/off-white where they meet AA; near the
    // crossover neither can, so use the higher-contrast pure endpoint instead.
    return if (contrast(preferred) >= 4.5f) preferred
    else if (contrast(Color.Black) >= contrast(Color.White)) Color.Black else Color.White
}
