package com.ivy.home

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeTrendRevealMotionTest {
    private class Clock(private val scope: TestScope) : MonotonicFrameClock {
        var frames = 0
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            delay(16)
            frames++
            return onFrame(scope.testScheduler.currentTime * 1_000_000)
        }
    }
    private fun scale(value: Float) = object : MotionDurationScale { override val scaleFactor = value }

    @Test fun `initial data is complete without an entry animation`() {
        val motion = HomeTrendRevealMotion(animateOnEntry = false)
        assertFalse(motion.animateOnEntry)
        assertEquals(1f, motion.progress.value, 0f)
    }

    @Test fun `replacement data draws progressively then stops requesting frames`() = runTest {
        val motion = HomeTrendRevealMotion(animateOnEntry = true)
        val clock = Clock(this)
        assertEquals(0f, motion.progress.value, 0f)
        val job = launch(clock + scale(1f)) { motion.reveal() }
        runCurrent()
        advanceTimeBy(160)
        runCurrent()
        assertTrue(motion.progress.value > 0f && motion.progress.value < 1f)
        advanceUntilIdle()
        assertTrue(job.isCompleted)
        assertEquals(1f, motion.progress.value, 0f)
        val frames = clock.frames
        advanceTimeBy(1000)
        assertEquals(frames, clock.frames)
    }

    @Test fun `disabled system motion settles on the next frame`() = runTest {
        val motion = HomeTrendRevealMotion(animateOnEntry = true)
        val clock = Clock(this)
        withContext(clock + scale(0f)) { motion.reveal() }
        assertEquals(1f, motion.progress.value, 0f)
        assertTrue(clock.frames <= 2)
    }

    @Test fun `disposing a chart cancels its animation without queued work`() = runTest {
        val motion = HomeTrendRevealMotion(animateOnEntry = true)
        val clock = Clock(this)
        val job = launch(clock + scale(1f)) { motion.reveal() }
        runCurrent()
        advanceTimeBy(80)
        job.cancelAndJoin()
        val progress = motion.progress.value
        val frames = clock.frames
        advanceTimeBy(1000)
        assertEquals(progress, motion.progress.value, 0f)
        assertEquals(frames, clock.frames)
    }
}
