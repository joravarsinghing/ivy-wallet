package com.ivy.wallet

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ivy.base.legacy.appContext
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

/**
 * Created by iliyan on 24.02.18.
 */
@HiltAndroidApp
class IvyAndroidApp : Application(), Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var authSession: com.ivy.security.AuthSessionController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appContext = this
        authSession.initialize(this)
        applicationScope.launch {
            combine(authSession.settings, authSession.locked, authSession.storageError) { settings, locked, error ->
                // Wait for vault initialization: an initial OFF placeholder must never
                // overwrite a legacy lock preference while it is being migrated.
                if (locked == null) null else settings.enabled || error != null
            }.filterNotNull().distinctUntilChanged().collect { protectionEnabled ->
                com.ivy.base.legacy.SharedPrefs(this@IvyAndroidApp)
                    .putBoolean(com.ivy.base.legacy.SharedPrefs.APP_LOCK_ENABLED, protectionEnabled)
                com.ivy.widget.balance.WalletBalanceWidgetReceiver.updateBroadcast(this@IvyAndroidApp)
            }
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}
