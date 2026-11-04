package com.ivy.transaction

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.Toaster
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.refreshWidget
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.TransactionId
import com.ivy.data.model.primitive.AssociationId
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.TagRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TagMapper
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.domain.features.Features
import com.ivy.domain.usecase.category.CategoryRuleRepository
import com.ivy.domain.usecase.category.FutureCategoryEntry
import com.ivy.legacy.data.EditTransactionDisplayLoan
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.temp.toDomain
import com.ivy.legacy.domain.deprecated.logic.AccountCreator
import com.ivy.legacy.utils.computationThread
import com.ivy.legacy.utils.ioThread
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.legacy.utils.uiThread
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.R
import com.ivy.ui.time.impl.DateTimePicker
import com.ivy.wallet.domain.action.account.AccountByIdAct
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.transaction.TrnByIdAct
import com.ivy.wallet.domain.data.CustomExchangeRateState
import com.ivy.wallet.domain.deprecated.logic.CategoryCreator
import com.ivy.wallet.domain.deprecated.logic.PlannedPaymentsLogic
import com.ivy.wallet.domain.deprecated.logic.SmartTitleSuggestionsLogic
import com.ivy.wallet.domain.deprecated.logic.currency.ExchangeRatesLogic
import com.ivy.wallet.domain.deprecated.logic.loantrasactions.LoanTransactionsLogic
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@Suppress("LargeClass")
@Stable
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val toaster: Toaster,
    private val loanDao: LoanDao,
    private val settingsDao: SettingsDao,
    private val nav: Navigation,
    private val sharedPrefs: SharedPrefs,
    private val exchangeRatesLogic: ExchangeRatesLogic,
    private val categoryCreator: CategoryCreator,
    private val accountCreator: AccountCreator,
    private val plannedPaymentsLogic: PlannedPaymentsLogic,
    private val smartTitleSuggestionsLogic: SmartTitleSuggestionsLogic,
    private val loanTransactionsLogic: LoanTransactionsLogic,
    private val accountsAct: AccountsAct,
    private val categoryRepository: CategoryRepository,
    private val trnByIdAct: TrnByIdAct,
    private val accountByIdAct: AccountByIdAct,
    private val transactionRepo: TransactionRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
    private val transactionMapper: TransactionMapper,
    private val tagRepository: TagRepository,
    private val tagMapper: TagMapper,
    private val tagCreator: com.ivy.wallet.domain.deprecated.logic.TagCreator,
    private val atomicWrites: com.ivy.data.repository.AtomicWriteRepository,
    private val features: Features,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
    private val dateTimePicker: DateTimePicker,
) : ComposeViewModel<EditTransactionViewState, EditTransactionViewEvent>() {

    val editorModals = com.ivy.legacy.ui.EditorModalHost()

    private var transactionType by mutableStateOf(TransactionType.EXPENSE)
    private var initialTitle by mutableStateOf<String?>(null)
    private var titleSuggestions by mutableStateOf(persistentSetOf<String>())
    private var currency by mutableStateOf("")
    private var description by mutableStateOf<String?>(null)
    private var dateTime by mutableStateOf<Instant?>(null)
    private var dueDate by mutableStateOf<Instant?>(null)
    private var accounts by mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private var categories by mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private var tags by mutableStateOf<ImmutableList<Tag>>(persistentListOf())
    private var transactionAssociatedTags by mutableStateOf<ImmutableList<TagId>>(persistentListOf())
    private var account by mutableStateOf<Account?>(null)
    private var toAccount by mutableStateOf<Account?>(null)
    private var category by mutableStateOf<Category?>(null)
    private var categorySelectionRevision = 0L
    private var suggestedCategory by mutableStateOf<Category?>(null)
    private var showRememberCategoryRule by mutableStateOf(false)
    private var rememberCategoryRule by mutableStateOf(false)
    private var amount by mutableDoubleStateOf(0.0)
    private var hasChanges by mutableStateOf(false)
    private var displayLoanHelper by mutableStateOf(EditTransactionDisplayLoan())

    private var paidHistory: Instant? = null

    // This is used to when the transaction is associated with a loan/loan record,
    // used to indicate the background updating of loan/loanRecord data
    private var backgroundProcessingStarted by mutableStateOf(false)

    private var customExchangeRateState by mutableStateOf(CustomExchangeRateState())

    private var loadedTransaction: Transaction? = null
    private var committedTransaction: Transaction? = null
    var ready by mutableStateOf(false)
        private set
    var loadError by mutableStateOf(false)
        private set
    private var editMode = false

    // Used for optimising in updating all loan/loanRecords
    private var accountsChanged = false

    private var title: String? = null
    private lateinit var baseUserCurrency: String
    private var tagSearchJob: Job? = null
    private var startedScreen: EditTransactionScreen? = null
    private var startedEntryId: UUID? = null
    private var unregisterBackGuard: (() -> Unit)? = null
    private var startupJob: Job? = null
    private val saveMutex = kotlinx.coroutines.sync.Mutex()
    private var writeRevision = 0L
    private var explicitSavePending by mutableStateOf(false)
    private var saveError by mutableStateOf<String?>(null)
    private val tagSearchDebounceTimeInMills: Long = 500

    fun start(screen: EditTransactionScreen, restored: List<String>? = null) {
        unregisterBackGuard?.invoke()
        unregisterBackGuard = nav.registerScreenBackHandler(screen) { explicitSavePending }
        val entryId = nav.currentEntryId
        if (startedScreen == screen && startedEntryId == entryId &&
            (ready || startupJob?.isActive == true)) return
        startupJob?.cancel()
        if (startedEntryId != entryId) { explicitSavePending = false; saveError = null }
        startedScreen = screen
        startedEntryId = entryId
        ready = false
        loadError = false
        startupJob = viewModelScope.launch {
            try {
            editMode = screen.initialTransactionId != null

            baseUserCurrency = baseCurrency()

            val tagList = async { getAllTags() }

            val getAccounts = accountsAct(Unit)
            if (getAccounts.isEmpty()) {
                closeScreen()
                return@launch
            }
            accounts = getAccounts

            categories = sortCategories()

            reset()

            val existing = screen.initialTransactionId?.let { trnByIdAct(it) }?.takeUnless { it.isDeleted }
            if (screen.initialTransactionId != null && existing == null) {
                loadError = true
                return@launch
            }
            committedTransaction = existing
            loadedTransaction = existing ?: Transaction(
                accountId = defaultAccountId(
                    screen = screen,
                    accounts = getAccounts
                ),
                categoryId = screen.categoryId,
                type = screen.type,
                amount = BigDecimal.ZERO,
                toAmount = BigDecimal.ZERO
            )

            if (restored != null && !restoreDraft(screen, restored)) return@launch

            tags = tagList.await()
            transactionAssociatedTags =
                tagRepository.findByAssociatedId(AssociationId(loadedTransaction().id)).map(Tag::id)
                    .toImmutableList()
            display(loadedTransaction!!)
            ready = true
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (_: Exception) { loadedTransaction = null; loadError = true }
        }
    }

    fun saveDraft(): List<String>? = runCatching { loadedTransaction?.takeIf { ready }?.let { value ->
        val draft = value.copy(type = transactionType, accountId = account?.id ?: value.accountId, toAccountId = toAccount?.id,
            categoryId = category?.id?.value, amount = amount.toBigDecimal(),
            toAmount = customExchangeRateState.convertedAmount?.toBigDecimal() ?: amount.toBigDecimal(), title = title,
            description = description, dateTime = dateTime, dueDate = dueDate, paidFor = paidHistory)
        com.ivy.design.system.ProcessDraftState.encode(TransactionEditorDraft.fields(draft) + mapOf(
            "baseline" to committedTransaction?.let(TransactionEditorDraft::fingerprint), "creating" to (!editMode).toString(),
            "rememberCategory" to rememberCategoryRule.toString(), "changedAccounts" to accountsChanged.toString(),
            "hasChanges" to hasChanges.toString()))
    } }.getOrNull()

    private suspend fun restoreDraft(screen: EditTransactionScreen, payload: List<String>): Boolean {
        val restored = TransactionEditorDraft.decode(payload)
        if (restored == null) {
            saveError = "The previous draft could not be reopened. Review this entry before saving."
            return true
        }
        val (draft, fields) = restored
        val current = committedTransaction
        val validIdentity = if (editMode) draft.id == screen.initialTransactionId else fields["creating"] == "true"
        if (!validIdentity || (current != null && fields["baseline"] != TransactionEditorDraft.fingerprint(current)) ||
            accounts.none { it.id == draft.accountId } || (draft.toAccountId != null && accounts.none { it.id == draft.toAccountId }) ||
            (draft.categoryId != null && categories.none { it.id.value == draft.categoryId })) {
            saveError = "This entry changed while Jvy was closed. Review the latest values before saving."
            return true
        }
        if (!editMode && trnByIdAct(draft.id) != null) {
            closeScreen() // Committed before acknowledgement; never reopen it as another Add.
            return false
        }
        loadedTransaction = (current ?: draft).copy(id = draft.id, type = draft.type, accountId = draft.accountId,
            toAccountId = draft.toAccountId, categoryId = draft.categoryId, amount = draft.amount, toAmount = draft.toAmount,
            title = draft.title, description = draft.description, dateTime = draft.dateTime, dueDate = draft.dueDate, paidFor = draft.paidFor)
        rememberCategoryRule = fields["rememberCategory"] == "true"
        accountsChanged = fields["changedAccounts"] == "true"
        hasChanges = fields["hasChanges"] == "true"
        return true
    }

    @Composable
    override fun uiState(): EditTransactionViewState {
        return EditTransactionViewState(
            transactionType = getTransactionType(),
            initialTitle = getInitialTitle(),
            titleSuggestions = getTitleSuggestions(),
            currency = getCurrency(),
            description = getDescription(),
            dateTime = getDateTime(),
            dueDate = getDueDate(),
            accounts = getAccounts(),
            categories = getCategories(),
            account = getAccount(),
            toAccount = getToAccount(),
            category = getCategory(),
            suggestedCategory = getSuggestedCategory(),
            amount = getAmount(),
            showRememberCategoryRule = getShowRememberCategoryRule(),
            rememberCategoryRule = getRememberCategoryRule(),
            hasChanges = getHasChanges(),
            displayLoanHelper = getDisplayLoanHelper(),
            backgroundProcessingStarted = getBackgroundProcessingStarted(),
            saving = explicitSavePending,
            saveError = saveError,
            customExchangeRateState = getCustomExchangeRateState(),
            tags = getTags(),
            transactionAssociatedTags = getTransactionAssociatedTags()
        )
    }

    @Composable
    private fun getTransactionType(): TransactionType {
        return transactionType
    }

    @Composable
    private fun getInitialTitle(): String? {
        return initialTitle
    }

    @Composable
    private fun getTitleSuggestions(): ImmutableSet<String> {
        return if (features.showTitleSuggestions.asEnabledState()) {
            titleSuggestions
        } else {
            persistentSetOf()
        }
    }

    @Composable
    private fun getCurrency(): String {
        return currency
    }

    @Composable
    private fun getDescription(): String? {
        return description
    }

    @Composable
    private fun getDateTime(): Instant? {
        return dateTime
    }

    @Composable
    private fun getDueDate(): Instant? {
        return dueDate
    }

    @Composable
    private fun getAccounts(): ImmutableList<Account> {
        return accounts
    }

    @Composable
    private fun getCategories(): ImmutableList<Category> {
        return categories
    }

    @Composable
    private fun getAccount(): Account? {
        return account
    }

    @Composable
    private fun getToAccount(): Account? {
        return toAccount
    }

    @Composable
    private fun getCategory(): Category? {
        return category
    }

    @Composable
    private fun getSuggestedCategory(): Category? {
        return suggestedCategory
    }

    @Composable
    private fun getAmount(): Double {
        return amount
    }

    @Composable
    private fun getShowRememberCategoryRule(): Boolean {
        return showRememberCategoryRule
    }

    @Composable
    private fun getRememberCategoryRule(): Boolean {
        return rememberCategoryRule
    }

    @Composable
    private fun getHasChanges(): Boolean {
        return hasChanges
    }

    @Composable
    private fun getDisplayLoanHelper(): EditTransactionDisplayLoan {
        return displayLoanHelper
    }

    @Composable
    private fun getBackgroundProcessingStarted(): Boolean {
        return backgroundProcessingStarted
    }

    @Composable
    private fun getCustomExchangeRateState(): CustomExchangeRateState {
        return customExchangeRateState
    }

    @Composable
    private fun getTags(): ImmutableList<Tag> {
        return tags
    }

    @Composable
    private fun getTransactionAssociatedTags(): ImmutableList<TagId> {
        return transactionAssociatedTags
    }

    @Suppress("CyclomaticComplexMethod")
    override fun onEvent(event: EditTransactionViewEvent) {
        if (explicitSavePending) return // Late picker/tag callbacks cannot replace a submitted draft.
        when (event) {
            is EditTransactionViewEvent.CreateAccount -> createAccount(event.data, event.onResult)
            is EditTransactionViewEvent.CreateCategory -> createCategory(event.data, event.onResult)
            EditTransactionViewEvent.Delete -> delete()
            EditTransactionViewEvent.Duplicate -> duplicate()
            is EditTransactionViewEvent.EditCategory -> editCategory(event.updatedCategory, event.onResult)
            is EditTransactionViewEvent.OnAccountChanged -> onAccountChanged(event.newAccount)
            is EditTransactionViewEvent.OnAmountChanged -> onAmountChanged(event.newAmount)
            is EditTransactionViewEvent.OnCategoryChanged -> onCategoryChanged(event.newCategory)
            is EditTransactionViewEvent.SetRememberCategoryRule ->
                updateRememberCategoryRule(event.enabled)
            is EditTransactionViewEvent.OnDescriptionChanged ->
                onDescriptionChanged(event.newDescription)

            is EditTransactionViewEvent.OnDueDateChanged -> onDueDateChanged(event.newDueDate)
            EditTransactionViewEvent.OnPayPlannedPayment -> onPayPlannedPayment()
            is EditTransactionViewEvent.OnChangeDate -> handleChangeDate()
            is EditTransactionViewEvent.OnChangeTime -> handleChangeTime()
            is EditTransactionViewEvent.OnSetTransactionType ->
                onSetTransactionType(event.newTransactionType)

            is EditTransactionViewEvent.OnTitleChanged -> onTitleChanged(event.newTitle)
            is EditTransactionViewEvent.OnToAccountChanged -> onToAccountChanged(event.newAccount)
            is EditTransactionViewEvent.Save -> save(event.closeScreen)
            is EditTransactionViewEvent.SetHasChanges -> setHasChanges(event.hasChangesValue)
            is EditTransactionViewEvent.UpdateExchangeRate -> updateExchangeRate(event.exRate)
            is EditTransactionViewEvent.TagEvent -> handleTagEvent(event)
        }
    }

    private fun handleTagEvent(event: EditTransactionViewEvent.TagEvent) {
        when (event) {
            is EditTransactionViewEvent.TagEvent.SaveTag -> onTagSaved(event.id, event.name, event.onResult)
            is EditTransactionViewEvent.TagEvent.OnTagSelect -> selectTag(event.selectedTag, true, event.onResult)
            is EditTransactionViewEvent.TagEvent.OnTagDeSelect -> selectTag(event.selectedTag, false, event.onResult)
            is EditTransactionViewEvent.TagEvent.OnTagSearch -> searchTag(event.query)
            is EditTransactionViewEvent.TagEvent.OnTagDelete -> deleteTag(event.selectedTag, event.onResult)
            is EditTransactionViewEvent.TagEvent.OnTagEdit -> updateTagInformation(event.oldTag, event.newTag, event.onResult)
        }
    }

    private suspend fun defaultAccountId(
        screen: EditTransactionScreen,
        accounts: List<Account>,
    ): UUID {
        if (screen.accountId != null) {
            return screen.accountId!!
        }

        val lastSelectedId = sharedPrefs.getString(
            SharedPrefs.LAST_SELECTED_ACCOUNT_ID,
            null
        )?.let { UUID.fromString(it) }
        if (lastSelectedId != null && ioThread {
                accounts.find {
                    it.id == lastSelectedId
                }
            } != null
        ) {
            // use last selected account
            return lastSelectedId
        }

        return accounts.first().id
    }

    private suspend fun display(transaction: Transaction) {
        this.title = transaction.title

        transactionType = transaction.type
        initialTitle = transaction.title
        dateTime = transaction.dateTime
        description = transaction.description
        dueDate = transaction.dueDate
        paidHistory = transaction.paidFor
        val selectedAccount = accountByIdAct(transaction.accountId)!!
        account = selectedAccount
        toAccount = transaction.toAccountId?.let {
            accountByIdAct(it)
        }
        category = transaction.categoryId?.let {
            categoryRepository.findById(CategoryId(it))
        }
        amount = transaction.amount.toDouble()

        updateCurrency(account = selectedAccount)

        customExchangeRateState = if (transaction.toAccountId == null) {
            CustomExchangeRateState()
        } else {
            val exchangeRate = if (transaction.amount.signum() == 0) BigDecimal.ONE else transaction.toAmount.divide(transaction.amount, java.math.MathContext.DECIMAL64)
            val toAccountCurrency =
                accounts.find { acc -> acc.id == transaction.toAccountId }?.currency
            CustomExchangeRateState(
                showCard = toAccountCurrency != account?.currency,
                exchangeRate = exchangeRate.toDouble(),
                convertedAmount = transaction.toAmount.toDouble(),
                toCurrencyCode = toAccountCurrency,
                fromCurrencyCode = currency
            )
        }

        displayLoanHelper = getDisplayLoanHelper(trans = transaction)
        refreshCategorySuggestion()
    }

    private suspend fun getDisplayLoanHelper(trans: Transaction): EditTransactionDisplayLoan {
        if (trans.loanId == null) {
            return EditTransactionDisplayLoan()
        }

        val loan =
            ioThread { loanDao.findById(trans.loanId!!) } ?: return EditTransactionDisplayLoan()
        val isLoanRecord = trans.loanRecordId != null

        val loanWarningDescription = if (isLoanRecord) {
            context.getString(
                R.string.note_transaction_associated_with_loan_record_of_loan,
                loan.name
            )
        } else {
            context.getString(
                R.string.note_you_are_trying_to_change_the_account_associated_with_the_loan,
                loan.name
            )
        }

        val loanCaption =
            if (isLoanRecord) {
                context.getString(
                    R.string.this_transaction_is_associated_with_loan_record,
                    loan.name
                )
            } else {
                context.getString(
                    R.string.this_transaction_is_associated_with_loan,
                    loan.name
                )
            }

        return EditTransactionDisplayLoan(
            isLoan = true,
            isLoanRecord = isLoanRecord,
            loanCaption = loanCaption,
            loanWarningDescription = loanWarningDescription
        )
    }

    private fun onAmountChanged(newAmount: Double) {
        viewModelScope.launch {
            loadedTransaction = loadedTransaction().copy(
                amount = newAmount.toBigDecimal()
            )
            amount = newAmount
            updateCustomExchangeRateState(amt = newAmount)

            saveIfEditMode()
        }
    }

    private fun onTitleChanged(newTitle: String?) {
        loadedTransaction = loadedTransaction().copy(
            title = newTitle
        )
        this.title = newTitle
        updateRememberCategoryRuleVisibility()

        saveIfEditMode()

        updateTitleSuggestions(newTitle)
    }

    private fun onDescriptionChanged(newDescription: String?) {
        loadedTransaction = loadedTransaction().copy(
            description = newDescription
        )
        description = newDescription
        updateRememberCategoryRuleVisibility()

        saveIfEditMode()
    }

    private fun onAccountChanged(newAccount: Account) {
        viewModelScope.launch {
            loadedTransaction = loadedTransaction().copy(
                accountId = newAccount.id
            )
            account = newAccount

            updateCustomExchangeRateState(fromAccount = newAccount)

            viewModelScope.launch {
                updateCurrency(account = newAccount)
            }

            accountsChanged = true

            // update last selected account
            sharedPrefs.putString(SharedPrefs.LAST_SELECTED_ACCOUNT_ID, newAccount.id.toString())

            saveIfEditMode()

            updateTitleSuggestions()
        }
    }

    private suspend fun updateCurrency(account: Account) {
        currency = account.currency ?: baseCurrency()
    }

    private fun onToAccountChanged(newAccount: Account) {
        viewModelScope.launch {
            loadedTransaction = loadedTransaction().copy(
                toAccountId = newAccount.id
            )
            toAccount = newAccount
            updateCustomExchangeRateState(toAccountValue = newAccount)

            saveIfEditMode()
        }
    }

    private fun onDueDateChanged(newDueDate: LocalDateTime?) {
        val newDueDateUtc = with(timeConverter) { newDueDate?.toUTC() }
        loadedTransaction = loadedTransaction().copy(
            dueDate = newDueDateUtc
        )
        dueDate = newDueDateUtc

        saveIfEditMode()
    }

    private fun handleChangeDate() {
        dateTimePicker.pickDate(
            initialDate = loadedTransaction?.dateTime,
        ) { localDate ->
            val localTime = loadedTransaction().dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: timeProvider.localTimeNow()
            loadedTransaction = loadedTransaction().copy(
                date = localDate,
            )
            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleChangeTime() {
        dateTimePicker.pickTime(
            initialTime = loadedTransaction?.dateTime?.let {
                with(timeConverter) {
                    it.toLocalDateTime()
                }
            }?.toLocalTime()
        ) { localTime ->
            val localDate = loadedTransaction().dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: timeProvider.localDateNow()
            loadedTransaction = loadedTransaction().copy(
                time = localTime,
            )
            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun updateDateTime(newDateTime: LocalDateTime) {
        val newDateTimeUtc = with(timeConverter) { newDateTime.toUTC() }
        loadedTransaction = loadedTransaction().copy(
            dateTime = newDateTimeUtc,
        )
        dateTime = newDateTimeUtc

        saveIfEditMode()
    }

    private fun onSetTransactionType(newTransactionType: TransactionType) {
        loadedTransaction = loadedTransaction().copy(
            type = newTransactionType
        )
        transactionType = newTransactionType
        saveIfEditMode()
    }

    private fun onPayPlannedPayment() {
        viewModelScope.launch {
            plannedPaymentsLogic.payOrGetLegacy(
                transaction = loadedTransaction(),
                syncTransaction = false
            ) { paidTransaction ->
                loadedTransaction = paidTransaction
                paidHistory = paidTransaction.paidFor
                dueDate = paidTransaction.dueDate
                dateTime = paidTransaction.dateTime

                saveIfEditMode(
                    closeScreen = true
                )
            }
        }
    }

    private fun delete() {
        viewModelScope.launch {
            ioThread {
                loadedTransaction?.let {
                    transactionRepo.deleteById(TransactionId(it.id))
                }
                closeScreen()
            }
        }
    }

    private fun duplicate() {
        viewModelScope.launch {
            ioThread {
                val id = UUID.randomUUID()
                 loadedTransaction()
                    .copy(
                        id = id,
                        dateTime = timeProvider.utcNow(),
                    )
                    .toDomain(transactionMapper)
                    ?.let {
                        transactionRepo.save(it)
                    }

                tagRepository.findByIds(transactionAssociatedTags).forEach {
                    associateTagToTransaction(it, id)
                }

                refreshWidget(WalletBalanceWidgetReceiver::class.java)
            }

            closeScreen()
        }
    }

    private fun createCategory(data: CreateCategoryData, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        val selectionRevision = categorySelectionRevision
        viewModelScope.launch {
            categoryCreator.createCategory(data, onResult) {
                val refreshed = sortCategories()
                if (nav.currentEntryId == entry) {
                    categories = refreshed
                    if (categorySelectionRevision == selectionRevision) onCategoryChanged(it)
                }
            }
        }
    }

    private fun onCategoryChanged(newCategory: Category?) {
        categorySelectionRevision++
        loadedTransaction = loadedTransaction().copy(
            categoryId = newCategory?.id?.value
        )
        category = newCategory
        suggestedCategory = null
        rememberCategoryRule = false
        showRememberCategoryRule = newCategory != null && hasRememberableText()

        saveIfEditMode()

        updateTitleSuggestions()
    }

    private fun updateRememberCategoryRule(enabled: Boolean) {
        rememberCategoryRule = enabled && showRememberCategoryRule
    }

    private fun refreshCategorySuggestion() {
        if (editMode || category != null) {
            suggestedCategory = null
            return
        }

        val transaction = loadedTransaction ?: return
        viewModelScope.launch {
            val today = LocalDate.now()
            val match = categoryRuleRepository.matchSuggestion(
                entry = FutureCategoryEntry(
                    id = transaction.id.toString(),
                    date = today,
                    title = transaction.title,
                    description = transaction.description,
                ),
                effectiveFrom = today,
            )
            val matchedCategory = match?.categoryId?.let { categoryId ->
                runCatching {
                    categoryRepository.findById(CategoryId(UUID.fromString(categoryId)))
                }.getOrNull()
            }
            if (category == null) {
                suggestedCategory = matchedCategory
            }
        }
    }

    private fun updateRememberCategoryRuleVisibility() {
        showRememberCategoryRule = category != null && hasRememberableText()
        if (!showRememberCategoryRule) {
            rememberCategoryRule = false
        }
        refreshCategorySuggestion()
    }

    private fun hasRememberableText(): Boolean =
        !title.isNullOrBlank() || !description.isNullOrBlank()

    private fun updateTitleSuggestions(title: String? = loadedTransaction().title) {
        viewModelScope.launch {
            titleSuggestions = ioThread {
                smartTitleSuggestionsLogic.suggest(
                    title = title,
                    categoryId = category?.id?.value,
                    accountId = account?.id
                )
            }.toPersistentSet()
        }
    }

    private fun editCategory(updatedCategory: Category, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            categoryCreator.editCategory(updatedCategory, onResult) {
                val refreshed = sortCategories()
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

    private fun save(closeScreen: Boolean = true, acknowledgeChanges: Boolean = true) {
        if (!ready || explicitSavePending || !validTransaction()) {
            return
        }

        val draft = try {
            transactionWriteSnapshot(loadedTransaction(), account?.id, toAccount?.id, category?.id?.value,
                transactionType, amount, customExchangeRateState.convertedAmount, title, description,
                dueDate, paidHistory, timeProvider.utcNow())
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (_: Exception) { saveError = "Could not save. Your changes are still here. Try again."; return }
        val saveRule = rememberCategoryRule && draft.categoryId != null
        val changedAccounts = accountsChanged
        loadedTransaction = draft
        val revision = ++writeRevision
        if (acknowledgeChanges) explicitSavePending = true
        saveError = null
        val ownerEntryId = startedEntryId
        viewModelScope.launch {
            try {
                saveMutex.lock()
                try {
                    if (startedEntryId == ownerEntryId) saveInternal(draft, saveRule, changedAccounts,
                        closeScreen, acknowledgeChanges, ownerEntryId, revision)
                } finally { saveMutex.unlock() }
            } finally { if (acknowledgeChanges && startedEntryId == ownerEntryId) explicitSavePending = false }
        }
    }

    private suspend fun saveInternal(draft: Transaction, saveRule: Boolean, changedAccounts: Boolean,
        closeScreen: Boolean, acknowledgeChanges: Boolean, ownerEntryId: UUID?, revision: Long) {
        try {
            atomicWrites.run {
                if (draft.loanId != null) {
                    loanTransactionsLogic.updateAssociatedLoanData(
                        draft,
                        onBackgroundProcessingStart = {
                            if (startedEntryId == ownerEntryId) backgroundProcessingStarted = true
                        },
                        onBackgroundProcessingEnd = {
                            if (startedEntryId == ownerEntryId) backgroundProcessingStarted = false
                        },
                        accountsChanged = changedAccounts
                    )

                }

                transactionRepo.save(requireNotNull(draft.toDomain(transactionMapper)))

                if (saveRule) {
                    categoryRuleRepository.saveFromTransaction(
                        title = draft.title,
                        description = draft.description,
                        categoryId = requireNotNull(draft.categoryId).toString(),
                    )
                }

            }

            if (startedEntryId == ownerEntryId) committedTransaction = draft

            // These are acknowledgements/side effects of the committed write, not staged state.
            if (isCurrentTransactionWrite(ownerEntryId, revision, startedEntryId, writeRevision)) {
                accountsChanged = false
                rememberCategoryRule = false
                saveError = null
            }
            try { refreshWidget(WalletBalanceWidgetReceiver::class.java) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { /* A widget refresh cannot undo a committed ledger write. */ }

            if (acknowledgeChanges && isCurrentTransactionWrite(ownerEntryId, revision, startedEntryId, writeRevision)) hasChanges = false
            if (closeScreen && isCurrentTransactionWrite(ownerEntryId, revision, nav.currentEntryId, writeRevision)) {
                closeScreen()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            if (isCurrentTransactionWrite(ownerEntryId, revision, startedEntryId, writeRevision))
                saveError = "Could not save. Your changes are still here. Try again."
        } finally {
            if (startedEntryId == ownerEntryId) backgroundProcessingStarted = false
        }
    }

    @JvmName("setHasChangesMethod")
    private fun setHasChanges(hasChangesValue: Boolean) {
        hasChanges = hasChangesValue
    }

    private suspend fun transferToAmount(
        amount: Double
    ): Double? {
        if (transactionType != TransactionType.TRANSFER) return null
        val toCurrency = toAccount?.currency ?: baseCurrency()
        val fromCurrency = account?.currency ?: baseCurrency()

        return exchangeRatesLogic.convertAmount(
            baseCurrency = baseCurrency(),
            amount = amount,
            fromCurrency = fromCurrency,
            toCurrency = toCurrency
        )
    }

    private suspend fun baseCurrency(): String = ioThread { settingsDao.findFirst().currency }

    fun closeScreen() {
        if (nav.backStackEmpty()) {
            nav.resetBackStack()
            nav.navigateTo(MainScreen)
        } else {
            nav.back()
        }
    }

    override fun onCleared() {
        unregisterBackGuard?.invoke()
        super.onCleared()
    }

    // Comment for push
    @Suppress("ReturnCount")
    private fun validTransaction(): Boolean {
        if (hasChosenSameSourceAndDestinationAccountToTransfer()) {
            viewModelScope.launch {
                toaster.show(R.string.msg_source_account_destination_account_same_for_transfer)
            }
            return false
        }
        if (hasNotChosenAccountToTransfer()) {
            viewModelScope.launch {
                toaster.show(R.string.msg_select_account_to_transfer)
            }
            return false
        }

        if (amount == 0.0) {
            return false
        }

        return true
    }

    private fun hasNotChosenAccountToTransfer(): Boolean {
        return transactionType == TransactionType.TRANSFER && toAccount == null
    }

    private fun hasChosenSameSourceAndDestinationAccountToTransfer(): Boolean {
        return transactionType == TransactionType.TRANSFER && toAccount == account
    }

    private fun reset() {
        loadedTransaction = null

        initialTitle = null
        title = null
        description = null
        dueDate = null
        category = null
        suggestedCategory = null
        showRememberCategoryRule = false
        rememberCategoryRule = false
        hasChanges = false
    }

    private fun loadedTransaction() = loadedTransaction ?: error("Loaded transaction is null")

    private fun updateExchangeRate(exRate: Double?) {
        viewModelScope.launch {
            updateCustomExchangeRateState(exchangeRate = exRate, resetRate = exRate == null)
        }
    }

    private suspend fun updateCustomExchangeRateState(
        toAccountValue: Account? = null,
        fromAccount: Account? = null,
        amt: Double? = null,
        exchangeRate: Double? = null,
        resetRate: Boolean = false
    ) {
        computationThread {
            val toAcc = toAccountValue ?: toAccount
            val fromAcc = fromAccount ?: account

            val toAccCurrencyCode = toAcc?.currency ?: baseUserCurrency
            val fromAccCurrencyCode = fromAcc?.currency ?: baseUserCurrency

            if (toAcc == null || fromAcc == null || (toAccCurrencyCode == fromAccCurrencyCode)) {
                customExchangeRateState = CustomExchangeRateState()
                return@computationThread
            }

            val exRate = exchangeRate
                ?: if (isCustomExchangeRateCurrencyCodeMatchingWithSourceAndDestinationAccountCurrencyCode(
                        toAccCurrencyCode = toAccCurrencyCode,
                        fromAccCurrencyCode = fromAccCurrencyCode
                    ) && !resetRate
                ) {
                    customExchangeRateState.exchangeRate
                } else {
                    exchangeRatesLogic.convertAmount(
                        baseCurrency = baseUserCurrency,
                        amount = 1.0,
                        fromCurrency = fromAccCurrencyCode,
                        toCurrency = toAccCurrencyCode
                    )
                }

            val amount = amt ?: amount

            val customTransferExchangeRateState = CustomExchangeRateState(
                showCard = true,
                toCurrencyCode = toAccCurrencyCode,
                fromCurrencyCode = fromAccCurrencyCode,
                exchangeRate = exRate,
                convertedAmount = exRate * amount
            )

            customExchangeRateState = customTransferExchangeRateState
            uiThread {
                saveIfEditMode()
            }
        }
    }

    private fun isCustomExchangeRateCurrencyCodeMatchingWithSourceAndDestinationAccountCurrencyCode(
        toAccCurrencyCode: String,
        fromAccCurrencyCode: String
    ): Boolean {
        return customExchangeRateState.showCard &&
                toAccCurrencyCode == customExchangeRateState.toCurrencyCode &&
                fromAccCurrencyCode == customExchangeRateState.fromCurrencyCode
    }

    private fun saveIfEditMode(closeScreen: Boolean = false) {
        if (editMode) {
            hasChanges = true

            save(closeScreen, acknowledgeChanges = false)
        }
    }

    private suspend fun getAllTags(): ImmutableList<Tag> =
        tagRepository.findAll().toImmutableList()

    private fun onTagSaved(id: TagId, name: String, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            tagCreator.create(id, name, onResult) {
                val refreshed = getAllTags()
                if (nav.currentEntryId == entry) tags = refreshed
            }
        }
    }

    private fun selectTag(selectedTag: Tag, selected: Boolean, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        val id = loadedTransaction().id
        viewModelScope.launch {
            tagCreator.select(selectedTag, id, selected, onResult) {
                val refreshed = tagRepository.findByAssociatedId(AssociationId(id)).map(Tag::id).toImmutableList()
                if (nav.currentEntryId == entry) transactionAssociatedTags = refreshed
            }
        }
    }

    private fun associateTagToTransaction(selectedTag: Tag, id: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            val associatedId = AssociationId(id)
            tagRepository.associateTagToEntity(associatedId, selectedTag.id)
            transactionAssociatedTags =
                tagRepository.findByAssociatedId(associatedId).map(Tag::id).toImmutableList()
        }
    }

    private fun searchTag(query: String) {
        val entry = nav.currentEntryId
        tagSearchJob?.cancel()
        tagSearchJob = viewModelScope.launch {
            delay(tagSearchDebounceTimeInMills) // Debounce effect
            val refreshed = ioThread {
                if (query.isBlank()) tagRepository.findAll().toImmutableList()
                else tagRepository.findByText(query.toLowerCaseLocal()).toImmutableList()
            }
            if (nav.currentEntryId == entry) tags = refreshed
        }
    }

    private fun deleteTag(selectedTag: Tag, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            tagCreator.delete(selectedTag, onResult) {
                val refreshed = getAllTags()
                if (nav.currentEntryId == entry) {
                    tags = refreshed
                    transactionAssociatedTags = transactionAssociatedTags.filterNot { it == selectedTag.id }.toImmutableList()
                }
            }
        }
    }

    private fun updateTagInformation(oldTag: Tag, newTag: Tag, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            tagCreator.edit(oldTag, newTag, onResult) {
                val refreshed = getAllTags()
                if (nav.currentEntryId == entry) tags = refreshed
            }
        }
    }

    private suspend fun sortCategories(): ImmutableList<Category> {
        val categories = categoryRepository.findAll()
        return if (shouldSortCategoriesAscending()) {
            categories.sortedBy { it.name.value }.toImmutableList()
        } else {
            categories.toImmutableList()
        }
    }

    private suspend fun shouldSortCategoriesAscending(): Boolean {
        return features.sortCategoriesAscending.isEnabled(context)
    }
}
