package com.ivy.reports

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.stringRes
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.component.IncomeExpensesCards
import com.ivy.legacy.ui.component.transaction.TransactionsDividerLine
import com.ivy.legacy.ui.component.transaction.transactions
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.GreenDark
import com.ivy.wallet.ui.theme.GreenLight
import com.ivy.wallet.ui.theme.IvyDark
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.Purple1Dark
import com.ivy.wallet.ui.theme.Red3Light
import com.ivy.wallet.ui.theme.components.BackButtonType
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.CircleButtonFilled
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyCheckboxWithText
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import com.ivy.wallet.ui.theme.components.IvyToolbar
import com.ivy.wallet.ui.theme.pureBlur
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ReportScreen(
    screen: ReportScreen
) {
    val viewModel: ReportViewModel = viewModel()
    val state = viewModel.uiState()
    val nav = navigation()
    val entry = nav.currentEntryId
    val saved = com.ivy.design.system.rememberProcessRestoration("report-page") {
        if (viewModel.readyEntry == entry) viewModel.savePage() else null
    }
    LaunchedEffect(entry) {
        val retained = viewModel.readyEntry == entry
        viewModel.start(entry, if (retained) viewModel.savePage() else saved.payload, refresh = retained)
    }
    if (viewModel.restorationNotice) com.ivy.legacy.ui.EditorRestorationNotice(viewModel::acknowledgeRestoration)
    if (viewModel.readyEntry != entry) {
        com.ivy.design.system.JvyEditorLoading(stringResource(R.string.reports), viewModel.startupFailed,
            { nav.back() }, { viewModel.start(entry, saved.payload) })
        return
    }
    androidx.compose.runtime.SideEffect { saved.consumed = true }

    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@ExperimentalFoundationApi
@Composable
internal fun BoxWithConstraintsScope.UI(
    state: ReportScreenState = ReportScreenState(),
    onEventHandler: (ReportScreenEvent) -> Unit = {}
) {
    val legacyTransactions = state.transactions
    val nav = navigation()
    val context = LocalContext.current

    val listState = rememberScrollPositionListState(key = "reports")
    var saveDialogVisible by rememberSaveable { mutableStateOf(false) }

    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f)
                .background(pureBlur())
                .clickableNoIndication(rememberInteractionSource()) {
                    // consume clicks
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.generating_report),
                style = UI.typo.b1.style(
                    fontWeight = FontWeight.ExtraBold,
                    color = Orange
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        state = listState
    ) {
        stickyHeader {
            Toolbar(
                onExport = {
                    onEventHandler.invoke(ReportScreenEvent.OnExport(context = context))
                },
                onFilter = {
                    onEventHandler.invoke(
                        ReportScreenEvent.OnFilterOverlayVisible(
                            filterOverlayVisible = true
                        )
                    )
                },
                onSave = { saveDialogVisible = true }
            )
        }

        item {
            SavedAnalyticsPanel(
                savedAnalytics = state.savedAnalytics,
                onApply = {
                    onEventHandler.invoke(ReportScreenEvent.OnApplySavedAnalytics(it))
                },
                onDelete = {
                    onEventHandler.invoke(ReportScreenEvent.OnDeleteSavedAnalytics(it))
                }
            )

            Spacer(Modifier.height(8.dp))

            BalanceRow(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp),
                textColor = UI.colors.pureInverse,
                currency = state.baseCurrency,
                balance = state.balance,
                balanceAmountPrefix = when {
                    state.balance > 0 -> "+"
                    else -> null
                }
            )

            Spacer(Modifier.height(20.dp))

            IncomeExpensesCards(
                history = state.history,
                currency = state.baseCurrency,
                income = state.income,
                expenses = state.expenses,
                hasAddButtons = false,
                itemColor = UI.colors.pure,
                incomeHeaderCardClicked = {
                    if (state.transactions.isNotEmpty()) {
                        nav.navigateTo(
                            PieChartStatisticScreen(
                                type = TransactionType.INCOME,
                                transactions = legacyTransactions.toImmutableList(),
                                accountList = state.accountIdFilters,
                                treatTransfersAsIncomeExpense = state.treatTransfersAsIncExp
                            )
                        )
                    }
                },
                expenseHeaderCardClicked = {
                    if (state.transactions.isNotEmpty()) {
                        nav.navigateTo(
                            PieChartStatisticScreen(
                                type = TransactionType.EXPENSE,
                                transactions = legacyTransactions.toImmutableList(),
                                accountList = state.accountIdFilters,
                                treatTransfersAsIncomeExpense = state.treatTransfersAsIncExp
                            )
                        )
                    }
                }
            )

            if (state.previousIncome != null && state.previousExpenses != null) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = com.ivy.design.system.financialText("Compared with the previous period: income ${comparisonLabel(state.income, state.previousIncome)} · expenses ${comparisonLabel(state.expenses, state.previousExpenses)}"),
                    style = UI.typo.c.style(color = Gray, fontWeight = FontWeight.Medium),
                )
            }

            if (state.showTransfersAsIncExpCheckbox) {
                IvyCheckboxWithText(
                    modifier = Modifier
                        .padding(16.dp),
                    text = stringResource(R.string.transfers_as_income_expense),
                    checked = state.treatTransfersAsIncExp
                ) {
                    onEventHandler.invoke(
                        ReportScreenEvent.OnTreatTransfersAsIncomeExpense(
                            transfersAsIncomeExpense = it
                        )
                    )
                }
            } else {
                Spacer(Modifier.height(32.dp))
            }

            TransactionsDividerLine(
                paddingHorizontal = 0.dp
            )

            Spacer(Modifier.height(4.dp))
        }

        if (state.filter != null) {
            transactions(
                baseData = AppBaseData(
                    baseCurrency = state.baseCurrency,
                    categories = state.categories,
                    accounts = state.accounts,
                ),

                upcoming = LegacyDueSection(
                    trns = state.upcomingTransactions,
                    stats = IncomeExpensePair(
                        income = state.upcomingIncome.toBigDecimal(),
                        expense = state.upcomingExpenses.toBigDecimal()
                    ),
                    expanded = state.upcomingExpanded
                ),

                setUpcomingExpanded = {
                    onEventHandler.invoke(ReportScreenEvent.OnUpcomingExpanded(upcomingExpanded = it))
                },

                overdue = LegacyDueSection(
                    trns = state.overdueTransactions,
                    stats = IncomeExpensePair(
                        income = state.overdueIncome.toBigDecimal(),
                        expense = state.overdueExpenses.toBigDecimal()
                    ),
                    expanded = state.overdueExpanded
                ),
                setOverdueExpanded = {
                    onEventHandler.invoke(ReportScreenEvent.OnOverdueExpanded(overdueExpanded = it))
                },

                history = state.history,
                lastItemSpacer = 48.dp,

                onPayOrGet = {
                    onEventHandler.invoke(ReportScreenEvent.OnPayOrGetLegacy(transaction = it))
                },
                emptyStateTitle = stringRes(R.string.no_transactions),
                emptyStateText = stringRes(R.string.no_transactions_for_your_filter),
                shouldShowAccountSpecificColorInTransactions = state.showAccountColorsInTransactions,
                onSkipTransaction = {
                    onEventHandler.invoke(ReportScreenEvent.SkipTransactionLegacy(transaction = it))
                },
                onSkipAllTransactions = {
                    onEventHandler.invoke(ReportScreenEvent.SkipTransactionsLegacy(transactions = it))
                }
            )
        } else {
            item {
                NoFilterEmptyState(
                    setFilterOverlayVisible = {
                        onEventHandler.invoke(
                            ReportScreenEvent.OnFilterOverlayVisible(
                                filterOverlayVisible = it
                            )
                        )
                    }
                )
            }
        }
    }

    SaveAnalyticsDialog(
        visible = saveDialogVisible,
        onDismiss = { saveDialogVisible = false },
        onSave = {
            saveDialogVisible = false
            onEventHandler.invoke(ReportScreenEvent.OnSaveCurrentFilter(it))
        }
    )

    FilterOverlay(
        visible = state.filterOverlayVisible,
        baseCurrency = state.baseCurrency,
        accounts = state.accounts,
        categories = state.categories,
        filter = state.filter,
        allTags = state.allTags,
        onClose = {
            onEventHandler.invoke(
                ReportScreenEvent.OnFilterOverlayVisible(
                    filterOverlayVisible = false
                )
            )
        },
        onSetFilter = {
            onEventHandler.invoke(ReportScreenEvent.OnFilter(filter = it))
        },
        onTagSearch = {
            onEventHandler.invoke(ReportScreenEvent.OnTagSearch(data = it))
        }
    )
}

private fun comparisonLabel(current: Double, previous: Double): String {
    if (previous == 0.0) return if (current == 0.0) "unchanged" else "new"
    val percentage = ((current - previous) / kotlin.math.abs(previous)) * 100.0
    return "${if (percentage >= 0) "+" else ""}${"%.0f".format(java.util.Locale.US, percentage)}%"
}

@Composable
private fun NoFilterEmptyState(
    setFilterOverlayVisible: (Boolean) -> Unit
) {
    com.ivy.design.system.JvyEmptyState(
        title = stringResource(R.string.no_filter),
        description = stringResource(R.string.invalid_filter_warning),
        icon = Icons.Outlined.FilterAlt,
        actionLabel = stringResource(R.string.set_filter),
        onAction = { setFilterOverlayVisible(true) },
    )
}

@Composable
private fun Toolbar(
    onExport: () -> Unit,
    onFilter: () -> Unit,
    onSave: () -> Unit
) {
    val nav = navigation()
    var actions by rememberSaveable { mutableStateOf(false) }
    com.ivy.design.system.JvyScreenHeader(stringResource(R.string.reports), { nav.back() }) {
        androidx.compose.material3.IconButton(onFilter) {
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_filter_xs), stringResource(R.string.filter))
        }
        Box {
            androidx.compose.material3.IconButton({ actions = true }) {
                androidx.compose.material3.Icon(Icons.Outlined.MoreVert, stringResource(R.string.jvy_navigation_actions))
            }
            androidx.compose.material3.DropdownMenu(actions, { actions = false }) {
                androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.save_report)) }, onClick = { actions = false; onSave() })
                androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.export_to_csv)) }, onClick = { actions = false; onExport() })
            }
        }
    }
}

@Composable
private fun SaveAnalyticsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.save_report)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.report_name)) }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSave(name)
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SavedAnalyticsPanel(
    savedAnalytics: kotlinx.collections.immutable.ImmutableList<com.ivy.data.db.entity.SavedAnalyticsEntity>,
    onApply: (UUID) -> Unit,
    onDelete: (UUID) -> Unit,
) {
    if (savedAnalytics.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.saved_reports),
            style = UI.typo.b1.style(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(4.dp))
        savedAnalytics.forEach { saved ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = saved.name,
                    style = UI.typo.b2.style()
                )
                TextButton(onClick = { onApply(saved.id) }) {
                    Text(stringResource(R.string.apply_filter))
                }
                TextButton(onClick = { onDelete(saved.id) }) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        val acc1 = Account("Cash", color = Green.toArgb())
        val acc2 = Account("DSK", color = GreenDark.toArgb())
        val cat1 = Category(
            name = NotBlankTrimmedString.unsafe("Science"),
            color = ColorInt(Purple1Dark.toArgb()),
            icon = IconAsset.unsafe("atom"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )
        val state = ReportScreenState(
            baseCurrency = "BGN",
            balance = -6405.66,
            income = 2000.0,
            expenses = 8405.66,
            upcomingIncome = 4800.23,
            upcomingExpenses = 0.0,
            overdueIncome = 2335.12,
            overdueExpenses = 0.0,
            history =
            persistentListOf(),
            upcomingTransactions = persistentListOf(),
            overdueTransactions = persistentListOf(),

            upcomingExpanded = true,
            overdueExpanded = true,
            filter = ReportFilter.emptyFilter("BGN"),
            loading = false,
            accounts = persistentListOf(
                acc1,
                acc2,
                Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
                Account("Revolut", color = IvyDark.toArgb()),
            ),
            categories = persistentListOf(
                cat1,
                Category(
                    name = NotBlankTrimmedString.unsafe("Pet"),
                    color = ColorInt(Red3Light.toArgb()),
                    icon = IconAsset.unsafe("pet"),
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
                Category(
                    name = NotBlankTrimmedString.unsafe("Home"),
                    color = ColorInt(Green.toArgb()),
                    icon = null,
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
            ),
        )

        UI(state = state)
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_NO_FILTER(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        val acc1 = Account("Cash", color = Green.toArgb())
        val acc2 = Account("DSK", color = GreenDark.toArgb())
        val cat1 = Category(
            name = NotBlankTrimmedString.unsafe("Science"),
            color = ColorInt(Purple1Dark.toArgb()),
            icon = IconAsset.unsafe("atom"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )
        val state = ReportScreenState(
            baseCurrency = "BGN",
            balance = 0.0,
            income = 0.0,
            expenses = 0.0,
            upcomingIncome = 0.0,
            upcomingExpenses = 0.0,
            overdueIncome = 0.0,
            overdueExpenses = 0.0,

            history = persistentListOf(),
            upcomingTransactions = persistentListOf(),
            overdueTransactions = persistentListOf(),

            upcomingExpanded = true,
            overdueExpanded = true,

            filter = null,
            loading = false,

            accounts = persistentListOf(
                acc1,
                acc2,
                Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
                Account("Revolut", color = IvyDark.toArgb()),
            ),
            categories = persistentListOf(
                cat1,
                Category(
                    name = NotBlankTrimmedString.unsafe("Pet"),
                    color = ColorInt(Red3Light.toArgb()),
                    icon = IconAsset.unsafe("pet"),
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
                Category(
                    name = NotBlankTrimmedString.unsafe("Home"),
                    color = ColorInt(Green.toArgb()),
                    icon = null,
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
            ),
        )

        UI(state = state)
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportUiTest(isDark: Boolean) {
    val theme = if (isDark) Theme.DARK else Theme.LIGHT
    Preview(theme)
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportNoFilterUiTest(isDark: Boolean) {
    val theme = if (isDark) Theme.DARK else Theme.LIGHT
    Preview_NO_FILTER(theme)
}
