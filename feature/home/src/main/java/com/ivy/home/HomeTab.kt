package com.ivy.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.legacy.stringRes
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.domain.usecase.cashflow.PlannedCashflowType
import com.ivy.frp.forward
import com.ivy.frp.then2
import com.ivy.home.customerjourney.CustomerJourney
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.BufferInfo
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.ivy.wallet.ui.theme.modal.DeleteModal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.HomeTab() {
    val viewModel: HomeViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    HomeCalendarEffect { viewModel.onEvent(HomeEvent.RefreshHomePeriod) }

    HomeUi(uiState, viewModel::onEvent)
}

@Suppress("LongMethod")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.HomeUi(
    uiState: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ivyContext = ivyWalletCtx()

    var choosePeriodModal: ChoosePeriodModalData? by rememberSaveable(stateSaver = com.ivy.wallet.ui.theme.modal.ChoosePeriodModalDataSaver) {
        mutableStateOf(null)
    }
    var skipAllModalVisible by rememberSaveable { mutableStateOf(false) }

    val baseCurrency = uiState.baseData.baseCurrency
    val homeTimeProvider = LocalTimeProvider.current
    val homeTimeConverter = LocalTimeConverter.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        val listState = rememberScrollPositionListState(
            key = "home_lazy_column",
            initialFirstVisibleItemIndex = ivyContext.transactionsListState
                ?.firstVisibleItemIndex ?: 0,
            initialFirstVisibleItemScrollOffset = ivyContext.transactionsListState
                ?.firstVisibleItemScrollOffset ?: 0
        )

        HomeLazyColumn(
            name = uiState.name,
            periodSelection = uiState.periodSelection,
            appliedPeriodSelection = uiState.appliedPeriodSelection,
            periodError = uiState.periodError,
            periodCapped = uiState.periodCapped,
            onHomePeriodSelected = { onEvent(HomeEvent.SetHomePeriod(it)) },
            onOpenPeriodChooser = {
                val selected = if (uiState.periodSelection == HomePeriodSelection.CurrentCycle) {
                    val start = HomePeriodSelection.CurrentCycle.resolve(homeTimeProvider.localDateNow(),
                        ivyContext.startDayOfMonth, null).window!!.from
                    HomePeriodSelection.BudgetMonth(java.time.YearMonth.from(start)).asTimePeriod(homeTimeConverter)
                } else uiState.period
                choosePeriodModal = ChoosePeriodModalData(period = selected)
            },
            hideBalance = uiState.hideBalance,
            hideIncome = uiState.hideIncome,
            onSetExpand = {
                onEvent(HomeEvent.SetExpanded(it))
            },
            balance = uiState.balance,
            protectedBuffer = uiState.buffer.amount,
            visibleDashboardCards = uiState.visibleDashboardCards,
            spendingBreakdown = uiState.spendingBreakdown,
            moneyComingUp = uiState.moneyComingUp,
            trend = uiState.trend,
            savedAnalytics = uiState.savedAnalytics,
            onTrendRangeSelected = { onEvent(HomeEvent.SetTrendRange(it)) },
            onTrendMetricToggle = { onEvent(HomeEvent.ToggleTrendMetric(it)) },
            onOpenTrendEditor = { onEvent(HomeEvent.OpenTrendEditor) },
            onCloseTrendEditor = { onEvent(HomeEvent.CloseTrendEditor) },
            onOpenDashboardEditor = { onEvent(HomeEvent.OpenDashboardEditor) },
            onOpenSavedReports = { onEvent(HomeEvent.OpenSavedReports) },
            onBalanceClick = {
                onEvent(HomeEvent.BalanceClick)
            },
            onHiddenBalanceClick = {
                onEvent(HomeEvent.HiddenBalanceClick)
            },
            onHiddenIncomeClick = {
                onEvent(HomeEvent.HiddenIncomeClick)
            },

            period = uiState.period,
            listState = listState,

            baseData = uiState.baseData,

            upcoming = uiState.upcoming,
            overdue = uiState.overdue,

            stats = uiState.stats,
            history = uiState.history,

            customerJourneyCards = uiState.customerJourneyCards,
            shouldShowAccountSpecificColorInTransactions = uiState.shouldShowAccountSpecificColorInTransactions,

            onPayOrGet = forward<Transaction>() then2 {
                HomeEvent.PayOrGetPlanned(it)
            } then2 onEvent,
            onDismiss = forward<CustomerJourneyCardModel>() then2 {
                HomeEvent.DismissCustomerJourneyCard(it)
            } then2 onEvent,
            onSkipTransaction = forward<Transaction>() then2 {
                HomeEvent.SkipPlanned(it)
            } then2 onEvent,
            setUpcomingExpanded = forward<Boolean>() then2 {
                HomeEvent.SetUpcomingExpanded(it)
            } then2 onEvent,
            setOverdueExpanded = forward<Boolean>() then2 {
                HomeEvent.SetOverdueExpanded(it)
            } then2 onEvent,
            onSkipAllTransactions = {
                skipAllModalVisible = true
            }
        )
    }

    ChoosePeriodModal(
        modal = choosePeriodModal,
        updateGlobalPeriod = false,
        validatePeriod = { selected ->
            runCatching { homePeriodFromLegacy(selected, homeTimeProvider.localDateNow(),
                homeTimeConverter).resolve(homeTimeProvider.localDateNow(), ivyContext.startDayOfMonth, null) }
                .exceptionOrNull()?.let { "Choose a period that starts today or earlier." }
        },
        dismiss = {
            choosePeriodModal = null
        },
        onPeriodSelected = forward<TimePeriod>() then2 {
            HomeEvent.SetPeriod(it)
        } then2 onEvent
    )

    DeleteModal(
        visible = skipAllModalVisible,
        title = stringResource(R.string.confirm_skip_all),
        description = stringResource(R.string.confirm_skip_all_description),
        dismiss = {
            skipAllModalVisible = false
        }
    ) {
        onEvent(HomeEvent.SkipAllPlanned(uiState.overdue.trns))
        skipAllModalVisible = false
    }

    if (uiState.dashboardEditorVisible) {
        HomeDashboardEditorDialog(
            visibleCards = uiState.visibleDashboardCards,
            onCardToggle = { onEvent(HomeEvent.ToggleDashboardCard(it)) },
            onClose = { onEvent(HomeEvent.CloseDashboardEditor) },
            onRestoreDefaults = { onEvent(HomeEvent.RestoreDashboardDefaults) },
        )
    }
}

@Suppress("LongParameterList")
@ExperimentalAnimationApi
@Composable
fun HomeLazyColumn(
    hideBalance: Boolean,
    hideIncome: Boolean,
    onSetExpand: (Boolean) -> Unit,
    listState: LazyListState,
    period: TimePeriod,

    baseData: AppBaseData,
    shouldShowAccountSpecificColorInTransactions: Boolean,

    upcoming: LegacyDueSection,
    overdue: LegacyDueSection,
    balance: BigDecimal,
    protectedBuffer: BigDecimal,
    visibleDashboardCards: Set<HomeDashboardCardId>,
    spendingBreakdown: HomeSpendingBreakdownState,
    moneyComingUp: MoneyComingUpState,
    trend: HomeTrendState,
    savedAnalytics: ImmutableList<com.ivy.data.db.entity.SavedAnalyticsEntity>,
    onTrendRangeSelected: (HomeTrendRange) -> Unit,
    onTrendMetricToggle: (HomeTrendMetric) -> Unit,
    onOpenTrendEditor: () -> Unit,
    onCloseTrendEditor: () -> Unit,
    stats: IncomeExpensePair,
    history: ImmutableList<TransactionHistoryItem>,

    customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,

    setUpcomingExpanded: (Boolean) -> Unit,
    setOverdueExpanded: (Boolean) -> Unit,

    onBalanceClick: () -> Unit,
    onOpenDashboardEditor: () -> Unit,
    onOpenSavedReports: () -> Unit,

    onPayOrGet: (Transaction) -> Unit,
    onDismiss: (CustomerJourneyCardModel) -> Unit,
    onHiddenBalanceClick: () -> Unit,
    onHiddenIncomeClick: () -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    modifier: Modifier = Modifier,
    name: String = "",
    periodSelection: HomePeriodSelection = HomePeriodSelection.CurrentCycle,
    appliedPeriodSelection: HomePeriodSelection = HomePeriodSelection.CurrentCycle,
    periodError: String? = null,
    periodCapped: Boolean = false,
    onHomePeriodSelected: (HomePeriodSelection) -> Unit = {},
    onOpenPeriodChooser: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_lazy_column"),
        state = listState,
        contentPadding = PaddingValues(bottom = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 224.dp else 176.dp),
    ) {
        item(key = "current_position", contentType = "header") {
            HomeCurrentHeader(name, baseData.baseCurrency, balance, hideBalance,
                if (hideBalance) onHiddenBalanceClick else onBalanceClick)
            Spacer(Modifier.height(16.dp))
        }
        item(key = "home_period", contentType = "controls") {
            HomeTimelineControls(periodSelection, appliedPeriodSelection, trend.window, trend.loading,
                periodError, periodCapped, onOpenPeriodChooser, onHomePeriodSelected,
                { onHomePeriodSelected(periodSelection) })
            Spacer(Modifier.height(16.dp))
            HomePeriodSummary(stats, baseData.baseCurrency, hideBalance || hideIncome, trend.window,
                if (hideBalance) onHiddenBalanceClick else onHiddenIncomeClick)
            Spacer(Modifier.height(16.dp))
        }
        item(key = "dashboard_customize", contentType = "toolbar") {
            HomeDashboardToolbar(onOpenEditor = onOpenDashboardEditor)
        }

        if (HomeDashboardCardId.MONEY_TRENDS in visibleDashboardCards) item(key = "trend", contentType = "chart") {
            Spacer(Modifier.height(16.dp))

            HomeTrendCard(
                state = trend,
                currency = baseData.baseCurrency,
                hideBalance = hideBalance,
                onRangeSelected = onTrendRangeSelected,
                onMetricToggle = onTrendMetricToggle,
                onOpenEditor = onOpenTrendEditor,
                onCloseEditor = onCloseTrendEditor,
            )
        }

        if (HomeDashboardCardId.SPENDING_BREAKDOWN in visibleDashboardCards) item(key = "spending", contentType = "breakdown") {
            Spacer(Modifier.height(16.dp))
            HomeSpendingBreakdownCard(
                state = spendingBreakdown,
                dateWindow = trend.window,
                currency = baseData.baseCurrency,
                hideBalance = hideBalance,
            )
        }

        item(key = "journey", contentType = "journey") {
            CustomerJourney(
                customerJourneyCards = customerJourneyCards,
                onDismiss = onDismiss
            )
        }

        item(key = "activity", contentType = "activity") {
            Spacer(Modifier.height(16.dp))
            HomeCompactActivity(
                baseData = baseData,
                upcoming = upcoming,
                overdue = overdue,
                history = history,
                dateWindow = trend.window,
                showPlanned = false,
                hideBalance = hideBalance,
                onPayOrGet = onPayOrGet,
                onSkipTransaction = onSkipTransaction,
                onSkipAllTransactions = onSkipAllTransactions,
            )
        }
        if (HomeDashboardCardId.SAFE_TO_SPEND in visibleDashboardCards || HomeDashboardCardId.MONEY_COMING_UP in visibleDashboardCards ||
            upcoming.trns.isNotEmpty() || overdue.trns.isNotEmpty()) item(key = "outlook", contentType = "outlook") {
            Spacer(Modifier.height(24.dp))
            androidx.compose.material3.Text(stringResource(R.string.jvy_home_today_upcoming),
                Modifier.padding(horizontal = 24.dp), style = com.ivy.design.system.JvyTypography.SectionTitle)
            if (HomeDashboardCardId.SAFE_TO_SPEND in visibleDashboardCards) {
                Spacer(Modifier.height(16.dp))
                SafeToSpendCard(balance, protectedBuffer, baseData.baseCurrency)
            }
            if (HomeDashboardCardId.MONEY_COMING_UP in visibleDashboardCards) {
                Spacer(Modifier.height(16.dp))
                MoneyComingUpCard(moneyComingUp, baseData.baseCurrency, hideBalance)
            }
            HomeCompactActivity(baseData, upcoming, overdue, emptyList(), hideBalance, onPayOrGet,
                onSkipTransaction, onSkipAllTransactions, showRecent = false)
        }
        if (HomeDashboardCardId.SAVED_REPORTS in visibleDashboardCards && savedAnalytics.isNotEmpty())
            item(key = "saved_reports", contentType = "reports") {
                Spacer(Modifier.height(16.dp))
                HomeSavedReportsCard(savedAnalytics, onOpenSavedReports)
            }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BoxWithConstraintsScope.PreviewHomeTab(isDark: Boolean = false) {
    IvyPreview(isDark) {
        HomeUi(
            uiState = HomeState(
                theme = Theme.AUTO,
                name = "",
                baseData = AppBaseData(
                    baseCurrency = "",
                    accounts = persistentListOf(),
                    categories = persistentListOf()
                ),
                balance = BigDecimal.ZERO,
                buffer = BufferInfo(
                    amount = BigDecimal.ZERO,
                    bufferDiff = BigDecimal.ZERO,
                ),
                customerJourneyCards = persistentListOf(),
                history = persistentListOf(),
                stats = IncomeExpensePair.zero(),
                upcoming = LegacyDueSection(
                    trns = persistentListOf(),
                    stats = IncomeExpensePair.zero(),
                    expanded = false,
                ),
                overdue = LegacyDueSection(
                    trns = persistentListOf(),
                    stats = IncomeExpensePair.zero(),
                    expanded = false,
                ),
                period = TimePeriod(month = Month.monthsList().first(), year = 2023),
                hideBalance = false,
                hideIncome = false,
                expanded = false,
                shouldShowAccountSpecificColorInTransactions = false,
                moneyComingUp = MoneyComingUpState(
                    status = MoneyComingUpStatus.CONTENT,
                    currentAvailable = BigDecimal("1200.00"),
                    upcomingIncome = BigDecimal("1500.00"),
                    upcomingExpenses = BigDecimal("800.00"),
                    lowestProjectedAvailable = BigDecimal("900.00"),
                    safeToSpend = BigDecimal("900.00"),
                    events = persistentListOf(
                        MoneyComingUpEvent(
                            date = java.time.LocalDate.of(2023, 1, 15),
                            type = PlannedCashflowType.INCOME,
                            amount = BigDecimal("1500.00"),
                        ),
                        MoneyComingUpEvent(
                            date = java.time.LocalDate.of(2023, 1, 20),
                            type = PlannedCashflowType.EXPENSE,
                            amount = BigDecimal("800.00"),
                        ),
                    ),
                )
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    IvyWalletPreview(theme) {
        PreviewHomeTab(isDark)
    }
}
