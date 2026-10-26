package com.ivy.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
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
    val goals = state.goals
    var modalKind by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var requestedRevision by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = goals.firstOrNull { it.id == selectedId }
    val modal = when (modalKind) {
        "add" -> GoalsModal.Add
        "detail" -> selected?.let(GoalsModal::Detail)
        "edit" -> selected?.let(GoalsModal::Edit)
        "reserve" -> selected?.let(GoalsModal::Reserve)
        else -> null
    }
    val add = { if (!state.loading && !state.saving) modalKind = "add" }
    if (requestedRevision?.let { state.saveRevision >= it } == true) SideEffect {
        modalKind = null
        requestedRevision = null
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(Modifier.fillMaxSize()) {
        Column(Modifier.widthIn(max = 960.dp).fillMaxSize().systemBarsPadding().align(Alignment.TopCenter)) {
            JvyScreenHeader("Goals", { onEvent(GoalsScreenEvent.Back) }, "Plan for what matters to you.")
            LazyVerticalGrid(columns = GridCells.Adaptive(if (LocalDensity.current.fontScale > 1.3f) 960.dp else 360.dp), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = if (goals.isEmpty()) 0.dp else 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.error != null && modal == null) item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Text(state.error, color = MaterialTheme.colorScheme.error, style = JvyTypography.Supporting)
                        JvyTextButton({ onEvent(GoalsScreenEvent.Retry) }) { Text("Retry") }
                    }
                }
                if (state.loading && goals.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (goals.isEmpty() && state.error == null) item(span = { GridItemSpan(maxLineSpan) }) {
                    JvyEmptyState("What would you like to plan for?",
                        "A laptop, a trip, or peace of mind. Set a target, then reserve money from your accounts without creating a transfer.",
                        Icons.Outlined.Flag, "Create your first goal", add)
                } else if (goals.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { ReservedSummary(goals.sumOf { it.savedAmount }, state.baseCurrency, goals.count { it.status == GoalStatus.ACTIVE }) }
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Your goals · ${goals.size}", style = JvyTypography.SectionTitle) }
                    items(goals.size, key = { goals[it].id }) { index ->
                        val goal = goals[index]
                        GoalCard(goal, state.accounts, state.asOfDate) { selectedId = goal.id; modalKind = "detail" }
                    }
                }
            }
            if (goals.isNotEmpty()) JvyPrimaryAction("Add goal", add)
        }
        GoalsModalHost(modal, state, onDismiss = { modalKind = null; requestedRevision = null },
            onCreated = { goal ->
                requestedRevision = state.saveRevision + 1
                onEvent(GoalsScreenEvent.GoalCreated(goal))
            }, onUpdated = { goal ->
                requestedRevision = state.saveRevision + 1
                onEvent(GoalsScreenEvent.GoalUpdated(goal))
            }, onReserve = { selectedId = it.id; modalKind = "reserve" },
            onEdit = { selectedId = it.id; modalKind = "edit" })
      }
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
    Card(onClick, Modifier.fillMaxWidth(), shape = JvyShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(goal.name, style = MaterialTheme.typography.titleMedium)
            Text(goal.type.label, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Reserved ${money(guidance.reservedAmount.toDouble(), goal.currency)}", style = JvyTypography.AmountSmall)
            ProgressTrack(goal.progress, accent)
            if (goal.reservationsNeedReview(accounts)) Text(reservationReviewMessage,
                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Remaining ${money(guidance.remainingAmount.toDouble(), goal.currency)}", style = JvyTypography.Supporting)
            Text(guidance.displayLabel(goal.currency), style = JvyTypography.Supporting, color = accent)
            goal.displayDeadline(LocalConfiguration.current.locales[0] ?: Locale.getDefault())
                ?.let { Text("By $it", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            val names = goal.allocations.mapNotNull { allocation -> accounts.firstOrNull { it.id == allocation.accountId }?.name }
            Text(if (names.isEmpty()) "Not reserved yet" else names.joinToString(), style = JvyTypography.Supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProgressTrack(progress: Float, color: Color) {
    LinearProgressIndicator(progress.coerceIn(0f, 1f), Modifier.fillMaxWidth().height(6.dp),
        color = color, trackColor = MaterialTheme.colorScheme.surface)
}

private fun money(amount: Double, currency: String) = String.format(Locale.US, "%,.2f %s", amount, currency)
