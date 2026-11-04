package com.ivy.exchangerates.modal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.Orange
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.l1_buildingBlocks.SpacerVer
import com.ivy.exchangerates.RatesEvent
import com.ivy.exchangerates.data.RateUi
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.wallet.ui.theme.components.IvyNameTextField
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalAdd
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import java.util.UUID

@Composable
fun BoxWithConstraintsScope.AddRateModal(
    visible: Boolean,
    baseCurrency: String,
    dismiss: () -> Unit,
    onAdd: (RatesEvent.AddRate) -> Unit,
) {
    if (!visible) return
    var toCurrency by rememberSaveable(baseCurrency, stateSaver = TextFieldValue.Saver) { mutableStateOf(selectEndTextFieldValue("")) }
    var amountModalVisible by remember { mutableStateOf(false) }
    var rate by rememberSaveable(baseCurrency) { mutableStateOf<Double?>(null) }
    val submission = remember { com.ivy.design.system.EditorSubmission() }
    val valid = validManualRate(baseCurrency, toCurrency.text, rate)

    IvyModal(
        id = null,
        visible = visible,
        title = "Add rate",
        dismiss = dismiss,
        dismissEnabled = !submission.saving,
        hasUnsavedChanges = toCurrency.text.isNotBlank() || rate != null,
        PrimaryAction = {
            ModalAdd(enabled = valid && !submission.saving) {
                val to = toCurrency
                val finalRate = rate
                submission.submit({ complete -> onAdd(
                    RatesEvent.AddRate(
                        RateUi(
                            from = baseCurrency,
                            to = to.text.trim().uppercase(java.util.Locale.ROOT),
                            rate = checkNotNull(finalRate),
                        ), onResult = complete
                    )
                ) }, onSuccess = dismiss)
            }
        }
    ) {
        IvyNameTextField(
            modifier = Modifier.padding(horizontal = 24.dp),
            underlineModifier = Modifier.padding(horizontal = 24.dp),
            value = toCurrency,
            hint = "Currency"
        ) {
            toCurrency = it
        }

        SpacerVer(height = 12.dp)
        if (!valid && (toCurrency.text.isNotBlank() || rate != null)) Text(
            androidx.compose.ui.res.stringResource(com.ivy.ui.R.string.jvy_rate_validation),
            Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.error)
        submission.error?.let { Text(it, Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.error) }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    amountModalVisible = true
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            text = "$baseCurrency-${toCurrency.text} = ${rate ?: "???"}",
            style = UI.typo.nH2.style(
                color = Orange,
                textAlign = TextAlign.Center,
            )
        )
    }

    AmountModal(
        id = remember { UUID.randomUUID() },
        visible = amountModalVisible,
        currency = "",
        initialAmount = rate,
        decimalCountMax = 12,
        dismiss = { amountModalVisible = false },
        onAmountChanged = {
            rate = it
        }
    )
}

internal fun validManualRate(base: String, target: String, rate: Double?): Boolean =
    com.ivy.data.model.primitive.AssetCode.from(target.trim().uppercase(java.util.Locale.ROOT)).isRight() &&
        !base.equals(target.trim(), ignoreCase = true) && rate != null && rate.isFinite() && rate > 0.0

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        AddRateModal(
            visible = true,
            baseCurrency = "USD",
            dismiss = {},
            onAdd = {},
        )
    }
}
