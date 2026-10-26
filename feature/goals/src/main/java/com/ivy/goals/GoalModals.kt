package com.ivy.goals

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ivy.design.system.*
import com.ivy.ui.money.JvyDecimalField
import com.ivy.ui.time.JvyDateField
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

internal sealed interface GoalsModal {
    data object Add : GoalsModal
    data class Edit(val goal: Goal) : GoalsModal
    data class Detail(val goal: Goal) : GoalsModal
    data class Reserve(val goal: Goal) : GoalsModal
}

@Composable
internal fun GoalsModalHost(modal: GoalsModal?, state: GoalScreenState, onDismiss: () -> Unit,
                            onCreated: (Goal) -> Unit, onUpdated: (Goal) -> Unit,
                            onReserve: (Goal) -> Unit, onEdit: (Goal) -> Unit) {
    if (modal == null) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        when (modal) {
            GoalsModal.Add -> GoalEditorContent(state.baseCurrency, state.asOfDate, onClose = onDismiss,
                saving = state.saving || state.loading, saveError = state.error, onSave = onCreated)
            is GoalsModal.Edit -> GoalEditorContent(state.baseCurrency, state.asOfDate, modal.goal, onDismiss,
                state.saving || state.loading, state.error, onUpdated)
            is GoalsModal.Detail -> GoalDetailContent(modal.goal, state.accounts, state.asOfDate, onDismiss,
                { onReserve(modal.goal) }, { onEdit(modal.goal) })
            is GoalsModal.Reserve -> ReserveMoneyContent(modal.goal, state.accounts,
                state.goals.filter { it.id != modal.goal.id }.flatMap { it.allocations }.groupBy { it.accountId }
                    .mapValues { (_, allocations) -> allocations.sumOf { it.amount.toBigDecimal() }.toDouble() },
                onClose = onDismiss, saving = state.saving || state.loading, saveError = state.error,
                onSave = { onUpdated(modal.goal.copy(allocations = it)) })
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun GoalEditorContent(currency: String, today: LocalDate, existing: Goal? = null,
                               onClose: () -> Unit, saving: Boolean = false, saveError: String? = null,
                               onSave: (Goal) -> Unit) {
    val draftId = rememberSaveable(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var target by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.targetAmount?.toBigDecimal()?.stripTrailingZeros()?.toPlainString().orEmpty())
    }
    var deadline by rememberSaveable(existing?.id) { mutableStateOf(existing?.resolvedDeadline()?.toString()) }
    var type by rememberSaveable(existing?.id) { mutableStateOf(existing?.type ?: GoalType.PURCHASE) }
    var attempted by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    val targetFocus = remember { FocusRequester() }
    val error = if (attempted) goalInputError(name, target) else null
    JvyFormDialogFrame(if (existing == null) "Create a goal" else "Edit goal", onClose, footer = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (error ?: saveError)?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.error) }
            JvyButton(onClick = {
                attempted = true
                if (name.isBlank()) nameFocus.requestFocus()
                else if (goalInputError(name, target) != null) targetFocus.requestFocus()
                if (goalInputError(name, target) == null) {
                    val day = deadline?.let(LocalDate::parse)
                    val goal = existing?.copy(name = name.trim(), type = type, targetAmount = target.toDouble(),
                        deadlineLabel = null, deadline = day)
                        ?: Goal(id = draftId, name = name.trim(), type = type,
                            targetAmount = target.toDouble(), currency = currency, deadline = day)
                    onSave(goal)
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !saving) {
                Text(if (saving) "Saving…" else if (existing == null) "Create goal" else "Save changes")
            }
        }
    }) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().focusRequester(nameFocus), label = { Text("Goal name") },
            placeholder = { Text("For example, a new laptop") }, singleLine = true, shape = JvyShapes.Control,
            isError = attempted && name.isBlank())
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Goal type", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalType.entries.forEach { candidate ->
                    FilterChip(candidate == type, { type = candidate }, label = { Text(candidate.label) }, shape = JvyShapes.Control)
                }
            }
            Text(when (type) {
                GoalType.EMERGENCY_FUND -> "A reserve for unexpected costs or a gap in income."
                GoalType.PURCHASE -> "Set a target for something you want to buy."
                GoalType.SINKING_FUND -> "Set money aside for an upcoming bill or planned expense."
                GoalType.CUSTOM -> "Choose a name and target for your own plan."
            }, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        JvyDecimalField("Target amount ($currency)", target, { target = it }, modifier = Modifier.fillMaxWidth().focusRequester(targetFocus))
        JvyDateField("Deadline (optional)", deadline?.let(LocalDate::parse), { deadline = it?.toString() },
            optional = true, initialDate = today)
        Text("Reserve money from your accounts after creating the goal. It stays in those accounts; no transfer is created.",
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun GoalDetailContent(goal: Goal, accounts: List<GoalAccount>, asOfDate: LocalDate,
                               onClose: () -> Unit, onReserve: () -> Unit, onEdit: () -> Unit) {
    val guidance = goal.fundingGuidance ?: goal.toFundingGuidance(asOfDate)
    JvyFormDialogFrame(goal.name, onClose, footer = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JvyButton(onReserve, Modifier.fillMaxWidth()) { Text("Reserve money") }
            JvyOutlinedButton(onEdit, Modifier.fillMaxWidth()) { Text("Edit goal") }
        }
    }) {
        Text(goal.type.label, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${money(guidance.reservedAmount.toDouble(), goal.currency)} reserved", style = JvyTypography.Amount)
        Text("${money(guidance.remainingAmount.toDouble(), goal.currency)} remaining of ${money(goal.targetAmount, goal.currency)}",
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProgressTrack(goal.progress, MaterialTheme.colorScheme.primary)
        if (goal.reservationsNeedReview(accounts)) Text(reservationReviewMessage,
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(guidance.displayLabel(goal.currency), style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.primary)
        goal.displayDeadline(LocalConfiguration.current.locales[0] ?: Locale.getDefault())
            ?.let { Text("Deadline: $it", style = JvyTypography.Supporting) }
        Text("Reserved from", style = MaterialTheme.typography.titleMedium)
        if (goal.allocations.isEmpty()) Text("Nothing reserved yet. Your available-to-spend total is unchanged.",
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else goal.allocations.forEach { allocation ->
            val account = accounts.firstOrNull { it.id == allocation.accountId }
            Column {
                Text(account?.name ?: "Unknown account", style = MaterialTheme.typography.titleSmall)
                Text(money(allocation.amount, account?.currency ?: goal.currency), style = JvyTypography.AmountSmall)
            }
        }
    }
}

internal fun reservationInputError(values: Map<String, String>, accounts: List<GoalAccount>, otherReserved: Map<String, Double>): String? {
    accounts.forEach { account ->
        val text = values[account.id].orEmpty()
        if (text.isBlank()) return@forEach
        val amount = text.toBigDecimalOrNull()
        val reserved = otherReserved[account.id] ?: 0.0
        if (!account.balance.isFinite() || !reserved.isFinite()) return "Refresh ${account.name}'s balance before reserving money."
        val available = (account.balance.toBigDecimal() - reserved.toBigDecimal()).max(java.math.BigDecimal.ZERO)
        if (amount == null || !amount.toDouble().isFinite() || amount.signum() < 0) return "Enter a valid reservation for ${account.name}."
        if (amount > available) return "${account.name} has ${money(available.toDouble(), account.currency)} available to reserve."
    }
    return null
}

@Composable
internal fun ReserveMoneyContent(goal: Goal, accounts: List<GoalAccount>, otherReservedByAccount: Map<String, Double>,
                                 onClose: () -> Unit, saving: Boolean = false, saveError: String? = null,
                                 onSave: (List<GoalAllocation>) -> Unit) {
    val editable = accounts.filter { it.currency == goal.currency }
    val untouched = goal.allocations.filter { allocation -> editable.none { it.id == allocation.accountId } }
    var amounts by rememberSaveable(goal.id) {
        mutableStateOf(editable.associate { account -> account.id to goal.allocations.firstOrNull { it.accountId == account.id }
            ?.amount?.toBigDecimal()?.stripTrailingZeros()?.toPlainString().orEmpty() })
    }
    var attempted by rememberSaveable(goal.id) { mutableStateOf(false) }
    val error = if (attempted) reservationInputError(amounts, editable, otherReservedByAccount) else null
    JvyFormDialogFrame("Reserve for ${goal.name}", onClose, footer = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (error ?: saveError)?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.error) }
            JvyButton({
                attempted = true
                if (reservationInputError(amounts, editable, otherReservedByAccount) == null) {
                    onSave(untouched + editable.mapNotNull { account -> amounts[account.id]?.toDoubleOrNull()
                        ?.takeIf { it > 0 }?.let { GoalAllocation(account.id, it) } })
                }
            }, Modifier.fillMaxWidth(), enabled = editable.isNotEmpty() && !saving) { Text(if (saving) "Saving…" else "Save reservations") }
        }
    }) {
        Text("Money stays in its account, but is excluded from Safe to Spend while reserved.",
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (editable.isEmpty()) Text("Add an account in ${goal.currency} in Accounts before reserving money.", style = JvyTypography.Supporting)
        editable.forEach { account ->
            Surface(shape = JvyShapes.Control, color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(account.name, style = MaterialTheme.typography.titleSmall)
                    val available = (account.balance - (otherReservedByAccount[account.id] ?: 0.0)).coerceAtLeast(0.0)
                    Text("Available ${money(available, account.currency)}", style = JvyTypography.Supporting,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    JvyDecimalField("Reserve (${account.currency})", amounts[account.id].orEmpty(), { amounts = amounts + (account.id to it) })
                }
            }
        }
        if (accounts.any { it.currency != goal.currency } || untouched.isNotEmpty()) Text(
            "New reservations use accounts in ${goal.currency}. Existing reservations in other currencies or unavailable accounts are kept unchanged; review them before relying on the goal total.",
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Reserved here: ${money(amounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }, goal.currency)}", style = JvyTypography.AmountSmall)
    }
}

private fun money(amount: Double, currency: String) = String.format(Locale.US, "%,.2f %s", amount, currency)
