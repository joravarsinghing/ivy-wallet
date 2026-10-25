package com.ivy.data.backup

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.data.DataObserver
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeBudgetDao
import com.ivy.data.db.dao.fake.FakeCategoryDao
import com.ivy.data.db.dao.fake.FakeLoanDao
import com.ivy.data.db.dao.fake.FakeLoanRecordDao
import com.ivy.data.db.dao.fake.FakePlannedPaymentDao
import com.ivy.data.db.dao.fake.FakeSettingsDao
import com.ivy.data.db.dao.fake.FakeTagAssociationDao
import com.ivy.data.db.dao.fake.FakeTagDao
import com.ivy.data.db.dao.fake.FakeTransactionDao
import com.ivy.data.db.dao.read.SavedAnalyticsDao
import com.ivy.data.db.dao.write.WriteSavedAnalyticsDao
import com.ivy.data.db.dao.read.GoalDao
import com.ivy.data.db.dao.read.GoalAllocationDao
import com.ivy.data.db.dao.read.CategoryRuleDao
import com.ivy.data.db.dao.write.WriteGoalDao
import com.ivy.data.db.dao.write.WriteGoalAllocationDao
import com.ivy.data.db.dao.write.WriteCategoryRuleDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.CategoryEntity
import com.ivy.data.db.entity.CategoryRuleEntity
import com.ivy.data.db.entity.GoalEntity
import com.ivy.data.db.entity.GoalAllocationEntity
import com.ivy.data.db.entity.GoalKind
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import com.ivy.data.repository.mapper.AccountMapper
import com.ivy.data.testResource
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import com.ivy.base.legacy.SharedPrefs
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BackupDataUseCaseTest {
    @Test fun `marketing backup restores and round trips all current planning records`() = runTest {
        val json = KotlinxSerializationModule.provideJson()
        val planning = PlanningStore()
        var wealth = com.ivy.data.model.wealth.WealthBackup()
        val wealthStore = mockk<com.ivy.data.repository.WealthRepository>(relaxed = true) {
            coEvery { read() } answers { wealth }
            coEvery { merge(any()) } answers { wealth = firstArg() }
        }
        val backup = newBackupDataUseCase(planning = planning, wealthStore = wealthStore)
        val payload = testResource("backups/jvy-marketing-2026-10.json").readText()
        val result = backup.importJson(payload)
        result.transactionsImported shouldBe 61
        result.accountsImported shouldBe 6
        planning.goals.size shouldBe 3
        planning.allocations.size shouldBe 4
        val restored = json.decodeFromString(IvyWalletCompleteData.serializer(), backup.generateJsonBackup())
        restored.transactions.size shouldBe 61
        restored.plannedPaymentRules.size shouldBe 4
        restored.loans.size shouldBe 2
        restored.loanRecords.size shouldBe 3
        restored.goals.size shouldBe 3
        restored.wealth!!.items.size shouldBe 12
        restored.wealth!!.valuations.size shouldBe 12
        restored.wealth!!.reviews.size shouldBe 3
        val again = newBackupDataUseCase()
        again.importJson(backup.generateJsonBackup()).transactionsImported shouldBe 61
    }

    @Test fun `AI preview is read only confirmation preserves preferences and repeats idempotently`() = runTest {
        val accountDao = FakeAccountDao()
        val transactionDao = FakeTransactionDao()
        val prefs = mockk<SharedPrefs>(relaxed = true)
        val useCase = newBackupDataUseCase(accountDao = accountDao, transactionDao = transactionDao, sharedPrefs = prefs)
        val payload = AiImportContract.guide.substringAfter("MINIMAL FICTIONAL EXAMPLE (do not reuse its IDs for real data):").substringBefore("JSON SCHEMA:").trim()
        val preview = useCase.prepareAiImport(payload)
        accountDao.findAll().size shouldBe 0
        transactionDao.findAll().size shouldBe 0
        useCase.importAiPreview(preview).transactionsImported shouldBe 1
        useCase.importAiPreview(preview).transactionsImported shouldBe 1
        transactionDao.findAll().size shouldBe 1
        verify(exactly = 0) { prefs.putBoolean(any(), any()) }
    }

    @Test fun `AI conflicting transaction ID is rejected before any write`() = runTest {
        val transactionDao = FakeTransactionDao()
        val useCase = newBackupDataUseCase(transactionDao = transactionDao)
        val payload = AiImportContract.guide.substringAfter("MINIMAL FICTIONAL EXAMPLE (do not reuse its IDs for real data):").substringBefore("JSON SCHEMA:").trim()
        val first = useCase.prepareAiImport(payload)
        useCase.importAiPreview(first)
        val conflict = useCase.prepareAiImport(payload.replace("45.50", "99.50"))
        try { useCase.importAiPreview(conflict); error("Expected conflicting ID refusal") }
        catch (_: IllegalArgumentException) { transactionDao.findAll().single().amount shouldBe 45.5 }
    }

    @Test fun `AI cannot resurrect a deleted transaction ID`() = runTest {
        val transactions = FakeTransactionDao()
        val payload = AiImportContract.guide.substringAfter("MINIMAL FICTIONAL EXAMPLE (do not reuse its IDs for real data):").substringBefore("JSON SCHEMA:").trim()
        val useCase = newBackupDataUseCase(transactionDao = transactions)
        val preview = useCase.prepareAiImport(payload)
        transactions.saveMany(preview.data.transactions.map { it.copy(isDeleted = true) })
        try { useCase.importAiPreview(preview); error("Expected deleted ID refusal") }
        catch (_: IllegalArgumentException) { transactions.findByIds(preview.data.transactions.map { it.id }).single().isDeleted shouldBe true }
    }

    // Ivy's persisted/backup timestamp contract is epoch milliseconds, not nanoseconds.
    private val fixtureTime = java.time.Instant.ofEpochMilli(1700000000123)

    private class PlanningStore {
        val goals = linkedMapOf<java.util.UUID, GoalEntity>()
        val allocations = linkedMapOf<Pair<java.util.UUID, java.util.UUID>, GoalAllocationEntity>()
        val rules = linkedMapOf<String, CategoryRuleEntity>()
        val goalsReader = mockk<GoalDao> {
            coEvery { findAll() } answers { goals.values.toList() }
        }
        val allocationsReader = mockk<GoalAllocationDao> {
            coEvery { findAll() } answers { allocations.values.toList() }
        }
        val rulesReader = mockk<CategoryRuleDao> {
            coEvery { findAll() } answers { rules.values.toList() }
        }
        val goalsWriter = mockk<WriteGoalDao> {
            coEvery { saveMany(any()) } answers { firstArg<List<GoalEntity>>().forEach { goals[it.id] = it } }
        }
        val allocationsWriter = mockk<WriteGoalAllocationDao> {
            coEvery { deleteByGoalId(any()) } answers {
                val goalId = firstArg<java.util.UUID>()
                allocations.keys.removeAll { it.first == goalId }
            }
            coEvery { saveMany(any()) } answers {
                firstArg<List<GoalAllocationEntity>>().forEach { allocations[it.goalId to it.accountId] = it }
            }
        }
        val rulesWriter = mockk<WriteCategoryRuleDao> {
            coEvery { saveMany(any()) } answers { firstArg<List<CategoryRuleEntity>>().forEach { rules[it.id] = it } }
        }
    }

    private fun newBackupDataUseCase(
        accountDao: FakeAccountDao = FakeAccountDao(),
        categoryDao: FakeCategoryDao = FakeCategoryDao(),
        transactionDao: FakeTransactionDao = FakeTransactionDao(),
        plannedPaymentDao: FakePlannedPaymentDao = FakePlannedPaymentDao(),
        budgetDao: FakeBudgetDao = FakeBudgetDao(),
        settingsDao: FakeSettingsDao = FakeSettingsDao(),
        loanDao: FakeLoanDao = FakeLoanDao(),
        loanRecordDao: FakeLoanRecordDao = FakeLoanRecordDao(),
        tagDao: FakeTagDao = FakeTagDao(),
        tagAssociationDao: FakeTagAssociationDao = FakeTagAssociationDao(),
        planning: PlanningStore = PlanningStore(),
        sharedPrefs: SharedPrefs = mockk(relaxed = true),
        wealthStore: com.ivy.data.repository.WealthRepository? = null,
    ): BackupDataUseCase {
        val accountMapper = AccountMapper(
            CurrencyRepository(
                settingsDao = settingsDao,
                writeSettingsDao = settingsDao,
                dispatchersProvider = TestDispatchersProvider,
            )
        )
        return BackupDataUseCase(
            accountDao = accountDao,
            accountMapper = accountMapper,
            accountRepository = AccountRepository(
                accountDao = accountDao,
                writeAccountDao = accountDao,
                mapper = accountMapper,
                dispatchersProvider = TestDispatchersProvider,
                memoFactory = fakeRepositoryMemoFactory(),
            ),
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            loanRecordDao = loanRecordDao,
            loanDao = loanDao,
            plannedPaymentRuleDao = plannedPaymentDao,
            transactionDao = transactionDao,
            transactionWriter = transactionDao,
            settingsDao = settingsDao,
            categoryWriter = categoryDao,
            settingsWriter = settingsDao,
            budgetWriter = budgetDao,
            loanWriter = loanDao,
            loanRecordWriter = loanRecordDao,
            plannedPaymentRuleWriter = plannedPaymentDao,

            context = mockk(relaxed = true),
            sharedPrefs = sharedPrefs,
            json = KotlinxSerializationModule.provideJson(),
            dispatchersProvider = TestDispatchersProvider,
            fileSystem = mockk(relaxed = true),
            dataObserver = DataObserver(),
            backupTransactionRunner = object : BackupTransactionRunner {
                override suspend fun <T> run(block: suspend () -> T): T = block()
            },
            tagsReader = tagDao,
            tagsWriter = tagDao,
            tagAssociationReader = tagAssociationDao,
            tagAssociationWriter = tagAssociationDao,
            savedAnalyticsReader = mockk<SavedAnalyticsDao>(relaxed = true),
            savedAnalyticsWriter = mockk<WriteSavedAnalyticsDao>(relaxed = true),
            wealthRepository = wealthStore ?: mockk<com.ivy.data.repository.WealthRepository>(relaxed = true).also {
                io.mockk.coEvery { it.read() } returns com.ivy.data.model.wealth.WealthBackup()
            },
            goalsReader = planning.goalsReader,
            goalAllocationsReader = planning.allocationsReader,
            categoryRulesReader = planning.rulesReader,
            goalsWriter = planning.goalsWriter,
            goalAllocationsWriter = planning.allocationsWriter,
            categoryRulesWriter = planning.rulesWriter,
        )
    }

    private suspend fun backupTestCase(backupVersion: String) {
        // given
        val originalBackupUseCase = newBackupDataUseCase()
        val backupJsonData = testResource("backups/$backupVersion.json")
            .readText(Charsets.UTF_16)

        // when
        val importedDataRes = originalBackupUseCase.importJson(backupJsonData, onProgress = {})

        // then
        importedDataRes.accountsImported shouldBeGreaterThan 0
        importedDataRes.transactionsImported shouldBeGreaterThan 0
        importedDataRes.categoriesImported shouldBeGreaterThan 0
        importedDataRes.failedRows.size shouldBe 0

        // Also - exporting and re-importing the data should work
        // given
        val exportedJson = originalBackupUseCase.generateJsonBackup()

        // when
        val freshBackupUseCase = newBackupDataUseCase()
        val reImportedDataRes = freshBackupUseCase.importJson(exportedJson, onProgress = {})
        // then
        reImportedDataRes shouldBe importedDataRes

        // Finally, exporting again should yield the same result
        freshBackupUseCase.generateJsonBackup() shouldBe exportedJson
    }

    @Test
    fun `backup compatibility with 450 (150)`() = runTest {
        backupTestCase("450-150")
    }

    @Test
    fun `planning data survives fresh restore including archived goals and inactive rules`() = runTest {
        val account = AccountEntity(name = "Savings", currency = "USD", color = 0)
        val category = CategoryEntity(name = "Groceries", color = 0)
        val goal = GoalEntity(name = "Emergency", kind = GoalKind.EMERGENCY_FUND, targetAmount = 25000.25, isActive = false, createdAt = fixtureTime)
        val allocation = GoalAllocationEntity(goal.id, account.id, 1000.75, createdAt = fixtureTime)
        val rule = CategoryRuleEntity("rule-1", category.id.toString(), "market", "DESCRIPTION", "CONTAINS", 5, false)
        val data = IvyWalletCompleteData(accounts = listOf(account), categories = listOf(category),
            goals = listOf(goal), goalAllocations = listOf(allocation), categoryRules = listOf(rule))
        val json = KotlinxSerializationModule.provideJson()
        val original = newBackupDataUseCase()
        original.importJson(json.encodeToString(data))
        val exported = original.generateJsonBackup()
        val restored = newBackupDataUseCase()
        restored.importJson(exported)
        val result = json.decodeFromString<IvyWalletCompleteData>(restored.generateJsonBackup())
        result.goals shouldBe data.goals
        result.goalAllocations shouldBe data.goalAllocations
        result.categoryRules shouldBe data.categoryRules
        restored.generateJsonBackup() shouldBe exported
    }

    @Test
    fun `existing account and category IDs are remapped inside planning references`() = runTest {
        val oldAccount = AccountEntity(name = "Savings", currency = "USD", color = 0)
        val oldCategory = CategoryEntity(name = "Groceries", color = 0)
        val currentAccount = oldAccount.copy(id = java.util.UUID.randomUUID())
        val currentCategory = oldCategory.copy(id = java.util.UUID.randomUUID())
        val accounts = FakeAccountDao().also { it.save(currentAccount) }
        val categories = FakeCategoryDao().also { it.save(currentCategory) }
        val goal = GoalEntity("House", GoalKind.PURCHASE, 100000.0, createdAt = fixtureTime)
        val planning = PlanningStore()
        val useCase = newBackupDataUseCase(accountDao = accounts, categoryDao = categories, planning = planning)
        val data = IvyWalletCompleteData(accounts = listOf(oldAccount), categories = listOf(oldCategory), goals = listOf(goal),
            goalAllocations = listOf(GoalAllocationEntity(goal.id, oldAccount.id, 250.0)),
            categoryRules = listOf(CategoryRuleEntity("r", oldCategory.id.toString(), "shop", "DESCRIPTION", "CONTAINS", 1, true)))
        useCase.importJson(KotlinxSerializationModule.provideJson().encodeToString(data))
        planning.allocations.values.single().accountId shouldBe currentAccount.id
        planning.rules.values.single().categoryId shouldBe currentCategory.id.toString()
    }

    @Test
    fun `restoring same name foreign account preserves both currencies and ledger references`() = runTest {
        val usd = AccountEntity("Cash", "USD", 0)
        val eur = AccountEntity("Cash", "EUR", 0)
        val accounts = FakeAccountDao().also { it.save(usd) }
        val oldTransaction = com.ivy.data.db.entity.TransactionEntity(usd.id, com.ivy.base.model.TransactionType.INCOME, 100.0)
        val newTransaction = oldTransaction.copy(accountId = eur.id, amount = 20.0, id = java.util.UUID.randomUUID())
        val transactions = FakeTransactionDao().also { it.save(oldTransaction) }
        val useCase = newBackupDataUseCase(accountDao = accounts, transactionDao = transactions)
        useCase.importJson(KotlinxSerializationModule.provideJson().encodeToString(IvyWalletCompleteData(accounts = listOf(eur), transactions = listOf(newTransaction))))
        accounts.findAll().associate { it.id to it.currency } shouldBe mapOf(usd.id to "USD", eur.id to "EUR")
        transactions.findAll().associate { it.id to it.accountId } shouldBe mapOf(oldTransaction.id to usd.id, newTransaction.id to eur.id)
    }

    @Test
    fun `same account ID currency conflict is rejected before transaction writes`() = runTest {
        val usd = AccountEntity("Cash", "USD", 0)
        val accounts = FakeAccountDao().also { it.save(usd) }
        val transactions = FakeTransactionDao()
        val useCase = newBackupDataUseCase(accountDao = accounts, transactionDao = transactions)
        val data = IvyWalletCompleteData(accounts = listOf(usd.copy(currency = "EUR")), transactions = listOf(
            com.ivy.data.db.entity.TransactionEntity(usd.id, com.ivy.base.model.TransactionType.INCOME, 100.0)))
        var rejected = false
        try { useCase.importJson(KotlinxSerializationModule.provideJson().encodeToString(data)) }
        catch (_: IllegalArgumentException) { rejected = true }
        rejected shouldBe true
        accounts.findAll().single().currency shouldBe "USD"
        transactions.findAll().size shouldBe 0
    }

    @Test
    fun `imported base currency cannot reinterpret existing implicit accounts`() = runTest {
        val usd = AccountEntity("Cash", null, 0)
        val eur = AccountEntity("Cash", null, 0)
        val accounts = FakeAccountDao().also { it.save(usd) }
        val settings = FakeSettingsDao().also { it.save(com.ivy.data.db.entity.SettingsEntity(com.ivy.base.legacy.Theme.LIGHT, "USD", 0.0, "")) }
        val useCase = newBackupDataUseCase(accountDao = accounts, settingsDao = settings)
        val data = IvyWalletCompleteData(accounts = listOf(eur), settings = listOf(
            com.ivy.data.db.entity.SettingsEntity(com.ivy.base.legacy.Theme.LIGHT, "EUR", 0.0, "")))
        useCase.importJson(KotlinxSerializationModule.provideJson().encodeToString(data))
        accounts.findAll().associate { it.id to it.currency } shouldBe mapOf(usd.id to "USD", eur.id to "EUR")
        settings.findFirst().currency shouldBe "EUR"
    }

    @Test
    fun `legacy backup without optional planning fields preserves existing planning data`() = runTest {
        val planning = PlanningStore()
        val goal = GoalEntity("Keep me", GoalKind.CUSTOM, 10.0, createdAt = fixtureTime)
        planning.goals[goal.id] = goal
        val useCase = newBackupDataUseCase(planning = planning)
        useCase.importJson(testResource("backups/450-150.json").readText(Charsets.UTF_16))
        planning.goals.values.toList() shouldBe listOf(goal)
    }

    @Test
    fun `goals-only backup is importable`() = runTest {
        val goal = GoalEntity("Goal", GoalKind.CUSTOM, 10.0, createdAt = fixtureTime)
        val planning = PlanningStore()
        val useCase = newBackupDataUseCase(planning = planning)
        useCase.importJson(KotlinxSerializationModule.provideJson().encodeToString(IvyWalletCompleteData(goals = listOf(goal))))
        planning.goals.values.toList() shouldBe listOf(goal)
    }

    @Test
    fun `negative reservation fails before any planning writes`() = runTest {
        val planning = PlanningStore()
        val goal = GoalEntity("Goal", GoalKind.CUSTOM, 10.0, createdAt = fixtureTime)
        val data = IvyWalletCompleteData(goals = listOf(goal), goalAllocations = listOf(
            GoalAllocationEntity(goal.id, java.util.UUID.randomUUID(), -5.0)))
        val failure = runCatching {
            newBackupDataUseCase(planning = planning).importJson(KotlinxSerializationModule.provideJson().encodeToString(data))
        }.exceptionOrNull()
        (failure is IllegalArgumentException) shouldBe true
        coVerify(exactly = 0) { planning.goalsWriter.saveMany(any()) }
        coVerify(exactly = 0) { planning.allocationsWriter.saveMany(any()) }
    }

    @Test
    fun `restore replaces imported goals reservations without affecting unrelated goals`() = runTest {
        val planning = PlanningStore()
        val goal = GoalEntity("Restored", GoalKind.CUSTOM, 10.0, createdAt = fixtureTime)
        val unrelated = GoalEntity("Unrelated", GoalKind.CUSTOM, 20.0, createdAt = fixtureTime)
        planning.goals[goal.id] = goal
        planning.goals[unrelated.id] = unrelated
        val stale = GoalAllocationEntity(goal.id, java.util.UUID.randomUUID(), 5.0)
        val keep = GoalAllocationEntity(unrelated.id, java.util.UUID.randomUUID(), 7.0)
        planning.allocations[stale.goalId to stale.accountId] = stale
        planning.allocations[keep.goalId to keep.accountId] = keep
        newBackupDataUseCase(planning = planning).importJson(KotlinxSerializationModule.provideJson()
            .encodeToString(IvyWalletCompleteData(goals = listOf(goal))))
        planning.allocations.values.toList() shouldBe listOf(keep)
    }

    @Test
    fun `historical references to deleted accounts and categories survive backup`() = runTest {
        val goal = GoalEntity("Archived", GoalKind.CUSTOM, 10.0, isActive = false, createdAt = fixtureTime)
        val allocation = GoalAllocationEntity(goal.id, java.util.UUID.randomUUID(), 5.0, createdAt = fixtureTime)
        val rule = CategoryRuleEntity("old", java.util.UUID.randomUUID().toString(), "shop", "DESCRIPTION", "CONTAINS", 0, false)
        val data = IvyWalletCompleteData(goals = listOf(goal), goalAllocations = listOf(allocation), categoryRules = listOf(rule))
        val json = KotlinxSerializationModule.provideJson()
        val source = newBackupDataUseCase()
        source.importJson(json.encodeToString(data))
        val destination = newBackupDataUseCase()
        destination.importJson(source.generateJsonBackup())
        val restored = json.decodeFromString<IvyWalletCompleteData>(destination.generateJsonBackup())
        restored.goalAllocations shouldBe listOf(allocation)
        restored.categoryRules shouldBe listOf(rule)
    }

    @Test
    fun `backup cannot export credentials or disable installation app lock`() = runTest {
        val prefs = mockk<SharedPrefs>(relaxed = true)
        val useCase = newBackupDataUseCase(sharedPrefs = prefs)
        val data = IvyWalletCompleteData(
            goals = listOf(GoalEntity("Goal", GoalKind.CUSTOM, 10.0, createdAt = fixtureTime)),
            sharedPrefs = hashMapOf(SharedPrefs.APP_LOCK_ENABLED to "false"),
        )
        val json = KotlinxSerializationModule.provideJson()
        useCase.importJson(json.encodeToString(data))
        verify(exactly = 0) { prefs.putBoolean(SharedPrefs.APP_LOCK_ENABLED, any()) }
        val exported = json.decodeFromString<IvyWalletCompleteData>(useCase.generateJsonBackup())
        exported.sharedPrefs.containsKey(SharedPrefs.APP_LOCK_ENABLED) shouldBe false
    }
}
