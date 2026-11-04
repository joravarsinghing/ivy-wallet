package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.design.system.JvyTypography
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.ui.theme.components.CurrencyPicker
import java.util.UUID

@Deprecated("Old design system adapter. Uses shared Jvy controls.")
@Composable
fun BoxWithConstraintsScope.CurrencyModal(
    title: String,
    initialCurrency: IvyCurrency?,
    visible: Boolean,
    dismiss: () -> Unit,
    id: UUID? = null,
    onSetCurrency: (String) -> Unit
) {
    if (!visible) return
    val localId = rememberSaveable { UUID.randomUUID().toString() }
    val sessionId = id?.toString() ?: localId
    var currencyCode by rememberSaveable(sessionId) {
        mutableStateOf((initialCurrency ?: IvyCurrency.getDefault()).code)
    }
    val currency = IvyCurrency.fromCode(currencyCode) ?: IvyCurrency.getDefault()
    IvyModal(
        id = UUID.fromString(sessionId),
        visible = true,
        title = title,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSave(modifier = Modifier.testTag("set_currency_save")) {
                onSetCurrency(currency.code)
                dismiss()
            }
        },
        scrollState = null,
    ) {
        Text(stringResource(R.string.supports_crypto),
            Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CurrencyPicker(
            modifier = Modifier.weight(1f),
            initialSelectedCurrency = currency,
            includeKeyboardShownInsetSpacer = false,
        ) { currencyCode = it.code }
    }
}
