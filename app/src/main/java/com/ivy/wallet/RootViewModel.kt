package com.ivy.wallet

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.repository.LegalRepository
import com.ivy.frp.test.TestIdlingResource
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.utils.ioThread
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
) : ViewModel() {

    companion object {
        const val EXTRA_ADD_TRANSACTION_TYPE = "add_transaction_type_extra"

    }

    val appLocked = authSession.locked
    val securitySettings = authSession.settings
    private var started = false

    fun start(systemDarkMode: Boolean, intent: Intent) {
        // The Activity/Compose tree is recreated on resize, rotation, and theme changes.
        if (started) return
        started = true
        viewModelScope.launch {
            TestIdlingResource.increment()

            ioThread {
                val theme = settingsDao.findAll().firstOrNull()?.theme
                    ?: if (systemDarkMode) Theme.DARK else Theme.LIGHT
                ivyContext.switchTheme(theme)

                ivyContext.initStartDayOfMonthInMemory(sharedPrefs = sharedPrefs)
            }

            TestIdlingResource.decrement()
        }

        viewModelScope.launch {
            TestIdlingResource.increment()

            ioThread {
                // Older classification builds used DataStore only. Migrate before either
                // Accounts or the direct More -> Net worth entry point can read the profile.
                val roleKeys = listOf(SharedPrefs.NET_WORTH_ASSET_ACCOUNT_IDS, SharedPrefs.NET_WORTH_LIABILITY_ACCOUNT_IDS)
                if (roleKeys.any { !sharedPrefs.has(it) }) {
                    try {
                        val legacy = dataStore.data.first()
                        roleKeys.filter { !sharedPrefs.has(it) }.forEach { key ->
                            sharedPrefs.putString(key, legacy[androidx.datastore.preferences.core.stringSetPreferencesKey(key)].orEmpty().joinToString(","))
                        }
                    } catch (error: kotlinx.coroutines.CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        // Optional wealth metadata must not block access to the existing ledger.
                    }
                }
                if (isOnboardingCompleted()) {
                    navigateOnboardedUser(intent)
                } else {
                    nav.navigateTo(OnboardingScreen)
                }
                if (!legalRepo.isDisclaimerAccepted()) {
                    nav.navigateTo(DisclaimerScreen)
                }
            }

            TestIdlingResource.decrement()
        }

        viewModelScope.launch {
            migrationsManager.executeMigrations()
        }
    }

    private fun navigateOnboardedUser(intent: Intent) {
        if (!handleSpecialStart(intent)) {
            nav.navigateTo(MainScreen)
            transactionReminderLogic.scheduleReminder()
        }
    }

    @Suppress("SwallowedException")
    private fun handleSpecialStart(intent: Intent): Boolean {
        val addTrnType: TransactionType? = try {
            intent.getSerializableExtra(EXTRA_ADD_TRANSACTION_TYPE) as? TransactionType
                ?: TransactionType.valueOf(intent.getStringExtra(EXTRA_ADD_TRANSACTION_TYPE) ?: "")
        } catch (e: IllegalArgumentException) {
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
