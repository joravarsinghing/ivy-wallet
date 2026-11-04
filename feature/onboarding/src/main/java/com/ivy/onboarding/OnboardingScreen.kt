package com.ivy.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.data.model.Category
import com.ivy.legacy.data.model.AccountBalance
import com.ivy.legacy.utils.onScreenStart
import com.ivy.navigation.OnboardingScreen
import com.ivy.onboarding.steps.OnboardingAccounts
import com.ivy.onboarding.steps.OnboardingCategories
import com.ivy.onboarding.steps.OnboardingSetCurrency
import com.ivy.onboarding.steps.OnboardingSplashLogin
import com.ivy.onboarding.steps.OnboardingType
import com.ivy.onboarding.viewmodel.OnboardingViewModel
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import kotlinx.collections.immutable.ImmutableList

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.OnboardingScreen(screen: OnboardingScreen, modifier: Modifier = Modifier) {
    val viewModel: OnboardingViewModel = viewModel()
    val restoration = com.ivy.design.system.rememberProcessRestoration("onboarding-step", viewModel::saveDraft)

    val state by viewModel.state
    val uiState = viewModel.uiState()
    val modalsResolved = com.ivy.legacy.ui.rememberEditorModalRestoration(viewModel.editorModals, viewModel.ready, uiState.currency.code,
        uiState.accounts.map { it.account }, uiState.categories, balances = uiState.accounts.associate { it.account.id to it.balance })

    val isSystemDarkTheme = isSystemInDarkTheme()
    val nav = com.ivy.navigation.navigation()
    onScreenStart {
        viewModel.start(
            screen = screen,
            isSystemDarkMode = isSystemDarkTheme,
            restored = restoration.payload,
        )
    }

    if (!modalsResolved) return
    if (viewModel.ready) androidx.compose.runtime.SideEffect { restoration.consumed = true }
    BoxWithConstraints(modifier.fillMaxSize()) {
    if (viewModel.startupError.value) {
        com.ivy.design.system.JvySubsectionScaffold(title = "Set up Jvy", onBack = { nav.onBackPressed() }) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxWidth().padding(24.dp)) {
                androidx.compose.material3.Text("Could not load setup. Your saved data has not been changed.",
                    modifier = androidx.compose.ui.Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
                com.ivy.design.system.JvyButton(onClick = { viewModel.start(screen, isSystemDarkTheme, restoration.payload) }) {
                    androidx.compose.material3.Text("Try again")
                }
            }
        }
        return@BoxWithConstraints
    }

    UI(
        onboardingState = state,
        currency = uiState.currency,

        accountSuggestions = uiState.accountSuggestions,
        accounts = uiState.accounts,

        categorySuggestions = uiState.categorySuggestions,
        categories = uiState.categories,
        categorySuggestionSubmission = viewModel.categorySuggestionSubmission,
        accountSuggestionSubmission = viewModel.accountSuggestionSubmission,
        editorModals = viewModel.editorModals,

        onEvent = viewModel::onEvent

    )
    }
}

@ExperimentalFoundationApi
@Composable
internal fun BoxWithConstraintsScope.UI(
    onboardingState: OnboardingState,
    currency: IvyCurrency,

    accountSuggestions: ImmutableList<CreateAccountData>,
    accounts: ImmutableList<AccountBalance>,

    categorySuggestions: ImmutableList<CreateCategoryData>,
    categories: ImmutableList<Category>,

    accountSuggestionSubmission: com.ivy.design.system.EditorSubmission = androidx.compose.runtime.remember { com.ivy.design.system.EditorSubmission { com.ivy.wallet.ui.theme.modal.edit.accountSuggestionErrorMessage(it) } },
    categorySuggestionSubmission: com.ivy.design.system.EditorSubmission = androidx.compose.runtime.remember { com.ivy.design.system.EditorSubmission() },
    editorModals: com.ivy.legacy.ui.EditorModalHost = androidx.compose.runtime.remember { com.ivy.legacy.ui.EditorModalHost() },
    onEvent: (OnboardingEvent) -> Unit = {}
) {
    when (onboardingState) {
        OnboardingState.SPLASH, OnboardingState.LOGIN -> {
            OnboardingSplashLogin(
                onboardingState = onboardingState,
                onSkip = { onEvent(OnboardingEvent.LoginOfflineAccount) }
            )
        }

        OnboardingState.CHOOSE_PATH -> {
            OnboardingType(
                onStartImport = { onEvent(OnboardingEvent.StartImport) },
                onStartFresh = { onEvent(OnboardingEvent.StartFresh) }
            )
        }

        OnboardingState.CURRENCY -> {
            OnboardingSetCurrency(
                preselectedCurrency = currency,
                onSetCurrency = { onEvent(OnboardingEvent.SetBaseCurrency(it)) }
            )
        }

        OnboardingState.ACCOUNTS -> {
            OnboardingAccounts(
                editorModals = editorModals,
                onAddSuggestedAccount = { data, complete -> onEvent(OnboardingEvent.CreateAccountSuggestion(data, complete)) },
                suggestionSubmission = accountSuggestionSubmission,
                baseCurrency = currency.code,
                suggestions = accountSuggestions,
                accounts = accounts,

                onCreateAccount = { data, complete -> onEvent(OnboardingEvent.CreateAccount(data, complete)) },
                onEditAccount = { account, newBalance, complete ->
                    onEvent(
                        OnboardingEvent.EditAccount(
                            account,
                            newBalance,
                            complete
                        )
                    )
                },

                onDoneClick = { onEvent(OnboardingEvent.OnAddAccountsDone) },
                onSkip = { onEvent(OnboardingEvent.OnAddAccountsSkip) }
            )
        }

        OnboardingState.CATEGORIES -> {
            OnboardingCategories(
                editorModals = editorModals,
                onAddSuggestedCategory = { data, complete -> onEvent(OnboardingEvent.CreateCategorySuggestion(data, complete)) },
                suggestionSubmission = categorySuggestionSubmission,
                suggestions = categorySuggestions,
                categories = categories,

                onCreateCategory = { data, complete -> onEvent(OnboardingEvent.CreateCategory(data, complete)) },
                onEditCategory = { category, complete -> onEvent(OnboardingEvent.EditCategory(category, complete)) },

                onDoneClick = { onEvent(OnboardingEvent.OnAddCategoriesDone) },
                onSkip = { onEvent(OnboardingEvent.OnAddCategoriesSkip) }
            )
        }
    }
}
