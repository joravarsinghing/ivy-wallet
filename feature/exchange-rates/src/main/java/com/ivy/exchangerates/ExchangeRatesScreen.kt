package com.ivy.exchangerates

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.l1_buildingBlocks.ColumnRoot
import com.ivy.design.l1_buildingBlocks.DividerW
import com.ivy.design.l1_buildingBlocks.SpacerHor
import com.ivy.design.l1_buildingBlocks.SpacerVer
import com.ivy.exchangerates.component.RateItem
import com.ivy.exchangerates.data.RateUi
import com.ivy.exchangerates.modal.AddRateModal
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ui.SearchInput
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.navigation.navigation
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Composable
fun BoxWithConstraintsScope.ExchangeRatesScreen() {
    val viewModel: ExchangeRatesViewModel = viewModel()
    val state = viewModel.uiState()

    ExchangeRatesContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun BoxWithConstraintsScope.ExchangeRatesContent(
    state: RatesState,
    onEvent: (RatesEvent) -> Unit,
) {
    val nav = navigation()
    var amountModalVisible by remember {
        mutableStateOf(false)
    }
    var rateToUpdate by remember {
        mutableStateOf<RateUi?>(null)
    }
    var amountModalId by remember {
        mutableStateOf(UUID.randomUUID())
    }
    val onRateClick = { rate: RateUi ->
        rateToUpdate = rate
        amountModalId = UUID.randomUUID()
        amountModalVisible = true
    }

    var addRateModalVisible by remember { mutableStateOf(false) }
    com.ivy.design.system.JvySubsectionScaffold(stringResource(R.string.exchange_rates), { nav.back() }, footer = {
        com.ivy.design.system.JvyPrimaryAction(stringResource(R.string.add_manual_exchange_rate), { addRateModalVisible = true })
    }) {
      ColumnRoot(statusBarPadding = false, navigationBarsPadding = false) {
        SpacerVer(height = 16.dp)
        SearchField(onSearch = { onEvent(RatesEvent.Search(it)) })
        SpacerVer(height = 4.dp)
        LazyColumn(Modifier.weight(1f)) {
            item(key = "freshness", contentType = "status") {
                androidx.compose.material3.Surface(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    shape = com.ivy.design.system.JvyShapes.Card, color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        Text("Exchange-rate lookup", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        Text(com.ivy.domain.usecase.exchange.ExchangeRateFreshness.SOURCE, style = com.ivy.design.system.JvyTypography.Supporting)
                        Text(rateFreshnessLabel(state.providerDate), style = com.ivy.design.system.JvyTypography.Supporting)
                        val checked = state.checkedAt?.let { runCatching {
                            java.time.Instant.parse(it).atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT))
                        }.getOrNull() }
                        Text(checked?.let { "Last successful refresh: $it" } ?: "No verified refresh recorded for ${state.baseCurrency}.", style = com.ivy.design.system.JvyTypography.Supporting)
                        Text("Daily reference rates, not live bank quotes. Fees and spreads may differ. Manual overrides are kept when refreshing.", style = com.ivy.design.system.JvyTypography.Supporting)
                        state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, style = com.ivy.design.system.JvyTypography.Supporting) }
                        com.ivy.design.system.JvyOutlinedButton({ onEvent(RatesEvent.Refresh) }, enabled = !state.refreshing) {
                            Text(if (state.refreshing) "Refreshing…" else "Refresh rates")
                        }
                    }
                }
            }
            ratesSection(text = "Manual")
            items(items = state.manual, key = { "manual-${it.from}-${it.to}" }, contentType = { "rate" }) { rate ->
                SpacerVer(height = 4.dp)
                RateItem(
                    rate = rate,
                    onDelete = { onEvent(RatesEvent.RemoveOverride(rate)) },
                    onClick = { onRateClick(rate) }
                )
            }
            ratesSection(text = "Automatic")
            items(items = state.automatic, key = { "automatic-${it.from}-${it.to}" }, contentType = { "rate" }) { rate ->
                SpacerVer(height = 4.dp)
                RateItem(
                    rate = rate,
                    onDelete = null,
                    onClick = { onRateClick(rate) }
                )
            }
            item(key = "last_item_spacer") {
                SpacerVer(height = 16.dp)
            }
        }
    }

    }

    AddRateModal(
        visible = addRateModalVisible,
        baseCurrency = state.baseCurrency,
        dismiss = {
            addRateModalVisible = false
        },
        onAdd = onEvent
    )

    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = "",
        initialAmount = rateToUpdate?.rate,
        dismiss = {
            amountModalVisible = false
        },
        decimalCountMax = 12,
        Header = {
            rateToUpdate?.let {
                SpacerVer(height = 24.dp)
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = "${it.from}-${it.to}",
                    style = UI.typo.nH2.style(
                        textAlign = TextAlign.Center,
                        color = UI.colors.primary
                    )
                )
            }
        },
        onAmountChanged = { newRate ->
            rateToUpdate?.let {
                onEvent(RatesEvent.UpdateRate(rateToUpdate!!, newRate))
            }
        }
    )
}

private fun LazyListScope.ratesSection(
    text: String
) {
    item {
        SpacerVer(height = 24.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            DividerW()
            SpacerHor(width = 16.dp)
            Text(
                text = text,
                style = UI.typo.h2
            )
            SpacerHor(width = 16.dp)
            DividerW()
        }
    }
}

@Composable
private fun SearchField(
    onSearch: (String) -> Unit,
) {
    var searchQueryTextFieldValue by remember {
        mutableStateOf(selectEndTextFieldValue(""))
    }

    SearchInput(
        searchQueryTextFieldValue = searchQueryTextFieldValue,
        hint = "Search currency",
        focus = false,
        showClearIcon = searchQueryTextFieldValue.text.isNotEmpty(),
        onSetSearchQueryTextField = {
            searchQueryTextFieldValue = it
            onSearch(it.text)
        }
    )
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        ExchangeRatesContent(
            state = RatesState(
                baseCurrency = "BGN",
                manual = persistentListOf(
                    RateUi("BGN", "USD", 1.85),
                    RateUi("BGN", "EUR", 1.96),
                ),
                automatic = persistentListOf(
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                    RateUi("XXX", "YYY", 1.23),
                )
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@Composable
fun ExchangeRatesScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}
