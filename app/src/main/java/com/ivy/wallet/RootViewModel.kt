package com.ivy.wallet

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.repository.LegalRepository
import com.ivy.frp.test.TestIdlingResource
import com.ivy.legacy.IvyWalletCtx
import com.ivy.base.threading.DispatchersProvider
import com.ivy.navigation.canResumeBeforeSetup
import com.ivy.navigation.DisclaimerScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.OnboardingScreen
import com.ivy.wallet.domain.deprecated.logic.notification.TransactionReminderLogic
import com.ivy.wallet.migrations.MigrationsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class RootViewModel @Inject constructor(
    private val ivyContext: IvyWalletCtx,
    private val nav: Navigation,
    private val settingsDao: SettingsDao,
    private val sharedPrefs: SharedPrefs,
    private val transactionReminderLogic: TransactionReminderLogic,
    private val migrationsManager: MigrationsManager,
    private val legalRepo: LegalRepository,
    private val authSession: com.ivy.security.AuthSessionController,
    private val dataStore: com.ivy.data.datastore.IvyDataStore,
    val screenCapturePreferences: com.ivy.security.ScreenCapturePreferences,
    private val routeValidator: RestoredRouteValidator,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {

    companion object {
        const val EXTRA_ADD_TRANSACTION_TYPE = "add_transaction_type_extra"

    }

    val appLocked = authSession.locked
    val securitySettings = authSession.settings
    private var started = false
    var startupError by mutableStateOf(false)
        private set
    var restorationNotice by mutableStateOf(false)
        private set

    fun acknowledgeRestoration() { restorationNotice = false }

    fun start(systemDarkMode: Boolean, intent: Intent, restoredNavigation: List<String>? = null) {
        // The Activity/Compose tree is recreated on resize, rotation, and theme changes.
        if (started) return
        started = true
        startupError = false
        viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                val bootstrap = withContext(dispatchers.io) {
                    val theme = settingsDao.findAll().firstOrNull()?.theme
                        ?: if (systemDarkMode) Theme.DARK else Theme.LIGHT
                    val onboarded = isOnboardingCompleted()
                    val accepted = legalRepo.isDisclaimerAccepted()
                    migrateOptionalWealthMetadata()
                    // Decode first, re-read live records second, publish only after both succeed.
                    val routes = restoredNavigation?.let(nav::savedDestinations).orEmpty()
                    val usable = routes.associateWith { route ->
                        (onboarded || route.canResumeBeforeSetup()) && routeValidator.usable(route)
                    }
                    Bootstrap(theme, onboarded, accepted, routes, usable)
                }
                Snapshot.withMutableSnapshot {
                    ivyContext.switchTheme(bootstrap.theme)
                    ivyContext.initStartDayOfMonthInMemory(sharedPrefs)
                    if (nav.currentScreen == null && restoredNavigation != null) {
                        nav.restoreState(restoredNavigation) { bootstrap.usable[it] == true }
                        restorationNotice = bootstrap.routes.isEmpty() || bootstrap.usable.values.any { !it }
                    }
                    if (bootstrap.onboarded) {
                        if (nav.currentScreen == OnboardingScreen) nav.resetBackStack()
                        if (nav.currentScreen == null || nav.currentScreen == OnboardingScreen) {
                            // A saved visit has consumed its external intent already, even if
                            // that visit can no longer be resumed. Never replay its Add action.
                            if (restoredNavigation != null || !handleSpecialStart(intent)) nav.navigateTo(MainScreen)
                        }
                    } else if (nav.currentScreen?.canResumeBeforeSetup() != true) {
                        nav.resetBackStack()
                        nav.navigateTo(OnboardingScreen)
                    }
                    if (!bootstrap.accepted && nav.currentScreen != DisclaimerScreen) nav.navigateTo(DisclaimerScreen)
                }
            } catch (error: CancellationException) {
                started = false
                throw error
            } catch (_: Exception) {
                started = false
                startupError = true
                return@launch
            } finally {
                TestIdlingResource.decrement()
            }
            // Optional housekeeping cannot turn a usable wallet into a startup failure.
            optionalHousekeeping { transactionReminderLogic.scheduleReminder() }
            optionalHousekeeping { migrationsManager.executeMigrations() }
        }
    }

    private data class Bootstrap(
        val theme: Theme,
        val onboarded: Boolean,
        val accepted: Boolean,
        val routes: List<com.ivy.navigation.Screen>,
        val usable: Map<com.ivy.navigation.Screen, Boolean>,
    )

    private suspend fun optionalHousekeeping(block: suspend () -> Unit) {
        try { block() }
        catch (error: CancellationException) { throw error }
        catch (_: Exception) { /* No financial data or storage exceptions in diagnostics. */ }
    }

    private suspend fun migrateOptionalWealthMetadata() = optionalHousekeeping {
        // Complete this before exposing Accounts/wealth; failure must not block the ledger.
        val roleKeys = listOf(SharedPrefs.NET_WORTH_ASSET_ACCOUNT_IDS, SharedPrefs.NET_WORTH_LIABILITY_ACCOUNT_IDS)
        if (roleKeys.any { !sharedPrefs.has(it) }) {
            val legacy = dataStore.data.first()
            roleKeys.filter { !sharedPrefs.has(it) }.forEach { key ->
                sharedPrefs.putString(key, legacy[androidx.datastore.preferences.core.stringSetPreferencesKey(key)].orEmpty().joinToString(","))
            }
        }
    }

    @Suppress("SwallowedException")
    private fun handleSpecialStart(intent: Intent): Boolean {
        val addTrnType: TransactionType? = try {
            intent.getSerializableExtra(EXTRA_ADD_TRANSACTION_TYPE) as? TransactionType
                ?: TransactionType.valueOf(intent.getStringExtra(EXTRA_ADD_TRANSACTION_TYPE) ?: "")
        } catch (_: RuntimeException) {
            // Exported entry points can contain malformed Bundles as well as an
            // unknown enum name. Reject the request; never process financial extras.
            null
        }

        if (addTrnType != null) {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = addTrnType
                )
            )

            return true
        }

        return false
    }

    private fun isOnboardingCompleted(): Boolean {
        return sharedPrefs.getBoolean(SharedPrefs.ONBOARDING_COMPLETED, false)
    }

    // App Lock & UserInactivity --------------------------------------------------------------------
    fun isAppLockEnabled(): Boolean {
        return authSession.settings.value.enabled
    }

    fun isAppLocked(): Boolean {
        // by default we assume that the app is locked
        return appLocked.value ?: true
    }

    fun lockApp() {
        authSession.lock()
    }

}
