package com.ivy.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ivy.design.system.*
import com.ivy.navigation.GoalsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import java.util.Locale

@Composable
fun BoxWithConstraintsScope.GoalsScreen(screen: GoalsScreen) {
    val viewModel: GoalsViewModel = screenScopedViewModel()
    val nav = navigation()
    GoalsScreenContent(viewModel.uiState.collectAsState().value) { event ->
        if (event == GoalsScreenEvent.Back) nav.back() else viewModel.onEvent(event)
    }
}

@Composable
fun BoxWithConstraintsScope.GoalsScreenContent(state: GoalScreenState, onEvent: (GoalsScreenEvent) -> Unit) {
    var goals by remember(state.goals) { mutableStateOf(state.goals) }
    var modal by remember { mutableStateOf<GoalsModal?>(null) }
    val add = { modal = GoalsModal.Add }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.widthIn(max = 960.dp).fillMaxSize().systemBarsPadding().align(Alignment.TopCenter)) {
            JvyScreenHeader("Goals", { onEvent(GoalsScreenEvent.Back) }, "Plan for what matters to you.")
            LazyVerticalGrid(columns = GridCells.Adaptive(if (LocalDensity.current.fontScale > 1.3f) 960.dp else 360.dp), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = if (goals.isEmpty()) 0.dp else 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (goals.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    JvyEmptyState("What would you like to plan for?",
                        "A laptop, a trip, or peace of mind. Set a target, then reserve money from your accounts without creating a transfer.",
                        Icons.Outlined.Flag, "Create your first goal", add)
                } else {
                    item(span = { GridItemSpan(maxLineSpan) }) { ReservedSummary(goals.sumOf { it.savedAmount }, state.baseCurrency, goals.count { it.status == GoalStatus.ACTIVE }) }
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Your goals · ${goals.size}", style = JvyTypography.SectionTitle) }
                    items(goals.size, key = { goals[it].id }) { index ->
                        val goal = goals[index]
                        GoalCard(goal, state.accounts, state.asOfDate) { modal = GoalsModal.Detail(goal) }
                    }
                }
            }
            if (goals.isNotEmpty()) JvyPrimaryAction("Add goal", add)
        }
        GoalsModalHost(modal, state.copy(goals = goals), onDismiss = { modal = null },
            onCreated = { goal ->
                val ready = goal.copy(fundingGuidance = goal.toFundingGuidance(state.asOfDate))
                goals = goals + ready; onEvent(GoalsScreenEvent.GoalCreated(ready)); modal = null
            }, onUpdated = { goal ->
                val ready = goal.copy(fundingGuidance = goal.toFundingGuidance(state.asOfDate))
                goals = goals.map { if (it.id == goal.id) ready else it }
                onEvent(GoalsScreenEvent.GoalUpdated(ready)); modal = null
            }, onReserve = { modal = GoalsModal.Reserve(it) })
    }
}

@Composable
private fun ReservedSummary(amount: Double, currency: String, count: Int) {
    Surface(shape = JvyShapes.Card, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reserved for goals", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(money(amount, currency), style = JvyTypography.Amount, color = MaterialTheme.colorScheme.onSurface)
            Text("$count active ${if (count == 1) "goal" else "goals"} · Excluded from Safe to Spend.",
                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, accounts: List<GoalAccount>, date: java.time.LocalDate, onClick: () -> Unit) {
    val guidance = goal.fundingGuidance ?: goal.toFundingGuidance(date)
    val accent = if (goal.type == GoalType.EMERGENCY_FUND) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    OutlinedCard(onClick, Modifier.fillMaxWidth(), shape = JvyShapes.Card) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(goal.name, style = MaterialTheme.typography.titleMedium)
            Text(goal.type.label, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Reserved ${money(guidance.reservedAmount.toDouble(), goal.currency)}", style = JvyTypography.AmountSmall)
            ProgressTrack(goal.progress, accent)
            Text("Remaining ${money(guidance.remainingAmount.toDouble(), goal.currency)}", style = JvyTypography.Supporting)
            Text(guidance.displayLabel(goal.currency), style = JvyTypography.Supporting, color = accent)
            goal.deadlineLabel?.takeUnless { it.equals(guidance.displayLabel(goal.currency), ignoreCase = true) }
                ?.let { Text(it, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            val names = goal.allocations.mapNotNull { allocation -> accounts.firstOrNull { it.id == allocation.accountId }?.name }
            Text(if (names.isEmpty()) "Not reserved yet" else names.joinToString(), style = JvyTypography.Supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProgressTrack(progress: Float, color: Color) {
    LinearProgressIndicator(progress.coerceIn(0f, 1f), Modifier.fillMaxWidth().height(6.dp),
        color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant)
}

private fun money(amount: Double, currency: String) = String.format(Locale.US, "%,.2f %s", amount, currency)
