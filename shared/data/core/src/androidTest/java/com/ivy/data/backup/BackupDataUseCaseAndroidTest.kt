package com.ivy.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.base.legacy.SharedPrefs
import com.ivy.data.DataObserver
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.file.FileSystem
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import com.ivy.data.repository.mapper.AccountMapper
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupDataUseCaseAndroidTest {

    private lateinit var db: IvyRoomDatabase
    private lateinit var useCase: BackupDataUseCase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, IvyRoomDatabase::class.java).build()
        val appContext = InstrumentationRegistry.getInstrumentation().context
        val accountMapper = AccountMapper(
            currencyRepository = CurrencyRepository(
                settingsDao = db.settingsDao,
                writeSettingsDao = db.writeSettingsDao,
                dispatchersProvider = TestDispatchersProvider,
            )
        )
        useCase = BackupDataUseCase(
            accountDao = db.accountDao,
            budgetDao = db.budgetDao,
            categoryDao = db.categoryDao,
            loanRecordDao = db.loanRecordDao,
            loanDao = db.loanDao,
            plannedPaymentRuleDao = db.plannedPaymentRuleDao,
            settingsDao = db.settingsDao,
            transactionDao = db.transactionDao,
            transactionWriter = db.writeTransactionDao,
            sharedPrefs = SharedPrefs(appContext),
            accountRepository = AccountRepository(
                accountDao = db.accountDao,
                writeAccountDao = db.writeAccountDao,
                mapper = accountMapper,
                dispatchersProvider = TestDispatchersProvider,
                memoFactory = fakeRepositoryMemoFactory(),
            ),
            accountMapper = accountMapper,
            categoryWriter = db.writeCategoryDao,
            settingsWriter = db.writeSettingsDao,
            budgetWriter = db.writeBudgetDao,
            loanWriter = db.writeLoanDao,
            loanRecordWriter = db.writeLoanRecordDao,
            plannedPaymentRuleWriter = db.writePlannedPaymentRuleDao,
            context = appContext,
            json = KotlinxSerializationModule.provideJson(),
            dispatchersProvider = TestDispatchersProvider,
            fileSystem = FileSystem(appContext),
            dataObserver = DataObserver(),
            backupTransactionRunner = RoomBackupTransactionRunner(db),
            tagsReader = db.tagDao,
            tagAssociationReader = db.tagAssociationDao,
            tagsWriter = db.writeTagDao,
            tagAssociationWriter = db.writeTagAssociationDao,
            savedAnalyticsReader = db.savedAnalyticsDao,
            savedAnalyticsWriter = db.writeSavedAnalyticsDao,
            wealthRepository = com.ivy.data.repository.WealthRepository(db),
            goalsReader = db.goalDao,
            goalAllocationsReader = db.goalAllocationDao,
            categoryRulesReader = db.categoryRuleDao,
            goalsWriter = db.writeGoalDao,
            goalAllocationsWriter = db.writeGoalAllocationDao,
            categoryRulesWriter = db.writeCategoryRuleDao,
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun backup450_150() = runBlocking {
        backupTestCase("450-150")
    }

    @Test
    fun planningMetadataSurvivesFreshDatabaseRestore() = runBlocking {
        val fixtureTime = java.time.Instant.ofEpochMilli(1700000000123)
        val account = com.ivy.data.db.entity.AccountEntity("Savings", "USD", 0)
        val category = com.ivy.data.db.entity.CategoryEntity("Groceries", 0)
        val goal = com.ivy.data.db.entity.GoalEntity("Emergency", com.ivy.data.db.entity.GoalKind.EMERGENCY_FUND, 25000.25, isActive = false, createdAt = fixtureTime)
        val allocation = com.ivy.data.db.entity.GoalAllocationEntity(goal.id, account.id, 1000.75, createdAt = fixtureTime)
        val rule = com.ivy.data.db.entity.CategoryRuleEntity("rule", category.id.toString(), "market", "DESCRIPTION", "CONTAINS", 5, false)
        db.writeAccountDao.save(account)
        db.writeCategoryDao.save(category)
        db.writeGoalDao.save(goal)
        db.writeGoalAllocationDao.save(allocation)
        db.writeCategoryRuleDao.save(rule)
        val exported = useCase.generateJsonBackup()
        closeDb()
        createDb()
        useCase.importJson(exported)
        org.junit.Assert.assertEquals(listOf(goal), db.goalDao.findAll())
        org.junit.Assert.assertEquals(listOf(allocation), db.goalAllocationDao.findAll())
        org.junit.Assert.assertEquals(listOf(rule), db.categoryRuleDao.findAll())
    }

    private suspend fun backupTestCase(version: String) {
        importBackupZipTestCase(version)
        importBackupJsonTestCase(version)

        // close and re-open the db to ensure fresh data
        closeDb()
        createDb()
        exportsAndImportsTestCase(version)
    }

    private suspend fun importBackupZipTestCase(version: String) {
        // given
        val backupUri = copyTestResourceToInternalStorage("backups/$version.zip")

        // when
        val res = useCase.importBackupFile(backupUri, onProgress = {})

        // then
        res.shouldBeSuccessful()
    }

    private suspend fun importBackupJsonTestCase(version: String) {
        // given
        val backupUri = copyTestResourceToInternalStorage("backups/$version.json")

        // when
        val res = useCase.importBackupFile(backupUri, onProgress = {})

        // then
        res.shouldBeSuccessful()
    }

    private suspend fun exportsAndImportsTestCase(version: String) {
        // given
        val backupUri = copyTestResourceToInternalStorage("backups/$version.zip")
        // preload data
        useCase.importBackupFile(backupUri, onProgress = {}).shouldBeSuccessful()
        val exportedFileUri = tempAndroidFile("exported", ".zip").toUri()

        // then
        useCase.exportToFile(exportedFileUri)
        val reImportRes = useCase.importBackupFile(backupUri, onProgress = {})

        // then
        reImportRes.shouldBeSuccessful()
    }

    private fun copyTestResourceToInternalStorage(resPath: String): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assetManager = context.assets
        val inputStream = assetManager.open(resPath)
        val outputFile = tempAndroidFile("temp-backup", resPath.split(".").last())
        outputFile.outputStream().use { fileOut ->
            fileOut.write(inputStream.readBytes())
        }
        return Uri.fromFile(outputFile)
    }

    private fun tempAndroidFile(prefix: String, suffix: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File.createTempFile(prefix, suffix, context.filesDir)
    }

    private fun ImportResult.shouldBeSuccessful() {
        failedRows.shouldBeEmpty()
        categoriesImported shouldBeGreaterThan 0
        accountsImported shouldBeGreaterThan 0
        transactionsImported shouldBeGreaterThan 0
    }
}
