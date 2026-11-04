package com.ivy.accounts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.system.*
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.money.formatDecimalMoney
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.util.UUID

@Composable
fun BoxWithConstraintsScope.AccountsTab(modifier: Modifier = Modifier) {
    val vm: AccountsViewModel = screenScopedViewModel()
    val state = vm.uiState()
    val nav = navigation()
    val context = ivyWalletCtx()
    val scroll = rememberScrollPositionListState(key = "accounts_lazy_column",
        initialFirstVisibleItemIndex = context.accountsListState?.firstVisibleItemIndex ?: 0,
        initialFirstVisibleItemScrollOffset = context.accountsListState?.firstVisibleItemScrollOffset ?: 0)
    AccountsContent(state, vm::onEvent, { id -> nav.navigateTo(TransactionsScreen(accountId = id, categoryId = null)) }, scroll,
        modifier = modifier.horizontalSwipeListener(sensitivity = 200, state = rememberSwipeListenerState(),
            onSwipeLeft = { context.selectMainTab(MainTab.HOME) }, onSwipeRight = { context.selectMainTab(MainTab.HOME) }))
}

/** Account management only. Wealth calculations belong to the Net worth feature. */
@Composable
@androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
// Keep the positional account-content adapter stable while callers migrate.
@Suppress("ComposeParameterOrder")
fun BoxWithConstraintsScope.AccountsContent(
    state: AccountsState, onEvent: (AccountsEvent) -> Unit = {}, openAccount: (UUID) -> Unit = {},
    scroll: LazyListState = rememberLazyListState(), modifier: Modifier = Modifier,
) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    val columns = if (maxWidth >= 840.dp && !largeText) 2 else 1
    LazyColumn(modifier.widthIn(max = 1200.dp).fillMaxSize().align(Alignment.TopCenter).systemBarsPadding(), state = scroll,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = if (largeText) 224.dp else 176.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item(key = "heading", contentType = "heading") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.accounts), style = MaterialTheme.typography.headlineMedium)
                        Text("Your everyday money, in one place", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onEvent(AccountsEvent.OnReorderModalVisible(true)) }, enabled = state.accountsData.size > 1) {
                        Icon(Icons.Outlined.SwapVert, "Reorder accounts")
                    }
                }
            }
            if (!state.hideTotalBalance) item(key = "totals", contentType = "totals") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Wallet balance", style = MaterialTheme.typography.titleSmall)
                        Text(com.ivy.design.system.financialText(accountMoney(state.totalBalanceWithoutExcluded.toBigDecimalOrNull(), state.baseCurrency, state.hideBalances)), style = JvyTypography.Amount)
                        Text("Accounts included in your wallet total. This is not your net worth.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Including excluded accounts", style = MaterialTheme.typography.labelLarge)
                        Text(com.ivy.design.system.financialText(accountMoney(state.totalBalanceWithExcluded.toBigDecimalOrNull(), state.baseCurrency, state.hideBalances)), style = JvyTypography.AmountSmall)
                    }
                }
            }
            if (state.accountsData.isEmpty()) item(key = "empty", contentType = "empty") {
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("A home for your everyday money", style = MaterialTheme.typography.titleMedium)
                        Text("Add a bank account, cash wallet or card using +. You can keep the balance up to date with your own entries.")
                    }
                }
            }
            items(state.accountsData.chunked(columns), key = { it.first().account.id.value.toString() }, contentType = { "account-row" }) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { data ->
                        AccountCardContent(data, state.baseCurrency, state.compactAccountsModeEnabled, state.hideBalances,
                            { openAccount(data.account.id.value) }, Modifier.weight(1f))
                    }
                    if (columns == 2 && row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    ReorderModalSingleType(visible = state.reorderVisible, initialItems = state.accountsData,
        dismiss = { onEvent(AccountsEvent.OnReorderModalVisible(false)) },
        onReordered = { onEvent(AccountsEvent.OnReorder(it)) }) { _, item ->
        Text(item.account.name.value, Modifier.fillMaxWidth().padding(end = 24.dp, top = 8.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun AccountCardContent(data: AccountData, baseCurrency: String, compact: Boolean, hidden: Boolean,
                               onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember(data.account.id) { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Compose's duration scale respects Android's reduced/disabled animation setting.
    val scale = animateFloatAsState(if (pressed) JvyMotion.PressedScale else 1f, tween(JvyMotion.FeedbackMillis), label = "Account press")
    val accent = accountAccent(data.account.color.value.toComposeColor(), MaterialTheme.colorScheme.surface)
    val currency = data.account.asset.code
    OutlinedCard(onClick = onClick, interactionSource = interaction, shape = JvyShapes.Card,
        modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }.testTag("account_${data.account.id.value}"),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemIconSDefaultIcon(iconName = data.account.icon?.id, defaultIcon = R.drawable.ic_custom_account_s, tint = accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(data.account.name.value, style = MaterialTheme.typography.titleMedium)
                    Text(if (data.account.includeInBalance) "$currency · Included in wallet" else "$currency · Excluded from wallet", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(com.ivy.design.system.financialText(accountMoney(BigDecimal.valueOf(data.balance), currency, hidden)), style = JvyTypography.Amount)
                if (currency != baseCurrency && data.balanceBaseCurrency != null) Text(
                    com.ivy.design.system.financialText(accountMoney(BigDecimal.valueOf(data.balanceBaseCurrency!!), baseCurrency, hidden)), Modifier.testTag("baseCurrencyEquivalent"),
                    style = JvyTypography.AmountSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!compact) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AccountMetricPair(stringResource(R.string.month_income), data.monthlyIncome, stringResource(R.string.month_expenses), data.monthlyExpenses, currency, hidden, JvyFinanceColors.asset(), JvyFinanceColors.debt())
                AccountMetricPair(stringResource(R.string.reserved_for_goals), data.reservedForGoals, "Available", (data.balance - data.reservedForGoals).coerceAtLeast(0.0), currency, hidden)
            }
        }
    }
}

@Composable
private fun AccountMetricPair(left: String, leftAmount: Double, right: String, rightAmount: Double, currency: String, hidden: Boolean,
                              leftColor: Color = MaterialTheme.colorScheme.onSurface, rightColor: Color = MaterialTheme.colorScheme.onSurface) {
    val large = LocalDensity.current.fontScale > 1.3f
    if (large) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccountMetric(left, leftAmount, currency, hidden, leftColor)
        AccountMetric(right, rightAmount, currency, hidden, rightColor)
    } else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // A translated/wrapped label must not push just one comparable amount down.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(left, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(right, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(com.ivy.design.system.financialText(accountMoney(BigDecimal.valueOf(leftAmount), currency, hidden)), Modifier.weight(1f), style = JvyTypography.AmountSmall, color = leftColor)
            Text(com.ivy.design.system.financialText(accountMoney(BigDecimal.valueOf(rightAmount), currency, hidden)), Modifier.weight(1f), style = JvyTypography.AmountSmall, color = rightColor)
        }
    }
}

@Composable
private fun AccountMetric(label: String, amount: Double, currency: String, hidden: Boolean, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(com.ivy.design.system.financialText(accountMoney(BigDecimal.valueOf(amount), currency, hidden)), style = JvyTypography.AmountSmall, color = color)
    }
}

internal fun accountMoney(amount: BigDecimal?, currency: String, hidden: Boolean): String = when {
    hidden -> "••••"
    amount == null -> "Balance unavailable"
    else -> formatDecimalMoney(amount, currency)
}

/** Preserve the chosen hue while keeping the small identifying glyph visible in either theme. */
internal fun accountAccent(chosen: Color, surface: Color): Color {
    val opaque = chosen.compositeOver(surface)
    fun contrast(ink: Color): Float = (maxOf(ink.luminance(), surface.luminance()) + .05f) /
        (minOf(ink.luminance(), surface.luminance()) + .05f)
    if (contrast(opaque) >= 3f) return opaque
    val endpoint = if (surface.luminance() < .179f) Color.White else Color.Black
    return (1..10).asSequence().map { lerp(opaque, endpoint, it / 10f) }.first { contrast(it) >= 3f }
}

@Composable
private fun AccountsPreview(dark: Boolean, compact: Boolean) {
    IvyWalletPreview(if (dark) Theme.DARK else Theme.LIGHT) { AccountsContent(accountPreviewState(compact)) }
}

@androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
fun accountPreviewState(compact: Boolean = false): AccountsState {
    fun item(id: Long, name: String, balance: Double, color: Color) = AccountData(
        Account(id = AccountId(UUID(0, id)), name = NotBlankTrimmedString.unsafe(name), color = ColorInt(color.toArgb()),
            asset = AssetCode.unsafe("AED"), icon = null, includeInBalance = true, orderNum = id.toDouble()),
        balance, null, 920.0, 3045.0, 250.0)
    return AccountsState("AED", persistentListOf(item(1, "Everyday banking", 12125.21, Color(0xFF176C50)), item(2, "Cash wallet", 820.0, Color(0xFF5C3DF5))),
        "12945.21", "", "12945.21", "", false, compact, false)
}

/** Kept for the existing preview/screenshot entrypoints. */
@Composable fun AccountsTabNonCompactUITest(dark: Boolean) = AccountsPreview(dark, false)
@Composable fun AccountsTabCompactUITest(dark: Boolean) = AccountsPreview(dark, true)
