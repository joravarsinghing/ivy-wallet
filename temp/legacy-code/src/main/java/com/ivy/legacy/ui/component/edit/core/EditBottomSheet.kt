package com.ivy.wallet.ui.edit.core

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.legacy.datamodel.Account
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.addKeyboardListener
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.consumeClicks
import com.ivy.legacy.utils.densityScope
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.keyboardOnlyWindowInsets
import com.ivy.legacy.utils.lerp
import com.ivy.legacy.utils.navigationBarInsets
import com.ivy.legacy.utils.onScreenStart
import com.ivy.legacy.utils.springBounce
import com.ivy.design.utils.thenIf
import com.ivy.legacy.utils.verticalSwipeListener
import com.ivy.base.model.TransactionType
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.GreenDark
import com.ivy.wallet.ui.theme.GreenLight
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.IvyDark
import com.ivy.wallet.ui.theme.components.ActionsRow
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.CircleButton
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.DURATION_MODAL_ANIM
import com.ivy.wallet.ui.theme.modal.ModalSave
import com.ivy.wallet.ui.theme.modal.ModalSet
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@Deprecated("Old design system. Use `:ivy-design` and Material3")
const val SWIPE_UP_EXPANDED_THRESHOLD = 200

@Suppress("LongMethod", "LongParameterList", "UnusedParameter", "ParameterNaming")
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun BoxWithConstraintsScope.EditBottomSheet(
    initialTransactionId: UUID?,
    type: TransactionType,
    accounts: List<Account>,
    selectedAccount: Account?,
    toAccount: Account?,
    amount: Double,
    currency: String,
    amountModalShown: Boolean,
    setAmountModalShown: (Boolean) -> Unit,
    ActionButton: @Composable () -> Unit,
    onAmountChanged: (Double) -> Unit,
    onSelectedAccountChanged: (Account) -> Unit,
    onToAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
    modifier: Modifier = Modifier, // Modifier comes after other parameters
    convertedAmount: Double? = null,
    convertedAmountCurrencyCode: String? = null,
    Header: @Composable () -> Unit = {},
    FormContent: @Composable ColumnScope.() -> Unit = {},
    formScrollState: ScrollState = rememberScrollState(),
    saveError: String? = null,
) {
    val label = when (type) {
        TransactionType.INCOME -> stringResource(R.string.add_money_to)
        TransactionType.EXPENSE -> stringResource(R.string.pay_with)
        TransactionType.TRANSFER -> stringResource(R.string.from)
    }
    Box(modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            Header()
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(formScrollState)) {
                Text(label, Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = com.ivy.design.system.JvyTypography.SectionTitle)
                AccountsRow(accounts, selectedAccount, onSelectedAccountChanged,
                    childrenTestTag = "from_account", onAddNewAccount = onAddNewAccount)
                if (type == TransactionType.TRANSFER) {
                    Text(stringResource(R.string.to), Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = com.ivy.design.system.JvyTypography.SectionTitle)
                    AccountsRow(accounts, toAccount, onToAccountChanged,
                        childrenTestTag = "to_account", onAddNewAccount = onAddNewAccount)
                }
                com.ivy.design.system.JvyOutlinedButton({ setAmountModalShown(true) },
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(if (amount.isFinite()) com.ivy.ui.money.formatDecimalMoney(amount.toBigDecimal(), currency) else "Enter a valid amount.",
                        style = com.ivy.design.system.JvyTypography.Amount)
                }
                if (type == TransactionType.TRANSFER && convertedAmount != null && convertedAmount.isFinite() && convertedAmountCurrencyCode != null) {
                    Text(com.ivy.ui.money.formatDecimalMoney(convertedAmount.toBigDecimal(), convertedAmountCurrencyCode),
                        Modifier.padding(horizontal = 24.dp), style = com.ivy.design.system.JvyTypography.Supporting)
                }
                FormContent()
                Spacer(Modifier.height(24.dp))
            }
            androidx.compose.material3.HorizontalDivider()
            saveError?.let { Text(it, Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                style = com.ivy.design.system.JvyTypography.Supporting) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End) { ActionButton() }
        }
    }
    val amountModalId = remember(initialTransactionId) { UUID.randomUUID() }
    AmountModal(id = amountModalId, visible = amountModalShown, currency = currency,
        initialAmount = amount.takeIf { it > 0 },
        Header = {
            Text(stringResource(R.string.account), Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = com.ivy.design.system.JvyTypography.SectionTitle)
            AccountsRow(accounts, selectedAccount, onSelectedAccountChanged,
                childrenTestTag = "amount_modal_account", onAddNewAccount = onAddNewAccount)
        },
        amountSpacerTop = 24.dp, dismiss = { setAmountModalShown(false) },
        onAmountChanged = onAmountChanged)
}
@Composable
@Suppress("ParameterNaming")
private fun AccountsRow(
    accounts: List<Account>,
    selectedAccount: Account?,
    onSelectedAccountChanged: (Account) -> Unit,
    modifier: Modifier = Modifier,
    childrenTestTag: String? = null,
    onAddNewAccount: () -> Unit,
) {
    val selectedIndex = selectedAccountIndex(accounts, selectedAccount?.id)
    val lazyState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex ?: 0)

    LaunchedEffect(selectedAccount?.id, selectedIndex) {
        // Padding is not a list item. The selected account's domain index is
        // therefore also its lazy index, including after list refresh/reorder.
        selectedIndex?.let { lazyState.scrollToItem(it) }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        state = lazyState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(accounts, key = { it.id.toString() }, contentType = { "account" }) { account ->
            Account(
                account = account,
                selected = selectedAccount?.id == account.id,
                testTag = childrenTestTag ?: "account"
            ) {
                onSelectedAccountChanged(account)
            }
        }

        item(key = "add_account", contentType = "action") {
            AddAccount {
                onAddNewAccount()
            }
        }

    }
}

@Composable
private fun Account(
    account: Account,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val accountColor = account.color.toComposeColor()
    val textColor =
        if (selected) findContrastTextColor(accountColor) else UI.colors.pureInverse

    val medium = androidx.compose.material3.MaterialTheme.colorScheme.outline
    val rFull = com.ivy.design.system.JvyShapes.Control

    Row(
        modifier = Modifier
            .widthIn(max = 272.dp)
            .clip(com.ivy.design.system.JvyShapes.Control)
            .border(2.dp, if (selected) textColor else medium, rFull)
            .thenIf(selected) {
                background(accountColor, rFull)
            }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(testTag)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        ItemIconSDefaultIcon(
            iconName = account.icon,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = textColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier.weight(1f, fill = false).padding(vertical = 10.dp),
            text = account.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = UI.typo.b2.style(
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun AddAccount(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(com.ivy.design.system.JvyShapes.Control)
            .border(2.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline,
                com.ivy.design.system.JvyShapes.Control)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIcon(
            icon = R.drawable.ic_plus,
            tint = UI.colors.pureInverse
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = stringResource(R.string.add_account),
            style = UI.typo.b2.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}
@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        val acc1 = Account("Cash", color = Green.toArgb())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            EditBottomSheet(
                amountModalShown = false,
                setAmountModalShown = {},
                initialTransactionId = null,
                type = TransactionType.INCOME,
                ActionButton = {
                    ModalSet {
                    }
                },
                accounts = listOf(
                    acc1,
                    Account("DSK", color = GreenDark.toArgb()),
                    Account("phyre", color = GreenLight.toArgb()),
                    Account("Revolut", color = IvyDark.toArgb()),
                ),
                selectedAccount = acc1,
                toAccount = null,
                amount = 12350.0,
                currency = "BGN",
                onAmountChanged = {},
                onSelectedAccountChanged = {},
                onToAccountChanged = {},
                onAddNewAccount = {}
            )
        }
    }
}

@Preview
@Composable
private fun Preview_Transfer() {
    IvyWalletPreview {
        val acc1 = Account("Cash", color = Green.toArgb())
        val acc2 = Account("DSK", color = GreenDark.toArgb())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            EditBottomSheet(
                amountModalShown = false,
                setAmountModalShown = {},
                initialTransactionId = UUID.randomUUID(),
                ActionButton = {
                    ModalSave {
                    }
                },
                type = TransactionType.TRANSFER,
                accounts = listOf(
                    acc1,
                    acc2,
                    Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
                    Account("Revolut", color = IvyDark.toArgb()),
                ),
                selectedAccount = acc1,
                toAccount = acc2,
                amount = 12350.0,
                currency = "BGN",
                onAmountChanged = {},
                onSelectedAccountChanged = {},
                onToAccountChanged = {},
                onAddNewAccount = {}
            )
        }
    }
}
