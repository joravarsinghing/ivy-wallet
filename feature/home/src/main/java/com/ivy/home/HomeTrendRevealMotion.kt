package com.ivy.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.ivy.design.system.JvyMotion

/** View-only reveal: exact paths remain unchanged and no financial amounts are interpolated. */
internal class HomeTrendRevealMotion(val animateOnEntry: Boolean) {
    val progress = Animatable(if (animateOnEntry) 0f else 1f)

    suspend fun reveal() {
        progress.animateTo(1f, tween(JvyMotion.ChartUpdateMillis, easing = FastOutSlowInEasing))
    }
}
