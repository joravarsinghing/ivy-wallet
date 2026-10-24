package com.ivy.main

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.accounts.AccountsTab
import com.ivy.base.model.TransactionType
import com.ivy.home.HomeTab
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.onScreenStart
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.navigation
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.MainScreen(screen: MainScreen) {
    val viewModel: MainViewModel = viewModel()

    val currency by viewModel.currency.observeAsState("")

    onScreenStart {
        viewModel.start(screen)
    }

    val ivyContext = ivyWalletCtx()
    UI(
        screen = screen,
        tab = ivyContext.mainTab,
        baseCurrency = currency,
        selectTab = viewModel::selectTab,
        onCreateAccount = viewModel::createAccount
    )
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BoxWithConstraintsScope.UI(
    screen: MainScreen,
    tab: MainTab,

    baseCurrency: String,

    selectTab: (MainTab) -> Unit,
    onCreateAccount: (CreateAccountData) -> Unit,
) {
    val savedTabs = rememberSaveableStateHolder()
    val rail = mainNavigationRailWidth(maxWidth, LocalDensity.current.fontScale)
    // Reserve real space for the rail. The previous overlaid rail obscured content
    // on folding/desktop windows rather than participating in layout.
    MainContentWindow(rail) {
        savedTabs.SaveableStateProvider(tab.name) {
            when (tab) {
                MainTab.HOME -> HomeTab()
                MainTab.ACCOUNTS -> AccountsTab()
                MainTab.PLAN -> MainToolsHub(planning = true)
                MainTab.NET_WORTH -> com.ivy.networth.NetWorthTab()
                MainTab.MORE -> MainToolsHub(planning = false)
            }
        }
    }

    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }

    val nav = navigation()
    BottomBar(
        tab = tab,
        selectTab = selectTab,

        onAddIncome = {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.INCOME
                )
            )
        },
        onAddExpense = {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.EXPENSE
                )
            )
        },
        onAddTransfer = {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.TRANSFER
                )
            )
        },
        onAddPlannedPayment = {
            nav.navigateTo(
                EditPlannedScreen(
                    type = TransactionType.EXPENSE,
                    plannedPaymentRuleId = null
                )
            )
        },

        showAddAccountModal = {
            accountModalData = AccountModalData(
                account = null,
                balance = 0.0,
                baseCurrency = baseCurrency
            )
        }
    )

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = { _, _ -> },
        dismiss = {
            accountModalData = null
        }
    )
}

/** One production content boundary, also exercised with real feature content in shell tests. */
@Composable
internal fun MainContentWindow(railWidth: Dp, content: @Composable BoxWithConstraintsScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = railWidth), content = content)
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun PreviewMainScreen() {
    IvyWalletPreview {
        UI(
            screen = MainScreen,
            tab = MainTab.HOME,
            baseCurrency = "BGN",
            selectTab = {},
            onCreateAccount = { }
        )
    }
}
