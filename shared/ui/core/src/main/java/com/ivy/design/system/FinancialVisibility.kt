package com.ivy.design.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.ivy.base.legacy.SharedPrefs

/** Read-only presentation policy. Never change financial data or redact backup payloads. */
data class FinancialVisibility(val hidden: Boolean = false) {
    fun text(value: String): String = if (hidden) REDACTED else value

    companion object { const val REDACTED = "••••" }
}

val LocalFinancialVisibility = staticCompositionLocalOf { FinancialVisibility() }

@Composable
fun financialText(value: String): String = LocalFinancialVisibility.current.text(value)

@Composable
fun FinancialVisibilityProvider(preferences: SharedPrefs, content: @Composable () -> Unit) {
    val changes = remember(preferences) { preferences.observeBoolean(SharedPrefs.HIDE_CURRENT_BALANCE) }
    val hidden by changes.collectAsState(
        initial = preferences.getBoolean(SharedPrefs.HIDE_CURRENT_BALANCE, false)
    )
    CompositionLocalProvider(LocalFinancialVisibility provides FinancialVisibility(hidden), content = content)
}
