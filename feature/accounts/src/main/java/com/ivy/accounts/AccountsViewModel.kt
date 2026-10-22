package com.ivy.accounts

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.datastore.IvyDataStore
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.GoalRepository
import com.ivy.domain.features.Features
import com.ivy.domain.usecase.networth.NetWorthAccountClassification
import com.ivy.domain.usecase.networth.NetWorthAccountRole
import com.ivy.domain.usecase.networth.NetWorthCalculator
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.data.model.toCloseTimeRange
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.ioThread
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.R
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.viewmodel.account.AccountDataAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@Stable
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AccountsViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val ivyContext: IvyWalletCtx,
    private val sharedPrefs: SharedPrefs,
    private val calcWalletBalanceAct: CalcWalletBalanceAct,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val accountDataAct: AccountDataAct,
    private val accountRepository: AccountRepository,
    private val goalRepository: GoalRepository,
    private val dataObserver: DataObserver,
    private val features: Features,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val dataStore: IvyDataStore,
    private val wealthWorkspace: com.ivy.domain.usecase.networth.WealthWorkspaceUseCase,
) : ComposeViewModel<AccountsState, AccountsEvent>() {
    private val netWorthAssetsKey = stringSetPreferencesKey("net_worth_asset_account_ids")
    private val netWorthLiabilitiesKey = stringSetPreferencesKey("net_worth_liability_account_ids")
    private var baseCurrency by mutableStateOf("")
    private var accountsData by mutableStateOf(listOf<AccountData>())
    private var totalBalanceWithExcluded by mutableStateOf("")
    private var totalBalanceWithExcludedText by mutableStateOf("")
    private var totalBalanceWithoutExcluded by mutableStateOf("")
    private var totalBalanceWithoutExcludedText by mutableStateOf("")
    private var reorderVisible by mutableStateOf(false)
    private var netWorth by mutableStateOf(NetWorthUiState(accounts = emptyList<NetWorthAccountUi>().toImmutableList()))

    init {
        viewModelScope.launch {
            dataObserver.writeEvents.collectLatest { event ->
                when (event) {
                    is DataWriteEvent.AccountChange -> {
                        onStart()
                    }

                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }

    @Composable
    override fun uiState(): AccountsState {
        LaunchedEffect(Unit) {
            onStart()
        }

        return AccountsState(
            baseCurrency = getBaseCurrency(),
            accountsData = getAccountsData(),
            totalBalanceWithExcluded = getTotalBalanceWithExcluded(),
            totalBalanceWithExcludedText = getTotalBalanceWithExcludedText(),
            totalBalanceWithoutExcluded = getTotalBalanceWithoutExcluded(),
            totalBalanceWithoutExcludedText = getTotalBalanceWithoutExcludedText(),
            reorderVisible = getReorderVisible(),
            compactAccountsModeEnabled = getCompactAccountsMode(),
            hideTotalBalance = getHideTotalBalance(),
            netWorth = netWorth,
        )
    }

    @Composable
    private fun getHideTotalBalance(): Boolean {
        return features.hideTotalBalance.asEnabledState()
    }

    @Composable
    private fun getBaseCurrency(): String {
        return baseCurrency
    }

    @Composable
    private fun getAccountsData(): ImmutableList<AccountData> {
        return accountsData.toImmutableList()
    }

    @Composable
    private fun getTotalBalanceWithExcluded(): String {
        return totalBalanceWithExcluded
    }

    @Composable
    private fun getTotalBalanceWithExcludedText(): String {
        return totalBalanceWithExcludedText
    }

    @Composable
    private fun getTotalBalanceWithoutExcluded(): String {
        return totalBalanceWithoutExcluded
    }

    @Composable
    private fun getTotalBalanceWithoutExcludedText(): String {
        return totalBalanceWithoutExcludedText
    }

    @Composable
    private fun getReorderVisible(): Boolean {
        return reorderVisible
    }

    @Composable
    private fun getCompactAccountsMode(): Boolean {
        return features.compactAccountsMode.asEnabledState()
    }

    override fun onEvent(event: AccountsEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is AccountsEvent.OnReorder -> reorder(event.reorderedList)
                is AccountsEvent.OnReorderModalVisible -> reorderModalVisible(event.reorderVisible)
                is AccountsEvent.OnNetWorthRoleChanged -> setNetWorthRole(event.accountId, event.role)
            }
        }
    }

    private suspend fun reorder(newOrder: List<AccountData>) {
        ioThread {
            newOrder.mapIndexed { index, accountData ->
                accountRepository.save(accountData.account.copy(orderNum = index.toDouble()))
            }
        }

        startInternally()
    }

    private fun onStart() {
        viewModelScope.launch(Dispatchers.Default) {
            startInternally()
        }
    }

    private suspend fun startInternally() {
        val period = com.ivy.legacy.data.model.TimePeriod.currentMonth(
            startDayOfMonth = ivyContext.startDayOfMonth
        ) // this must be monthly
        val range = period.toRange(ivyContext.startDayOfMonth, timeConverter, timeProvider)

        val baseCurrencyCode = baseCurrencyAct(Unit)
        val accounts = accountRepository.findAll().toImmutableList()

        val includeTransfersInCalc =
            sharedPrefs.getBoolean(SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE, false)

        val accountsDataList = accountDataAct(
            AccountDataAct.Input(
                accounts = accounts,
                range = range.toCloseTimeRange(),
                baseCurrency = baseCurrencyCode,
                includeTransfersInCalc = includeTransfersInCalc
            )
        )

        val legacyRoles = dataStore.data.first()
        val legacyAssets = legacyRoles[netWorthAssetsKey].orEmpty().mapNotNull { it.toUuidOrNull() }.toSet()
        val legacyLiabilities = legacyRoles[netWorthLiabilitiesKey].orEmpty().mapNotNull { it.toUuidOrNull() }.toSet()
        val assetIds = sharedPrefs.getStringList(SharedPrefs.NET_WORTH_ASSET_ACCOUNT_IDS)
            .mapNotNull { it.toUuidOrNull() }
            .toSet()
            .ifEmpty { legacyAssets }
        val liabilityIds = sharedPrefs.getStringList(SharedPrefs.NET_WORTH_LIABILITY_ACCOUNT_IDS)
            .mapNotNull { it.toUuidOrNull() }
            .toSet()
            .ifEmpty { legacyLiabilities }
        if (legacyAssets.isNotEmpty() || legacyLiabilities.isNotEmpty()) {
            persistNetWorthRoles(assetIds, liabilityIds)
        }
        baseCurrency = baseCurrencyCode
        val totalBalanceWithExcludedAccounts = calcWalletBalanceAct(
            CalcWalletBalanceAct.Input(
                baseCurrency = baseCurrencyCode,
                withExcluded = true
            )
        ).toDouble()

        val totalBalanceWithoutExcludedAccounts = calcWalletBalanceAct(
            CalcWalletBalanceAct.Input(
                baseCurrency = baseCurrencyCode
            )
        ).toDouble()

        val reservedByAccount = buildMap {
            goalRepository.findActiveGoals().forEach { goal ->
                goalRepository.findAllocations(goal.id).forEach { allocation ->
                    put(
                        allocation.accountId,
                        (get(allocation.accountId) ?: 0.0) + allocation.reservedAmount
                    )
                }
            }
        }

        accountsData = accountsDataList.map { accountData ->
            accountData.copy(
                reservedForGoals = reservedByAccount[accountData.account.id.value] ?: 0.0
            )
        }
        totalBalanceWithExcluded = totalBalanceWithExcludedAccounts.toString()
        totalBalanceWithExcludedText = context.getString(
            R.string.total,
            baseCurrencyCode,
            totalBalanceWithExcludedAccounts.format(
                baseCurrencyCode
            )
        )
        totalBalanceWithoutExcluded = totalBalanceWithoutExcludedAccounts.toString()
        totalBalanceWithoutExcludedText = context.getString(
            R.string.total_exclusive,
            baseCurrencyCode,
            totalBalanceWithoutExcludedAccounts.format(
                baseCurrencyCode
            )
        )
        // An optional wealth calculation must never prevent the ledger/accounts from loading.
        try {
            val wealth = wealthWorkspace.load(timeProvider.localDateNow(), baseCurrencyCode)
            netWorth = NetWorthUiState(
                assets = wealth.current.assets,
                liabilities = wealth.current.liabilities,
                accounts = emptyList<NetWorthAccountUi>().toImmutableList(),
                setupIssues = wealth.current.issues.size,
                hasWealthItems = wealth.current.lines.isNotEmpty(),
            )
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            netWorth = NetWorthUiState(accounts = emptyList<NetWorthAccountUi>().toImmutableList(), setupIssues = 1)
        }
    }

    private fun reorderModalVisible(visible: Boolean) {
        reorderVisible = visible
    }

    private suspend fun setNetWorthRole(accountId: UUID, role: NetWorthAccountRole?) {
        dataStore.edit { preferences ->
            val assets = preferences[netWorthAssetsKey].orEmpty().toMutableSet()
            val liabilities = preferences[netWorthLiabilitiesKey].orEmpty().toMutableSet()
            assets.remove(accountId.toString())
            liabilities.remove(accountId.toString())
            when (role) {
                NetWorthAccountRole.ASSET -> assets += accountId.toString()
                NetWorthAccountRole.LIABILITY -> liabilities += accountId.toString()
                null -> Unit
            }
            preferences[netWorthAssetsKey] = assets
            preferences[netWorthLiabilitiesKey] = liabilities
        }
        val savedRoles = dataStore.data.first()
        val assets = savedRoles[netWorthAssetsKey].orEmpty().mapNotNull { it.toUuidOrNull() }.toSet()
        val liabilities = savedRoles[netWorthLiabilitiesKey].orEmpty().mapNotNull { it.toUuidOrNull() }.toSet()
        persistNetWorthRoles(assets, liabilities)
        updateNetWorth(accountsData, assets, liabilities)
        saveNetWorthSnapshot()
    }

    private fun SharedPrefs.getStringList(key: String): List<String> =
        getString(key, null)
            ?.split(",")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?: emptyList()

    private fun persistNetWorthRoles(assets: Set<UUID>, liabilities: Set<UUID>) {
        sharedPrefs.putString(SharedPrefs.NET_WORTH_ASSET_ACCOUNT_IDS, assets.joinToString(","))
        sharedPrefs.putString(SharedPrefs.NET_WORTH_LIABILITY_ACCOUNT_IDS, liabilities.joinToString(","))
    }

    private fun saveNetWorthSnapshot() {
        if (netWorth.accounts.isEmpty() || netWorth.unclassifiedCount > 0) return
        val period = timeProvider.localDateNow().toString().substring(0, 7)
        val current = "${period}|${netWorth.assets.toDouble()}|${netWorth.liabilities.toDouble()}"
        val existing = sharedPrefs.getString(SharedPrefs.NET_WORTH_HISTORY, null)
            ?.split(";")
            ?.filter { it.isNotBlank() && !it.startsWith("$period|") }
            .orEmpty()
        sharedPrefs.putString(
            SharedPrefs.NET_WORTH_HISTORY,
            (existing + current).takeLast(12).joinToString(";")
        )
    }

    private fun updateNetWorth(
        data: List<AccountData>,
        assetIds: Set<UUID>,
        liabilityIds: Set<UUID>,
    ) {
        val accounts = data.map { accountData ->
            val id = accountData.account.id.value
            NetWorthAccountUi(
                id = id,
                name = accountData.account.name.value,
                balance = BigDecimal.valueOf(
                    accountData.balanceBaseCurrency
                        ?: if (accountData.account.asset.code == baseCurrency) accountData.balance else 0.0
                ),
                role = when {
                    id in assetIds -> NetWorthAccountRole.ASSET
                    id in liabilityIds -> NetWorthAccountRole.LIABILITY
                    else -> null
                },
            )
        }.toImmutableList()
        val classifications = accounts.mapNotNull { account ->
            account.role?.let { NetWorthAccountClassification(com.ivy.data.model.AccountId(account.id), it) }
        }
        val balances = accounts.associate { com.ivy.data.model.AccountId(it.id) to it.balance }
        val snapshot = NetWorthCalculator.calculate(balances, classifications)
        val history = sharedPrefs.getString(SharedPrefs.NET_WORTH_HISTORY, null)
            ?.split(";")
            ?.mapNotNull { value ->
                val parts = value.split("|")
                if (parts.size != 3) return@mapNotNull null
                val assets = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                val liabilities = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                NetWorthHistoryPoint(parts[0], assets - liabilities)
            }
            .orEmpty()
            .toImmutableList()
        netWorth = NetWorthUiState(snapshot.assets, snapshot.liabilities, accounts, history)
    }
}

private fun String.toUuidOrNull(): UUID? =
    runCatching { UUID.fromString(this) }.getOrNull()
