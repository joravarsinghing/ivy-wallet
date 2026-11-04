package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import com.ivy.base.model.LoanRecordType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.system.*
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.LoanRecord
import com.ivy.legacy.legacy.ui.theme.components.DateTimeRow
import com.ivy.legacy.ui.EditorModalHost
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanRecordData
import com.ivy.wallet.domain.deprecated.logic.model.EditLoanRecordData
import com.ivy.wallet.ui.theme.components.IvyCheckboxWithText
import com.ivy.wallet.ui.theme.modal.edit.*
import java.time.Instant
import java.util.UUID

data class LoanRecordModalData(
    val loanRecord: LoanRecord?, val baseCurrency: String, val loanAccountCurrencyCode: String? = null,
    val selectedAccount: Account? = null, val createLoanRecordTransaction: Boolean = false,
    val isLoanInterest: Boolean = false, val id: UUID = UUID.randomUUID(),
    internal val submission: EditorSubmission = loanEditorSubmission(),
    internal val editors: EditorModalHost = EditorModalHost(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxWithConstraintsScope.LoanRecordModal(
    modal: LoanRecordModalData?, dateTime: Instant, onSetDate: () -> Unit, onSetTime: () -> Unit,
    onCreate: (CreateLoanRecordData, (Result<Unit>) -> Unit) -> Unit,
    onEdit: (EditLoanRecordData, (Result<Unit>) -> Unit) -> Unit,
    onDelete: (LoanRecord, (Result<Unit>) -> Unit) -> Unit, dismiss: () -> Unit,
    accounts: List<Account> = emptyList(),
    onCreateAccount: (CreateAccountData, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
) {
    val initial = modal?.loanRecord
    val key = modal?.id?.toString()
    val latestId by rememberUpdatedState(modal?.id)
    val submission = modal?.submission ?: remember { loanEditorSubmission() }
    val converter = LocalTimeConverter.current
    val chosenDate = initial?.dateTime ?: dateTime
    val initialDate = rememberSaveable(key) { chosenDate.toString() }
    var note by rememberSaveable(key) { mutableStateOf(initial?.note.orEmpty()) }
    var amount by rememberSaveable(key) { mutableStateOf(initial?.amount ?: 0.0) }
    var selectedId by rememberSaveable(key) { mutableStateOf(modal?.selectedAccount?.id?.toString()) }
    val account = accounts.find { it.id.toString() == selectedId }
    val currentCurrency = account?.currency ?: modal?.baseCurrency.orEmpty()
    var currency by rememberSaveable(key) { mutableStateOf(currentCurrency) }
    val currencyChanged = currency != currentCurrency
    var createTransaction by rememberSaveable(key) { mutableStateOf(modal?.createLoanRecordTransaction ?: false) }
    var interest by rememberSaveable(key) { mutableStateOf(modal?.isLoanInterest ?: false) }
    var type by rememberSaveable(key) { mutableStateOf(initial?.loanRecordType ?: LoanRecordType.DECREASE) }
    var recalculate by rememberSaveable(key) { mutableStateOf(false) }
    val foreign = modal?.loanAccountCurrencyCode != null && currency != modal.loanAccountCurrencyCode
    var amountVisible by rememberSaveable(key) { mutableStateOf(false) }
    var deleteVisible by rememberSaveable(key) { mutableStateOf(false) }
    var confirmCurrency by rememberSaveable(key) { mutableStateOf(false) }
    var opened by rememberSaveable(key) { mutableStateOf(false) }
    val amountId = rememberSaveable(key) { UUID.randomUUID().toString() }
    val editors = modal?.editors ?: remember { EditorModalHost() }
    val childrenResolved = com.ivy.legacy.ui.rememberEditorModalRestoration(editors, modal != null, currency, accounts, emptyList())
    val child = editors.account
    fun editableNow() = !submission.saving && !submission.requiresReview
    val editable = editableNow()
    val valid = amount.isFinite() && amount > 0 && account != null && !currencyChanged
    val close = { if (latestId == modal?.id && !submission.saving) dismiss() }
    fun save() {
        val id = modal?.id ?: return
        if (latestId != id || !editableNow() || !valid) return
        submission.submit({ complete ->
            if (initial == null) onCreate(CreateLoanRecordData(note.trim().takeIf { it.isNotEmpty() }, amount,
                chosenDate, interest, account, createTransaction, loanRecordType = type, id = id), complete)
            else onEdit(EditLoanRecordData(newLoanRecord = initial.copy(note = note.trim().takeIf { it.isNotEmpty() },
                amount = amount, dateTime = chosenDate, interest = interest, accountId = account?.id, loanRecordType = type),
                originalLoanRecord = initial, createLoanRecordTransaction = createTransaction, reCalculateLoanAmount = recalculate), complete)
        }, onSuccess = { if (latestId == id) dismiss() })
    }
    val dirty = note != initial?.note.orEmpty() || amount != (initial?.amount ?: 0.0) ||
        selectedId != modal?.selectedAccount?.id?.toString() || interest != (modal?.isLoanInterest ?: false) ||
        type != (initial?.loanRecordType ?: LoanRecordType.DECREASE) || recalculate ||
        createTransaction != (modal?.createLoanRecordTransaction ?: false) || chosenDate.toString() != initialDate
    LaunchedEffect(key) { if (modal != null && initial == null && !opened && editableNow()) { opened = true; amountVisible = true } }
    IvyModal(id = modal?.id, visible = modal != null, dismiss = close, dismissEnabled = !submission.saving,
        title = stringResource(if (initial == null) R.string.new_record else R.string.edit_record),
        hasUnsavedChanges = dirty && !submission.requiresReview,
        PrimaryAction = { LoanEditorActions(submission, initial != null, valid, close) {
            if (initial != null && currency != modal.baseCurrency && currency != modal.loanAccountCurrencyCode) confirmCurrency = true else save()
        } }) {
        OutlinedTextField(note, { if (editableNow()) note = it }, Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            enabled = editable, label = { Text(stringResource(R.string.note)) }, shape = JvyShapes.Control, textStyle = JvyTypography.Body)
        Spacer(Modifier.height(16.dp))
        DateTimeRow(with(converter) { chosenDate.toLocalDateTime() }, onSetDate, onSetTime, enabled = editable)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.associated_account), Modifier.padding(horizontal = 24.dp), style = JvyTypography.Supporting)
        Spacer(Modifier.height(JvySpacing.Related))
        LoanAccountsRow(accounts, account?.id, editable, { if (editableNow()) { selectedId = it.id.toString(); currency = it.currency ?: modal?.baseCurrency.orEmpty() } },
            { if (editableNow()) editors.account = AccountModalData(null, currency, 0.0) })
        if (currencyChanged) Text(stringResource(R.string.jvy_draft_currency_review),
            Modifier.padding(horizontal = 24.dp).semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.loan_record_type), Modifier.padding(horizontal = 24.dp), style = JvyTypography.Supporting)
        Spacer(Modifier.height(JvySpacing.Related))
        FlowRow(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(JvySpacing.Related),
            verticalArrangement = Arrangement.spacedBy(JvySpacing.Related)) {
            LoanRecordType.entries.forEach { choice ->
                JvyOutlinedButton({ if (editableNow()) { type = choice; if (choice == LoanRecordType.INCREASE) interest = false } },
                    Modifier.semantics { selected = type == choice }, enabled = editable,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (choice == type) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) {
                    Text(stringResource(if (choice == LoanRecordType.INCREASE) R.string.increase_loan else R.string.decrease_loan))
                }
            }
        }
        Spacer(Modifier.height(JvySpacing.Related))
        IvyCheckboxWithText(text = stringResource(R.string.create_main_transaction), checked = createTransaction,
            modifier = Modifier.padding(horizontal = 16.dp), enabled = editable) { if (editableNow()) createTransaction = it }
        if (type == LoanRecordType.DECREASE) IvyCheckboxWithText(text = stringResource(R.string.mark_as_interest), checked = interest,
            modifier = Modifier.padding(horizontal = 16.dp), enabled = editable) { if (editableNow()) interest = it }
        if (foreign && initial != null) IvyCheckboxWithText(text = stringResource(R.string.recalculate_amount_with_today_exchange_rates),
            checked = recalculate, modifier = Modifier.padding(horizontal = 16.dp), enabled = editable) { if (editableNow()) recalculate = it }
        ModalAmountSection(label = stringResource(R.string.enter_record_amount_uppercase), currency = currency, amount = amount,
            amountPaddingTop = 16.dp, amountPaddingBottom = 16.dp, enabled = editable) { if (editableNow()) amountVisible = true }
        if (initial != null) JvyTextButton({ deleteVisible = true }, Modifier.padding(horizontal = 24.dp), enabled = editable) {
            Text(stringResource(R.string.delete), color = if (editable) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        }
    }
    AmountModal(UUID.fromString(amountId), visible = modal != null && amountVisible && editable, currency = currency,
        initialAmount = amount, dismiss = { amountVisible = false }) { if (editableNow()) amount = it }
    if (childrenResolved) AccountModal(child, onCreateAccount, { _, _, complete -> complete(Result.failure(IllegalStateException())) },
        { editors.closeAccount(child?.id) })
    DeleteModal(visible = modal != null && confirmCurrency, title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.account_change_warning), buttonText = stringResource(R.string.confirm),
        iconStart = R.drawable.ic_agreed, dismiss = { confirmCurrency = false }) { confirmCurrency = false; save() }
    DeleteModal(visible = modal != null && deleteVisible, title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.record_deletion_warning, note), dismiss = { deleteVisible = false }) {
        deleteVisible = false
        if (initial != null && editableNow()) {
            val id = modal.id
            submission.submit({ complete -> onDelete(initial, complete) }, onSuccess = { if (latestId == id) dismiss() })
        }
    }
}
