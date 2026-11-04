package com.ivy.wallet

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.play.core.review.ReviewManagerFactory
import com.ivy.IvyNavGraph
import com.ivy.base.legacy.Theme
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.design.api.IvyDesign
import com.ivy.design.api.IvyUI
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.domain.RootScreen
import com.ivy.home.customerjourney.CustomerJourneyCardsProvider
import com.ivy.legacy.Constants
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.appDesign
import com.ivy.legacy.utils.activityForResultLauncher
import com.ivy.legacy.utils.sendToCrashlytics
import com.ivy.legacy.utils.simpleActivityForResultLauncher
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.ui.R
import com.ivy.ui.time.TimeFormatter
import com.ivy.ui.time.impl.DateTimePicker
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import com.ivy.widget.transaction.AddTransactionWidget
import com.ivy.widget.transaction.AddTransactionWidgetCompact
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class RootActivity : AppCompatActivity(), RootScreen {
    @Inject
    lateinit var ivyContext: IvyWalletCtx

    @Inject
    lateinit var navigation: Navigation

    @Inject
    lateinit var customerJourneyLogic: CustomerJourneyCardsProvider

    @Inject
    lateinit var timeConverter: TimeConverter

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var timeFormatter: TimeFormatter

    @Inject
    lateinit var dateTimePicker: DateTimePicker

    @Inject
    lateinit var sharedPrefs: com.ivy.base.legacy.SharedPrefs

    private lateinit var createFileLauncher: ActivityResultLauncher<String>
    private var onFileCreated: ((fileUri: Uri) -> Unit)? = null

    private lateinit var openFileLauncher: ActivityResultLauncher<Unit>
    private var onFileOpened: ((fileUri: Uri) -> Unit)? = null
    private data class ExportRequest(val name: String, val onCreated: (Uri) -> Unit)
    private var pendingExport by mutableStateOf<ExportRequest?>(null)
    private var pendingNavigationState: ArrayList<String>? = null

    private val viewModel: RootViewModel by viewModels()
    private val securityUiViewModel: com.ivy.security.ui.SecurityViewModel by viewModels()

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Fail private before Compose has read the user's unlocked-screen preference.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setupApp()
        if (navigation.currentScreen == null) {
            pendingNavigationState = savedInstanceState?.getStringArrayList("jvy.navigation.v1")
            if (pendingNavigationState != null) {
                savedInstanceState?.getString("jvy.mainTab.v1")?.let { name ->
                    runCatching { com.ivy.legacy.data.model.MainTab.valueOf(name) }.getOrNull()?.let(ivyContext::selectMainTab)
                }
                savedInstanceState?.getStringArrayList("jvy.period.v1")?.let(com.ivy.legacy.data.model.TimePeriodSavedState::decode)?.let { period ->
                    val localized = period.month?.let { com.ivy.legacy.data.model.Month.fromMonthValue(it.monthValue) }
                    ivyContext.updateSelectedPeriodInMemory(period.copy(month = localized))
                }
            }
        }
        setContent {
            val viewModel: RootViewModel = viewModel()
            val isSystemInDarkTheme = isSystemInDarkTheme()
            // Read the mutable legacy theme once at the root so the entire app observes the
            // same value. Recreating the design wrapper at each call site can otherwise leave
            // parts of the navigation tree with colors from the previous theme.
            val activeTheme = ivyContext.theme
            val ivyDesign = remember(ivyContext) { appDesign(ivyContext) }

            LaunchedEffect(isSystemInDarkTheme) {
                viewModel.start(isSystemInDarkTheme, intent, pendingNavigationState)
            }

            val appLocked by viewModel.appLocked.collectAsState()
            val lockSettings by viewModel.securitySettings.collectAsState()
            LaunchedEffect(lockSettings.enabled) {
                // Security settings bypass the legacy Settings toggle. Redact an already
                // installed widget as soon as an app lock is enabled, not on the next launch.
                WalletBalanceWidgetReceiver.updateBroadcast(this@RootActivity)
            }
            val capturePreference by viewModel.screenCapturePreferences.allowed.collectAsState()
            val securityUi = securityUiViewModel
            val sensitive = com.ivy.security.shouldProtectScreenCapture(
                locked = appLocked, lockEnabled = lockSettings.enabled, preference = capturePreference,
                credentialsVisible = navigation.currentScreen == com.ivy.navigation.SecurityScreen || securityUi.recoveryCode != null,
            )
            SideEffect {
                if (sensitive) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            val savedScreens = rememberSaveableStateHolder()
            val entryId = navigation.currentEntryId
            androidx.compose.runtime.DisposableEffect(entryId) {
                onDispose {
                    if (entryId != null && !navigation.retainsEntry(entryId)) {
                        savedScreens.removeState("route:$entryId")
                    }
                }
            }
            com.ivy.design.system.FinancialVisibilityProvider(sharedPrefs) {
            NavigationRoot(navigation = navigation) { screen ->
            when (appLocked) {
                null -> { // display nothing
                }
                true -> {
                    key(activeTheme) {
                        IvyUI(
                            design = ivyDesign,
                            timeConverter = timeConverter,
                            timeProvider = timeProvider,
                            timeFormatter = timeFormatter,
                        ) {
                            com.ivy.security.ui.SecurityLockScreen()
                        }
                    }
                }

                false -> {
                    // Keep NavigationRoot alive so the current route survives a theme switch,
                    // but recreate the themed screen subtree to discard stale colors.
                    savedScreens.SaveableStateProvider("route:$entryId") {
                        key(activeTheme) {
                            IvyUI(
                                design = ivyDesign,
                                includeSurface = screen?.isLegacy ?: true,
                                timeConverter = timeConverter,
                                timeProvider = timeProvider,
                                timeFormatter = timeFormatter,
                            ) {
                                IvyNavGraph(screen)
                            }
                        }
                    }
                }
            }
            }

            IvyMaterial3Theme(
                dark = isDarkThemeEnabled(
                    ivyDesign = ivyDesign,
                    systemDarkTheme = isSystemInDarkTheme
                ),
                isTrueBlack = activeTheme == Theme.AMOLED_DARK
            ) {
                if (appLocked == false) {
                    if (viewModel.startupError) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = {},
                            title = { androidx.compose.material3.Text(getString(R.string.jvy_startup_failed_title)) },
                            text = { androidx.compose.material3.Text(getString(R.string.jvy_startup_failed_message)) },
                            confirmButton = { com.ivy.design.system.JvyTextButton({
                                viewModel.start(isSystemInDarkTheme, intent, pendingNavigationState)
                            }) { androidx.compose.material3.Text(getString(R.string.jvy_startup_retry)) } },
                            dismissButton = { com.ivy.design.system.JvyTextButton({ finish() }) {
                                androidx.compose.material3.Text(getString(R.string.cancel))
                            } },
                        )
                    } else if (viewModel.restorationNotice) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = viewModel::acknowledgeRestoration,
                            title = { androidx.compose.material3.Text(getString(R.string.jvy_restore_review_title)) },
                            text = { androidx.compose.material3.Text(getString(R.string.jvy_restore_review_message)) },
                            confirmButton = { com.ivy.design.system.JvyTextButton(viewModel::acknowledgeRestoration) {
                                androidx.compose.material3.Text(getString(R.string.done))
                            } },
                        )
                    }
                    dateTimePicker.Content()
                    com.ivy.security.ui.SecurityRecoveryHost()
                    pendingExport?.let { request ->
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { pendingExport = null },
                            title = { androidx.compose.material3.Text(getString(R.string.jvy_export_private_title)) },
                            text = { androidx.compose.material3.Text(getString(R.string.jvy_export_private_message)) },
                            confirmButton = { com.ivy.design.system.JvyTextButton({
                                pendingExport = null
                                onFileCreated = request.onCreated
                                createFileLauncher.launch(request.name)
                            }) { androidx.compose.material3.Text(getString(R.string.jvy_export_continue)) } },
                            dismissButton = { com.ivy.design.system.JvyTextButton({ pendingExport = null }) {
                                androidx.compose.material3.Text(getString(R.string.cancel))
                            } },
                        )
                    }
                }
            }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        (if (navigation.currentScreen == null) pendingNavigationState else
            navigation.saveState() ?: arrayListOf("jvy-navigation-unavailable"))?.let {
            outState.putStringArrayList("jvy.navigation.v1", it)
        }
        outState.putString("jvy.mainTab.v1", ivyContext.mainTab.name)
        com.ivy.legacy.data.model.TimePeriodSavedState.encode(ivyContext.selectedPeriod)?.let { outState.putStringArrayList("jvy.period.v1", it) }
        super.onSaveInstanceState(outState)
    }

    private fun setupApp() {
        setupActivityForResultLaunchers()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupDatePicker()
        setupTimePicker()
        AddTransactionWidget.updateBroadcast(this)
        AddTransactionWidgetCompact.updateBroadcast(this)
        WalletBalanceWidgetReceiver.updateBroadcast(this)
    }

    private fun setupDatePicker() {
        val picker = dateTimePicker
        ivyContext.onShowDatePicker = { minDate, maxDate, initialDate, onDatePicked ->
            picker.pickCalendarDate(initialDate, minDate, maxDate, onDatePicked)
        }
    }

    private fun setupTimePicker() {
        val picker = dateTimePicker
        ivyContext.onShowTimePicker = { initialTime, onTimePicked -> picker.pickTime(initialTime, onTimePicked) }
    }

    private fun isDarkThemeEnabled(ivyDesign: IvyDesign, systemDarkTheme: Boolean): Boolean {
        return when (ivyDesign.context().theme) {
            Theme.LIGHT -> false
            Theme.DARK -> true
            Theme.AMOLED_DARK -> true
            else -> systemDarkTheme
        }
    }

    private fun setupActivityForResultLaunchers() {
        createFileLauncher()

        openFileLauncher()
    }

    private fun createFileLauncher() {
        createFileLauncher = activityForResultLauncher(
            createIntent = { _, fileName ->
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = when {
                        fileName.endsWith(".zip", ignoreCase = true) -> "application/zip"
                        fileName.endsWith(".json", ignoreCase = true) -> "application/json"
                        else -> "text/csv"
                    }
                    putExtra(Intent.EXTRA_TITLE, fileName)

                    // Optionally, specify a URI for the directory that should be opened in
                    // the system file picker before your app creates the document.
                    putExtra(
                        DocumentsContract.EXTRA_INITIAL_URI,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .toURI()
                    )
                }
            }
        ) { _, intent ->
            val callback = onFileCreated
            onFileCreated = null
            intent?.data?.also {
                if (callback != null) callback(it) else fileRequestExpired()
            }
        }

        ivyContext.createNewFile = { fileName, onFileCreatedCallback ->
            if (pendingExport == null && onFileCreated == null) {
                pendingExport = ExportRequest(fileName, onFileCreatedCallback)
            }
        }
    }

    private fun openFileLauncher() {
        openFileLauncher = simpleActivityForResultLauncher(
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
        ) { _, intent ->
            val callback = onFileOpened
            onFileOpened = null
            intent?.data?.also {
                if (callback != null) callback(it) else fileRequestExpired()
            }
        }

        ivyContext.openFile = { onFileOpenedCallback ->
            onFileOpened = onFileOpenedCallback

            openFileLauncher.launch(Unit)
        }
    }

    private fun fileRequestExpired() {
        Toast.makeText(this, getString(R.string.jvy_file_request_expired), Toast.LENGTH_LONG).show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Exactly the same policy as the Compose observer: a focus change must not
        // undo the owner's capture preference or expose a one-time recovery code.
        if (com.ivy.security.shouldProtectScreenCapture(
                locked = viewModel.appLocked.value,
                lockEnabled = viewModel.securitySettings.value.enabled,
                preference = viewModel.screenCapturePreferences.allowed.value,
                credentialsVisible = navigation.currentScreen == com.ivy.navigation.SecurityScreen || securityUiViewModel.recoveryCode != null,
            )) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onBackPressed() {
        if (viewModel.isAppLocked()) {
            super.onBackPressed()
        } else {
            if (!navigation.onBackPressed()) {
                super.onBackPressed()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "PrintStackTrace")
    override fun openUrlInBrowser(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.data = Uri.parse(url)
            startActivity(browserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            e.sendToCrashlytics("Cannot open URL in browser, intent not supported.")
            Toast.makeText(
                this,
                "No browser app found. Visit manually: $url",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun shareIvyWallet() {
        val share = Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, Constants.URL_JVY_WALLET_REPO)
                type = "text/plain"
            },
            "Share Jvy Wallet"
        )
        startActivity(share)
    }

    @Suppress("SwallowedException")
    override fun openGooglePlayAppPage(appId: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId")))
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appId")
                )
            )
        }
    }

    override fun shareCSVFile(fileUri: Uri) {
        val intent = Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "text/csv"
            },
            null
        )
        startActivity(intent)
    }

    override fun shareZipFile(fileUri: Uri) {
        val intent = Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "application/zip"
            },
            null
        )
        startActivity(intent)
    }

    override val isDebug: Boolean
        get() = BuildConfig.DEBUG
    override val buildVersionName: String
        get() = BuildConfig.VERSION_NAME
    override val buildVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    override fun reviewIvyWallet(dismissReviewCard: Boolean) {
        val manager = ReviewManagerFactory.create(this)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = task.result
                reviewInfo.let { review ->
                    val flow = manager.launchReviewFlow(this, review!!)
                    flow.addOnCompleteListener {
                        // The flow has finished. The API does not indicate whether the user
                        // reviewed or not, or even whether the review dialog was shown. Thus, no
                        // matter the result, we continue our app flow.
                        if (dismissReviewCard) {
                            customerJourneyLogic.dismissCard(CustomerJourneyCardsProvider.rateUsCard())
                        }

                        openGooglePlayAppPage(packageName)
                    }
                }
            } else {
                openGooglePlayAppPage(packageName)
            }
        }
    }

    override fun <T> pinWidget(widget: Class<T>) {
        val appWidgetManager: AppWidgetManager = this.getSystemService(AppWidgetManager::class.java)
        val addTransactionWidget = ComponentName(this, widget)
        appWidgetManager.requestPinAppWidget(addTransactionWidget, null, null)
    }
}
