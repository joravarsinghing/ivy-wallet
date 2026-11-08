package com.ivy.onboarding.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Theme
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.write.WriteSettingsDao
import com.ivy.ui.ComposeViewModel
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.legacy.LogoutLogic
import com.ivy.data.model.Category
import com.ivy.data.repository.CategoryRepository
import com.ivy.legacy.data.model.AccountBalance
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Settings
import com.ivy.legacy.domain.deprecated.logic.AccountCreator
import com.ivy.legacy.utils.OpResult
import com.ivy.legacy.utils.ioThread
import com.ivy.navigation.Navigation
import com.ivy.navigation.OnboardingScreen
import com.ivy.onboarding.OnboardingDetailState
import com.ivy.onboarding.OnboardingEvent
import com.ivy.onboarding.OnboardingState
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.CategoryCreator
import com.ivy.wallet.domain.deprecated.logic.PreloadDataLogic
import com.ivy.wallet.domain.deprecated.logic.WalletAccountLogic
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.domain.deprecated.logic.notification.TransactionReminderLogic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import com.ivy.base.threading.DispatchersProvider
import java.util.UUID
import javax.inject.Inject

@Stable
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val nav: Navigation,
    private val accountDao: AccountDao,
    private val settingsDao: SettingsDao,
    private val accountLogic: WalletAccountLogic,
    private val categoryCreator: CategoryCreator,
    private val categoryRepository: CategoryRepository,
    private val accountCreator: AccountCreator,

    private val accountsAct: AccountsAct,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val settingsWriter: WriteSettingsDao,

    // Only OnboardingRouter stuff
    private val sharedPrefs: SharedPrefs,
    transactionReminderLogic: TransactionReminderLogic,
    preloadDataLogic: PreloadDataLogic,
    logoutLogic: LogoutLogic,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<OnboardingDetailState, OnboardingEvent>() {

    private val _state = mutableStateOf(OnboardingState.SPLASH)
    val state: State<OnboardingState> = _state
    val editorModals = com.ivy.legacy.ui.EditorModalHost()
    private var startedEntry: UUID? = null
    private var initialized by androidx.compose.runtime.mutableStateOf(false)
    val ready: Boolean get() = initialized
    private var startupJob: Job? = null
    private val _startupError = mutableStateOf(false)
    val startupError: State<Boolean> = _startupError
    private val accountSuggestionIntents = com.ivy.legacy.domain.creation.CreationIntentPool<CreateAccountData>()
    private val categorySuggestionIntents = com.ivy.legacy.domain.creation.CreationIntentPool<CreateCategoryData>()
    val accountSuggestionSubmission = com.ivy.design.system.EditorSubmission { com.ivy.wallet.ui.theme.modal.edit.accountSuggestionErrorMessage(it) }
    val categorySuggestionSubmission = com.ivy.design.system.EditorSubmission()

    private val _currency = mutableStateOf(IvyCurrency.getDefault())
    private val _opGoogleSignIn = mutableStateOf<OpResult<Unit>?>(null)
    private val _accounts = mutableStateOf(listOf<AccountBalance>().toImmutableList())
    private val _accountSuggestions = mutableStateOf(listOf<CreateAccountData>().toImmutableList())
    private val _categories = mutableStateOf(listOf<Category>().toImmutableList())
    private val _categorySuggestions =
        mutableStateOf(listOf<CreateCategoryData>().toImmutableList())

    @Composable
    override fun uiState(): OnboardingDetailState {
        return OnboardingDetailState(
            currency = _currency.value,
            accounts = _accounts.value,
            accountSuggestions = _accountSuggestions.value,
            categories = _categories.value,
            categorySuggestions = _categorySuggestions.value
        )
    }

    private val router = OnboardingRouter(
        state = _state,
        opGoogleSignIn = _opGoogleSignIn,
        accounts = _accounts,
        accountSuggestions = _accountSuggestions,
        categories = _categories,
        categorySuggestions = _categorySuggestions,

        nav = nav,
        accountDao = accountDao,
        sharedPrefs = sharedPrefs,
        categoryRepository = categoryRepository,
        preloadDataLogic = preloadDataLogic,
        transactionReminderLogic = transactionReminderLogic,
        logoutLogic = logoutLogic,
        syncExchangeRatesUseCase = syncExchangeRatesUseCase,
        dispatchers = dispatchers,
    )

    fun saveDraft(): List<String>? = if (initialized) com.ivy.design.system.ProcessDraftState.encode(mapOf("step" to state.value.name)) else null

    fun start(screen: OnboardingScreen, isSystemDarkMode: Boolean, restored: List<String>? = null) {
        val entry = nav.currentEntryId ?: return
        if (startedEntry == entry && (initialized || startupJob?.isActive == true)) return
        startupJob?.cancel()
        if (startedEntry != entry) {
            _state.value = OnboardingState.SPLASH
            initialized = false
        }
        startedEntry = entry
        _startupError.value = false
        startupJob = viewModelScope.launch {
            try {
            val currency = initiateSettings(isSystemDarkMode)
            if (startedEntry != entry || !nav.retainsEntry(entry)) return@launch
            _currency.value = currency
            router.initBackHandling(
                screen = screen,
                viewModelScope = viewModelScope,
                restartOnboarding = {
                    initialized = false
                    start(screen, isSystemDarkMode)
                }
            )

            val step = com.ivy.design.system.ProcessDraftState.decode(restored)?.get("step")?.let {
                runCatching { OnboardingState.valueOf(it) }.getOrNull()
            }
            if (step != null) router.restoreStep(step, currency, ::accountsWithBalance) else router.splashNext()
            initialized = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (startedEntry == entry) _startupError.value = true
            }
        }
    }

    private suspend fun initiateSettings(isSystemDarkMode: Boolean): IvyCurrency {
        val defaultCurrency = IvyCurrency.getDefault()
        return withContext(dispatchers.io) {
            val existing = settingsDao.findFirstOrNull()
            if (existing == null) {
                settingsWriter.save(
                    Settings(
                        theme = if (isSystemDarkMode) Theme.DARK else Theme.LIGHT,
                        name = "",
                        baseCurrency = defaultCurrency.code,
                        bufferAmount = 1000.0.toBigDecimal()
                    ).toEntity()
                )
            }
            existing?.currency?.let(IvyCurrency::fromCode) ?: defaultCurrency
        }
    }

    override fun onEvent(event: OnboardingEvent) {
        viewModelScope.launch {
            when (event) {
                is OnboardingEvent.CreateAccountSuggestion -> createSuggestedAccount(event.data, event.onResult)
                is OnboardingEvent.CreateCategorySuggestion -> createSuggestedCategory(event.data, event.onResult)
                is OnboardingEvent.CreateAccount -> createAccount(event.data, event.onResult)
                is OnboardingEvent.CreateCategory -> createCategory(event.data, event.onResult)
                is OnboardingEvent.EditAccount -> editAccount(event.account, event.newBalance, event.onResult)
                is OnboardingEvent.EditCategory -> editCategory(event.updatedCategory, event.onResult)
                is OnboardingEvent.ImportFinished -> importFinished(event.success)
                OnboardingEvent.ImportSkip -> importSkip()
                OnboardingEvent.LoginOfflineAccount -> loginOfflineAccount()
                OnboardingEvent.OnAddAccountsDone -> onAddAccountsDone()
                OnboardingEvent.OnAddAccountsSkip -> onAddAccountsSkip()
                OnboardingEvent.OnAddCategoriesDone -> onAddCategoriesDone()
                OnboardingEvent.OnAddCategoriesSkip -> onAddCategoriesSkip()
                is OnboardingEvent.SetBaseCurrency -> setBaseCurrency(event.baseCurrency)
                OnboardingEvent.StartFresh -> startFresh()
                OnboardingEvent.StartImport -> startImport()
            }
        }
    }

    private suspend fun loginOfflineAccount() {
        router.offlineAccountNext()
    }
    // Step 1 ---------------------------------------------------------------------------------------

    // Step 2 ---------------------------------------------------------------------------------------
    private fun startImport() {
        router.startImport()
    }

    fun importSkip() {
        router.importSkip()
    }

    fun importFinished(success: Boolean) {
        router.importFinished(success)
    }

    private fun startFresh() {
        router.startFresh()
    }
    // Step 2 ---------------------------------------------------------------------------------------

    private suspend fun setBaseCurrency(baseCurrency: IvyCurrency) {
        updateBaseCurrency(baseCurrency)
        router.setBaseCurrencyNext(
            baseCurrency = baseCurrency,
            accountsWithBalance = { accountsWithBalance() }
        )
    }

    private suspend fun updateBaseCurrency(baseCurrency: IvyCurrency) {
        withContext(dispatchers.io) {
            // This is the explicit initial currency confirmation, not Settings' later
            // currency-change action. AI imports establish their implicit units here.
            check(!sharedPrefs.getBoolean(com.ivy.base.legacy.SharedPrefs.ONBOARDING_COMPLETED, false))
            settingsWriter.saveInitialCurrency(
                settingsDao.findFirst().copy(
                    currency = baseCurrency.code
                )
            )
        }
        _currency.value = baseCurrency
    }

    // --------------------- Accounts ---------------------------------------------------------------
    private suspend fun editAccount(account: Account, newBalance: Double, onResult: (Result<Unit>) -> Unit) {
        accountCreator.editAccount(account, newBalance, onResult) {
            _accounts.value = accountsWithBalance()
        }
    }

    private suspend fun createSuggestedAccount(source: CreateAccountData, onResult: (Result<Unit>) -> Unit) {
        val intent = accountSuggestionIntents.request(source.id) { source.copy(id = java.util.UUID.randomUUID()) }
        createAccount(intent) { result ->
            accountSuggestionIntents.complete(source.id, intent, result)
            onResult(result)
        }
    }

    private suspend fun createSuggestedCategory(source: CreateCategoryData, onResult: (Result<Unit>) -> Unit) {
        val intent = categorySuggestionIntents.request(source.id) { source.copy(id = java.util.UUID.randomUUID()) }
        createCategory(intent) { result ->
            categorySuggestionIntents.complete(source.id, intent, result)
            onResult(result)
        }
    }

    private suspend fun createAccount(data: CreateAccountData, onResult: (Result<Unit>) -> Unit) {
        accountCreator.createAccount(data, onResult) {
            _accounts.value = accountsWithBalance()
        }
    }

    private suspend fun accountsWithBalance(): ImmutableList<AccountBalance> = withContext(dispatchers.io) {
        accountsAct(Unit)
            .map {
                AccountBalance(
                    account = it,
                    balance = withContext(dispatchers.io) { accountLogic.calculateAccountBalance(it) }
                )
            }.toImmutableList()
    }

    private suspend fun onAddAccountsDone() {
        router.accountsNext()
    }

    private suspend fun onAddAccountsSkip() {
        router.accountsSkip()
    }
    // --------------------- Accounts ---------------------------------------------------------------

    // ---------------------------- Categories ------------------------------------------------------
    private suspend fun editCategory(updatedCategory: Category, onResult: (Result<Unit>) -> Unit) {
        categoryCreator.editCategory(updatedCategory, onResult) {
            _categories.value = categoryRepository.findAll().toImmutableList()
        }
    }

    private suspend fun createCategory(data: CreateCategoryData, onResult: (Result<Unit>) -> Unit) {
        categoryCreator.createCategory(data, onResult) {
            _categories.value = categoryRepository.findAll().toImmutableList()
        }
    }

    private suspend fun onAddCategoriesDone() {
        router.categoriesNext(baseCurrency = _currency.value)
    }

    private suspend fun onAddCategoriesSkip() {
        router.categoriesSkip(baseCurrency = _currency.value)
    }
    // ---------------------------- Categories ------------------------------------------------------
}
