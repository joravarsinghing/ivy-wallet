package com.ivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.l0_system.Orange
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.utils.hideKeyboard
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.EditTransactionDisplayLoan
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.edit.TransactionDateTime
import com.ivy.legacy.ui.component.edit.core.Description
import com.ivy.legacy.ui.component.tags.AddTagButton
import com.ivy.legacy.ui.component.tags.ShowTagModal
import com.ivy.legacy.utils.onScreenStart
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.domain.data.CustomExchangeRateState
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.ui.edit.core.Category
import com.ivy.wallet.ui.edit.core.DueDate
import com.ivy.wallet.ui.edit.core.EditBottomSheet
import com.ivy.wallet.ui.edit.core.Title
import com.ivy.wallet.ui.edit.core.Toolbar
import com.ivy.wallet.ui.theme.components.AddPrimaryAttributeButton
import com.ivy.wallet.ui.theme.components.ChangeTransactionTypeModal
import com.ivy.wallet.ui.theme.components.CustomExchangeRateCard
import com.ivy.wallet.ui.theme.components.IvyCheckboxWithText
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.ModalAdd
import com.ivy.wallet.ui.theme.modal.ModalCheck
import com.ivy.wallet.ui.theme.modal.ModalSave
import com.ivy.wallet.ui.theme.modal.ProgressModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.ivy.wallet.ui.theme.modal.edit.ChooseCategoryModal
import com.ivy.wallet.ui.theme.modal.edit.DescriptionModal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.roundToInt

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.EditTransactionScreen(screen: EditTransactionScreen) {
    val viewModel: EditTransactionViewModel = screenScopedViewModel()
    val restoration = com.ivy.design.system.rememberProcessRestoration("transaction-draft", viewModel::saveDraft)
    val uiState = viewModel.uiState()
    val modalsResolved = com.ivy.legacy.ui.rememberEditorModalRestoration(viewModel.editorModals, viewModel.ready, uiState.currency,
        uiState.accounts, uiState.categories, uiState.tags)

    LaunchedEffect(Unit) {
        viewModel.start(screen, restoration.payload)
    }

    if (!viewModel.ready || !modalsResolved) {
        com.ivy.design.system.JvyEditorLoading(stringResource(R.string.transactions), viewModel.loadError,
            viewModel::closeScreen, { viewModel.start(screen, restoration.payload) })
        return
    }
    androidx.compose.runtime.SideEffect { restoration.consumed = true }

    val view = LocalView.current

    UI(
        editorModals = viewModel.editorModals,
        screen = screen,
        transactionType = uiState.transactionType,
        baseCurrency = uiState.currency,
        initialTitle = uiState.initialTitle,
        titleSuggestions = uiState.titleSuggestions,
        description = uiState.description,
        dateTime = uiState.dateTime,
        category = uiState.category,
        suggestedCategory = uiState.suggestedCategory,
        account = uiState.account,
        toAccount = uiState.toAccount,
        dueDate = uiState.dueDate,
        amount = uiState.amount,
        showRememberCategoryRule = uiState.showRememberCategoryRule,
        rememberCategoryRule = uiState.rememberCategoryRule,
        loanData = uiState.displayLoanHelper,
        backgroundProcessing = uiState.backgroundProcessingStarted,
        saveError = uiState.saveError,
        saving = uiState.saving,
        customExchangeRateState = uiState.customExchangeRateState,

        categories = uiState.categories,
        accounts = uiState.accounts,
        tags = uiState.tags,
        transactionAssociatedTags = uiState.transactionAssociatedTags,
        hasChanges = uiState.hasChanges,
        onSetDate = {
            viewModel.onEvent(EditTransactionViewEvent.OnChangeDate)
        },
        onSetTime = {
            viewModel.onEvent(EditTransactionViewEvent.OnChangeTime)
        },
        onTitleChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnTitleChanged(it))
        },
        onDescriptionChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnDescriptionChanged(it))
        },
        onAmountChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(it))
        },
        onCategoryChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnCategoryChanged(it))
        },
        onRememberCategoryRuleChange = {
            viewModel.onEvent(EditTransactionViewEvent.SetRememberCategoryRule(it))
        },
        onAccountChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnAccountChanged(it))
        },
        onToAccountChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnToAccountChanged(it))
        },
        onDueDateChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnDueDateChanged(it))
        },
        onSetTransactionType = {
            viewModel.onEvent(EditTransactionViewEvent.OnSetTransactionType(it))
        },
        onCreateCategory = { data, complete ->
            viewModel.onEvent(EditTransactionViewEvent.CreateCategory(data, complete))
        },
        onEditCategory = { category, complete ->
            viewModel.onEvent(EditTransactionViewEvent.EditCategory(category, complete))
        },
        onPayPlannedPayment = {
            viewModel.onEvent(EditTransactionViewEvent.OnPayPlannedPayment)
        },
        onSave = {
            view.hideKeyboard()
            viewModel.onEvent(EditTransactionViewEvent.Save(it))
        },
        onSetHasChanges = {
            viewModel.onEvent(EditTransactionViewEvent.SetHasChanges(it))
        },
        onDelete = {
            viewModel.onEvent(EditTransactionViewEvent.Delete)
        },
        onDuplicate = {
            viewModel.onEvent(EditTransactionViewEvent.Duplicate)
        },
        onCreateAccount = { data, complete ->
            viewModel.onEvent(EditTransactionViewEvent.CreateAccount(data, complete))
        },
        onExchangeRateChange = {
            viewModel.onEvent(EditTransactionViewEvent.UpdateExchangeRate(it))
        },
        onTagOperation = {
            viewModel.onEvent(it)
        }
    )
}

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@ExperimentalFoundationApi
@Composable
internal fun BoxWithConstraintsScope.UI(
    screen: EditTransactionScreen,
    transactionType: TransactionType,
    baseCurrency: String,
    initialTitle: String?,
    titleSuggestions: ImmutableSet<String>,
    description: String?,
    category: Category?,
    suggestedCategory: Category?,
    dateTime: Instant?,
    account: Account?,
    toAccount: Account?,
    dueDate: Instant?,
    amount: Double,
    showRememberCategoryRule: Boolean,
    rememberCategoryRule: Boolean,

    customExchangeRateState: CustomExchangeRateState,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,
    tags: ImmutableList<Tag>,
    transactionAssociatedTags: ImmutableList<TagId>,
    onTitleChange: (String?) -> Unit,
    onDescriptionChange: (String?) -> Unit,
    onAmountChange: (Double) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onRememberCategoryRuleChange: (Boolean) -> Unit,
    onAccountChange: (Account) -> Unit,
    onToAccountChange: (Account) -> Unit,
    onDueDateChange: (LocalDateTime?) -> Unit,
    onSetDate: () -> Unit,
    onSetTime: () -> Unit,
    onSetTransactionType: (TransactionType) -> Unit,

    onCreateCategory: (CreateCategoryData, (Result<Unit>) -> Unit) -> Unit,
    onEditCategory: (Category, (Result<Unit>) -> Unit) -> Unit,
    onPayPlannedPayment: () -> Unit,
    onSave: (closeScreen: Boolean) -> Unit,
    onSetHasChanges: (hasChanges: Boolean) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onCreateAccount: (CreateAccountData, (Result<Unit>) -> Unit) -> Unit,
    onExchangeRateChange: (Double?) -> Unit = { },
    onTagOperation: (EditTransactionViewEvent.TagEvent) -> Unit = {},
    loanData: EditTransactionDisplayLoan = EditTransactionDisplayLoan(),
    backgroundProcessing: Boolean = false,
    saveError: String? = null,
    saving: Boolean = false,
    hasChanges: Boolean = false,
    editorModals: com.ivy.legacy.ui.EditorModalHost = remember { com.ivy.legacy.ui.EditorModalHost() },

    ) {
    var chooseCategoryModalVisible by rememberSaveable { mutableStateOf(false) }
    var tagModelVisible by rememberSaveable { mutableStateOf(false) }
    val categoryModalData = editorModals.category
    val accountModalData = editorModals.account
    var descriptionModalVisible by rememberSaveable { mutableStateOf(false) }
    var deleteTrnModalVisible by remember { mutableStateOf(false) }
    var changeTransactionTypeModalVisible by rememberSaveable { mutableStateOf(false) }
    var amountModalShown by rememberSaveable { mutableStateOf(false) }
    var exchangeRateAmountModalShown by rememberSaveable { mutableStateOf(false) }
    var accountChangeModal by remember { mutableStateOf(false) }
    val waitModalVisible by remember(backgroundProcessing) {
        mutableStateOf(backgroundProcessing)
    }
    var selectedAcc by remember(account) {
        mutableStateOf(account)
    }

    val amountModalId =
        UUID.fromString(rememberSaveable(screen.initialTransactionId, customExchangeRateState.exchangeRate) { UUID.randomUUID().toString() })

    var titleTextFieldValue by rememberSaveable(screen.initialTransactionId, initialTitle, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                initialTitle ?: ""
            )
        )
    }
    val titleFocus = FocusRequester()
    val scrollState = rememberScrollState()

    // This is to scroll the column to the customExchangeCard composable when it is shown
    var customExchangeRatePosition by remember { mutableFloatStateOf(0F) }
    LaunchedEffect(key1 = customExchangeRateState.showCard) {
        val scrollInt =
            if (customExchangeRateState.showCard) customExchangeRatePosition.roundToInt() else 0
        scrollState.animateScrollTo(scrollInt)
    }

    val editorHeader: @Composable () -> Unit = {
        Toolbar(
            type = transactionType,
            allowTypeChange = !loanData.isLoanRecord,
            enabled = !saving,
            initialTransactionId = screen.initialTransactionId,
            onDeleteTrnModal = {
                deleteTrnModalVisible = true
            },
            onChangeTransactionTypeModal = {
                changeTransactionTypeModalVisible = true
            },
            showDuplicateButton = true,
            onDuplicate = onDuplicate
        )
    }
    val editorContent: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        Title(
            type = transactionType,
            titleFocus = titleFocus,
            initialTransactionId = screen.initialTransactionId,

            titleTextFieldValue = titleTextFieldValue,
            setTitleTextFieldValue = {
                titleTextFieldValue = it
            },
            suggestions = titleSuggestions,
            scrollState = scrollState,

            onTitleChanged = onTitleChange,
            onNext = {
                when {
                    shouldFocusAmount(amount = amount) -> {
                        amountModalShown = true
                    }

                    else -> {
                        onSave(true)
                    }
                }
            }
        )

        if (loanData.loanCaption != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = loanData.loanCaption!!,
                style = UI.typo.nB2.style(
                    color = UI.colors.mediumInverse,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        Spacer(Modifier.height(32.dp))

        Category(category = category, onChooseCategory = {
            chooseCategoryModalVisible = true
        })

        if (suggestedCategory != null && category == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        R.string.suggested_category,
                        suggestedCategory.name.value,
                    ),
                    style = UI.typo.nB2.style(color = UI.colors.mediumInverse),
                )
                TextButton(onClick = { onCategoryChange(suggestedCategory) }) {
                    Text(text = stringResource(R.string.use_suggested_category))
                }
            }
        }

        if (showRememberCategoryRule && category != null) {
            IvyCheckboxWithText(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(R.string.remember_category_for_similar_transactions),
                checked = rememberCategoryRule,
                onCheckedChange = onRememberCategoryRuleChange,
            )
        }

        Spacer(Modifier.height(16.dp))

        AddTagButton(transactionAssociatedTags = transactionAssociatedTags, onClick = {
            tagModelVisible = true
        })

        Spacer(Modifier.height(32.dp))

        val ivyContext = ivyWalletCtx()

        val timeConverter = LocalTimeConverter.current
        if (dueDate != null) {
            DueDate(dueDate = dueDate) {
                ivyContext.datePicker(
                    initialDate = with(timeConverter) {
                        dueDate.toLocalDate()
                    }
                ) {
                    onDueDateChange(it.atTime(12, 0))
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        Description(
            description = description,
            onAddDescription = { descriptionModalVisible = true },
            onEditDescription = { descriptionModalVisible = true }
        )

        TransactionDateTime(
            dateTime = dateTime,
            dueDateTime = dueDate,
            onEditDate = onSetDate,
            onEditTime = onSetTime,
        )

        if (transactionType == TransactionType.TRANSFER && customExchangeRateState.showCard) {
            Spacer(Modifier.height(12.dp))
            CustomExchangeRateCard(
                fromCurrencyCode = baseCurrency,
                toCurrencyCode = customExchangeRateState.toCurrencyCode ?: baseCurrency,
                exchangeRate = customExchangeRateState.exchangeRate,
                onRefresh = {
                    // Set exchangeRate to null to reset
                    onExchangeRateChange(null)
                },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    customExchangeRatePosition = coordinates.positionInParent().y * 0.3f
                }
            ) {
                exchangeRateAmountModalShown = true
            }
        }

        if (dueDate == null && transactionType != TransactionType.TRANSFER && dateTime == null) {
            Spacer(Modifier.height(12.dp))

            val nav = navigation()
            AddPrimaryAttributeButton(
                icon = R.drawable.ic_planned_payments,
                text = stringResource(R.string.add_planned_date_payment),
                onClick = {
                    nav.back()
                    nav.navigateTo(
                        EditPlannedScreen(
                            plannedPaymentRuleId = null,
                            type = transactionType,
                            amount = amount,
                            accountId = account?.id,
                            categoryId = category?.id?.value,
                            title = titleTextFieldValue.text,
                            description = description,
                        )
                    )
                }
            )
        }
    }

    var initialFlowStarted by rememberSaveable(screen.initialTransactionId) { mutableStateOf(false) }
    onScreenStart {
        if (!initialFlowStarted) {
        initialFlowStarted = true
        if (screen.initialTransactionId == null) {
            amountModalShown = true
        }
        }
    }

    EditBottomSheet(
        Header = editorHeader,
        FormContent = editorContent,
        formScrollState = scrollState,
        saveError = saveError,
        initialTransactionId = screen.initialTransactionId,
        type = transactionType,
        accounts = accounts,
        selectedAccount = account,
        toAccount = toAccount,
        amount = amount,
        currency = baseCurrency,
        convertedAmount = customExchangeRateState.convertedAmount,
        convertedAmountCurrencyCode = customExchangeRateState.toCurrencyCode,

        ActionButton = {
            val payOrGet = screen.initialTransactionId != null && dueDate != null && !hasChanges
            val label = when {
                payOrGet -> if (transactionType == TransactionType.EXPENSE) R.string.pay else R.string.get
                screen.initialTransactionId == null -> R.string.add
                else -> R.string.save
            }
            com.ivy.design.system.JvyButton({
                if (payOrGet) onPayPlannedPayment()
                else onSave(!(screen.initialTransactionId != null && dueDate != null))
            }, Modifier.fillMaxWidth(), enabled = !saving) {
                androidx.compose.material3.Text(stringResource(label))
            }
        },

        amountModalShown = amountModalShown,
        setAmountModalShown = {
            amountModalShown = it
        },

        onAmountChanged = {
            onAmountChange(it)
            if (shouldFocusCategory(category)) {
                chooseCategoryModalVisible = true
            } else if (shouldFocusTitle(titleTextFieldValue, transactionType)) {
                titleFocus.requestFocus()
            }
        },
        onSelectedAccountChanged = {
            if (loanData.isLoan && account?.currency != it.currency) {
                selectedAcc = it
                accountChangeModal = true
            } else {
                onAccountChange(it)
            }
        },
        onToAccountChanged = onToAccountChange,
        onAddNewAccount = {
            editorModals.account = AccountModalData(
                account = null, baseCurrency = baseCurrency, balance = 0.0
            )
        }
    )

    // Modals
    ChooseCategoryModal(
        visible = chooseCategoryModalVisible,
        initialCategory = category,
        categories = categories,
        showCategoryModal = { editorModals.category = CategoryModalData(it) },
        onCategoryChanged = {
            onCategoryChange(it)
            if (shouldFocusTitle(titleTextFieldValue, transactionType)) {
                titleFocus.requestFocus()
            } else if (shouldFocusAmount(amount = amount)) {
                amountModalShown = true
            }
        },
        dismiss = {
            chooseCategoryModalVisible = false
        }
    )

    CategoryModal(modal = categoryModalData, onCreateCategory = { createData, complete ->
        onCreateCategory(createData) { result ->
            if (result.isSuccess) chooseCategoryModalVisible = false
            complete(result)
        }
    }, onEditCategory = onEditCategory, dismiss = {
        editorModals.closeCategory(categoryModalData?.id)
    })

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = { _, _, complete -> complete(Result.failure(IllegalStateException("Editing is unavailable in this create-only context."))) },
        dismiss = {
            editorModals.closeAccount(accountModalData?.id)
        }
    )

    DescriptionModal(
        visible = descriptionModalVisible,
        description = description,
        onDescriptionChanged = onDescriptionChange,
        dismiss = {
            descriptionModalVisible = false
        }
    )

    DeleteModal(
        visible = deleteTrnModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.transaction_confirm_deletion_description),
        dismiss = { deleteTrnModalVisible = false }
    ) {
        onDelete()
    }

    ChangeTransactionTypeModal(
        visible = changeTransactionTypeModalVisible,
        includeTransferType = true,
        initialType = transactionType,
        dismiss = {
            changeTransactionTypeModalVisible = false
        }
    ) {
        onSetTransactionType(it)
    }

    DeleteModal(
        visible = accountChangeModal,
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_change_description),
        buttonText = stringResource(R.string.confirm),
        iconStart = R.drawable.ic_agreed,
        dismiss = {
            accountChangeModal = false
        }
    ) {
        selectedAcc?.let { onAccountChange(it) }
        accountChangeModal = false
    }

    ProgressModal(
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_loan_change),
        visible = waitModalVisible
    )

    AmountModal(
        id = amountModalId,
        visible = exchangeRateAmountModalShown,
        currency = "",
        initialAmount = customExchangeRateState.exchangeRate,
        dismiss = { exchangeRateAmountModalShown = false },
        decimalCountMax = IvyCurrency.getDecimalPlaces(
            customExchangeRateState.toCurrencyCode ?: baseCurrency
        ),
        onAmountChanged = {
            onExchangeRateChange(it)
        }
    )

    ShowTagModal(
        editorHost = editorModals.tags,
        visible = tagModelVisible,
        onDismiss = {
            tagModelVisible = false
            // Reset TagList, avoids showing incorrect tag list when user has searched for a tag
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagSearch(""))
        },
        allTagList = tags,
        selectedTagList = transactionAssociatedTags,
        onTagAdd = { id, name, complete ->
            onTagOperation(EditTransactionViewEvent.TagEvent.SaveTag(id, name, complete))
        },
        onTagEdit = { oldTag, newTag, complete ->
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagEdit(oldTag, newTag, complete))
        },
        onTagDelete = { tag, complete ->
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagDelete(tag, complete))
        },
        onTagSelected = { tag, complete ->
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagSelect(tag, complete))
        },
        onTagDeSelected = { tag, complete ->
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagDeSelect(tag, complete))
        },
        onTagSearch = {
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagSearch(it))
        }
    )
}

private fun shouldFocusCategory(
    category: Category?,
): Boolean = category == null

private fun shouldFocusTitle(
    titleTextFieldValue: TextFieldValue,
    type: TransactionType
): Boolean = titleTextFieldValue.text.isBlank() && type != TransactionType.TRANSFER

private fun shouldFocusAmount(amount: Double) = amount == 0.0

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 27, 0, 35)
    .toInstant(ZoneOffset.UTC)

@ExperimentalFoundationApi
@Preview
@Composable
private fun BoxWithConstraintsScope.Preview(isDark: Boolean = false) {
    IvyPreview(isDark) {
        UI(
            screen = EditTransactionScreen(null, TransactionType.EXPENSE),
            initialTitle = "",
            titleSuggestions = persistentSetOf(),
            tags = persistentListOf(),
            transactionAssociatedTags = persistentListOf(),
            baseCurrency = "BGN",
            dateTime = testDateTime,
            description = null,
            category = null,
            suggestedCategory = null,
            account = Account(name = "phyre", Orange.toArgb()),
            toAccount = null,
            amount = 0.0,
            showRememberCategoryRule = false,
            rememberCategoryRule = false,
            dueDate = null,
            transactionType = TransactionType.INCOME,
            customExchangeRateState = CustomExchangeRateState(),

            categories = persistentListOf(),
            accounts = persistentListOf(),

            onDueDateChange = {},
            onCategoryChange = {},
            onAccountChange = {},
            onToAccountChange = {},
            onDescriptionChange = {},
            onTitleChange = {},
            onAmountChange = {},
            onRememberCategoryRuleChange = {},

            onCreateCategory = { _, _ -> },
            onEditCategory = { _, _ -> },
            onPayPlannedPayment = {},
            onSave = {},
            onSetHasChanges = {},
            onDelete = {},
            onDuplicate = {},
            onCreateAccount = { _, _ -> },
            onSetDate = {},
            onSetTime = {},
            onSetTransactionType = {}
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditTransactionScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    IvyWalletPreview(theme) {
        Preview(isDark)
    }
}
