# Best practices and security alignment update: public-release hardening

Date: 2026-10-28. Implementation evidence, not a device-security certification.
Resumed review and exact core diff refreshed: 2026-10-30.

## Alignment areas and impact

- **High — private-data isolation:** remove Firebase runtime dependencies and the
  diagnostics/plugin initialization path, disable inherited survey writes, retain
  no-op legacy diagnostics adapters without forwarding their private arguments.
- **High — financial display privacy:** reactive preference redaction covers shared
  amount presenters and newly audited pages; hidden chart geometry/percentages are
  suppressed. Widget updates write zero placeholders when private instead of
  retaining the magnitudes in the widget's presentation cache.
  Root security-setting changes also request a widget refresh immediately, not
  only through the legacy Settings toggle or the next activity launch.
- **Medium — least privilege:** remove unused location, broad storage, alarm-setting
  and keyguard-disabling permissions. User-selected files use Android's document
  picker. Disable cleartext traffic; supporting exchange-rate requests use HTTPS.
- **Medium — exported entry robustness:** reject malformed launch extras without
  executing a financial action. Root remains public for the launcher/widgets;
  Quick Settings retains the system binding permission. Do not apply a blanket
  signature permission that would break system entry points.
- **Medium — screen-capture consistency:** focus changes and Compose use the same
  policy; locked/credential/recovery surfaces remain protected. This does not
  replace hardware checks for recents, capture, biometric and PIN behavior.
- **Medium — financial import confidentiality/integrity:** remove raw rejected
  rows and exception payloads from import diagnostics. Unknown/impossible dates,
  malformed grouping and nonfinite amounts now fail validation instead of
  acquiring invented historical timestamps or silently magnified amounts.

## Scope

Runtime dependencies/build: app, legacy-code, poll implementation.
Manifest: app manifest. Entry points: RootActivity and RootViewModel.
Presentation: FinancialVisibility, SharedPrefs observer, shared amount presenters,
Goals/Budgets/Loans/Reports/forecast/chart surfaces and balance widget.
Export: explicit private-data warning, MIME selection and safe expired callbacks.
Application-wide lock/widget observation: IvyAndroidApp and RootActivity.
Import diagnostics: ImportViewModel, CSVViewModel and BackupDataUseCase.
Failed-row UI: ImportResultUI and ImportResultPresentation mask the entire raw
CSV payload under hidden balances, because columns vary by source. The visible
row index is retained; arbitrary account names/amounts do not reach Text semantics.
Import validation: CSVImporter and CsvValueParsing; rejection and canonical-income
tests plus sanitized date fixtures (these parser changes are separately visible
in the working-tree diff). Valid legacy timestamp conversion is retained, not
certified as an exhaustive timezone audit.
No new dependency, key/package change or database/backup-schema migration.

## Implementation diff (dependency, manifest and entry-boundary core)

The following is the exact working-tree diff for the core security boundaries;
the working-tree diff also contains the associated UI changes.

```diff
diff --git a/app/build.gradle.kts b/app/build.gradle.kts
index f569b531..2d5ebecd 100644
--- a/app/build.gradle.kts
+++ b/app/build.gradle.kts
@@ -10,8 +10,6 @@ plugins {
     id("dagger.hilt.android.plugin")
     id("org.jetbrains.kotlin.plugin.serialization")
     id("com.google.devtools.ksp")
-    id("com.google.gms.google-services")
-    id("com.google.firebase.crashlytics")
     id("io.gitlab.arturbosch.detekt")
 }

@@ -173,8 +171,8 @@ android {
     lint {
         disable += "ComposeViewModelInjection"
         checkDependencies = true
-        abortOnError = false
-        checkReleaseBuilds = false
+        abortOnError = true
+        checkReleaseBuilds = true
         htmlReport = true
         htmlOutput = file("${project.rootDir}/build/reports/lint/lint.html")
         xmlReport = true
@@ -229,7 +227,6 @@ dependencies {
     implementation(libs.bundles.compose)
     implementation(libs.bundles.activity)
     implementation(libs.bundles.google)
-    implementation(libs.bundles.firebase)
     implementation(libs.datastore)
     implementation(libs.androidx.security)
     implementation(libs.androidx.biometrics)
diff --git a/app/src/main/AndroidManifest.xml b/app/src/main/AndroidManifest.xml
index ba00952b..e6571186 100644
--- a/app/src/main/AndroidManifest.xml
+++ b/app/src/main/AndroidManifest.xml
@@ -7,17 +7,7 @@
     <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
     <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
     <uses-permission android:name="android.permission.INTERNET" />
-    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
-    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
-    <uses-permission android:name="com.android.alarm.permission.SET_ALARM" />
-    <uses-permission
-        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
-        android:maxSdkVersion="28"
-        android:requestLegacyExternalStorage="true" />
-    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
     <uses-permission android:name="android.permission.VIBRATE" />
-    <!--  Show activity over the lockscreen  -->
-    <uses-permission android:name="android.permission.DISABLE_KEYGUARD" />
     <uses-permission android:name="android.permission.WAKE_LOCK" />

     <application
@@ -26,11 +16,14 @@
         android:hardwareAccelerated="true"
         android:icon="@mipmap/ic_launcher"
         android:label="@string/app_name"
-        android:requestLegacyExternalStorage="true"
         android:roundIcon="@mipmap/ic_launcher_round"
         android:supportsRtl="false"
         android:theme="@style/AppTheme"
-        android:usesCleartextTraffic="true">
+        android:usesCleartextTraffic="false">
+
+        <!-- Defense in depth: public builds must not initialize inherited diagnostics. -->
+        <meta-data android:name="firebase_data_collection_default_enabled" android:value="false" />
+        <meta-data android:name="firebase_crashlytics_collection_enabled" android:value="false" />

         <activity
             android:name=".RootActivity"
diff --git a/app/src/main/java/com/ivy/wallet/RootActivity.kt b/app/src/main/java/com/ivy/wallet/RootActivity.kt
index a6302a4e..b07cdf56 100644
--- a/app/src/main/java/com/ivy/wallet/RootActivity.kt
+++ b/app/src/main/java/com/ivy/wallet/RootActivity.kt
@@ -24,6 +24,8 @@ import androidx.compose.runtime.collectAsState
 import androidx.compose.runtime.getValue
 import androidx.compose.runtime.key
 import androidx.compose.runtime.remember
+import androidx.compose.runtime.mutableStateOf
+import androidx.compose.runtime.setValue
 import androidx.compose.runtime.saveable.rememberSaveableStateHolder
 import androidx.core.view.WindowCompat
 import androidx.lifecycle.viewmodel.compose.viewModel
@@ -78,13 +80,19 @@ class RootActivity : AppCompatActivity(), RootScreen {
     @Inject
     lateinit var dateTimePicker: DateTimePicker

+    @Inject
+    lateinit var sharedPrefs: com.ivy.base.legacy.SharedPrefs
+
     private lateinit var createFileLauncher: ActivityResultLauncher<String>
-    private lateinit var onFileCreated: (fileUri: Uri) -> Unit
+    private var onFileCreated: ((fileUri: Uri) -> Unit)? = null

     private lateinit var openFileLauncher: ActivityResultLauncher<Unit>
-    private lateinit var onFileOpened: (fileUri: Uri) -> Unit
+    private var onFileOpened: ((fileUri: Uri) -> Unit)? = null
+    private data class ExportRequest(val name: String, val onCreated: (Uri) -> Unit)
+    private var pendingExport by mutableStateOf<ExportRequest?>(null)

     private val viewModel: RootViewModel by viewModels()
+    private val securityUiViewModel: com.ivy.security.ui.SecurityViewModel by viewModels()

     @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
     override fun onCreate(savedInstanceState: Bundle?) {
@@ -108,8 +116,13 @@ class RootActivity : AppCompatActivity(), RootScreen {

             val appLocked by viewModel.appLocked.collectAsState()
             val lockSettings by viewModel.securitySettings.collectAsState()
+            LaunchedEffect(lockSettings.enabled) {
+                // Security settings bypass the legacy Settings toggle. Redact an already
+                // installed widget as soon as an app lock is enabled, not on the next launch.
+                WalletBalanceWidgetReceiver.updateBroadcast(this@RootActivity)
+            }
             val capturePreference by viewModel.screenCapturePreferences.allowed.collectAsState()
-            val securityUi: com.ivy.security.ui.SecurityViewModel = viewModel()
+            val securityUi = securityUiViewModel
             val sensitive = com.ivy.security.shouldProtectScreenCapture(
                 locked = appLocked, lockEnabled = lockSettings.enabled, preference = capturePreference,
                 credentialsVisible = navigation.currentScreen == com.ivy.navigation.SecurityScreen || securityUi.recoveryCode != null,
@@ -119,6 +132,15 @@ class RootActivity : AppCompatActivity(), RootScreen {
                 else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
             }
             val savedScreens = rememberSaveableStateHolder()
+            val entryId = navigation.currentEntryId
+            androidx.compose.runtime.DisposableEffect(entryId) {
+                onDispose {
+                    if (entryId != null && !navigation.retainsEntry(entryId)) {
+                        savedScreens.removeState("route:$entryId")
+                    }
+                }
+            }
+            com.ivy.design.system.FinancialVisibilityProvider(sharedPrefs) {
             NavigationRoot(navigation = navigation) { screen ->
             when (appLocked) {
                 null -> { // display nothing
@@ -139,7 +161,7 @@ class RootActivity : AppCompatActivity(), RootScreen {
                 false -> {
                     // Keep NavigationRoot alive so the current route survives a theme switch,
                     // but recreate the themed screen subtree to discard stale colors.
-                    savedScreens.SaveableStateProvider("route:${screen}") {
+                    savedScreens.SaveableStateProvider("route:$entryId") {
                         key(activeTheme) {
                             IvyUI(
                                 design = ivyDesign,
@@ -166,8 +188,24 @@ class RootActivity : AppCompatActivity(), RootScreen {
                 if (appLocked == false) {
                     dateTimePicker.Content()
                     com.ivy.security.ui.SecurityRecoveryHost()
+                    pendingExport?.let { request ->
+                        androidx.compose.material3.AlertDialog(
+                            onDismissRequest = { pendingExport = null },
+                            title = { androidx.compose.material3.Text(getString(R.string.jvy_export_private_title)) },
+                            text = { androidx.compose.material3.Text(getString(R.string.jvy_export_private_message)) },
+                            confirmButton = { com.ivy.design.system.JvyTextButton({
+                                pendingExport = null
+                                onFileCreated = request.onCreated
+                                createFileLauncher.launch(request.name)
+                            }) { androidx.compose.material3.Text(getString(R.string.jvy_export_continue)) } },
+                            dismissButton = { com.ivy.design.system.JvyTextButton({ pendingExport = null }) {
+                                androidx.compose.material3.Text(getString(R.string.cancel))
+                            } },
+                        )
+                    }
                 }
             }
+            }
         }
     }

@@ -213,7 +251,11 @@ class RootActivity : AppCompatActivity(), RootScreen {
             createIntent = { _, fileName ->
                 Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                     addCategory(Intent.CATEGORY_OPENABLE)
-                    type = "application/csv"
+                    type = when {
+                        fileName.endsWith(".zip", ignoreCase = true) -> "application/zip"
+                        fileName.endsWith(".json", ignoreCase = true) -> "application/json"
+                        else -> "text/csv"
+                    }
                     putExtra(Intent.EXTRA_TITLE, fileName)

                     // Optionally, specify a URI for the directory that should be opened in
@@ -226,15 +268,17 @@ class RootActivity : AppCompatActivity(), RootScreen {
                 }
             }
         ) { _, intent ->
+            val callback = onFileCreated
+            onFileCreated = null
             intent?.data?.also {
-                onFileCreated(it)
+                if (callback != null) callback(it) else fileRequestExpired()
             }
         }

         ivyContext.createNewFile = { fileName, onFileCreatedCallback ->
-            onFileCreated = onFileCreatedCallback
-
-            createFileLauncher.launch(fileName)
+            if (pendingExport == null && onFileCreated == null) {
+                pendingExport = ExportRequest(fileName, onFileCreatedCallback)
+            }
         }
     }

@@ -245,8 +289,10 @@ class RootActivity : AppCompatActivity(), RootScreen {
                 type = "*/*"
             }
         ) { _, intent ->
+            val callback = onFileOpened
+            onFileOpened = null
             intent?.data?.also {
-                onFileOpened(it)
+                if (callback != null) callback(it) else fileRequestExpired()
             }
         }

@@ -257,9 +303,20 @@ class RootActivity : AppCompatActivity(), RootScreen {
         }
     }

+    private fun fileRequestExpired() {
+        Toast.makeText(this, getString(R.string.jvy_file_request_expired), Toast.LENGTH_LONG).show()
+    }
+
     override fun onWindowFocusChanged(hasFocus: Boolean) {
         super.onWindowFocusChanged(hasFocus)
-        if (viewModel.isAppLockEnabled() || viewModel.isAppLocked() || navigation.currentScreen == com.ivy.navigation.SecurityScreen) {
+        // Exactly the same policy as the Compose observer: a focus change must not
+        // undo the owner's capture preference or expose a one-time recovery code.
+        if (com.ivy.security.shouldProtectScreenCapture(
+                locked = viewModel.appLocked.value,
+                lockEnabled = viewModel.securitySettings.value.enabled,
+                preference = viewModel.screenCapturePreferences.allowed.value,
+                credentialsVisible = navigation.currentScreen == com.ivy.navigation.SecurityScreen || securityUiViewModel.recoveryCode != null,
+            )) {
             window.setFlags(
                 WindowManager.LayoutParams.FLAG_SECURE,
                 WindowManager.LayoutParams.FLAG_SECURE
diff --git a/app/src/main/java/com/ivy/wallet/RootViewModel.kt b/app/src/main/java/com/ivy/wallet/RootViewModel.kt
index f62bce11..0d6b42cd 100644
--- a/app/src/main/java/com/ivy/wallet/RootViewModel.kt
+++ b/app/src/main/java/com/ivy/wallet/RootViewModel.kt
@@ -114,7 +114,9 @@ class RootViewModel @Inject constructor(
         val addTrnType: TransactionType? = try {
             intent.getSerializableExtra(EXTRA_ADD_TRANSACTION_TYPE) as? TransactionType
                 ?: TransactionType.valueOf(intent.getStringExtra(EXTRA_ADD_TRANSACTION_TYPE) ?: "")
-        } catch (e: IllegalArgumentException) {
+        } catch (_: RuntimeException) {
+            // Exported entry points can contain malformed Bundles as well as an
+            // unknown enum name. Reject the request; never process financial extras.
             null
         }

diff --git a/feature/import-data/src/main/java/com/ivy/importdata/csv/CSVViewModel.kt b/feature/import-data/src/main/java/com/ivy/importdata/csv/CSVViewModel.kt
index 07adfdac..1325d7a4 100644
--- a/feature/import-data/src/main/java/com/ivy/importdata/csv/CSVViewModel.kt
+++ b/feature/import-data/src/main/java/com/ivy/importdata/csv/CSVViewModel.kt
@@ -454,7 +454,7 @@ class CSVViewModel @Inject constructor(
         csv = processFile(uri, normalizeCSV = normalizeCSV)?.toImmutableList()
         columns = csv?.firstOrNull()
     } catch (e: Exception) {
-        e.printStackTrace()
+        timber.log.Timber.e("Could not open the selected CSV file.")
     }

     private fun processFile(
diff --git a/feature/import-data/src/main/java/com/ivy/importdata/csvimport/ImportViewModel.kt b/feature/import-data/src/main/java/com/ivy/importdata/csvimport/ImportViewModel.kt
index 4759c43b..8eabfec5 100644
--- a/feature/import-data/src/main/java/com/ivy/importdata/csvimport/ImportViewModel.kt
+++ b/feature/import-data/src/main/java/com/ivy/importdata/csvimport/ImportViewModel.kt
@@ -139,7 +139,8 @@ class ImportViewModel @Inject constructor(
             TestIdlingResource.increment()
             _importStep.value = ImportStep.LOADING
             try {
-                _importResult.value = backupDataUseCase.importAiPreview(preview)
+                val result: com.ivy.data.backup.ImportResult = backupDataUseCase.importAiPreview(preview)
+                _importResult.value = result
                 _aiPreview.value = null
                 _importStep.value = ImportStep.RESULT
             } catch (error: CancellationException) {
@@ -187,7 +188,7 @@ class ImportViewModel @Inject constructor(

                     _importStep.value = ImportStep.RESULT
                 } catch (e: Exception) {
-                    Timber.e(e, "Import error")
+                    Timber.e("Import failed; details are available in the in-app result.")
                     _importResult.value = failureResult(e)
                     _importStep.value = ImportStep.RESULT
                 } finally {
@@ -235,12 +236,12 @@ class ImportViewModel @Inject constructor(
                 )

                 if (result.failedRows.isNotEmpty()) {
-                    Timber.e("Import failed rows: ${result.failedRows}")
+                    Timber.e("Import rejected ${result.failedRows.size} rows; see the in-app result.")
                 }

                 result
             } catch (e: Exception) {
-                Timber.e(e, "CSV import error")
+                Timber.e("CSV import failed; details are available in the in-app result.")
                 ImportResult(
                     rowsFound = 0,
                     transactionsImported = 0,
diff --git a/feature/poll/impl/build.gradle.kts b/feature/poll/impl/build.gradle.kts
index 2281f2fd..37c5fc24 100644
--- a/feature/poll/impl/build.gradle.kts
+++ b/feature/poll/impl/build.gradle.kts
@@ -14,7 +14,6 @@ dependencies {
   implementation(projects.shared.ui.navigation)
   implementation(projects.feature.poll.public)

-  implementation(libs.firebase.firestore)

   testImplementation(projects.shared.ui.testing)
-}
\ No newline at end of file
+}
diff --git a/feature/poll/impl/src/main/java/com/ivy/poll/impl/data/PollRepositoryImpl.kt b/feature/poll/impl/src/main/java/com/ivy/poll/impl/data/PollRepositoryImpl.kt
index 9151e89e..89ac9fd3 100644
--- a/feature/poll/impl/src/main/java/com/ivy/poll/impl/data/PollRepositoryImpl.kt
+++ b/feature/poll/impl/src/main/java/com/ivy/poll/impl/data/PollRepositoryImpl.kt
@@ -4,9 +4,6 @@ import androidx.datastore.preferences.core.Preferences
 import androidx.datastore.preferences.core.booleanPreferencesKey
 import androidx.datastore.preferences.core.edit
 import arrow.core.Either
-import com.google.firebase.firestore.FieldValue
-import com.google.firebase.firestore.ktx.firestore
-import com.google.firebase.ktx.Firebase
 import com.ivy.data.datastore.IvyDataStore
 import com.ivy.domain.usecase.android.DeviceId
 import com.ivy.poll.data.PollRepository
@@ -35,32 +32,9 @@ class PollRepositoryImpl @Inject constructor(
     deviceId: DeviceId,
     poll: PollId,
     option: PollOptionId
-  ): Either<String, Unit> = suspendCoroutine { cont ->
-    // Define a reference to the document using the deviceId as its ID
-    val voteDocRef = Firebase.firestore
-      .collection("polls")
-      .document(poll.id)
-      .collection("votes")
-      .document(deviceId.value)
-
-    // Prepare the data for the vote
-    val voteData = mapOf(
-      "option" to option.value,
-      "timestamp" to FieldValue.serverTimestamp() // Good practice to store a timestamp
-    )
-
-    // Use .set() to create or overwrite the document with the specific ID
-    voteDocRef.set(voteData)
-      .addOnSuccessListener {
-        cont.resume(Either.Right(Unit))
-      }
-      .addOnFailureListener { e ->
-        val message = e.message ?: "null message"
-        cont.resume(Either.Left("FireStore - $message"))
-      }
-  }
+  ): Either<String, Unit> = Either.Left("This inherited survey is closed. No response was sent.")

   private fun votedKey(poll: PollId): Preferences.Key<Boolean> {
     return booleanPreferencesKey("poll.${poll.id}_voted")
   }
-}
\ No newline at end of file
+}
diff --git a/shared/data/core/src/main/java/com/ivy/data/backup/BackupDataUseCase.kt b/shared/data/core/src/main/java/com/ivy/data/backup/BackupDataUseCase.kt
index 779de529..77d9392f 100644
--- a/shared/data/core/src/main/java/com/ivy/data/backup/BackupDataUseCase.kt
+++ b/shared/data/core/src/main/java/com/ivy/data/backup/BackupDataUseCase.kt
@@ -192,7 +192,7 @@ class BackupDataUseCase @Inject constructor(
         } catch (e: kotlinx.coroutines.CancellationException) {
             throw e
         } catch (e: Exception) {
-            Timber.e(e, "Import error")
+            Timber.e("Backup import failed; details are available in the in-app result.")
             ImportResult(
                 rowsFound = 0,
                 transactionsImported = 0,
@@ -284,6 +284,12 @@ class BackupDataUseCase @Inject constructor(
         backupTransactionRunner.run {
             validatePlanningData(ivyWalletCompleteData)
             if (aiProfile) rejectAiConflicts(ivyWalletCompleteData)
+            // Check before inserting rows or deleting old settings. Failure rolls back
+            // the complete merge, including imports with archived implicit-unit records.
+            require(ivyWalletCompleteData.settings.map { it.currency.uppercase(java.util.Locale.ROOT) }.distinct().size <= 1) {
+                "The backup has conflicting wallet currencies. Nothing was imported."
+            }
+            ivyWalletCompleteData.settings.firstOrNull()?.let { settingsWriter.validateCurrencyChange(it.currency) }
             // An imported base currency must not reinterpret old implicit-currency accounts.
             val implicitAccounts = accountDao.findAll().filter { it.currency.isNullOrBlank() && !it.isDeleted }
             accountRepository.saveMany(with(accountMapper) {
diff --git a/temp/legacy-code/build.gradle.kts b/temp/legacy-code/build.gradle.kts
index 79690aba..807149a7 100644
--- a/temp/legacy-code/build.gradle.kts
+++ b/temp/legacy-code/build.gradle.kts
@@ -17,7 +17,6 @@ dependencies {

     implementation(libs.bundles.activity)
     implementation(libs.bundles.opencsv)
-    implementation(libs.bundles.firebase)
     implementation(libs.bundles.ktor)
     implementation(libs.androidx.work)
     implementation(libs.datastore)
diff --git a/temp/legacy-code/src/main/java/com/ivy/legacy/utils/FirebaseExt.kt b/temp/legacy-code/src/main/java/com/ivy/legacy/utils/FirebaseExt.kt
index adbe5ed4..f8ebaff4 100644
--- a/temp/legacy-code/src/main/java/com/ivy/legacy/utils/FirebaseExt.kt
+++ b/temp/legacy-code/src/main/java/com/ivy/legacy/utils/FirebaseExt.kt
@@ -1,24 +1,17 @@
 package com.ivy.legacy.utils

-import com.google.firebase.crashlytics.FirebaseCrashlytics
-
+/** Compatibility shims only. No diagnostics SDK, network or potentially private logs. */
+@Suppress("UNUSED_PARAMETER")
 fun sendToCrashlytics(
     msg: String
-) {
-    DeveloperException(msg).sendToCrashlytics(msg)
-}
+) = Unit

+@Suppress("UNUSED_PARAMETER")
 fun Exception.sendToCrashlytics(
     clarification: String? = null
-) {
-    clarification?.let {
-        logToCrashlytics("Log: $it")
-    }
-    FirebaseCrashlytics.getInstance().recordException(this)
-}
+) = Unit

-fun logToCrashlytics(msg: String) {
-    FirebaseCrashlytics.getInstance().log(msg)
-}
+@Suppress("UNUSED_PARAMETER")
+fun logToCrashlytics(msg: String) = Unit

 class DeveloperException(msg: String) : Exception(msg)
```

## Verification gates

- Final integrated compilation, 1,469 unit tests across 143 suites and app lint
  passed on 2026-10-30. The existing lint baseline is unchanged. This is not
  proof that every baselined finding or device-only security behavior is cleared.
- The actual `releaseRuntimeClasspath` resolution and
  `processReleaseMainManifest` tasks passed. The runtime graph contains no Firebase
  SDK. The merged release manifest has no Firebase initializer, location, broad
  storage, keyguard-disabling or SET_ALARM permissions, and cleartext is false.
  Rechecked after the final source changes on 2026-10-30.
- The actual `:app:verifyJvyReleaseSigning` task refused the repository's public
  test certificate with the intended error. This verifies the refusal gate, not
  an owner-key signature or an installable APK. No APK/version allocation occurred.
- Real device: unknown/malformed intent extras, locked shortcut/widget launches,
  manual save only, private capture/recents and backing out of file pickers.
- Observe actual offline and connected traffic. SDK removal is not a claim that
  Android, exchange-rate services or explicitly opened websites make no requests.
- Real-device validation remains blocked without attached hardware.
