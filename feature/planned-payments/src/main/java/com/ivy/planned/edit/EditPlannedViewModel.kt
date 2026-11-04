package com.ivy.planned.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.model.TransactionType
import com.ivy.base.threading.DispatchersProvider
import com.ivy.base.time.TimeConverter
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.IntervalType
import com.ivy.data.repository.CategoryRepository
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.legacy.domain.deprecated.logic.AccountCreator
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.deprecated.logic.CategoryCreator
import com.ivy.wallet.domain.deprecated.logic.PlannedPaymentWriter
import com.ivy.wallet.domain.deprecated.logic.PlannedPaymentConflict
import com.ivy.design.system.ProcessDraftState
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.ui.theme.modal.RecurringRuleModalData
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@Stable
@HiltViewModel
class EditPlannedViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryRepository: CategoryRepository,
    private val settingsDao: SettingsDao,
    private val nav: Navigation,
    private val plannedPaymentRuleDao: PlannedPaymentRuleDao,
    private val plannedWriter: PlannedPaymentWriter,
    private val categoryCreator: CategoryCreator,
    private val accountCreator: AccountCreator,
    private val accountsAct: AccountsAct,
    private val timeConverter: TimeConverter,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<EditPlannedScreenState, EditPlannedScreenEvent>() {

    private var transactionType by mutableStateOf(TransactionType.INCOME)
    private var startDate by mutableStateOf<LocalDateTime?>(null)
    private var intervalN by mutableStateOf<Int?>(null)
    private var intervalType by mutableStateOf<IntervalType?>(null)
    private var oneTime by mutableStateOf(false)
    private var initialTitle by mutableStateOf<String?>(null)
    private var description by mutableStateOf<String?>(null)
    private var account by mutableStateOf<Account?>(null)
    private var category by mutableStateOf<Category?>(null)
    private var categorySelectionRevision = 0L
    private var amount by mutableDoubleStateOf(0.0)
    private var currency by mutableStateOf("")
    private var categories by mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private var accounts by mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private var categoryModalVisible by mutableStateOf(false)
    private var descriptionModalVisible by mutableStateOf(false)
    private var deleteTransactionModalVisible by mutableStateOf(false)
    private var transactionTypeModalVisible by mutableStateOf(false)
    private var amountModalVisible by mutableStateOf(false)
    private var recurringRuleModalData by mutableStateOf<RecurringRuleModalData?>(null)
    val editorModals = com.ivy.legacy.ui.EditorModalHost()
    private var categoryModalData: CategoryModalData?
        get() = editorModals.category
        set(value) { editorModals.category = value }
    private var accountModalData: AccountModalData?
        get() = editorModals.account
        set(value) { editorModals.account = value }

    private var loadedRule: PlannedPaymentRule? = null
    private var committedRule: PlannedPaymentRule? = null
    var ready by mutableStateOf(false)
        private set
    var loadError by mutableStateOf(false)
        private set
    private var editMode = false
    private var title: String? = null
    private var startedScreen: EditPlannedScreen? = null
    private var startedEntryId: java.util.UUID? = null
    private var unregisterBackGuard: (() -> Unit)? = null
    private var startupJob: kotlinx.coroutines.Job? = null
    private val saveMutex = kotlinx.coroutines.sync.Mutex()
    private var explicitSavePending by mutableStateOf(false)
    private var saveError by mutableStateOf<String?>(null)
    private var writeRevision = 0L

    @Composable
    override fun uiState(): EditPlannedScreenState {
        return EditPlannedScreenState(
            currency = getCurrency(),
            saving = explicitSavePending,
            saveError = saveError,
            categories = getCategories(),
            accounts = getAccounts(),
            transactionType = getTransactionType(),
            startDate = getStartDate(),
            intervalN = getIntervalN(),
            oneTime = getOneTime(),
            account = getAccount(),
            category = getCategory(),
            amount = getAmount(),
            initialTitle = getInitialTitle(),
            description = getDescription(),
            intervalType = getIntervalType(),
            categoryModalVisible = getCategoryModalVisibility(),
            categoryModalData = getCategoryModalData(),
            accountModalData = getAccountModalData(),
            deleteTransactionModalVisible = getDeleteTransactionModalVisibility(),
            descriptionModalVisible = getDescriptionModalVisibility(),
            amountModalVisible = getAmountModalVisibility(),
            transactionTypeModalVisible = getTransactionTypeModalVisibility(),
            recurringRuleModalData = getRecurringRuleModalData()
        )
    }

    @Composable
    private fun getCurrency(): String {
        return currency
    }

    @Composable
    private fun getCategories(): ImmutableList<Category> {
        return categories
    }

    @Composable
    private fun getAccounts(): ImmutableList<Account> {
        return accounts
    }

    @Composable
    private fun getTransactionType(): TransactionType {
        return transactionType
    }

    @Composable
    private fun getStartDate(): LocalDateTime? {
        return startDate
    }

    @Composable
    private fun getIntervalN(): Int? {
        return intervalN
    }

    @Composable
    private fun getIntervalType(): IntervalType? {
        return intervalType
    }

    @Composable
    private fun getOneTime(): Boolean {
        return oneTime
    }

    @Composable
    private fun getInitialTitle(): String? {
        return initialTitle
    }

    @Composable
    private fun getDescription(): String? {
        return description
    }

    @Composable
    private fun getAccount(): Account? {
        return account
    }

    @Composable
    private fun getCategory(): Category? {
        return category
    }

    @Composable
    private fun getAmount(): Double {
        return amount
    }

    @Composable
    private fun getCategoryModalVisibility(): Boolean {
        return categoryModalVisible
    }

    @Composable
    private fun getDescriptionModalVisibility(): Boolean {
        return descriptionModalVisible
    }

    @Composable
    private fun getDeleteTransactionModalVisibility(): Boolean {
        return deleteTransactionModalVisible
    }

    @Composable
    private fun getTransactionTypeModalVisibility(): Boolean {
        return transactionTypeModalVisible
    }

    @Composable
    private fun getAmountModalVisibility(): Boolean {
        return amountModalVisible
    }

    @Composable
    private fun getCategoryModalData(): CategoryModalData? {
        return categoryModalData
    }

    @Composable
    private fun getAccountModalData(): AccountModalData? {
        return accountModalData
    }

    @Composable
    private fun getRecurringRuleModalData(): RecurringRuleModalData? {
        return recurringRuleModalData
    }

    override fun onEvent(event: EditPlannedScreenEvent) {
        if (explicitSavePending) return // Also reject late picker callbacks, not only disabled taps.
        when (event) {
            is EditPlannedScreenEvent.OnSave -> save(event.closeScreen)
            is EditPlannedScreenEvent.OnDelete -> delete()
            is EditPlannedScreenEvent.OnSetTransactionType ->
                updateTransactionType(event.newTransactionType)

            is EditPlannedScreenEvent.OnDescriptionChanged ->
                updateDescription(event.newDescription)

            is EditPlannedScreenEvent.OnCreateAccount -> createAccount(event.data, event.onResult)
            is EditPlannedScreenEvent.OnCreateCategory -> createCategory(event.data, event.onResult)
            is EditPlannedScreenEvent.OnAccountChanged -> updateAccount(event.newAccount)
            is EditPlannedScreenEvent.OnAmountChanged -> updateAmount(event.newAmount)
            is EditPlannedScreenEvent.OnTitleChanged -> updateTitle(event.newTitle)
            is EditPlannedScreenEvent.OnRuleChanged ->
                updateRule(event.startDate, event.oneTime, event.intervalN, event.intervalType)

            is EditPlannedScreenEvent.OnCategoryChanged -> updateCategory(event.newCategory)
            is EditPlannedScreenEvent.OnEditCategory -> editCategory(event.updatedCategory, event.onResult)
            is EditPlannedScreenEvent.OnCategoryModalVisible ->
                categoryModalVisible = event.visible

            is EditPlannedScreenEvent.OnCategoryModalDataChanged ->
                if (event.expectedId == null || categoryModalData?.id == event.expectedId) {
                    categoryModalData = event.categoryModalData
                }

            is EditPlannedScreenEvent.OnAccountModalDataChanged -> {
                if (event.expectedId == null || accountModalData?.id == event.expectedId) accountModalData = event.accountModalData
            }

            is EditPlannedScreenEvent.OnDescriptionModalVisible ->
                descriptionModalVisible = event.visible

            is EditPlannedScreenEvent.OnTransactionTypeModalVisible ->
                transactionTypeModalVisible = event.visible

            is EditPlannedScreenEvent.OnAmountModalVisible ->
                amountModalVisible = event.visible

            is EditPlannedScreenEvent.OnDeleteTransactionModalVisible ->
                deleteTransactionModalVisible = event.visible

            is EditPlannedScreenEvent.OnRecurringRuleModalDataChanged ->
                recurringRuleModalData = event.recurringRuleModalData
        }
    }

    fun start(screen: EditPlannedScreen, restored: List<String>? = null) {
        unregisterBackGuard?.invoke()
        unregisterBackGuard = nav.registerScreenBackHandler(screen) { explicitSavePending }
        val entryId = nav.currentEntryId
        if (startedScreen == screen && startedEntryId == entryId &&
            (ready || startupJob?.isActive == true)) return
        startupJob?.cancel()
        startedScreen = screen
        startedEntryId = entryId
        ready = false
        loadError = false
        explicitSavePending = false
        saveError = null
        startupJob = viewModelScope.launch {
            try {
            transactionType = screen.type
            editMode = screen.plannedPaymentRuleId != null

            val accounts = accountsAct(Unit)
            if (accounts.isEmpty()) {
                closeScreen()
                return@launch
            }
            this@EditPlannedViewModel.accounts = accounts
            categories = categoryRepository.findAll().toImmutableList()

            reset()

            val existing = screen.plannedPaymentRuleId?.let { withContext(dispatchers.io) { plannedPaymentRuleDao.findById(it)?.toLegacyDomain() } }
            if (screen.plannedPaymentRuleId != null && existing == null) {
                loadError = true
                return@launch // A deleted edit must never become a new payment.
            }
            committedRule = existing
            loadedRule = existing ?: PlannedPaymentRule(
                startDate = null,
                intervalN = null,
                intervalType = null,
                oneTime = false,
                type = screen.type,
                amount = screen.amount ?: 0.0,
                accountId = screen.accountId ?: accounts.first().id,
                categoryId = screen.categoryId,
                title = screen.title,
                description = screen.description
            )
            if (restored != null) restoreDraft(screen, restored)
            if (nav.currentEntryId != entryId) return@launch
            display(loadedRule!!)
            ready = true
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (_: Exception) { loadedRule = null; loadError = true }
        }
    }

    fun saveDraft(): List<String>? = loadedRule?.takeIf { ready }?.let { rule ->
        ProcessDraftState.encode(PlannedEditorDraft.fields(rule) + mapOf(
            "baseline" to committedRule?.let(PlannedEditorDraft::fingerprint),
            "creating" to (!editMode).toString(), "categoryVisible" to categoryModalVisible.toString(),
            "descriptionVisible" to descriptionModalVisible.toString(), "amountVisible" to amountModalVisible.toString(),
            "typeVisible" to transactionTypeModalVisible.toString(),
            "recurringId" to recurringRuleModalData?.id?.toString(),
            "recurringStart" to recurringRuleModalData?.initialStartDate?.toString(),
            "recurringCount" to recurringRuleModalData?.initialIntervalN?.toString(),
            "recurringType" to recurringRuleModalData?.initialIntervalType?.name,
            "recurringOneTime" to recurringRuleModalData?.initialOneTime?.toString(),
        ))
    }

    private suspend fun restoreDraft(screen: EditPlannedScreen, payload: List<String>) {
        val (draft, fields) = PlannedEditorDraft.decode(payload) ?: run {
            saveError = "The previous draft could not be reopened. Review this payment before saving."
            return
        }
        val current = committedRule
        val validIdentity = if (editMode) draft.id == screen.plannedPaymentRuleId else fields["creating"] == "true"
        if (!validIdentity || (current != null && fields["baseline"] != PlannedEditorDraft.fingerprint(current)) ||
            accounts.none { it.id == draft.accountId } || (draft.categoryId != null && categories.none { it.id.value == draft.categoryId })) {
            saveError = "This payment changed while Jvy was closed. Review the latest values before saving."
            return
        }
        if (!editMode && withContext(dispatchers.io) { plannedPaymentRuleDao.findById(draft.id) } != null) {
            // The creation was committed before process death. Don't reopen it as Add.
            closeScreen()
            return
        }
        loadedRule = (current ?: draft).copy(type = draft.type, accountId = draft.accountId, categoryId = draft.categoryId,
            amount = draft.amount, title = draft.title, description = draft.description, startDate = draft.startDate,
            oneTime = draft.oneTime, intervalN = draft.intervalN, intervalType = draft.intervalType)
        categoryModalVisible = fields["categoryVisible"] == "true"
        descriptionModalVisible = fields["descriptionVisible"] == "true"
        amountModalVisible = fields["amountVisible"] == "true"
        transactionTypeModalVisible = fields["typeVisible"] == "true"
        recurringRuleModalData = fields["recurringId"]?.let { id -> runCatching {
            RecurringRuleModalData(fields["recurringStart"]?.let(LocalDateTime::parse), fields["recurringCount"]?.toInt(),
                fields["recurringType"]?.let(IntervalType::valueOf), fields["recurringOneTime"] == "true", java.util.UUID.fromString(id))
        }.getOrNull() }
    }

    private suspend fun display(rule: PlannedPaymentRule) {
        this.title = rule.title

        transactionType = rule.type
        startDate = with(timeConverter) { rule.startDate?.toLocalDateTime() }
        intervalN = rule.intervalN
        oneTime = rule.oneTime
        intervalType = rule.intervalType
        initialTitle = rule.title
        description = rule.description
        val selectedAccount = withContext(dispatchers.io) { accountDao.findById(rule.accountId)!!.toLegacyDomain() }
        account = selectedAccount
        category = rule.categoryId?.let {
            withContext(dispatchers.io) { categoryRepository.findById(CategoryId(it)) }
        }
        amount = rule.amount

        updateCurrency(account = selectedAccount)
    }

    private suspend fun updateCurrency(account: Account) {
        currency = account.currency ?: baseCurrency()
    }

    private suspend fun baseCurrency(): String = withContext(dispatchers.io) { settingsDao.findFirst().currency }

    private fun updateRule(
        startDate: LocalDateTime,
        oneTime: Boolean,
        intervalN: Int?,
        intervalType: IntervalType?
    ) {
        loadedRule = loadedRule().copy(
            startDate = with(timeConverter) { startDate.toUTC() },
            intervalN = intervalN,
            intervalType = intervalType,
            oneTime = oneTime
        )
        this@EditPlannedViewModel.startDate = startDate
        this@EditPlannedViewModel.intervalN = intervalN
        this@EditPlannedViewModel.intervalType = intervalType
        this@EditPlannedViewModel.oneTime = oneTime

        saveIfEditMode()
    }

    private fun updateAmount(newAmount: Double) {
        loadedRule = loadedRule().copy(
            amount = newAmount
        )
        this@EditPlannedViewModel.amount = newAmount

        saveIfEditMode()
    }

    private fun updateTitle(newTitle: String?) {
        loadedRule = loadedRule().copy(
            title = newTitle
        )
        this.title = newTitle

        saveIfEditMode()
    }

    private fun updateDescription(newDescription: String?) {
        loadedRule = loadedRule().copy(
            description = newDescription
        )
        this@EditPlannedViewModel.description = newDescription

        saveIfEditMode()
    }

    private fun updateCategory(newCategory: Category?) {
        categorySelectionRevision++
        loadedRule = loadedRule().copy(
            categoryId = newCategory?.id?.value
        )
        this@EditPlannedViewModel.category = newCategory

        saveIfEditMode()
    }

    private fun updateAccount(newAccount: Account) {
        loadedRule = loadedRule().copy(
            accountId = newAccount.id
        )
        this@EditPlannedViewModel.account = newAccount

        viewModelScope.launch {
            updateCurrency(account = newAccount)
        }

        saveIfEditMode()
    }

    private fun updateTransactionType(newTransactionType: TransactionType) {
        loadedRule = loadedRule().copy(
            type = newTransactionType
        )
        this@EditPlannedViewModel.transactionType = newTransactionType

        saveIfEditMode()
    }

    private fun saveIfEditMode() {
        if (editMode) {
            save(false)
        }
    }

    private fun save(closeScreen: Boolean = true) {
        if (!ready || explicitSavePending || !validate()) {
            return
        }

        if (closeScreen) explicitSavePending = true
        saveError = null
        val ownerEntryId = startedEntryId
        val submitted = loadedRule().copy(type = transactionType, startDate = with(timeConverter) { startDate?.toUTC() },
            intervalN = intervalN, intervalType = intervalType, oneTime = oneTime, categoryId = category?.id?.value,
            accountId = account?.id ?: return, title = title?.trim(), description = description?.trim(), amount = amount, isSynced = false)
        val creating = !editMode
        val revision = ++writeRevision
        viewModelScope.launch {
            saveMutex.lock()
            try {
                if (startedEntryId != ownerEntryId) return@launch
                committedRule = plannedWriter.save(submitted, creating, committedRule)

                if (closeScreen && nav.currentEntryId == ownerEntryId && revision == writeRevision) {
                    closeScreen()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: PlannedPaymentConflict) {
                if (startedEntryId == ownerEntryId && revision == writeRevision)
                    saveError = "This payment or its account changed. Close and reopen it to review the latest values."
            } catch (_: Exception) {
                if (startedEntryId == ownerEntryId && revision == writeRevision)
                    saveError = "Could not save. Your changes are still here. Try again."
            } finally {
                saveMutex.unlock()
                if (closeScreen && startedEntryId == ownerEntryId) explicitSavePending = false
            }
        }
    }

    private fun validate(): Boolean {
        if (transactionType == TransactionType.TRANSFER) {
            return false
        }

        if (!amount.isFinite() || amount <= 0.0) {
            return false
        }

        return if (oneTime) validateOneTime() else validateRecurring()
    }

    override fun onCleared() {
        unregisterBackGuard?.invoke()
        super.onCleared()
    }

    private fun validateOneTime(): Boolean {
        return startDate != null
    }

    private fun validateRecurring(): Boolean {
        return startDate != null &&
                intervalN != null &&
                intervalN!! > 0 &&
                intervalType != null
    }

    private fun delete() {
        val rule = loadedRule ?: return
        if (!ready || explicitSavePending) return
        explicitSavePending = true
        val owner = startedEntryId
        viewModelScope.launch {
            saveMutex.lock()
            try {
                if (startedEntryId != owner) return@launch
                plannedWriter.delete(rule.id)
                if (nav.currentEntryId == owner) closeScreen()
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (_: Exception) {
                if (startedEntryId == owner) saveError = "Could not delete. Your payment is still here. Try again."
            } finally {
                saveMutex.unlock()
                if (startedEntryId == owner) { explicitSavePending = false; deleteTransactionModalVisible = false }
            }
        }
    }

    fun closeScreen() {
        if (!nav.back()) { nav.resetBackStack(); nav.navigateTo(com.ivy.navigation.PlannedPaymentsScreen) }
    }

    private fun createCategory(data: CreateCategoryData, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        val selectionRevision = categorySelectionRevision
        viewModelScope.launch {
            categoryCreator.createCategory(data, onResult) {
                val refreshed = categoryRepository.findAll().toImmutableList()
                if (nav.currentEntryId == entry) {
                    categories = refreshed
                    if (categorySelectionRevision == selectionRevision) updateCategory(it)
                }
            }
        }
    }

    private fun editCategory(updatedCategory: Category, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            categoryCreator.editCategory(updatedCategory, onResult) {
                val refreshed = categoryRepository.findAll().toImmutableList()
                if (nav.currentEntryId == entry) categories = refreshed
            }
        }
    }

    private fun createAccount(data: CreateAccountData, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            accountCreator.createAccount(data, onResult) {
                val refreshed = accountsAct(Unit)
                if (nav.currentEntryId == entry) accounts = refreshed
            }
        }
    }

    private fun reset() {
        loadedRule = null

        initialTitle = null
        description = null
        category = null
    }

    private fun loadedRule() = loadedRule ?: error("Loaded transaction is null")
}
