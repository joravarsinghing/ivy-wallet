package com.ivy.design.system

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ivy.design.system.colors.IvyColors

@Composable
fun IvyMaterial3Theme(
    isTrueBlack: Boolean,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) ivyDarkColorScheme(isTrueBlack) else ivyLightColorScheme(),
        typography = JvyTypography.Material,
        shapes = JvyShapes.Material,
        content = content,
    )
}

internal fun ivyLightColorScheme(): ColorScheme = ColorScheme(
    primary = IvyColors.Purple.primary,
    onPrimary = IvyColors.White,
    primaryContainer = Color(0xFFEEE9FE),
    onPrimaryContainer = IvyColors.Purple.dark,
    inversePrimary = IvyColors.Purple.dark,
    secondary = IvyColors.Green.dark,
    onSecondary = IvyColors.White,
    secondaryContainer = Color(0xFFE7F4ED),
    onSecondaryContainer = Color(0xFF176C50),
    tertiary = IvyColors.Orange.dark,
    onTertiary = IvyColors.White,
    tertiaryContainer = Color(0xFFFBEEE4),
    onTertiaryContainer = Color(0xFF93421C),

    error = Color(0xFFB3262D),
    onError = IvyColors.White,
    errorContainer = Color(0xFFFCE8E7),
    onErrorContainer = Color(0xFF8F2424),

    background = IvyColors.White,
    onBackground = IvyColors.Black,
    surface = IvyColors.White,
    onSurface = IvyColors.Black,
    surfaceVariant = Color(0xFFF0F0F4),
    onSurfaceVariant = Color(0xFF555560),
    surfaceTint = IvyColors.Black,
    inverseSurface = IvyColors.DarkGray,
    inverseOnSurface = IvyColors.White,

    outline = Color(0xFF83838E),
    outlineVariant = Color(0xFFDBDBE3),
    scrim = IvyColors.ExtraDarkGray.copy(alpha = 0.8f)
)

internal fun ivyDarkColorScheme(isTrueBlack: Boolean): ColorScheme = ColorScheme(
    primary = IvyColors.Purple.extraLight,
    onPrimary = IvyColors.Purple.extraDark,
    primaryContainer = IvyColors.Purple.dark,
    onPrimaryContainer = Color(0xFFEEE9FE),
    inversePrimary = IvyColors.Purple.dark,
    secondary = Color(0xFF8ADBBC),
    onSecondary = Color(0xFF10392A),
    secondaryContainer = Color(0xFF19372F),
    onSecondaryContainer = Color(0xFF8ADBBC),
    tertiary = Color(0xFFF0BC92),
    onTertiary = Color(0xFF4B2B13),
    tertiaryContainer = Color(0xFF3E2B23),
    onTertiaryContainer = Color(0xFFF0BC92),

    error = Color(0xFFFFB4AD),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF522323),
    onErrorContainer = Color(0xFFFFDAD5),

    background = if (isTrueBlack) IvyColors.TrueBlack else Color(0xFF141418),
    onBackground = IvyColors.White,
    surface = if (isTrueBlack) IvyColors.TrueBlack else Color(0xFF1C1C21),
    onSurface = IvyColors.White,
    surfaceVariant = Color(0xFF28282F),
    onSurfaceVariant = Color(0xFFC5C5D0),
    surfaceTint = IvyColors.White,
    inverseSurface = IvyColors.LightGray,
    inverseOnSurface = if (isTrueBlack) IvyColors.TrueBlack else IvyColors.Black,

    outline = Color(0xFF9696A2),
    outlineVariant = Color(0xFF44444F),
    scrim = IvyColors.ExtraLightGray.copy(alpha = 0.8f)
)
