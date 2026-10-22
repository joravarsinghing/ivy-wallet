package com.ivy.security

/** In-memory authorization only: deliberately never saved in a Bundle or backup. */
class LockSession(private val elapsedMillis: () -> Long) {
    var enabled = false
        private set
    var locked = true
        private set
    var generation = 0L
        private set
    private var backgroundAt: Long? = null

    fun configure(enabled: Boolean, initial: Boolean = false) {
        if (initial || this.enabled != enabled) {
            this.enabled = enabled
            locked = enabled
            generation++
        }
    }

    fun background() { if (backgroundAt == null) backgroundAt = elapsedMillis() }

    fun foreground(deviceLocked: Boolean) {
        if (enabled && (deviceLocked || !locked && backgroundAt?.let { elapsedMillis() - it >= 60_000L } == true)) lock()
        backgroundAt = null
    }

    fun lock() {
        if (enabled) { locked = true; generation++ }
    }

    fun authenticate(requestGeneration: Long): Boolean {
        if (requestGeneration != generation) return false
        locked = false
        backgroundAt = null
        return true
    }
}
