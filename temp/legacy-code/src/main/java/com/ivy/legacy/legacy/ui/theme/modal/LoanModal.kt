package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ivy.data.model.LoanType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.system.*
import com.ivy.domain.legacy.ui.IvyColorPicker
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.legacy.ui.theme.components.DateTimeRow
import com.ivy.legacy.ui.EditorModalHost
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanData
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.components.IvyCheckboxWithText
import com.ivy.wallet.ui.theme.modal.edit.*
import java.time.Instant
import java.util.UUID

data class LoanModalData(
    val loan: Loan?, val baseCurrency: String, val selectedAccount: Account? = null,
    val autoFocusKeyboard: Boolean = true, val autoOpenAmountModal: Boolean = false,
    val createLoanTransaction: Boolean = false, val id: UUID = UUID.randomUUID(),
    internal val submission: EditorSubmission = loanEditorSubmission(),
    internal val editors: EditorModalHost = EditorModalHost(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxWithConstraintsScope.LoanModal(
    modal: LoanModalData?, dateTime: Instant, onSetDate: () -> Unit, onSetTime: () -> Unit,
    onCreateLoan: (CreateLoanData, (Result<Unit>) -> Unit) -> Unit,
    onEditLoan: (Loan, Boolean, (Result<Unit>) -> Unit) -> Unit,
    accounts: List<Account> = emptyList(),
    onCreateAccount: (CreateAccountData, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    @Suppress("UNUSED_PARAMETER") onPerformCalculations: () -> Unit = {},
    dismiss: () -> Unit,
) {
    val loan = modal?.loan
    val key = modal?.id?.toString()
    val latestId by rememberUpdatedState(modal?.id)
    val submission = modal?.submission ?: remember { loanEditorSubmission() }
    val converter = LocalTimeConverter.current
    val chosenDate = loan?.dateTime ?: with(converter) { dateTime.toLocalDateTime() }
    val initialDate = rememberSaveable(key) { chosenDate.toString() }
    var name by rememberSaveable(key, stateSaver = TextFieldValue.Saver) { mutableStateOf(selectEndTextFieldValue(loan?.name)) }
    var amount by rememberSaveable(key) { mutableStateOf(loan?.amount ?: 0.0) }
    var type by rememberSaveable(key) { mutableStateOf(loan?.type ?: LoanType.BORROW) }
    var color by rememberSaveable(key, stateSaver = Saver<Color, Int>({ it.toArgb() }, { Color(it) })) { mutableStateOf(loan?.color?.let(::Color) ?: Ivy) }
    var icon by rememberSaveable(key) { mutableStateOf(loan?.icon) }
    var note by rememberSaveable(key) { mutableStateOf(loan?.note.orEmpty()) }
    var selectedId by rememberSaveable(key) { mutableStateOf(modal?.selectedAccount?.id?.toString()) }
    val selected = accounts.find { it.id.toString() == selectedId }
    val currentCurrency = selected?.currency ?: modal?.baseCurrency.orEmpty()
    var currency by rememberSaveable(key) { mutableStateOf(currentCurrency) }
    val currencyChanged = currency != currentCurrency
    var createTransaction by rememberSaveable(key) { mutableStateOf(modal?.createLoanTransaction ?: false) }
    var amountVisible by rememberSaveable(key) { mutableStateOf(false) }
    var iconVisible by rememberSaveable(key) { mutableStateOf(false) }
    var confirmCurrency by rememberSaveable(key) { mutableStateOf(false) }
    var opened by rememberSaveable(key) { mutableStateOf(false) }
    val amountId = rememberSaveable(key) { UUID.randomUUID().toString() }
    val editors = modal?.editors ?: remember { EditorModalHost() }
    val childrenResolved = com.ivy.legacy.ui.rememberEditorModalRestoration(editors, modal != null, currency, accounts, emptyList())
    fun editableNow() = !submission.saving && !submission.requiresReview
    val editable = editableNow()
    val valid = name.text.isNotBlank() && amount.isFinite() && amount > 0 && selected != null && !currencyChanged
    val close = { if (latestId == modal?.id && !submission.saving) dismiss() }
    fun save() {
        val id = modal?.id ?: return
        if (latestId != id || !editableNow() || !valid) return
        submission.submit({ complete ->
            if (loan == null) onCreateLoan(CreateLoanData(name.text.trim(), amount, type, color, icon,
                selected, note.trim().takeIf { it.isNotEmpty() }, createTransaction, chosenDate, id), complete)
            else onEditLoan(loan.copy(name = name.text.trim(), amount = amount, type = type,
                color = color.toArgb(), icon = icon, note = note.trim().takeIf { it.isNotEmpty() },
                accountId = selected?.id, dateTime = chosenDate), createTransaction, complete)
        }, onSuccess = { if (latestId == id) dismiss() })
    }
    val dirty = name.text != loan?.name.orEmpty() || amount != (loan?.amount ?: 0.0) ||
        type != (loan?.type ?: LoanType.BORROW) || color.toArgb() != (loan?.color ?: Ivy.toArgb()) ||
        icon != loan?.icon || note != loan?.note.orEmpty() || selectedId != modal?.selectedAccount?.id?.toString() ||
        createTransaction != (modal?.createLoanTransaction ?: false) || chosenDate.toString() != initialDate
    LaunchedEffect(key) { if (modal?.autoOpenAmountModal == true && !opened && editableNow()) { opened = true; amountVisible = true } }
    IvyModal(id = modal?.id, visible = modal != null, dismiss = close, dismissEnabled = !submission.saving,
        title = stringResource(if (loan == null) R.string.new_loan else R.string.edit_loan),
        hasUnsavedChanges = dirty && !submission.requiresReview,
        PrimaryAction = { LoanEditorActions(submission, loan != null, valid, close) {
            if (loan != null && currency != (modal.selectedAccount?.currency ?: modal.baseCurrency)) confirmCurrency = true else save()
        } }) {
        IconNameRow(hint = stringResource(R.string.loan_name), defaultIcon = R.drawable.ic_custom_loan_m,
            color = color, icon = icon, autoFocusKeyboard = modal?.autoFocusKeyboard ?: false,
            nameTextFieldValue = name, setNameTextFieldValue = { if (editableNow()) name = it },
            showChooseIconModal = { if (editableNow()) iconVisible = true }, enabled = editable)
        Spacer(Modifier.height(16.dp))
        DateTimeRow(chosenDate, onSetDate, onSetTime, enabled = editable)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.loan_type), Modifier.padding(horizontal = 24.dp), style = JvyTypography.Supporting)
        Spacer(Modifier.height(JvySpacing.Related))
        FlowRow(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(JvySpacing.Related),
            verticalArrangement = Arrangement.spacedBy(JvySpacing.Related)) {
            LoanType.entries.forEach { choice ->
                JvyOutlinedButton({ if (editableNow()) type = choice }, Modifier.semantics { this.selected = type == choice }, enabled = editable,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (choice == type) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) {
                    Text(stringResource(if (choice == LoanType.BORROW) R.string.borrow_money else R.string.lend_money))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        IvyColorPicker(selectedColor = color, enabled = editable, onColorSelected = { if (editableNow()) color = it })
        Spacer(Modifier.height(JvySpacing.Group))
        OutlinedTextField(note, { if (editableNow()) note = it }, Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            enabled = editable, label = { Text(stringResource(R.string.note)) }, shape = JvyShapes.Control, textStyle = JvyTypography.Body)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.associated_account), Modifier.padding(horizontal = 24.dp), style = JvyTypography.Label)
        Spacer(Modifier.height(JvySpacing.Related))
        LoanAccountsRow(accounts, selected?.id, editable, { if (editableNow()) { selectedId = it.id.toString(); currency = it.currency ?: modal?.baseCurrency.orEmpty() } },
            { if (editableNow()) editors.account = AccountModalData(null, currency, 0.0) })
        if (currencyChanged) Text(stringResource(R.string.jvy_draft_currency_review),
            Modifier.padding(horizontal = 24.dp).semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(JvySpacing.Related))
        IvyCheckboxWithText(text = stringResource(R.string.create_main_transaction), checked = createTransaction,
            modifier = Modifier.padding(horizontal = 16.dp), enabled = editable) { if (editableNow()) createTransaction = it }
        ModalAmountSection(label = stringResource(R.string.enter_loan_amount_uppercase), currency = currency, amount = amount,
            amountPaddingTop = 16.dp, amountPaddingBottom = 16.dp, enabled = editable) { if (editableNow()) amountVisible = true }
    }
    AmountModal(UUID.fromString(amountId), visible = modal != null && amountVisible && editable, currency = currency,
        initialAmount = amount, dismiss = { amountVisible = false }) { if (editableNow()) amount = it }
    val accountModal = editors.account
    if (childrenResolved) AccountModal(accountModal, onCreateAccount, { _, _, complete -> complete(Result.failure(IllegalStateException())) },
        { editors.closeAccount(accountModal?.id) })
    ChooseIconModal(visible = modal != null && iconVisible && editable, initialIcon = icon ?: "loan", color = color,
        dismiss = { iconVisible = false }) { if (editableNow()) icon = it }
    DeleteModal(visible = modal != null && confirmCurrency, title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_change_warning), buttonText = stringResource(R.string.confirm),
        iconStart = R.drawable.ic_agreed, dismiss = { confirmCurrency = false }) {
        confirmCurrency = false
        save()
    }
}
