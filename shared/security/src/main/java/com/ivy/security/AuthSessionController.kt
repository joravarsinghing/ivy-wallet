package com.ivy.security

import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: SecurityStore,
) : Application.ActivityLifecycleCallbacks {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val session = LockSession(SystemClock::elapsedRealtime)
    private val _locked = MutableStateFlow<Boolean?>(null)
    val locked = _locked.asStateFlow()
    private val _settings = MutableStateFlow(SecuritySettings(UnlockMode.OFF, false, false))
    val settings = _settings.asStateFlow()
    private val _storageError = MutableStateFlow<String?>(null)
    val storageError = _storageError.asStateFlow()
    private var started = 0
    private var initialized = false
    var requestGeneration: Long? = null
        private set
    private var requestSequence = 0L
    private var requestEpoch = 0L
    private var authenticationAt: Long? = null
    private var deviceWasLocked = false
    val generation get() = session.generation
    val deviceLocked get() = (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked

    fun initialize(application: Application) {
        if (initialized) return
        initialized = true
        application.registerActivityLifecycleCallbacks(this)
        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) lock()
            }
        }, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch {
            try { refresh(initial = true) }
            catch (_: Exception) {
                _storageError.value = "Android secure storage could not be opened. Restart Jvy Wallet and try again. Protection has not been disabled."
                _locked.value = true
            }
        }
    }
    suspend fun refresh(initial: Boolean = false, authorizedGeneration: Long? = null) {
        val value = try { store.settings() } catch (error: Exception) {
            _storageError.value = "Android secure storage could not be opened. Restart Jvy Wallet and try again."
            _locked.value = true
            throw error
        }
        _storageError.value = null
        val authorized = authorizedGeneration != null && authorizedGeneration == session.generation && !deviceLocked && started > 0
        _settings.value = value
        session.configure(value.enabled, initial)
        if (value.enabled && (deviceWasLocked || deviceLocked)) session.lock()
        if (authorized) session.authenticate(session.generation)
        if (value.enabled && started == 0) session.background()
        _locked.value = session.locked
    }
    fun beginAuthentication(): Long? {
        if (requestGeneration != null || deviceLocked) return null
        requestEpoch = session.generation
        return (++requestSequence).also { requestGeneration = it }
    }
    fun finishAuthentication(request: Long, success: Boolean, unlock: Boolean = true): Boolean {
        if (requestGeneration != request) return false
        requestGeneration = null
        if (!success || deviceLocked || requestEpoch != session.generation) return false
        if (unlock && !session.authenticate(requestEpoch)) return false
        authenticationAt = SystemClock.elapsedRealtime()
        _locked.value = session.locked
        return true
    }
    fun recentlyAuthenticated(): Boolean = authenticationAt?.let { SystemClock.elapsedRealtime() - it < 60_000L } == true && _locked.value == false
    fun lock() {
        session.lock()
        authenticationAt = null
        requestGeneration = null
        _locked.value = session.locked
    }
    override fun onActivityStarted(activity: Activity) {
        started++
        deviceWasLocked = deviceLocked
        val prior = session.generation
        session.foreground(deviceLocked)
        if (session.generation != prior) { requestGeneration = null; authenticationAt = null }
        if (_locked.value != null) _locked.value = session.locked
    }
    override fun onActivityStopped(activity: Activity) {
        started = (started - 1).coerceAtLeast(0)
        if (started == 0 && !activity.isChangingConfigurations) session.background()
    }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) {
        deviceWasLocked = deviceLocked
        if (deviceLocked) lock()
    }
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
