package com.ivy.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration

/** One screen-level subscription and one midnight wake-up, never a per-card timer or polling. */
@Composable
internal fun HomeCalendarEffect(refresh: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val provider = LocalTimeProvider.current
    val converter = LocalTimeConverter.current
    val currentRefresh by rememberUpdatedState(refresh)
    var clockRevision by remember { mutableStateOf(0) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                clockRevision++
                currentRefresh()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(owner, provider, converter, clockRevision) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            currentRefresh()
            while (isActive) {
                val midnight = with(converter) { provider.localDateNow().plusDays(1).atStartOfDay().toUTC() }
                delay(Duration.between(provider.utcNow(), midnight).toMillis().coerceAtLeast(1))
                currentRefresh()
            }
        }
    }
}
