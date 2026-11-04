package com.ivy.design.l0_system

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

@Suppress("DEPRECATION")
class IdentifierContrastTest {
    private fun contrast(ink: Color, background: Color) =
        (maxOf(ink.luminance(), background.luminance()) + .05f) /
            (minOf(ink.luminance(), background.luminance()) + .05f)

    @Test fun `bright green and yellow use the readable dark endpoint`() {
        assertEquals(Black, findContrastTextColor(Color(0xff18ba92)))
        assertEquals(Black, findContrastTextColor(Color(0xffffcc3d)))
    }
    @Test fun `deep purple and black retain the preferred light endpoint`() {
        assertEquals(White, findContrastTextColor(Color(0xff352680)))
        assertEquals(White, findContrastTextColor(Black))
    }
    @Test fun `preferred Ivy endpoints fall back where neither reaches AA`() {
        val background = Color(.46f, .46f, .46f)
        assertTrue(contrast(White, background) < 4.5f)
        assertTrue(contrast(Black, background) < 4.5f)
        assertTrue(contrast(findContrastTextColor(background), background) >= 4.5f)
    }
    @Test fun `foreground reaches AA across an opaque RGB grid without changing the background`() {
        for (red in 0..255 step 17) for (green in 0..255 step 17) for (blue in 0..255 step 17) {
            val background = Color(red, green, blue)
            assertTrue("Insufficient contrast for $background", contrast(findContrastTextColor(background), background) >= 4.5f)
        }
    }
}
