package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.system.JvyShapes
import com.ivy.design.system.JvyTypography
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency

/**
 * Shared selector for onboarding and modal callers. The containing shell owns IME insets.
 * Retain the old signature while removing screen-height/keyboard-listener positioning.
 */
@Deprecated("Old design system adapter. Shared Material controls use the Jvy design language.")
@Suppress("ComposeParameterOrder", "UNUSED_PARAMETER")
@Composable
fun CurrencyPicker(
    modifier: Modifier = Modifier,
    initialSelectedCurrency: IvyCurrency?,
    preselectedCurrency: IvyCurrency = IvyCurrency.getDefault(),
    includeKeyboardShownInsetSpacer: Boolean,
    lastItemSpacer: Dp = 0.dp,
    onKeyboardShown: (keyboardVisible: Boolean) -> Unit = {},
    onSelectedCurrencyChanged: (IvyCurrency) -> Unit
) {
    var selectedCode by rememberSaveable(initialSelectedCurrency?.code, preselectedCurrency.code) {
        mutableStateOf((initialSelectedCurrency ?: preselectedCurrency).code)
    }
    var query by rememberSaveable { mutableStateOf("") }
    val selected = IvyCurrency.fromCode(selectedCode) ?: preselectedCurrency
    val currencies = remember(query) {
        IvyCurrency.getAvailable()
            .filter { query.isBlank() || it.code.contains(query.trim(), ignoreCase = true) ||
                it.name.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareBy<IvyCurrency> { it.isCrypto }.thenBy { it.code })
    }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp,
            bottom = lastItemSpacer.coerceAtLeast(16.dp)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("search") {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_currency)) },
                modifier = Modifier.fillMaxWidth().testTag("search_input"),
                textStyle = JvyTypography.Body, singleLine = true, shape = JvyShapes.Control,
            )
        }
        item("selection") {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${stringResource(R.string.selected_text)} · ${selected.code}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(selected.name, style = JvyTypography.Supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (currencies.isEmpty()) item("empty") {
            Text(stringResource(R.string.jvy_currency_no_results),
                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(currencies, key = { it.code }) { currency ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(selected = selectedCode == currency.code, role = Role.RadioButton,
                        onClick = {
                            selectedCode = currency.code
                            onSelectedCurrencyChanged(currency)
                        })
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(selected = selectedCode == currency.code, onClick = null)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(currency.code, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(currency.name, style = JvyTypography.Supporting,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
