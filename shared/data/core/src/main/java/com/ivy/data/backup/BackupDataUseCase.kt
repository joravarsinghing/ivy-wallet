package com.ivy.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.unzip
import com.ivy.base.legacy.zip
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.BudgetDao
import com.ivy.data.db.dao.read.CategoryDao
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.dao.read.LoanRecordDao
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.read.TagAssociationDao
import com.ivy.data.db.dao.read.TagDao
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.data.db.dao.write.WriteBudgetDao
import com.ivy.data.db.dao.write.WriteCategoryDao
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.db.dao.write.WriteSettingsDao
import com.ivy.data.db.dao.write.WriteTagAssociationDao
import com.ivy.data.db.dao.write.WriteTagDao
import com.ivy.data.db.dao.write.WriteTransactionDao
import com.ivy.data.file.FileSystem
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.mapper.AccountMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

// TODO: Legacy code, needs improvements
class BackupDataUseCase @Inject constructor(
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val loanRecordDao: LoanRecordDao,
    private val loanDao: LoanDao,
    private val plannedPaymentRuleDao: PlannedPaymentRuleDao,
    private val settingsDao: SettingsDao,
    private val transactionDao: TransactionDao,
    private val transactionWriter: WriteTransactionDao,
    private val sharedPrefs: SharedPrefs,
    private val accountRepository: AccountRepository,
    private val accountMapper: AccountMapper,
    private val categoryWriter: WriteCategoryDao,
    private val settingsWriter: WriteSettingsDao,
    private val budgetWriter: WriteBudgetDao,
    private val loanWriter: WriteLoanDao,
    private val loanRecordWriter: WriteLoanRecordDao,
    private val plannedPaymentRuleWriter: WritePlannedPaymentRuleDao,
    @ApplicationContext
    private val context: Context,
    private val json: Json,
    private val dispatchersProvider: DispatchersProvider,
    private val fileSystem: FileSystem,
    private val dataObserver: DataObserver,
    private val backupTransactionRunner: BackupTransactionRunner,
    private val tagsReader: TagDao,
    private val tagAssociationReader: TagAssociationDao,
    private val tagsWriter: WriteTagDao,
    private val tagAssociationWriter: WriteTagAssociationDao
) {
    suspend fun exportToFile(
        zipFileUri: Uri
    ) {
        val jsonString = generateJsonBackup()
        val file = createJsonDataFile(jsonString)
        try {
            zip(context = context, zipFileUri, listOf(file))
        } finally {
            file.delete()
        }
    }

    private fun createJsonDataFile(jsonString: String): File {
        val fileNamePrefix = "data"
        val fileNameSuffix = ".json"
        val outputDir = context.cacheDir

        val file = File.createTempFile(fileNamePrefix, fileNameSuffix, outputDir)
        FileOutputStream(file).use { it.write(jsonString.toByteArray(Charsets.UTF_16)) }

        return file
    }

    suspend fun generateJsonBackup(): String {
        return withContext(dispatchersProvider.io) {
            val accounts = async { accountDao.findAll() }
            val budgets = async { budgetDao.findAll() }
            val categories = async { categoryDao.findAll() }
            val loanRecords = async { loanRecordDao.findAll() }
            val loans = async { loanDao.findAll() }
            val plannedPaymentRules =
                async { plannedPaymentRuleDao.findAll() }
            val settings = async { settingsDao.findAll() }
            val transactions = async { transactionDao.findAll() }
            val sharedPrefs = async { getSharedPrefsData() }
            val tags = async { tagsReader.findAll() }
            val tagAssociations = async { tagAssociationReader.findAll() }

            val completeData = IvyWalletCompleteData(
                accounts = accounts.await(),
                budgets = budgets.await(),
                categories = categories.await(),
                loanRecords = loanRecords.await(),
                loans = loans.await(),
                plannedPaymentRules = plannedPaymentRules.await(),
                settings = settings.await(),
                transactions = transactions.await(),
                sharedPrefs = sharedPrefs.await(),
                tags = tags.await(),
                tagAssociations = tagAssociations.await()
            )

            json.encodeToString(completeData)
        }
    }

    private fun getSharedPrefsData(): HashMap<String, String> {
        val hashmap = HashMap<String, String>()
        hashmap[SharedPrefs.SHOW_NOTIFICATIONS] =
            sharedPrefs.getBoolean(SharedPrefs.SHOW_NOTIFICATIONS, true).toString()

        hashmap[SharedPrefs.APP_LOCK_ENABLED] =
            sharedPrefs.getBoolean(SharedPrefs.APP_LOCK_ENABLED, false).toString()

        hashmap[SharedPrefs.HIDE_CURRENT_BALANCE] =
            sharedPrefs.getBoolean(SharedPrefs.HIDE_CURRENT_BALANCE, false).toString()

        hashmap[SharedPrefs.HIDE_INCOME] =
            sharedPrefs.getBoolean(SharedPrefs.HIDE_INCOME, false).toString()

        hashmap[SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE] =
            sharedPrefs.getBoolean(SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE, false).toString()

        return hashmap
    }

    suspend fun importBackupFile(
        backupFileUri: Uri,
        onProgress: suspend (progressPercent: Double) -> Unit
    ): ImportResult = withContext(dispatchersProvider.io) {
        var imported = false
        return@withContext try {
            val jsonString = readBackupPayload(backupFileUri, onProgress)
                ?: error("The selected file is not a readable Ivy Wallet backup.")

            importJson(jsonString, onProgress).also {
                imported = it.errorMessage == null
            }
        } catch (e: Exception) {
            Timber.e(e, "Import error")
            ImportResult(
                rowsFound = 0,
                transactionsImported = 0,
                accountsImported = 0,
                categoriesImported = 0,
                failedRows = persistentListOf(),
                errorMessage = e.userFacingImportMessage()
            )
        } finally {
            if (imported) {
                dataObserver.post(DataWriteEvent.AllDataChange)
            }
        }
    }

    private suspend fun readBackupPayload(
        backupFileUri: Uri,
        onProgress: suspend (progressPercent: Double) -> Unit,
    ): String? {
        val zipped = runCatching {
            extractAndReadBackupZip(backupFileUri, onProgress)
        }.getOrNull()
        if (!zipped.isNullOrBlank()) return zipped

        return fileSystem.readAuto(backupFileUri).getOrNull()?.takeIf { it.isNotBlank() }
    }

    private suspend fun extractAndReadBackupZip(
        backupFileUri: Uri,
        onProgress: suspend (progressPercent: Double) -> Unit
    ): String? {
        val folderName = "backup" + System.currentTimeMillis()
        val cacheFolderPath = File(context.cacheDir, folderName)

        return try {
            unzip(context, backupFileUri, cacheFolderPath)

            onProgress(0.05)

            val filesList = cacheFolderPath.walkTopDown()
                .filter { it.isFile && hasJsonExtension(it) }
                .toList()

            onProgress(0.1)

            val jsonFile = filesList.singleOrNull()
                ?: error("The backup archive must contain exactly one JSON data file.")

            fileSystem.readAuto(jsonFile.toUri()).getOrNull()
        } finally {
            cacheFolderPath.deleteRecursively()
        }
    }

    suspend fun importJson(
        jsonString: String,
        onProgress: suspend (Double) -> Unit = {},
        clearCacheDir: Boolean = false,
    ): ImportResult {
        require(jsonString.isNotBlank()) { "The backup file is empty." }
        val modifiedJsonString = accommodateExistingAccountsAndCategories(jsonString)
        val ivyWalletCompleteData = modifiedJsonString?.let {
            json.decodeFromString<IvyWalletCompleteData>(it)
        } ?: error("Failed to parse backup JSON.")

        require(ivyWalletCompleteData.hasImportableData()) {
            "The backup contains no accounts, transactions, or settings to import."
        }

        onProgress(0.4)
        backupTransactionRunner.run {
            insertDataToDb(completeData = ivyWalletCompleteData, onProgress = onProgress)
        }
        applySharedPrefs(ivyWalletCompleteData.sharedPrefs)
        onProgress(1.0)

        return ImportResult(
            rowsFound = ivyWalletCompleteData.transactions.size,
            transactionsImported = ivyWalletCompleteData.transactions.size,
            accountsImported = ivyWalletCompleteData.accounts.size,
            categoriesImported = ivyWalletCompleteData.categories.size,
            failedRows = persistentListOf()
        )
    }

    private fun IvyWalletCompleteData.hasImportableData(): Boolean =
        accounts.isNotEmpty() ||
            transactions.isNotEmpty() ||
            categories.isNotEmpty() ||
            settings.isNotEmpty() ||
            budgets.isNotEmpty() ||
            loans.isNotEmpty() ||
            plannedPaymentRules.isNotEmpty()

    private suspend fun accommodateExistingAccountsAndCategories(jsonString: String?): String? {
        if (jsonString == null) return null

        val ivyWalletCompleteData = json.decodeFromString<IvyWalletCompleteData>(jsonString)
        val replacementPairs = getReplacementPairs(ivyWalletCompleteData)

        var modifiedString = jsonString
        replacementPairs.forEach {
            modifiedString = modifiedString!!.replace(it.first.toString(), it.second.toString())
        }

        return modifiedString
    }

    private suspend fun insertDataToDb(
        completeData: IvyWalletCompleteData,
        onProgress: suspend (progressPercent: Double) -> Unit = {}
    ) {
        withContext(dispatchersProvider.io) {
            transactionWriter.saveMany(completeData.transactions)
            onProgress(0.6)

            val accounts = async {
                val domainAccounts = with(accountMapper) {
                    completeData.accounts.mapNotNull { entity ->
                        entity.toDomain().getOrNull()
                    }
                }
                accountRepository.saveMany(domainAccounts)
            }
            val budgets = async { budgetWriter.saveMany(completeData.budgets) }
            val categories =
                async { categoryWriter.saveMany(completeData.categories) }
            accounts.await()
            budgets.await()
            categories.await()

            onProgress(0.7)

            val loans = async { loanWriter.saveMany(completeData.loans) }
            val loanRecords =
                async { loanRecordWriter.saveMany(completeData.loanRecords) }

            loans.await()
            loanRecords.await()

            onProgress(0.8)

            val tags = async { tagsWriter.save(completeData.tags) }
            val tagAssociations = async { tagAssociationWriter.save(completeData.tagAssociations) }

            val plannedPayments =
                async { plannedPaymentRuleWriter.saveMany(completeData.plannedPaymentRules) }
            val settings = async {
                settingsWriter.deleteAll()
                settingsWriter.saveMany(completeData.settings)
            }

            plannedPayments.await()
            settings.await()
            tags.await()
            tagAssociations.await()

            onProgress(0.9)
        }
    }

    private fun applySharedPrefs(values: HashMap<String, String>) {
        sharedPrefs.putBoolean(
            SharedPrefs.SHOW_NOTIFICATIONS,
            (values[SharedPrefs.SHOW_NOTIFICATIONS] ?: "true").toBoolean()
        )
        sharedPrefs.putBoolean(
            SharedPrefs.APP_LOCK_ENABLED,
            (values[SharedPrefs.APP_LOCK_ENABLED] ?: "false").toBoolean()
        )
        sharedPrefs.putBoolean(
            SharedPrefs.HIDE_CURRENT_BALANCE,
            (values[SharedPrefs.HIDE_CURRENT_BALANCE] ?: "false").toBoolean()
        )
        sharedPrefs.putBoolean(
            SharedPrefs.HIDE_INCOME,
            (values[SharedPrefs.HIDE_INCOME] ?: "false").toBoolean()
        )
        sharedPrefs.putBoolean(
            SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE,
            (values[SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE] ?: "false").toBoolean()
        )
    }

    /** This is used to replace account & category Ids in backup data with existing Ids
     *  This removes the problem of duplicate Accounts & Categories
     *
     *  returns a Pair<A,B> of IDs where A is the UUID that needs to be replaced with B
     */
    private suspend fun getReplacementPairs(
        completeData: IvyWalletCompleteData
    ): List<Pair<UUID, UUID>> {
        return withContext(dispatchersProvider.io) {
            val existingAccountsList = accountDao.findAll()
            val existingCategoryList = categoryDao.findAll()

            val backupAccountsList = completeData.accounts
            val backupCategoryList = completeData.categories

            if (existingAccountsList.isEmpty() && existingCategoryList.isEmpty()) {
                return@withContext emptyList()
            }

            val sumAccountList = existingAccountsList + backupAccountsList
            val sumCategoriesList = existingCategoryList + backupCategoryList

            val accountsReplace = async {
                sumAccountList.groupBy { it.name }.filter { it.value.size == 2 }.map {
                    val accountsZero = it.value[0]
                    val accountsFirst = it.value[1]

                    if (backupAccountsList.contains(accountsZero)) {
                        Pair(accountsZero.id, accountsFirst.id)
                    } else {
                        Pair(accountsFirst.id, accountsZero.id)
                    }
                }
            }

            val categoriesReplace = async {
                sumCategoriesList.groupBy { it.name }.filter { it.value.size == 2 }.map {
                    val categoryZero = it.value[0]
                    val categoryFirst = it.value[1]

                    if (completeData.categories.contains(categoryZero)) {
                        Pair(categoryZero.id, categoryFirst.id)
                    } else {
                        Pair(categoryFirst.id, categoryZero.id)
                    }
                }
            }

            accountsReplace.await() + categoriesReplace.await()
        }
    }

    private fun hasJsonExtension(file: File): Boolean {
        val name = file.name
        val lastIndexOf = name.lastIndexOf(".")
        if (lastIndexOf == -1) {
            return false
        }

        return (name.substring(lastIndexOf).equals(".json", true))
    }


    private fun Throwable.userFacingImportMessage(): String = when (this) {
        is kotlinx.serialization.SerializationException ->
            "This file is not a supported Ivy Wallet backup, or it is damaged."

        else -> message?.takeIf { it.isNotBlank() }
            ?: "The import could not be completed. Check the result before retrying."
    }
}
