package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.ivy.design.system.*
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.deprecated.logic.LoanWriteConflict
import java.util.UUID

fun loanEditorSubmission() = EditorSubmission(terminalFailure = { it is LoanWriteConflict }, failureMessage = {
    if (it is LoanWriteConflict) "This loan or record has changed or is no longer available. Close this form and review the current details."
    else if (it is com.ivy.wallet.domain.deprecated.logic.loantrasactions.LoanConversionUnavailable)
        "Could not convert this amount. Choose an account in the loan's currency, or add the missing exchange rate first. Your changes are still here."
    else "Could not save. Your changes are still here. Try again."
})

@Composable
internal fun LoanEditorActions(submission: EditorSubmission, editing: Boolean, valid: Boolean, close: () -> Unit, save: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (submission.saving) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(androidx.compose.ui.res.stringResource(com.ivy.ui.R.string.jvy_saving), style = JvyTypography.Supporting)
        }
        submission.error?.let {
            Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error,
                style = JvyTypography.Supporting)
        }
        if (submission.requiresReview) JvyButton(close, Modifier.fillMaxWidth()) { Text(androidx.compose.ui.res.stringResource(com.ivy.ui.R.string.done)) }
        else ModalAddSave(if (editing) true else null, enabled = valid && !submission.saving, onClick = save)
    }
}

@Composable
internal fun LoanAccountsRow(accounts: List<Account>, selectedId: UUID?, enabled: Boolean,
                             selected: (Account) -> Unit, add: () -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(accounts.map { it.id }, selectedId) {
        accounts.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 }?.let { state.scrollToItem(it) }
    }
    LazyRow(Modifier.fillMaxWidth(), state = state, contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(accounts, key = { it.id.toString() }) { account ->
            JvyOutlinedButton({ selected(account) }, Modifier.widthIn(max = 240.dp).semantics { this.selected = account.id == selectedId }, enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (account.id == selectedId) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = if (account.id == selectedId) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)) {
                Text(account.name, style = JvyTypography.Supporting)
            }
        }
        item(key = "add") {
            JvyOutlinedButton(add, enabled = enabled) { Text(androidx.compose.ui.res.stringResource(com.ivy.ui.R.string.add_account)) }
        }
    }
}
