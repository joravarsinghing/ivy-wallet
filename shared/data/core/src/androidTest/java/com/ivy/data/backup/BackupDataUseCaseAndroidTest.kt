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
import com.ivy.data.model.wealth.*
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

    @Test
    fun unsafeArchiveDoesNotTouchLedgerOrSiblingFiles() = runBlocking {
        val account = com.ivy.data.db.entity.AccountEntity("Savings", "USD", 0)
        db.writeAccountDao.save(account)
        val archive = tempAndroidFile("unsafe-backup", ".zip")
        val sentinel = File.createTempFile("sentinel", ".txt",
            InstrumentationRegistry.getInstrumentation().context.cacheDir).apply { writeText("unchanged") }
        try {
            java.util.zip.ZipOutputStream(archive.outputStream()).use { output ->
                output.putNextEntry(java.util.zip.ZipEntry("../${sentinel.name}"))
                output.write("overwritten".toByteArray())
                output.closeEntry()
            }
            val result = useCase.importBackupFile(archive.toUri(), onProgress = {})
            org.junit.Assert.assertNotNull(result.errorMessage)
            org.junit.Assert.assertEquals(0, result.accountsImported)
            org.junit.Assert.assertEquals(listOf(account), db.accountDao.findAll())
            org.junit.Assert.assertEquals("unchanged", sentinel.readText())
        } finally {
            archive.delete()
            sentinel.delete()
        }
    }

    @Test
    fun boundedFileReaderRejectsOversizedInputWithSafetyError() {
        val input = tempAndroidFile("bounded-backup", ".json").apply { writeText("x".repeat(64)) }
        try {
            val context = InstrumentationRegistry.getInstrumentation().context
            val failure = FileSystem(context).readAutoBounded(input.toUri(), maxBytes = 32)
                .fold({ it.e }, { null })
            org.junit.Assert.assertTrue(failure is com.ivy.base.legacy.UnsafeArchiveException)
        } finally {
            input.delete()
        }
    }

    @Test
    fun datedFinancialEditPersistsAndSurvivesFreshDatabaseRestore() = runBlocking {
        val repository = com.ivy.data.repository.WealthRepository(db)
        val item = WealthItem("item", "Estimate", WealthKind.OTHER_ASSET, "USD", createdDate = "2026-01-01")
        val policy = WealthPolicy("p1", item.id, "2026-01-01", recordedAt = "2026-10-24T12:00:00Z")
        val value = WealthValuation("v1", item.id, "2026-01-01", "500", recordedAt = policy.recordedAt)
        repository.save(item, policy, value)
        repository.save(item.copy(kind = WealthKind.OTHER_DEBT),
            policy.copy(id = "p2", effectiveDate = "2026-10-01"),
            value.copy(id = "v2", effectiveDate = "2026-10-01"))
        val edited = repository.read()
        org.junit.Assert.assertEquals(item, edited.items.single { it.id == item.id })
        org.junit.Assert.assertFalse(edited.policyOn(item.id, "2026-01-31")!!.archived)
        org.junit.Assert.assertTrue(edited.policyOn(item.id, "2026-10-24")!!.archived)
        org.junit.Assert.assertEquals(BalanceConvention.POSITIVE_OWED, edited.items.single { it.id != item.id }.convention)
        val exported = useCase.generateJsonBackup()
        closeDb()
        createDb()
        useCase.importJson(exported)
        val restored = com.ivy.data.repository.WealthRepository(db).read()
        org.junit.Assert.assertEquals(edited.items.toSet(), restored.items.toSet())
        org.junit.Assert.assertEquals(edited.policies.toSet(), restored.policies.toSet())
        org.junit.Assert.assertEquals(edited.valuations.toSet(), restored.valuations.toSet())
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
