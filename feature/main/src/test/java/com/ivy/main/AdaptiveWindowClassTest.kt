package com.ivy.main

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Test

class AdaptiveWindowClassTest {
    @Test
    fun `phone width uses compact shell`() {
        adaptiveWindowClass(411.dp) shouldBe AdaptiveWindowClass.COMPACT
    }

    @Test
    fun `square foldable width uses medium shell`() {
        adaptiveWindowClass(720.dp) shouldBe AdaptiveWindowClass.MEDIUM
    }

    @Test
    fun `desktop window width uses expanded shell`() {
        adaptiveWindowClass(1280.dp) shouldBe AdaptiveWindowClass.EXPANDED
    }
}
