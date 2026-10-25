package com.ivy.loans.loan

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import com.ivy.design.system.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.data.model.LoanType
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.humanReadableType
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.getDefaultFIATCurrency
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.loans.loan.Constants.SWIPE_HORIZONTAL_THRESHOLD
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.navigation.LoanDetailsScreen
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.ui.theme.Blue
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.LoanModal
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.LocalDateTime

@Composable
fun BoxWithConstraintsScope.LoansScreen(screen: LoansScreen) {
    val viewModel: LoanViewModel = viewModel()
    val state = viewModel.uiState()
    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: LoanScreenState,
    onEventHandler: (LoanScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    val scrollState = rememberScrollPositionListState(key = "loans_lazy_column",
        initialFirstVisibleItemIndex = ivyWalletCtx().loanListState?.firstVisibleItemIndex ?: 0,
        initialFirstVisibleItemScrollOffset = ivyWalletCtx().loanListState?.firstVisibleItemScrollOffset ?: 0)
    LoanPlanningContent(state, onEventHandler, { nav.back() }, scrollState) { item ->
        nav.navigateTo(LoanDetailsScreen(loanId = item.loan.id))
    }

    ReorderModalSingleType(
        visible = state.reorderModalVisible,
        initialItems = if (state.selectedTab == LoanTab.PENDING) state.pendingLoans else state.completedLoans,
        dismiss = { onEventHandler(LoanScreenEvent.OnReOrderModalShow(false)) },
        onReordered = { onEventHandler(LoanScreenEvent.OnReordered(it)) }
    ) { _, item -> Text(item.loan.name, Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall) }

    if (state.loanModalData != null) {
        LoanModal(accounts = state.accounts,
            onCreateAccount = { onEventHandler(LoanScreenEvent.OnCreateAccount(it)) }, modal = state.loanModalData,
            onCreateLoan = { onEventHandler(LoanScreenEvent.OnLoanCreate(it)) }, onEditLoan = { _, _ -> },
            dismiss = { onEventHandler(LoanScreenEvent.OnLoanModalDismiss) }, dateTime = state.dateTime,
            onSetDate = { onEventHandler(LoanScreenEvent.OnChangeDate) }, onSetTime = { onEventHandler(LoanScreenEvent.OnChangeTime) })
    }
}

@Composable
fun LoanPlanningContent(state: LoanScreenState, onEvent: (LoanScreenEvent) -> Unit = {},
                        onBack: () -> Unit = {}, listState: LazyListState = rememberLazyListState(),
                        onLoanClick: (DisplayLoan) -> Unit = {}) {
    val loans = if (state.selectedTab == LoanTab.PENDING) state.pendingLoans else state.completedLoans
    val add = { onEvent(LoanScreenEvent.OnAddLoan) }
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
    Column(
        modifier = Modifier
            .widthIn(max = 960.dp).fillMaxSize().align(Alignment.TopCenter)
            .horizontalSwipeListener(
                sensitivity = SWIPE_HORIZONTAL_THRESHOLD,
                state = rememberSwipeListenerState(),
                onSwipeLeft = {
                    onEvent(LoanScreenEvent.OnTabChanged(LoanTab.COMPLETED))
                },
                onSwipeRight = {
                    onEvent(LoanScreenEvent.OnTabChanged(LoanTab.PENDING))
                }
            )
    ) {
        JvyScreenHeader(stringResource(R.string.loans), onBack) {
            if (loans.isNotEmpty()) IconButton(onClick = { onEvent(LoanScreenEvent.OnReOrderModalShow(true)) }) {
                Icon(Icons.Outlined.Sort, stringResource(R.string.reorder))
            }
        }
        LoanTabControls(state.selectedTab) { onEvent(LoanScreenEvent.OnTabChanged(it)) }
        LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.totalOweAmount.isNotEmpty() || state.totalOwedAmount.isNotEmpty()) item {
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.totalOweAmount.isNotEmpty()) Text(stringResource(R.string.you_owe, state.totalOweAmount), style = JvyTypography.Supporting)
                    if (state.totalOwedAmount.isNotEmpty()) Text(stringResource(R.string.you_are_owed, state.totalOwedAmount), style = JvyTypography.Supporting)
                }
            }
            if (loans.isEmpty()) item {
                JvyEmptyState(if (state.selectedTab == LoanTab.PENDING) stringResource(R.string.no_loans) else "No completed loans yet",
                    "Keep track of money you borrowed or lent, and record repayments in one place.",
                    Icons.Outlined.ReceiptLong, stringResource(R.string.add_loan), add)
            }
            items(loans, key = { it.loan.id }) { item -> LoanItem(item) { onLoanClick(item) } }
        }
        if (loans.isNotEmpty()) JvyPrimaryAction(stringResource(R.string.add_loan), add)
    }
    }
}

@Composable
private fun LoanItem(displayLoan: DisplayLoan, onClick: () -> Unit) {
    val loan = displayLoan.loan
    val currency = displayLoan.currencyCode ?: getDefaultFIATCurrency().currencyCode
    val remaining = displayLoan.loanTotalAmount - displayLoan.amountPaid
    OutlinedCard(onClick, Modifier.padding(horizontal = 24.dp).fillMaxWidth().testTag("loan_item"), shape = JvyShapes.Card) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = JvyShapes.Control, color = loan.color.toComposeColor()) {
                    ItemIconSDefaultIcon(modifier = Modifier.padding(8.dp), iconName = loan.icon,
                        defaultIcon = R.drawable.ic_custom_loan_s, tint = findContrastTextColor(loan.color.toComposeColor()))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) { Text(loan.name, style = MaterialTheme.typography.titleMedium)
                    Text(loan.humanReadableType(), style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(com.ivy.ui.money.formatDecimalMoney(java.math.BigDecimal.valueOf(remaining), currency), style = JvyTypography.Amount)
            Text(displayLoan.formattedDisplayText, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(displayLoan.percentPaid.toFloat().coerceIn(0f, 1f), Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 20, 0, 35)

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    val state = LoanScreenState(
        baseCurrency = "BGN",
        selectedTab = LoanTab.PENDING,
        completedLoans = persistentListOf(
            DisplayLoan(
                loan = Loan(
                    name = "Loan 3",
                    icon = "bank",
                    color = Blue.toArgb(),
                    amount = 7000.0,
                    type = LoanType.LEND,
                    dateTime = testDateTime
                ),
                loanTotalAmount = 7000.0,
                amountPaid = 8000.0,
                percentPaid = 0.8
            ),
        ),
        pendingLoans = persistentListOf(
            DisplayLoan(
                loan = Loan(
                    name = "Loan 3",
                    icon = "bank",
                    color = Blue.toArgb(),
                    amount = 7000.0,
                    type = LoanType.LEND,
                    dateTime = testDateTime
                ),
                loanTotalAmount = 7000.0,
                amountPaid = 8000.0,
                percentPaid = 0.8
            ),
        ),
        accounts = persistentListOf(),
        totalOweAmount = "1000.00 INR",
        totalOwedAmount = "1500.0 INR",
        loanModalData = null,
        reorderModalVisible = false,
        selectedAccount = null,
        paidOffLoanVisibility = true,
        dateTime = Instant.now()
    )
    IvyWalletPreview(theme) {
        UI(
            state = state
        ) {}
    }
}

/** For screenshot testing */
@Composable
fun LoanScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}
