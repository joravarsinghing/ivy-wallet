package com.ivy.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.navigation.GoalsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.components.IvyButton
import java.util.Locale

@Composable
fun BoxWithConstraintsScope.GoalsScreen(screen: GoalsScreen) {
    @Suppress("UNUSED_VARIABLE")
    val route = screen
    val viewModel: GoalsViewModel = screenScopedViewModel()
    val nav = navigation()

    GoalsScreenContent(
        state = viewModel.uiState.collectAsState().value,
        onEvent = { event ->
            if (event == GoalsScreenEvent.Back) {
                nav.back()
            } else {
                viewModel.onEvent(event)
            }
        }
    )
}

/**
 * Public integration seam for the eventual repository-backed screen.
 * The default route above deliberately stays local so this module can be previewed
 * before navigation, Room, or account reservation APIs are wired.
 */
@Composable
fun BoxWithConstraintsScope.GoalsScreenContent(
    state: GoalScreenState,
    onEvent: (GoalsScreenEvent) -> Unit
) {
    var goals by remember(state.goals) { mutableStateOf(state.goals) }
    var modal by remember { mutableStateOf<GoalsModal?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 112.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Goals",
                style = UI.typo.h2.style(
                    color = UI.colors.pureInverse,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Give your money a job, then watch it get there.",
                style = UI.typo.b2.style(color = UI.colors.gray)
            )
            Spacer(Modifier.height(20.dp))

            ReservedSummary(
                reservedAmount = goals.sumOf { it.savedAmount },
                currency = state.baseCurrency,
                goalsCount = goals.count { it.status == GoalStatus.ACTIVE }
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your goals",
                    style = UI.typo.b1.style(
                        color = UI.colors.pureInverse,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${goals.size}",
                    style = UI.typo.b2.style(
                        color = UI.colors.gray,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(Modifier.height(12.dp))

            if (goals.isEmpty()) {
                GoalsEmptyState(onAdd = { modal = GoalsModal.Add })
            } else {
                goals.forEach { goal ->
                    GoalCard(
                        goal = goal,
                        accounts = state.accounts,
                        asOfDate = state.asOfDate,
                        onClick = { modal = GoalsModal.Detail(goal) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(UI.colors.pure)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Back",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onEvent(GoalsScreenEvent.Back) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                style = UI.typo.b2.style(
                    color = UI.colors.gray,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.weight(1f))
            IvyButton(
                text = "Add goal",
                backgroundGradient = Gradient.solid(UI.colors.primary),
                onClick = { modal = GoalsModal.Add }
            )
        }

        GoalsModalHost(
            modal = modal,
            state = state.copy(goals = goals),
            onDismiss = { modal = null },
            onCreated = { goal ->
                val goalWithGuidance = goal.copy(
                    fundingGuidance = goal.toFundingGuidance(state.asOfDate)
                )
                goals = goals + goalWithGuidance
                onEvent(GoalsScreenEvent.GoalCreated(goalWithGuidance))
                modal = null
            },
            onUpdated = { goal ->
                val goalWithGuidance = goal.copy(
                    fundingGuidance = goal.toFundingGuidance(state.asOfDate)
                )
                goals = goals.map { if (it.id == goal.id) goalWithGuidance else it }
                onEvent(GoalsScreenEvent.GoalUpdated(goalWithGuidance))
                modal = null
            },
            onReserve = { goal -> modal = GoalsModal.Reserve(goal) }
        )
    }
}

@Composable
private fun ReservedSummary(
    reservedAmount: Double,
    currency: String,
    goalsCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(UI.colors.primary)
            .padding(20.dp)
    ) {
        Text(
            text = "Reserved for goals",
            style = UI.typo.b2.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = money(reservedAmount, currency),
            style = UI.typo.h1.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (goalsCount == 0) {
                "Create a goal to start reserving money."
            } else {
                "$goalsCount active ${if (goalsCount == 1) "goal" else "goals"}"
            },
            style = UI.typo.c.style(color = UI.colors.pureInverse)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Excluded from Safe to Spend while reserved.",
            style = UI.typo.c.style(color = UI.colors.pureInverse.copy(alpha = 0.82f))
        )
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    accounts: List<GoalAccount>,
    asOfDate: java.time.LocalDate,
    onClick: () -> Unit
) {
    val guidance = goal.fundingGuidance ?: goal.toFundingGuidance(asOfDate)
    val accent = when (goal.type) {
        GoalType.EMERGENCY_FUND -> UI.colors.green
        GoalType.PURCHASE -> UI.colors.primary
        GoalType.SINKING_FUND -> UI.colors.orange
        GoalType.CUSTOM -> UI.colors.primary1
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(UI.colors.medium)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = goal.type.symbol,
                    style = UI.typo.b1.style(color = accent, fontWeight = FontWeight.ExtraBold)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = UI.typo.b1.style(
                        color = UI.colors.pureInverse,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = goal.type.label,
                    style = UI.typo.c.style(color = UI.colors.gray)
                )
            }
            Text(
                text = money(guidance.reservedAmount.toDouble(), goal.currency),
                style = UI.typo.b2.style(
                    color = UI.colors.pureInverse,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.height(18.dp))
        ProgressTrack(progress = goal.progress, color = accent)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Reserved ${money(guidance.reservedAmount.toDouble(), goal.currency)}",
                style = UI.typo.c.style(color = accent, fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Remaining ${money(guidance.remainingAmount.toDouble(), goal.currency)}",
                style = UI.typo.c.style(color = UI.colors.gray)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = guidance.displayLabel(goal.currency),
                style = UI.typo.c.style(color = accent, fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = goal.deadlineLabel ?: "No deadline",
                style = UI.typo.c.style(color = UI.colors.gray),
                textAlign = TextAlign.End
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val accountNames = goal.allocations.mapNotNull { allocation ->
                accounts.firstOrNull { it.id == allocation.accountId }?.name
            }
            Text(
                text = if (accountNames.isEmpty()) "Not reserved yet" else accountNames.joinToString(),
                style = UI.typo.c.style(color = UI.colors.gray),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
internal fun ProgressTrack(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(UI.colors.gray.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
private fun GoalsEmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "○",
            style = UI.typo.h1.style(color = UI.colors.primary, fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Nothing saved for yet",
            style = UI.typo.b1.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Create a laptop, travel, or emergency fund goal. You can split it across accounts.",
            modifier = Modifier.padding(horizontal = 20.dp),
            style = UI.typo.b2.style(color = UI.colors.gray, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(20.dp))
        IvyButton(
            text = "Create your first goal",
            backgroundGradient = Gradient.solid(UI.colors.primary),
            onClick = onAdd
        )
    }
}

private fun money(amount: Double, currency: String): String {
    return String.format(Locale.US, "%,.2f %s", amount, currency)
}

private val GoalType.symbol: String
    get() = when (this) {
        GoalType.EMERGENCY_FUND -> "!"
        GoalType.PURCHASE -> "↗"
        GoalType.SINKING_FUND -> "↓"
        GoalType.CUSTOM -> "✦"
    }
