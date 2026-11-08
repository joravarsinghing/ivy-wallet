package com.ivy.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Installation-only presentation preference; never restored from financial backups. */
@Singleton
class ScreenCapturePreferences @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("jvy-screen-capture", Context.MODE_PRIVATE)
    private val _allowed = MutableStateFlow<Boolean?>(if (prefs.contains("allowed")) prefs.getBoolean("allowed", false) else null)
    val allowed = _allowed.asStateFlow()

    fun setAllowed(value: Boolean) {
        // Apply persists through recreation/restarts; no credential or ledger changes.
        prefs.edit().putBoolean("allowed", value).apply()
        _allowed.value = value
    }
}

/** A null preference preserves the previous default: lock-enabled screens are private. */
fun shouldProtectScreenCapture(
    locked: Boolean?, lockEnabled: Boolean, preference: Boolean?, credentialsVisible: Boolean,
): Boolean = locked != false || credentialsVisible || !(preference ?: !lockEnabled)
