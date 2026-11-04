package com.ivy.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.budgets.model.DisplayBudget
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.system.*
import com.ivy.legacy.utils.format
import com.ivy.navigation.BudgetScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType

@Composable
fun BoxWithConstraintsScope.BudgetScreen(screen: BudgetScreen) {
    val viewModel: BudgetViewModel = screenScopedViewModel()
    val nav = navigation()
    val state = viewModel.uiState()
    val resolved = com.ivy.legacy.ui.restoreEditorDescriptor("budget-editor", state.timeRange != null, viewModel::saveModal, viewModel::restoreModal)
    if (!resolved) return
    BudgetScreenContent(state, onEvent = viewModel::onEvent, onBack = { nav.back() })
}

@Composable
fun BudgetScreenContent(state: BudgetScreenState, modifier: Modifier = Modifier, onEvent: (BudgetScreenEvent) -> Unit = {}, onBack: () -> Unit = {}) {
    val add = {
        onEvent(BudgetScreenEvent.OnBudgetModalData(BudgetModalData(null, state.baseCurrency, state.categories, state.accounts)))
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(Modifier.widthIn(max = 960.dp).fillMaxSize().systemBarsPadding().align(Alignment.TopCenter)) {
            JvyScreenHeader(stringResource(R.string.budgets), onBack,
                state.timeRange?.toDisplay(LocalTimeFormatter.current)) {
                if (state.budgets.isNotEmpty()) IconButton(onClick = { onEvent(BudgetScreenEvent.OnReorderModalVisible(true)) }) {
                    Icon(Icons.Outlined.Sort, stringResource(R.string.reorder))
                }
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(if (LocalDensity.current.fontScale > 1.3f) 960.dp else 360.dp), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = if (state.budgets.isEmpty()) 0.dp else 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.budgets.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    JvyEmptyState(stringResource(R.string.no_budgets), stringResource(R.string.no_budgets_text),
                        Icons.Outlined.AccountBalanceWallet, actionLabel = stringResource(R.string.add_budget), onAction = add)
                } else {
                    if (state.categoryBudgetsTotal > 0 || state.appBudgetMax > 0) item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (state.categoryBudgetsTotal > 0) Text(financialText(stringResource(R.string.for_categories, state.categoryBudgetsTotal.format(state.baseCurrency), state.baseCurrency)),
                                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.appBudgetMax > 0) Text(financialText(stringResource(R.string.app_budget, state.appBudgetMax.format(state.baseCurrency), state.baseCurrency)),
                                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.appBudgetMax > 0 && state.categoryBudgetsTotal > 0) state.totalRemainingBudgetText?.let {
                                Text(financialText("Categories · $it"), style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) { BudgetInsightCard(state.insight, state.baseCurrency, overlappingLimits = state.appBudgetMax > 0 && state.categoryBudgetsTotal > 0) }
                    items(state.budgets, key = { it.budget.id }) { item ->
                        BudgetItem(item, state.baseCurrency) {
                            onEvent(BudgetScreenEvent.OnBudgetModalData(BudgetModalData(item.budget, state.baseCurrency,
                                state.categories, state.accounts, autoFocusKeyboard = false)))
                        }
                    }
                }
            }
            if (state.budgets.isNotEmpty()) JvyPrimaryAction(stringResource(R.string.add_budget), add)
        }
        ReorderModalSingleType(visible = state.reorderModalVisible, initialItems = state.budgets,
            dismiss = { onEvent(BudgetScreenEvent.OnReorderModalVisible(false)) },
            onReordered = { onEvent(BudgetScreenEvent.OnReorder(it)) }) { _, item ->
            Text(item.budget.name, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.titleSmall)
        }
        BudgetModal(state.budgetModalData, onCreate = { onEvent(BudgetScreenEvent.OnCreateBudget(it)) },
            onEdit = { onEvent(BudgetScreenEvent.OnEditBudget(it)) }, onDelete = { onEvent(BudgetScreenEvent.OnDeleteBudget(it)) },
            saving = state.saving, saveError = state.saveError,
            dismiss = { onEvent(BudgetScreenEvent.OnBudgetModalData(null)) })
    }
}

@Composable
private fun BudgetItem(item: DisplayBudget, currency: String, onClick: () -> Unit) {
    val remaining = item.budget.amount - item.spentAmount
    val exceeded = remaining < 0
    val progress = if (item.budget.amount > 0) (item.spentAmount / item.budget.amount).toFloat().coerceIn(0f, 1f)
        else if (item.spentAmount > 0) 1f else 0f
    val accent = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    OutlinedCard(onClick, Modifier.fillMaxWidth(), shape = JvyShapes.Card) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.budget.name, style = MaterialTheme.typography.titleMedium)
            Text(determineBudgetType(item.budget.parseCategoryIds().size), style = JvyTypography.Supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(if (exceeded) R.string.budget_exceeded_by else R.string.left_to_spend), style = JvyTypography.Supporting)
            Text(financialText("${kotlin.math.abs(remaining).format(currency)} $currency"), style = JvyTypography.Amount, color = accent)
            if (!LocalFinancialVisibility.current.hidden) LinearProgressIndicator(progress, Modifier.fillMaxWidth().height(6.dp), color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Text(financialText("${item.spentAmount.format(currency)} / ${item.budget.amount.format(currency)} $currency"),
                style = JvyTypography.AmountSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
