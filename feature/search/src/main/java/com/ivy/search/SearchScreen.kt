package com.ivy.search

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.design.utils.IvyComponentPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.ui.SearchInput
import com.ivy.legacy.ui.component.transaction.transactions
import com.ivy.legacy.utils.densityScope
import com.ivy.legacy.utils.keyboardOnlyWindowInsets
import com.ivy.legacy.utils.keyboardVisibleState
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.screenScopedViewModel
import com.ivy.navigation.navigation
import com.ivy.design.system.JvyScreenHeader
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.modal.DURATION_MODAL_ANIM
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SearchScreen(screen: SearchScreen) {
    val viewModel: SearchViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    val entry = navigation().currentEntryId
    val saved = com.ivy.design.system.rememberProcessRestoration("search-query") {
        if (viewModel.readyEntry == entry) viewModel.saveQuery() else null
    }
    LaunchedEffect(entry) { viewModel.start(entry, saved.payload); saved.consumed = true }
    if (viewModel.readyEntry != entry) {
        val nav = navigation()
        com.ivy.design.system.JvyEditorLoading(stringResource(R.string.search_transactions).removeSuffix("..."), false, { nav.back() }, {})
        return
    }

    SearchUi(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
internal fun SearchUi(
    uiState: SearchState,
    onEvent: (SearchEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        val nav = navigation()
        JvyScreenHeader(stringResource(R.string.search_transactions).removeSuffix("..."), { nav.back() })

        val listState = rememberLazyListState()

        var searchQueryTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(selectEndTextFieldValue(uiState.searchQuery))
        }

        SearchInput(
            searchQueryTextFieldValue = searchQueryTextFieldValue,
            hint = stringResource(R.string.search_transactions),
            showClearIcon = searchQueryTextFieldValue.text.isNotEmpty(),
            onSetSearchQueryTextField = {
                searchQueryTextFieldValue = it
                onEvent(SearchEvent.Search(it.text))
            }
        )

        var lastQuery by rememberSaveable { mutableStateOf(searchQueryTextFieldValue.text) }
        LaunchedEffect(searchQueryTextFieldValue.text) {
            if (lastQuery != searchQueryTextFieldValue.text) {
                lastQuery = searchQueryTextFieldValue.text
                listState.scrollToItem(index = 0, scrollOffset = 0)
            }
        }

        if (uiState.loading) androidx.compose.material3.LinearProgressIndicator()
        if (uiState.failed) {
            Column(Modifier.fillMaxWidth().padding(horizontal = com.ivy.design.system.JvySpacing.PageGutter)) {
            androidx.compose.material3.Text(stringResource(com.ivy.search.R.string.search_load_failed),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                style = com.ivy.design.system.JvyTypography.Supporting)
            com.ivy.design.system.JvyTextButton({ onEvent(SearchEvent.Search(searchQueryTextFieldValue.text)) }) {
                androidx.compose.material3.Text(stringResource(com.ivy.search.R.string.search_retry))
            }
            }
        }

        Spacer(Modifier.height(16.dp))
        val emptyStateTitle = stringResource(R.string.no_transactions)
        val emptyStateText = stringResource(
            R.string.no_transactions_for_query,
            searchQueryTextFieldValue.text
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState

        ) {
            if (!uiState.loading && !uiState.failed && uiState.resultsMatchQuery && uiState.transactions.isNotEmpty()) item(key = "search-activity-chart", contentType = "chart") {
                com.ivy.legacy.ui.chart.ContextualActivityChart("search-activity", uiState.transactions, uiState.accounts, uiState.baseCurrency)
            }
            if (!uiState.failed && (!uiState.loading || (uiState.resultsMatchQuery && uiState.transactions.isNotEmpty()))) transactions(
                baseData = AppBaseData(
                    baseCurrency = uiState.baseCurrency,
                    accounts = uiState.accounts,
                    categories = uiState.categories
                ),
                upcoming = null,
                setUpcomingExpanded = { },
                overdue = null,
                setOverdueExpanded = { },
                history = uiState.transactions,
                onPayOrGet = { },
                emptyStateTitle = emptyStateTitle,
                emptyStateText = emptyStateText,
                dateDividerMarginTop = 16.dp,
                shouldShowAccountSpecificColorInTransactions = uiState.shouldShowAccountSpecificColorInTransactions
            )

            item {
                val keyboardVisible by keyboardVisibleState()
                val keyboardShownInsetDp by animateDpAsState(
                    targetValue = densityScope {
                        if (keyboardVisible) keyboardOnlyWindowInsets().bottom.toDp() else 0.dp
                    },
                    animationSpec = tween(DURATION_MODAL_ANIM)
                )

                Spacer(Modifier.height(keyboardShownInsetDp))
                // add keyboard height margin at bototm so the list can scroll to bottom
            }
        }
    }
}

@Preview
@Composable
private fun Preview(isDark: Boolean = false) {
    IvyPreview(isDark) {
        SearchUi(
            uiState = SearchState(
                searchQuery = "",
                transactions = persistentListOf(),
                baseCurrency = "",
                accounts = persistentListOf(),
                categories = persistentListOf(),
                shouldShowAccountSpecificColorInTransactions = false
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchUiTest(isDark: Boolean) {
    val theme = if (isDark) Theme.DARK else Theme.LIGHT
    IvyComponentPreview(theme = theme) {
        Preview(isDark)
    }
}
