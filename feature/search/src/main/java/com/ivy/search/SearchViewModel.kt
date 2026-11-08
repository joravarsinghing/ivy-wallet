package com.ivy.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.ui.ComposeViewModel
import com.ivy.data.model.Category
import com.ivy.data.repository.CategoryRepository
import com.ivy.domain.features.Features
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.utils.getDefaultFIATCurrency
import com.ivy.base.threading.DispatchersProvider
import com.ivy.design.system.ProcessDraftState
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.util.UUID
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.transaction.AllTrnsAct
import com.ivy.wallet.domain.action.transaction.TrnsWithDateDivsAct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val trnsWithDateDivsAct: TrnsWithDateDivsAct,
    private val accountsAct: AccountsAct,
    private val categoryRepository: CategoryRepository,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val allTrnsAct: AllTrnsAct,
    private val features: Features,
    private val dispatchers: DispatchersProvider,
) : ComposeViewModel<SearchState, SearchEvent>() {

    private val transactions =
        mutableStateOf<ImmutableList<TransactionHistoryItem>>(persistentListOf())
    private val baseCurrency = mutableStateOf<String>(getDefaultFIATCurrency().currencyCode)
    private val accounts = mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private val categories = mutableStateOf<ImmutableList<Category>>(persistentListOf())
    private val searchQuery = mutableStateOf("")
    private val loading = mutableStateOf(false)
    private val failed = mutableStateOf(false)
    private val completedQuery = mutableStateOf<String?>(null)
    private var searchJob: Job? = null
    private var generation = 0L
    private val startedEntry = mutableStateOf<UUID?>(null)
    val readyEntry: UUID? get() = startedEntry.value

    fun saveQuery(): List<String>? = ProcessDraftState.encode(mapOf("query" to searchQuery.value))
    fun start(entry: UUID?, restored: List<String>?) {
        if (entry == null) return
        if (startedEntry.value == entry) {
            if (searchJob?.isActive != true) search(searchQuery.value)
            return
        }
        val query = ProcessDraftState.decode(restored)?.get("query")?.takeIf { it.length <= 4096 }.orEmpty()
        search(query)
        startedEntry.value = entry
    }

    @Composable
    fun getShouldShowAccountSpecificColorInTransactions(): Boolean {
        return features.showAccountColorsInTransactions.asEnabledState()
    }

    @Composable
    override fun uiState(): SearchState {
        return currentState(getShouldShowAccountSpecificColorInTransactions())
    }

    internal fun currentState(showAccountColors: Boolean = false): SearchState {
        return SearchState(
            searchQuery = searchQuery.value,
            transactions = transactions.value,
            baseCurrency = baseCurrency.value,
            accounts = accounts.value,
            categories = categories.value,
            shouldShowAccountSpecificColorInTransactions = showAccountColors,
            loading = loading.value, failed = failed.value,
            resultsMatchQuery = completedQuery.value == searchQuery.value,
        )
    }

    override fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.Search -> search(event.query)
        }
    }

    private fun search(query: String) {
        searchQuery.value = query
        val normalizedQuery = query.lowercase().trim()
        val request = ++generation
        searchJob?.cancel()
        loading.value = true
        failed.value = false
        searchJob = viewModelScope.launch {
            try {
            val result = withContext(dispatchers.io) {
                val currency = baseCurrencyAct(Unit)
                val filteredTransactions = allTrnsAct(Unit)
                    .filter { transaction ->
                        normalizedQuery.isEmpty() || transaction.title.matchesQuery(normalizedQuery) ||
                                transaction.description.matchesQuery(normalizedQuery)
                    }
                val history = trnsWithDateDivsAct(
                    TrnsWithDateDivsAct.Input(
                        baseCurrency = currency,
                        transactions = filteredTransactions
                    )
                ).toImmutableList()
                SearchState(query, history, currency, accountsAct(Unit), categoryRepository.findAll().toImmutableList(), false)
            }
            if (request == generation) {
                completedQuery.value = query
                transactions.value = result.transactions
                baseCurrency.value = result.baseCurrency
                accounts.value = result.accounts
                categories.value = result.categories
            }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (request == generation) failed.value = true }
            finally { if (request == generation) loading.value = false }
        }
    }

    private fun NotBlankTrimmedString?.matchesQuery(query: String): Boolean {
        return this?.value?.lowercase()?.contains(query) == true
    }
}
