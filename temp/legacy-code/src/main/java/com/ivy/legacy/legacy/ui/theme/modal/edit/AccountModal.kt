package com.ivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.repository.AccountWriteConflict
import com.ivy.design.system.EditorSubmission
import com.ivy.design.system.JvyButton
import com.ivy.design.system.JvyOutlinedButton
import com.ivy.design.system.JvyTypography
import com.ivy.domain.legacy.ui.IvyColorPicker
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.components.IvyCheckboxWithText
import com.ivy.wallet.ui.theme.modal.*
import java.util.UUID

fun accountSaveErrorMessage(failure: Throwable): String = when (failure) {
    is AccountWriteConflict.AlreadySaved -> "This account was already saved. Close this form and review it in Accounts before making changes."
    is AccountWriteConflict.MissingAccount -> "This account no longer exists. Close this form and choose another account."
    else -> "Could not save. Your changes are still here. Try again."
}

fun accountEditorSubmission() = EditorSubmission(terminalFailure = { it is AccountWriteConflict },
    failureMessage = ::accountSaveErrorMessage)

fun accountSuggestionErrorMessage(failure: Throwable): String = when (failure) {
    is AccountWriteConflict.AlreadySaved -> "This account was already added. Review it below before making changes."
    is AccountWriteConflict.MissingAccount -> "That account was removed. Review your accounts before adding it again."
    else -> accountSaveErrorMessage(failure)
}

@Deprecated("Old design system. Use :ivy-design and Material3")
data class AccountModalData(
    val account: Account?,
    val baseCurrency: String,
    val balance: Double,
    val adjustBalanceMode: Boolean = false,
    val forceNonZeroBalance: Boolean = false,
    val autoFocusKeyboard: Boolean = true,
    val id: UUID = UUID.randomUUID(),
    internal val submission: EditorSubmission = accountEditorSubmission(),
)

@Deprecated("Old design system. Use :ivy-design and Material3")
@Composable
fun BoxWithConstraintsScope.AccountModal(
    modal: AccountModalData?,
    onCreateAccount: (CreateAccountData, (Result<Unit>) -> Unit) -> Unit,
    onEditAccount: (Account, Double, (Result<Unit>) -> Unit) -> Unit,
    dismiss: () -> Unit,
    submission: EditorSubmission = modal?.submission ?: remember { accountEditorSubmission() },
) {
    val account = modal?.account
    val key = modal?.id?.toString()
    val latestId by rememberUpdatedState(modal?.id)
    var name by rememberSaveable(key, stateSaver = TextFieldValue.Saver) { mutableStateOf(selectEndTextFieldValue(account?.name)) }
    var color by rememberSaveable(key, stateSaver = Saver<Color, Int>(save = { it.toArgb() }, restore = { Color(it) })) {
        mutableStateOf(account?.color?.let(::Color) ?: Ivy)
    }
    var amount by rememberSaveable(key) { mutableStateOf(modal?.balance ?: 0.0) }
    var currency by rememberSaveable(key) { mutableStateOf(account?.currency ?: modal?.baseCurrency.orEmpty()) }
    var icon by rememberSaveable(key) { mutableStateOf(account?.icon) }
    var include by rememberSaveable(key) { mutableStateOf(account?.includeInBalance ?: true) }
    val terminalConflict = submission.requiresReview
    var amountVisible by rememberSaveable(key) { mutableStateOf(false) }
    var currencyVisible by rememberSaveable(key) { mutableStateOf(false) }
    var iconVisible by rememberSaveable(key) { mutableStateOf(false) }
    var adjustPickerOpened by rememberSaveable(key) { mutableStateOf(false) }
    fun editableNow() = !submission.saving && !submission.requiresReview
    val editable = editableNow()
    fun valid(target: Double) = name.text.isNotBlank() && currency.isNotBlank() && target.isFinite() &&
        (modal?.forceNonZeroBalance != true || target > 0)
    fun submit(target: Double) {
        if (!editableNow() || !valid(target)) return
        val session = modal?.id ?: return
        if (latestId != session) return
        submission.submit({ complete ->
            val acknowledged: (Result<Unit>) -> Unit = { result ->
                complete(result)
            }
            if (account == null) {
                onCreateAccount(CreateAccountData(name.text.trim(), currency, color, icon, target, include, session), acknowledged)
            } else {
                onEditAccount(account.copy(name = name.text.trim(), currency = currency, color = color.toArgb(),
                    icon = icon, includeInBalance = include), target, acknowledged)
            }
        }, onSuccess = { if (latestId == session) dismiss() })
    }
    LaunchedEffect(modal?.id) {
        if (modal?.adjustBalanceMode == true && !adjustPickerOpened && editableNow()) {
            adjustPickerOpened = true
            amountVisible = true
        }
    }
    IvyModal(
        id = modal?.id, visible = modal != null,
        title = stringResource(if (account == null) R.string.new_account else R.string.edit_account),
        hasUnsavedChanges = !terminalConflict && (name.text != account?.name.orEmpty() || amount != (modal?.balance ?: 0.0) ||
            currency != (account?.currency ?: modal?.baseCurrency.orEmpty()) || color.toArgb() != (account?.color ?: Ivy.toArgb()) ||
            icon != account?.icon || include != (account?.includeInBalance ?: true)),
        dismiss = dismiss, dismissEnabled = !submission.saving, shiftIfKeyboardShown = false,
        PrimaryAction = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                if (submission.saving) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.jvy_saving), Modifier.fillMaxWidth().padding(vertical = 8.dp), style = JvyTypography.Supporting)
                }
                submission.error?.let {
                    Text(it, Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                        color = MaterialTheme.colorScheme.error, style = JvyTypography.Supporting)
                }
                if (terminalConflict) JvyButton(dismiss) { Text(stringResource(R.string.done)) }
                else ModalAddSave(item = account, enabled = editable && valid(amount)) { submit(amount) }
            }
        }
    ) {
        IconNameRow(hint = stringResource(R.string.account_name), defaultIcon = R.drawable.ic_custom_account_m,
            color = color, icon = icon, autoFocusKeyboard = modal?.autoFocusKeyboard ?: true,
            nameTextFieldValue = name, setNameTextFieldValue = { if (editableNow()) name = it },
            showChooseIconModal = { if (editableNow()) iconVisible = true }, enabled = editable)
        Spacer(Modifier.height(16.dp))
        IvyColorPicker(selectedColor = color, enabled = editable, onColorSelected = { if (editableNow()) color = it })
        Spacer(Modifier.height(16.dp))
        ModalAmountSection(
            label = stringResource(R.string.enter_account_balance).uppercase(), currency = currency, amount = amount,
            amountPaddingTop = 16.dp, amountPaddingBottom = 16.dp, enabled = editable,
            Header = {
                Spacer(Modifier.height(16.dp))
                JvyOutlinedButton({ currencyVisible = true }, Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    .testTag("account_modal_currency"), enabled = editable) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(currency.uppercase(), style = JvyTypography.AmountSmall)
                        IvyCurrency.fromCode(currency)?.name?.takeIf(String::isNotEmpty)?.let {
                            Text(it, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                IvyCheckboxWithText(text = stringResource(R.string.include_account), checked = include,
                    modifier = Modifier.padding(start = 16.dp).align(Alignment.Start), enabled = editable) { if (editableNow()) include = it }
            }
        ) { if (editableNow()) amountVisible = true }
        if (modal?.forceNonZeroBalance == true) {
            Text("This account needs a balance greater than zero.", Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val amountId = rememberSaveable(key) { UUID.randomUUID().toString() }
    AmountModal(id = UUID.fromString(amountId), visible = amountVisible, currency = currency, initialAmount = amount,
        showPlusMinus = true, dismiss = { amountVisible = false }) {
        amountVisible = false
        if (editableNow()) {
            amount = it
            if (modal?.adjustBalanceMode == true) submit(it)
        }
    }
    CurrencyModal(title = stringResource(R.string.choose_currency), initialCurrency = IvyCurrency.fromCode(currency),
        visible = currencyVisible, dismiss = { currencyVisible = false }) { if (editableNow()) currency = it }
    ChooseIconModal(visible = iconVisible, initialIcon = icon ?: "account", color = color,
        dismiss = { iconVisible = false }) { if (editableNow()) icon = it }
}

@Preview @Composable private fun Preview() {
    IvyWalletPreview {
        AccountModal(AccountModalData(null, "BGN", 0.0), { _, _ -> }, { _, _, _ -> }, {})
    }
}
