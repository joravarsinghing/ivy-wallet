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
import com.ivy.data.db.dao.read.SavedAnalyticsDao
import com.ivy.data.db.dao.write.WriteBudgetDao
import com.ivy.data.db.dao.write.WriteCategoryDao
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.db.dao.write.WriteSettingsDao
import com.ivy.data.db.dao.write.WriteTagAssociationDao
import com.ivy.data.db.dao.write.WriteTagDao
import com.ivy.data.db.dao.write.WriteTransactionDao
import com.ivy.data.db.dao.write.WriteSavedAnalyticsDao
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
    private val tagAssociationWriter: WriteTagAssociationDao,
    private val savedAnalyticsReader: SavedAnalyticsDao,
    private val savedAnalyticsWriter: WriteSavedAnalyticsDao,
    private val wealthRepository: com.ivy.data.repository.WealthRepository,
    private val goalsReader: com.ivy.data.db.dao.read.GoalDao,
    private val goalAllocationsReader: com.ivy.data.db.dao.read.GoalAllocationDao,
    private val categoryRulesReader: com.ivy.data.db.dao.read.CategoryRuleDao,
    private val goalsWriter: com.ivy.data.db.dao.write.WriteGoalDao,
    private val goalAllocationsWriter: com.ivy.data.db.dao.write.WriteGoalAllocationDao,
    private val categoryRulesWriter: com.ivy.data.db.dao.write.WriteCategoryRuleDao,
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
        // Read linked collections from one consistent Room snapshot.
        return backupTransactionRunner.run { generateJsonBackupInTransaction() }
    }

    private suspend fun generateJsonBackupInTransaction(): String {
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
            val sharedPrefsData = async { getSharedPrefsData() }
            val tags = async { tagsReader.findAll() }
            val tagAssociations = async { tagAssociationReader.findAll() }
            val savedAnalytics = async { savedAnalyticsReader.findAll() }
            val netWorthAssetAccountIds = async { sharedPrefs.getStringList(SharedPrefs.NET_WORTH_ASSET_ACCOUNT_IDS) }
            val netWorthLiabilityAccountIds = async { sharedPrefs.getStringList(SharedPrefs.NET_WORTH_LIABILITY_ACCOUNT_IDS) }
            val netWorthHistory = async { sharedPrefs.getNetWorthHistory() }

            val completeData = IvyWalletCompleteData(
                accounts = accounts.await(),
                budgets = budgets.await(),
                categories = categories.await(),
                loanRecords = loanRecords.await(),
                loans = loans.await(),
                plannedPaymentRules = plannedPaymentRules.await(),
                settings = settings.await(),
                transactions = transactions.await(),
                sharedPrefs = sharedPrefsData.await(),
                tags = tags.await(),
                tagAssociations = tagAssociations.await(),
                savedAnalytics = savedAnalytics.await(),
                netWorthAssetAccountIds = netWorthAssetAccountIds.await(),
                netWorthLiabilityAccountIds = netWorthLiabilityAccountIds.await(),
                netWorthHistory = netWorthHistory.await(),
                netWorthSettingsVersion = 1,
                wealth = wealthRepository.read(),
                goals = goalsReader.findAll(),
                goalAllocations = goalAllocationsReader.findAll(),
                categoryRules = categoryRulesReader.findAll(),
            )

            json.encodeToString(completeData)
        }
    }

    private fun getSharedPrefsData(): HashMap<String, String> {
        val hashmap = HashMap<String, String>()
        hashmap[SharedPrefs.SHOW_NOTIFICATIONS] =
            sharedPrefs.getBoolean(SharedPrefs.SHOW_NOTIFICATIONS, true).toString()

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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
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
        val zipped = try {
            extractAndReadBackupZip(backupFileUri, onProgress)
        } catch (error: com.ivy.base.legacy.UnsafeArchiveException) {
            throw error // Never retry an unsafe archive as plain text.
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) { null }
        if (!zipped.isNullOrBlank()) return zipped

        return fileSystem.readAutoBounded(backupFileUri).fold({ throw it.e }, { it.takeIf(String::isNotBlank) })
    }

    private suspend fun extractAndReadBackupZip(
        backupFileUri: Uri,
        onProgress: suspend (progressPercent: Double) -> Unit
    ): String? {
        val cacheFolderPath = java.nio.file.Files.createTempDirectory(context.cacheDir.toPath(), "backup-").toFile()

        return try {
            unzip(context, backupFileUri, cacheFolderPath)

            onProgress(0.05)

            val filesList = cacheFolderPath.walkTopDown()
                .filter { it.isFile && hasJsonExtension(it) }
                .toList()

            onProgress(0.1)

            val jsonFile = filesList.singleOrNull()
                ?: error("The backup archive must contain exactly one JSON data file.")

            fileSystem.readAutoBounded(jsonFile.toUri()).fold({ throw it.e }, { it })
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
        // Validate original snapshot checksums before adapting source IDs to existing accounts.
        val originalData = json.decodeFromString<IvyWalletCompleteData>(jsonString)
        originalData.wealth?.let(wealthRepository::validate)
        val modifiedJsonString = accommodateExistingAccountsAndCategories(jsonString)
        val decodedData = modifiedJsonString?.let {
            json.decodeFromString<IvyWalletCompleteData>(it)
        } ?: error("Failed to parse backup JSON.")
        val ivyWalletCompleteData = decodedData.copy(wealth = decodedData.wealth?.let { wealth ->
            wealth.copy(snapshots = wealth.snapshots.map { snapshot ->
                val hash = java.security.MessageDigest.getInstance("SHA-256").digest(snapshot.inputs.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
                snapshot.copy(inputFingerprint = hash)
            })
        })

        require(ivyWalletCompleteData.hasImportableData()) {
            "The backup contains no accounts, transactions, or settings to import."
        }
        ivyWalletCompleteData.wealth?.let(wealthRepository::validate)

        onProgress(0.4)
        backupTransactionRunner.run {
            validatePlanningData(ivyWalletCompleteData)
            // An imported base currency must not reinterpret old implicit-currency accounts.
            val implicitAccounts = accountDao.findAll().filter { it.currency.isNullOrBlank() && !it.isDeleted }
            accountRepository.saveMany(with(accountMapper) {
                // Resolve through the same mapper as the ledger, including the locale fallback
                // on old installations which have accounts but no saved settings row.
                implicitAccounts.mapNotNull { it.toDomain().getOrNull() }
            })
            insertDataToDb(completeData = ivyWalletCompleteData, onProgress = onProgress)
            ivyWalletCompleteData.wealth?.let { wealthRepository.merge(it) }
        }
        applySharedPrefs(ivyWalletCompleteData.sharedPrefs)
        applyNetWorthSettings(ivyWalletCompleteData)
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
            || savedAnalytics.isNotEmpty()
            || goals.isNotEmpty() || goalAllocations.isNotEmpty() || categoryRules.isNotEmpty()
            || wealth?.items?.isNotEmpty() == true

    private fun validatePlanningData(data: IvyWalletCompleteData) {
        // Keep historical/orphaned metadata too: older builds allowed deleting referenced
        // accounts/categories without cleaning these tables. Do not make their backups
        // impossible to restore, or invent replacement ledger records for missing parents.
        require(data.goals.all { it.targetAmount.isFinite() && it.targetAmount > 0 }) {
            "A backup goal has an invalid target amount."
        }
        require(data.goalAllocations.all {
            it.reservedAmount.isFinite() && it.reservedAmount >= 0
        }) { "A backup reservation has an invalid amount." }
    }

    private suspend fun accommodateExistingAccountsAndCategories(jsonString: String?): String? {
        if (jsonString == null) return null

        val decoded = json.decodeFromString<IvyWalletCompleteData>(jsonString)
        val incomingBase = decoded.settings.firstOrNull()?.currency ?: settingsDao.findFirstOrNull()?.currency
        val ivyWalletCompleteData = decoded.copy(accounts = decoded.accounts.map { account ->
            account.copy(currency = restoreCurrency(account.currency, incomingBase))
        })
        val replacementPairs = getReplacementPairs(ivyWalletCompleteData)

        var modifiedString = json.encodeToString(ivyWalletCompleteData)
        replacementPairs.forEach {
            modifiedString = modifiedString.replace(it.first.toString(), it.second.toString())
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

            // Restore metadata only after its account/category parents exist. Empty fields
            // in older backups must not erase planning data already on this installation.
            goalsWriter.saveMany(completeData.goals)
            // Incoming goals carry their complete allocation set. Without clearing only
            // those goals, removed reservations would survive an otherwise successful restore.
            completeData.goals.forEach { goalAllocationsWriter.deleteByGoalId(it.id) }
            goalAllocationsWriter.saveMany(completeData.goalAllocations)
            categoryRulesWriter.saveMany(completeData.categoryRules)

            onProgress(0.7)

            val loans = async { loanWriter.saveMany(completeData.loans) }
            val loanRecords =
                async { loanRecordWriter.saveMany(completeData.loanRecords) }

            loans.await()
            loanRecords.await()

            onProgress(0.8)

            val tags = async { tagsWriter.save(completeData.tags) }
            val tagAssociations = async { tagAssociationWriter.save(completeData.tagAssociations) }
            val savedAnalytics = async {
                savedAnalyticsWriter.saveMany(completeData.savedAnalytics)
            }

            val plannedPayments =
                async { plannedPaymentRuleWriter.saveMany(completeData.plannedPaymentRules) }
            val settings = async {
                if (completeData.settings.isNotEmpty()) {
                    settingsWriter.deleteAll()
                    settingsWriter.saveMany(completeData.settings)
                }
            }

            plannedPayments.await()
            settings.await()
            tags.await()
            tagAssociations.await()
            savedAnalytics.await()

            onProgress(0.9)
        }
    }

    private fun applySharedPrefs(values: HashMap<String, String>) {
        sharedPrefs.putBoolean(
            SharedPrefs.SHOW_NOTIFICATIONS,
            (values[SharedPrefs.SHOW_NOTIFICATIONS] ?: "true").toBoolean()
        )
        // Authentication belongs to this installation. Imported preferences cannot disable it.
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

    private fun applyNetWorthSettings(data: IvyWalletCompleteData) {
        // New fields are optional so backups made by older Ivy Wallet versions remain valid.
        if (data.netWorthSettingsVersion >= 1) {
            sharedPrefs.putString(
                SharedPrefs.NET_WORTH_ASSET_ACCOUNT_IDS,
                data.netWorthAssetAccountIds.joinToString(",")
            )
            sharedPrefs.putString(
                SharedPrefs.NET_WORTH_LIABILITY_ACCOUNT_IDS,
                data.netWorthLiabilityAccountIds.joinToString(",")
            )
            sharedPrefs.putString(
                SharedPrefs.NET_WORTH_HISTORY,
                data.netWorthHistory.joinToString(";") { snapshot ->
                    "${snapshot.period}|${snapshot.assets}|${snapshot.liabilities}"
                }
            )
        }
    }

    private fun SharedPrefs.getStringList(key: String): List<String> =
        getString(key, null)
            ?.split(",")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?: emptyList()

    private fun SharedPrefs.getNetWorthHistory(): List<NetWorthBackupSnapshot> =
        getString(SharedPrefs.NET_WORTH_HISTORY, null)
            ?.split(";")
            ?.mapNotNull { value ->
                val parts = value.split("|")
                if (parts.size != 3) return@mapNotNull null
                NetWorthBackupSnapshot(
                    period = parts[0],
                    assets = parts[1].toDoubleOrNull() ?: return@mapNotNull null,
                    liabilities = parts[2].toDoubleOrNull() ?: return@mapNotNull null,
                )
            }
            ?: emptyList()

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

            val accountsReplace = accountRestoreReplacements(existingAccountsList, backupAccountsList,
                settingsDao.findFirstOrNull()?.currency, completeData.settings.firstOrNull()?.currency)
            val existingCategories = existingCategoryList.groupBy { it.name }
            val incomingCategories = backupCategoryList.groupBy { it.name }
            val existingIds = existingCategoryList.map { it.id }.toSet()
            val incomingIds = backupCategoryList.map { it.id }.toSet()
            val categoriesReplace = backupCategoryList.filter { it.id !in existingIds }.mapNotNull { category ->
                existingCategories[category.name]?.singleOrNull()?.takeIf {
                    it.id !in incomingIds && incomingCategories[category.name]?.size == 1
                }?.let { category.id to it.id }
            }
            accountsReplace + categoriesReplace
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
