package com.ivy.design.system

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class JvyDesignTest {
    @Test fun `all Material roles retain Ivy's interface family`() {
        val type = JvyTypography.Material
        listOf(type.displayLarge, type.displayMedium, type.displaySmall,
            type.headlineLarge, type.headlineMedium, type.headlineSmall,
            type.titleLarge, type.titleMedium, type.titleSmall,
            type.bodyLarge, type.bodyMedium, type.bodySmall,
            type.labelLarge, type.labelMedium, type.labelSmall).forEach {
            assertEquals(JvyFonts.Interface, it.fontFamily)
        }
    }
    @Test fun `money keeps Open Sans tabular figures and a bounded scale`() {
        assertEquals(JvyFonts.Numbers, JvyTypography.Amount.fontFamily)
        assertEquals("tnum", JvyTypography.Amount.fontFeatureSettings)
        assertEquals(24.sp, JvyTypography.Amount.fontSize)
        assertEquals(32.sp, JvyTypography.AmountLarge.fontSize)
        assertEquals(16.sp, JvyTypography.AmountSmall.fontSize)
        assertEquals(FontWeight.Bold, JvyTypography.Amount.fontWeight)
    }
    @Test fun `body and page sizes come from the shared readable scale`() {
        assertEquals(28.sp, JvyTypography.PageTitle.fontSize)
        assertEquals(16.sp, JvyTypography.Body.fontSize)
        assertEquals(14.sp, JvyTypography.Supporting.fontSize)
        assertEquals(12.sp, JvyTypography.Label.fontSize)
        assertEquals(JvyShapes.Control, JvyShapes.Material.extraSmall)
        assertEquals(JvyShapes.Control, JvyShapes.Material.small)
        assertEquals(JvyShapes.Card, JvyShapes.Material.medium)
    }
    @Test fun `text and financial accent pairs meet AA in light dark and AMOLED`() {
        listOf(ivyLightColorScheme(), ivyDarkColorScheme(false), ivyDarkColorScheme(true)).forEach { color ->
            listOf(color.onSurface to color.surface, color.onBackground to color.background,
                color.onSurfaceVariant to color.surfaceVariant, color.onSurface to color.surfaceVariant,
                color.primary to color.surfaceVariant, color.secondary to color.surfaceVariant, color.error to color.surfaceVariant,
                color.primary to color.surface,
                color.onPrimary to color.primary, color.onSecondary to color.secondary,
                color.onSecondaryContainer to color.secondaryContainer, color.secondary to color.surface,
                color.onTertiaryContainer to color.tertiaryContainer, color.tertiary to color.surface,
                color.error to color.surface).forEach { (ink, paper) ->
                assertTrue("Insufficient contrast $ink on $paper: ${contrast(ink, paper)}", contrast(ink, paper) >= 4.5)
            }
            assertTrue(contrast(color.outline, color.surface) >= 3.0)
        }
    }
    private fun contrast(a: Color, b: Color): Double {
        fun luminance(c: Color): Double {
            fun channel(v: Float) = if (v <= 0.04045f) v.toDouble() / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            return channel(c.red) * 0.2126 + channel(c.green) * 0.7152 + channel(c.blue) * 0.0722
        }
        return (maxOf(luminance(a), luminance(b)) + 0.05) / (minOf(luminance(a), luminance(b)) + 0.05)
    }

    @Test fun `vivid chart outlines meet nontext contrast on every insight surface`() {
        listOf(ivyLightColorScheme() to JvyChartColors.Light,
            ivyDarkColorScheme(false) to JvyChartColors.Dark,
            ivyDarkColorScheme(true) to JvyChartColors.Dark).forEach { (scheme, palette) ->
            listOf(palette.balance, palette.income, palette.spending).forEach { ink ->
                assertTrue("Plot $ink on ${scheme.surfaceVariant}", contrast(ink, scheme.surfaceVariant) >= 3.0)
            }
        }
    }
}
