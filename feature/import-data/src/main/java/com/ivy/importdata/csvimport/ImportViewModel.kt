package com.ivy.importdata.csvimport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.frp.test.TestIdlingResource
import com.ivy.legacy.domain.deprecated.logic.csv.CSVImporter
import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.legacy.utils.asLiveData
import com.ivy.base.legacy.getFileName
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.Navigation
import com.ivy.onboarding.viewmodel.OnboardingViewModel
import com.ivy.wallet.domain.deprecated.logic.csv.CSVMapper
import com.ivy.wallet.domain.deprecated.logic.csv.CSVNormalizer
import com.ivy.data.file.FileSystem
import com.ivy.data.backup.ImportResult
import com.ivy.data.backup.AiImportContract
import com.ivy.data.backup.AiImportPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val ivyContext: com.ivy.legacy.IvyWalletCtx,
    private val nav: Navigation,
    private val fileReader: FileSystem,
    private val csvNormalizer: CSVNormalizer,
    private val csvMapper: CSVMapper,
    private val csvImporter: CSVImporter,
    private val backupDataUseCase: BackupDataUseCase
) : ViewModel() {
    private val _importStep = MutableLiveData<ImportStep>()
    val importStep = _importStep.asLiveData()

    private val _importType = MutableLiveData<ImportType>()
    val importType = _importType.asLiveData()

    private val _importProgressPercent = MutableLiveData<Int>()
    val importProgressPercent = _importProgressPercent.asLiveData()

    private val _importResult = MutableLiveData<ImportResult>()
    val importResult = _importResult.asLiveData()

    private val _aiPreview = MutableLiveData<AiImportPreview?>(null)
    val aiPreview = _aiPreview.asLiveData()
    private val _aiError = MutableLiveData<String?>(null)
    val aiError = _aiError.asLiveData()
    private var aiBusy = false

    fun start(screen: ImportScreen) {
        nav.onBackPressed[screen] = {
            when (importStep.value) {
                ImportStep.IMPORT_FROM -> false
                ImportStep.INSTRUCTIONS -> {
                    _importStep.value = ImportStep.IMPORT_FROM
                    true
                }

                ImportStep.AI_GUIDE -> {
                    _aiError.value = null
                    _importStep.value = ImportStep.IMPORT_FROM
                    true
                }

                ImportStep.AI_REVIEW -> {
                    _aiPreview.value = null
                    _aiError.value = null
                    _importStep.value = ImportStep.AI_GUIDE
                    true
                }

                ImportStep.LOADING -> {
                    // do nothing, disable back
                    true
                }

                ImportStep.RESULT -> {
                    _importStep.value = ImportStep.IMPORT_FROM
                    true
                }

                null -> false
            }
        }
    }

    fun openAiGuide() {
        _aiError.value = null
        _aiPreview.value = null
        _importStep.value = ImportStep.AI_GUIDE
    }

    fun selectAiFile() {
        if (aiBusy) return
        ivyContext.openFile { uri ->
            viewModelScope.launch {
                if (aiBusy) return@launch
                aiBusy = true
                TestIdlingResource.increment()
                _aiError.value = null
                _importStep.value = ImportStep.LOADING
                try {
                    val payload = withContext(Dispatchers.IO) {
                        fileReader.readAutoBounded(uri, AiImportContract.MAX_BYTES).fold(
                            { throw IllegalArgumentException("Cannot read this JSON file. Choose an uncompressed JSON file smaller than 8 MB.") }, { it })
                    }
                    _aiPreview.value = backupDataUseCase.prepareAiImport(payload)
                    _importStep.value = ImportStep.AI_REVIEW
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _aiPreview.value = null
                    _aiError.value = safeAiError(error)
                    _importStep.value = ImportStep.AI_GUIDE
                } finally {
                    aiBusy = false
                    TestIdlingResource.decrement()
                }
            }
        }
    }

    fun confirmAiImport() {
        val preview = _aiPreview.value ?: return
        if (aiBusy) return
        aiBusy = true
        viewModelScope.launch {
            TestIdlingResource.increment()
            _importStep.value = ImportStep.LOADING
            try {
                _importResult.value = backupDataUseCase.importAiPreview(preview)
                _aiPreview.value = null
                _importStep.value = ImportStep.RESULT
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _aiPreview.value = null
                _aiError.value = safeAiError(error)
                _importStep.value = ImportStep.AI_GUIDE
            } finally {
                aiBusy = false
                TestIdlingResource.decrement()
            }
        }
    }

    // Never expose parser excerpts or log the converted financial payload.
    private fun safeAiError(error: Exception): String = if (error is IllegalArgumentException &&
        error !is kotlinx.serialization.SerializationException) {
        error.message?.takeIf { it.length <= 300 } ?: "The file did not pass validation. Compare it with the copied guide."
    } else "The file could not be imported. Compare it with the copied guide; nothing was imported."

    @ExperimentalStdlibApi
    fun uploadFile(context: Context) {
        val importType = importType.value ?: return

        ivyContext.openFile { fileUri ->
            viewModelScope.launch {
                TestIdlingResource.increment()
                try {
                    _importStep.value = ImportStep.LOADING
                    _importProgressPercent.value = 0

                    _importResult.value = if (hasCSVExtension(context, fileUri)) {
                        restoreCSVFile(fileUri = fileUri, importType = importType)
                    } else {
                        backupDataUseCase.importBackupFile(
                            backupFileUri = fileUri
                        ) { progressPercent ->
                            com.ivy.legacy.utils.uiThread {
                                _importProgressPercent.value =
                                    (progressPercent * 100).roundToInt()
                            }
                        }
                    }

                    _importStep.value = ImportStep.RESULT
                } catch (e: Exception) {
                    Timber.e(e, "Import error")
                    _importResult.value = failureResult(e)
                    _importStep.value = ImportStep.RESULT
                } finally {
                    TestIdlingResource.decrement()
                }
            }
        }
    }

    @ExperimentalStdlibApi
    private suspend fun restoreCSVFile(fileUri: Uri, importType: ImportType): ImportResult {
        return com.ivy.legacy.utils.ioThread {
            val rawCSV = fileReader.readAuto(fileUri).getOrNull()
            if (rawCSV.isNullOrBlank()) {
                return@ioThread ImportResult(
                    rowsFound = 0,
                    transactionsImported = 0,
                    accountsImported = 0,
                    categoriesImported = 0,
                    failedRows = persistentListOf(),
                    errorMessage = "The selected CSV file is empty or unreadable."
                )
            }

            val normalizedCSV = csvNormalizer.normalize(
                rawCSV = rawCSV,
                importType = importType
            )

            val mapping = csvMapper.mapping(
                type = importType,
                headerRow = normalizedCSV.lineSequence().firstOrNull()
            )

            return@ioThread try {
                val result = csvImporter.import(
                    csv = normalizedCSV,
                    rowMapping = mapping,
                    onProgress = { progressPercent ->
                        com.ivy.legacy.utils.uiThread {
                            _importProgressPercent.value =
                                (progressPercent * 100).roundToInt()
                        }
                    }
                )

                if (result.failedRows.isNotEmpty()) {
                    Timber.e("Import failed rows: ${result.failedRows}")
                }

                result
            } catch (e: Exception) {
                Timber.e(e, "CSV import error")
                ImportResult(
                    rowsFound = 0,
                    transactionsImported = 0,
                    accountsImported = 0,
                    categoriesImported = 0,
                    failedRows = persistentListOf(),
                    errorMessage = e.userFacingImportMessage()
                )
            }
        }
    }

    fun setImportType(importType: ImportType) {
        _importType.value = importType
        _importStep.value = ImportStep.INSTRUCTIONS
    }

    fun skip(
        screen: ImportScreen,
        onboardingViewModel: OnboardingViewModel
    ) {
        if (screen.launchedFromOnboarding) {
            onboardingViewModel.importSkip()
        }

        nav.back()
        resetState()
    }

    fun finish(
        screen: ImportScreen,
        onboardingViewModel: OnboardingViewModel
    ) {
        if (screen.launchedFromOnboarding) {
            val result = importResult.value
            val importSuccess = result != null &&
                result.errorMessage == null &&
                (result.transactionsImported > 0 || result.accountsImported > 0 ||
                    result.categoriesImported > 0 || result.duplicatesSkipped > 0)
            onboardingViewModel.importFinished(
                success = importSuccess
            )
        }

        nav.back()
        resetState()
    }

    private fun resetState() {
        _aiPreview.value = null
        _aiError.value = null
        _importStep.value = ImportStep.IMPORT_FROM
    }

    private suspend fun hasCSVExtension(
        context: Context,
        fileUri: Uri
    ): Boolean = com.ivy.legacy.utils.ioThread {
        val fileName = context.getFileName(fileUri)
        fileName?.endsWith(suffix = ".csv", ignoreCase = true) ?: false
    }

    private fun failureResult(error: Throwable): ImportResult = ImportResult(
        rowsFound = 0,
        transactionsImported = 0,
        accountsImported = 0,
        categoriesImported = 0,
        failedRows = persistentListOf(),
        errorMessage = error.userFacingImportMessage()
    )

    private fun Throwable.userFacingImportMessage(): String =
        message?.takeIf { it.isNotBlank() }
            ?: "The import could not be completed. Check the result before retrying."
}
