package com.ivy.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalTitle
import java.util.UUID

internal sealed interface GoalsModal {
    data object Add : GoalsModal
    data class Detail(val goal: Goal) : GoalsModal
    data class Reserve(val goal: Goal) : GoalsModal
}

@Composable
internal fun BoxScope.GoalsModalHost(
    modal: GoalsModal?,
    state: GoalScreenState,
    onDismiss: () -> Unit,
    onCreated: (Goal) -> Unit,
    onUpdated: (Goal) -> Unit,
    onReserve: (Goal) -> Unit
) {
    IvyModal(
        id = modal?.let { UUID.nameUUIDFromBytes(it.toString().toByteArray()) },
        visible = modal != null,
        dismiss = onDismiss,
        PrimaryAction = {
            when (modal) {
                GoalsModal.Add -> Spacer(Modifier.width(1.dp))
                is GoalsModal.Detail -> IvyButton(
                    text = "Done",
                    backgroundGradient = Gradient.solid(UI.colors.primary),
                    onClick = onDismiss
                )
                is GoalsModal.Reserve -> Spacer(Modifier.width(1.dp))
                null -> Spacer(Modifier.width(1.dp))
            }
        }
    ) {
        when (val current = modal) {
            GoalsModal.Add -> AddGoalContent(onCreated = onCreated)
            is GoalsModal.Detail -> GoalDetailContent(
                goal = current.goal,
                accounts = state.accounts,
                onReserve = { onReserve(current.goal) }
            )
            is GoalsModal.Reserve -> ReserveMoneyContent(
                goal = current.goal,
                accounts = state.accounts,
                otherReservedByAccount = state.goals
                    .filter { it.id != current.goal.id }
                    .flatMap { it.allocations }
                    .groupBy { it.accountId }
                    .mapValues { (_, values) -> values.sumOf { it.amount } },
                onSave = { allocations ->
                    onUpdated(current.goal.copy(allocations = allocations))
                }
            )
            null -> Unit
        }
    }
}

@Composable
private fun AddGoalContent(onCreated: (Goal) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(GoalType.PURCHASE) }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(8.dp))
        ModalTitle(text = "Create a goal")
        Spacer(Modifier.height(20.dp))
        Text(
            text = "What are you saving for?",
            style = UI.typo.b2.style(color = UI.colors.gray, fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("New laptop") }
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Goal type",
            style = UI.typo.b2.style(color = UI.colors.gray, fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            GoalType.values().forEach { candidate ->
                TypeChoice(
                    label = candidate.label,
                    selected = candidate == type,
                    onClick = { type = candidate }
                )
                if (candidate != GoalType.CUSTOM) Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = target,
            onValueChange = { target = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Target amount (AED)") }
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = deadline,
            onValueChange = { deadline = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Deadline (optional)") },
            placeholder = { Text("April 2027") }
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "You can assign money from one or more accounts after creating the goal.",
            style = UI.typo.c.style(color = UI.colors.gray)
        )
        Spacer(Modifier.height(96.dp))

        // This button keeps the wizard independently usable in previews. The parent action row
        // remains intentionally simple because the final storage layer owns validation/errors.
        IvyButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Create goal",
            wrapContentMode = false,
            backgroundGradient = Gradient.solid(UI.colors.primary),
            enabled = name.isNotBlank() && (target.toDoubleOrNull() ?: 0.0) > 0.0,
            onClick = {
                onCreated(
                    Goal(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        type = type,
                        targetAmount = target.toDoubleOrNull() ?: 0.0,
                        currency = "AED",
                        deadlineLabel = deadline.trim().ifBlank { null }
                    )
                )
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TypeChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .weight(1f)
            .background(
                if (selected) UI.colors.primary else UI.colors.medium,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        style = UI.typo.c.style(
            color = if (selected) UI.colors.pureInverse else UI.colors.gray,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun GoalDetailContent(
    goal: Goal,
    accounts: List<GoalAccount>,
    onReserve: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(8.dp))
        ModalTitle(text = goal.name)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${goal.type.label} · ${goal.deadlineLabel ?: "No deadline"}",
            style = UI.typo.b2.style(color = UI.colors.gray)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "${money(goal.savedAmount, goal.currency)} reserved",
            style = UI.typo.h2.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${money(goal.remainingAmount, goal.currency)} remaining of ${money(goal.targetAmount, goal.currency)}",
            style = UI.typo.b2.style(color = UI.colors.gray)
        )
        Spacer(Modifier.height(16.dp))
        ProgressTrack(progress = goal.progress, color = UI.colors.primary)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Reserved from",
            style = UI.typo.b1.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Spacer(Modifier.height(10.dp))
        if (goal.allocations.isEmpty()) {
            Text(
                text = "No money is reserved yet. Your available-to-spend total is unchanged.",
                style = UI.typo.b2.style(color = UI.colors.gray)
            )
        } else {
            goal.allocations.forEach { allocation ->
                val account = accounts.firstOrNull { it.id == allocation.accountId }
                AllocationRow(
                    label = account?.name ?: "Unknown account",
                    amount = money(allocation.amount, goal.currency)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        IvyButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Reserve money",
            wrapContentMode = false,
            backgroundGradient = Gradient.solid(UI.colors.primary),
            onClick = onReserve
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReserveMoneyContent(
    goal: Goal,
    accounts: List<GoalAccount>,
    otherReservedByAccount: Map<String, Double>,
    onSave: (List<GoalAllocation>) -> Unit
) {
    val existing = remember(goal.id) {
        goal.allocations.associate { it.accountId to it.amount.toString() }
    }
    var amounts by remember(goal.id) { mutableStateOf(existing) }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(8.dp))
        ModalTitle(text = "Reserve for ${goal.name}")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Reserved money stays in its account, but is excluded from safe-to-spend.",
            style = UI.typo.b2.style(color = UI.colors.gray)
        )
        Spacer(Modifier.height(20.dp))
        accounts.forEach { account ->
            val current = amounts[account.id].orEmpty()
            val availableForThisGoal = (
                account.balance - (otherReservedByAccount[account.id] ?: 0.0)
                ).coerceAtLeast(0.0)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            style = UI.typo.b2.style(
                                color = UI.colors.pureInverse,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Available to reserve ${money(availableForThisGoal, account.currency)}",
                            style = UI.typo.c.style(color = UI.colors.gray)
                        )
                    }
                    OutlinedTextField(
                        value = current,
                        onValueChange = { updated -> amounts = amounts + (account.id to updated) },
                        modifier = Modifier.width(132.dp),
                        singleLine = true,
                        label = { Text(account.currency) }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Total reserved here: ${money(amounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }, goal.currency)}",
            style = UI.typo.b2.style(color = UI.colors.primary, fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(96.dp))
        IvyButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Apply reservations",
            wrapContentMode = false,
            backgroundGradient = Gradient.solid(UI.colors.primary),
            onClick = {
                onSave(
                    amounts.mapNotNull { (accountId, value) ->
                                value.toDoubleOrNull()?.let { amount ->
                                    val account = accounts.firstOrNull { it.id == accountId }
                                    val max = account?.let {
                                        (it.balance - (otherReservedByAccount[accountId] ?: 0.0))
                                            .coerceAtLeast(0.0)
                                    } ?: 0.0
                                    GoalAllocation(accountId, amount.coerceIn(0.0, max))
                                }?.takeIf { it.amount > 0.0 }
                    }
                )
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AllocationRow(label: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = UI.typo.b2.style(color = UI.colors.gray)
        )
        Text(
            text = amount,
            style = UI.typo.b2.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun money(amount: Double, currency: String): String {
    return "%,.2f %s".format(java.util.Locale.US, amount, currency)
}
