package com.ivy.loans.loan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.model.processByType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.read.LoanRecordDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.model.LoanType
import com.ivy.frp.test.TestIdlingResource
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.domain.deprecated.logic.AccountCreator
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.getDefaultFIATCurrency
import com.ivy.legacy.utils.ioThread
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.time.impl.DateTimePicker
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.loan.LoansAct
import com.ivy.wallet.domain.deprecated.logic.LoanCreator
import com.ivy.wallet.domain.deprecated.logic.loantrasactions.LoanTransactionsLogic
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanData
import com.ivy.wallet.ui.theme.modal.LoanModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@Stable
@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRecordDao: LoanRecordDao,
    private val settingsDao: SettingsDao,
    private val loanCreator: LoanCreator,
    private val sharedPrefs: SharedPrefs,
    private val accountCreator: AccountCreator,
    private val loanTransactionsLogic: LoanTransactionsLogic,
    private val loansAct: LoansAct,
    private val accountsAct: AccountsAct,
    private val loanWriter: WriteLoanDao,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
    private val nav: com.ivy.navigation.Navigation,
    private val dateTimePicker: DateTimePicker
) : ComposeViewModel<LoanScreenState, LoanScreenEvent>() {

    private var baseCurrencyCode by mutableStateOf(getDefaultFIATCurrency().currencyCode)
    private var completedLoans by mutableStateOf<ImmutableList<DisplayLoan>>(persistentListOf())
    private var pendingLoans by mutableStateOf<ImmutableList<DisplayLoan>>(persistentListOf())
    private var accounts by mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private var selectedAccount by mutableStateOf<Account?>(null)
    private var loanModalData by mutableStateOf<LoanModalData?>(null)
    private var reorderModalVisible by mutableStateOf(false)
    private var dateTime by mutableStateOf<Instant>(timeProvider.utcNow())
    private var selectedTab by mutableStateOf(LoanTab.PENDING)

    /** If true paid off loans will be visible */
    private var paidOffLoanVisibility by mutableStateOf(true)

    /** Contains all loans including both paidOff and pending*/
    private var allLoans: ImmutableList<DisplayLoan> = persistentListOf()
    private var defaultCurrencyCode = ""
    private var initializedEntry: UUID? = null
    private var refreshJob: kotlinx.coroutines.Job? = null
    private var summaryOwe by mutableStateOf("")
    private var summaryOwed by mutableStateOf("")
    private var loadError by mutableStateOf<String?>(null)
    private var loading by mutableStateOf(false)
    private var refreshGeneration = 0L
    private var modalRestorationLoaded by mutableStateOf(false)
    val modalRestorationReady get() = modalRestorationLoaded && initializedEntry == nav.currentEntryId
    fun saveModal(): List<String>? = com.ivy.loans.LoanEditorRestoration.save(loanModalData,
        allLoans.firstOrNull { it.loan.id == loanModalData?.loan?.id }?.loan, null, null, dateTime, selectedTab.name)
    fun restoreModal(payload: List<String>): Boolean {
        if (loanModalData != null) return true
        val recovered = com.ivy.loans.LoanEditorRestoration.restore(payload, baseCurrencyCode, allLoans.map { it.loan }, emptyList(), accounts) ?: return false
        loanModalData = recovered.loan
        dateTime = recovered.date
        recovered.tab?.let { runCatching { LoanTab.valueOf(it) }.getOrNull() }?.let { selectedTab = it }
        return true
    }

    @Composable
    override fun uiState(): LoanScreenState {
        LaunchedEffect(Unit) {
            start()
        }

        return LoanScreenState(
            baseCurrency = getBaseCurrencyCode(),
            accounts = getAccounts(),
            selectedAccount = getSelectedAccount(),
            loanModalData = getLoanModalData(),
            reorderModalVisible = getReorderModalVisible(),
            totalOweAmount = summaryOwe,
            totalOwedAmount = summaryOwed,
            loadError = loadError,
            loading = loading,
            paidOffLoanVisibility = getPaidOffLoanVisibility(),
            dateTime = dateTime,
            selectedTab = getSelectedTab(),
            completedLoans = getCompletedLoans(),
            pendingLoans = getPendingLoans()
        )
    }

    fun setTab(tab: LoanTab) {
        selectedTab = tab
    }

    @Composable
    private fun getSelectedTab(): LoanTab {
        return selectedTab
    }

    @Composable
    private fun getCompletedLoans(): ImmutableList<DisplayLoan> {
        return completedLoans
    }

    @Composable
    private fun getPendingLoans(): ImmutableList<DisplayLoan> {
        return pendingLoans
    }

    @Composable
    private fun getReorderModalVisible() = reorderModalVisible

    @Composable
    private fun getLoanModalData() = loanModalData

    @Composable
    private fun getBaseCurrencyCode(): String {
        return baseCurrencyCode
    }

    @Composable
    private fun getSelectedAccount() = selectedAccount

    @Composable
    private fun getAccounts() = accounts

    @Composable
    private fun getPaidOffLoanVisibility(): Boolean = paidOffLoanVisibility

    override fun onEvent(event: LoanScreenEvent) {
        when (event) {
            LoanScreenEvent.Retry -> start(force = true)
            is LoanScreenEvent.OnLoanCreate -> {
                createLoan(event.createLoanData, event.onResult)
            }

            is LoanScreenEvent.OnAddLoan -> {
                loanModalData = LoanModalData(
                    loan = null,
                    baseCurrency = baseCurrencyCode,
                    selectedAccount = selectedAccount
                )
            }

            is LoanScreenEvent.OnLoanModalDismiss -> {
                loanModalData = null
                dateTime = timeProvider.utcNow()
            }

            is LoanScreenEvent.OnReOrderModalShow -> {
                reorderModalVisible = event.show
            }

            is LoanScreenEvent.OnReordered -> {
                reorder(event.reorderedList)
            }

            is LoanScreenEvent.OnCreateAccount -> {
                createAccount(event.accountData, event.onResult)
            }

            LoanScreenEvent.OnTogglePaidOffLoanVisibility -> {
                updatePaidOffLoanVisibility()
            }

            is LoanScreenEvent.OnChangeDate -> {
                handleChangeDate()
            }

            is LoanScreenEvent.OnChangeTime -> {
                handleChangeTime()
            }

            is LoanScreenEvent.OnTabChanged -> {
                setTab(event.tab)
            }
        }
    }

    private fun start(force: Boolean = false) {
        val entry = nav.currentEntryId
        if (!force && initializedEntry == entry && refreshJob?.isActive == true) return
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        loadError = null
        loading = true
        if (initializedEntry != entry) {
            modalRestorationLoaded = false
            loanModalData = null
            dateTime = timeProvider.utcNow()
        }
        initializedEntry = entry
        refreshJob = viewModelScope.launch {
            TestIdlingResource.increment()
            try {
            defaultCurrencyCode = ioThread {
                settingsDao.findFirst().currency
            }.also {
                baseCurrencyCode = it
            }

            initialiseAccounts()

            allLoans = ioThread {
                loansAct(Unit)
                    .map { loan ->
                        val (amountPaid, loanTotalAmount) = calculateAmountPaidAndTotalAmount(loan)
                        val percentPaid = if (loanTotalAmount != 0.0) {
                            amountPaid / loanTotalAmount
                        } else {
                            0.0
                        }
                        var currCode = findCurrencyCode(accounts, loan.accountId)

                        DisplayLoan(
                            loan = loan,
                            loanTotalAmount = loanTotalAmount,
                            amountPaid = amountPaid,
                            currencyCode = currCode,
                            formattedDisplayText = "${amountPaid.format(currCode)} $currCode / ${
                                loanTotalAmount.format(
                                    currCode
                                )
                            } $currCode (${
                                percentPaid.times(
                                    100
                                ).format(2)
                            }%)",
                            percentPaid = percentPaid
                        )
                    }.toImmutableList()
            }
            val summaries = ioThread {
                outstandingLoanSummary(allLoans, LoanType.BORROW, defaultCurrencyCode) to
                    outstandingLoanSummary(allLoans, LoanType.LEND, defaultCurrencyCode)
            }
            summaryOwe = summaries.first
            summaryOwed = summaries.second
            loadPendingLoans()
            loadCompletedLoans()
            modalRestorationLoaded = true
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) {
                if (generation == refreshGeneration) loadError = "Could not refresh loans. Your saved data is unchanged. Try again."
            } finally {
                if (generation == refreshGeneration) loading = false
                TestIdlingResource.decrement()
            }
        }
    }

    private suspend fun initialiseAccounts() {
        val accountsList = accountsAct(Unit)
        accounts = accountsList
        selectedAccount = defaultAccountId(accountsList)
        selectedAccount?.let {
            baseCurrencyCode = it.currency ?: defaultCurrencyCode
        }
    }

    private fun handleChangeDate() {
        val id = loanModalData?.id ?: return
        dateTimePicker.pickDate(
            initialDate = loanModalData?.loan?.dateTime?.let {
                with(timeConverter) { it.toUTC() }
            } ?: dateTime
        ) { localDate ->
            if (loanModalData?.id != id) return@pickDate
            val localTime = loanModalData?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.toLocalTime() }

            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleChangeTime() {
        val id = loanModalData?.id ?: return
        dateTimePicker.pickTime(
            initialTime = loanModalData?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.toLocalTime() }
        ) { localTime ->
            if (loanModalData?.id != id) return@pickTime
            val localDate = loanModalData?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: with(timeConverter) { dateTime.toLocalDate() }

            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun updateDateTime(newDateTime: LocalDateTime) {
        val newDateTimeUtc = with(timeConverter) { newDateTime.toUTC() }
        loanModalData?.let { currentData ->
            loanModalData = currentData.copy(
                loan = currentData.loan?.copy(
                    dateTime = newDateTime
                )
            )
            dateTime = newDateTimeUtc
        }
    }

    private fun createLoan(data: CreateLoanData, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                loanCreator.create(data, onResult, relatedWrite = {
                    loanTransactionsLogic.Loan.createAssociatedLoanTransaction(data, it.id)
                }) { if (nav.currentEntryId == entry) start(force = true) }
            } finally { TestIdlingResource.decrement() }
        }
    }

    private fun reorder(newOrder: List<DisplayLoan>) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            ioThread {
                newOrder.forEachIndexed { index, item ->
                    loanWriter.save(
                        item.loan.toEntity().copy(
                            orderNum = index.toDouble(),
                            isSynced = false
                        )
                    )
                }
            }
            start(force = true)

            TestIdlingResource.decrement()
        }
    }

    private fun loadCompletedLoans() {
        completedLoans = allLoans.filter { loan -> loan.percentPaid >= 1.0 }.toImmutableList()
    }

    private fun loadPendingLoans() {
        pendingLoans = allLoans.filter { loan -> loan.percentPaid < 1.0 }.toImmutableList()
    }

    private fun createAccount(data: CreateAccountData, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                accountCreator.createAccount(data, onResult) {
                    val refreshed = accountsAct(Unit)
                    if (nav.currentEntryId == entry) accounts = refreshed
                }
            } finally { TestIdlingResource.decrement() }
        }
    }

    private fun defaultAccountId(
        accounts: List<Account>,
    ): Account? {
        val lastSelectedId =
            sharedPrefs.getString(SharedPrefs.LAST_SELECTED_ACCOUNT_ID, null)?.let {
                UUID.fromString(it)
            }

        lastSelectedId?.let { uuid ->
            return accounts.find { it.id == uuid }
        } ?: run {
            return if (accounts.isNotEmpty()) accounts[0] else null
        }
    }

    private fun findCurrencyCode(accounts: List<Account>, accountId: UUID?): String {
        return accountId?.let {
            accounts.find { account -> account.id == it }?.currency
        } ?: defaultCurrencyCode
    }

    /**
     *  Calculates the total amount paid and the total loan amount including any changes made to the loan.
     *  @return A Pair containing the total amount paid and the total loan amount.
     */
    private suspend fun calculateAmountPaidAndTotalAmount(loan: Loan): Pair<Double, Double> {
        val loanRecords = ioThread { loanRecordDao.findAllByLoanId(loanId = loan.id) }
        val (amountPaid, loanTotalAmount) = loanRecords.fold(0.0 to loan.amount) { value, loanRecord ->
            val (currentAmountPaid, currentLoanTotalAmount) = value
            if (loanRecord.interest) return@fold value
            val convertedAmount = loanRecord.convertedAmount ?: loanRecord.amount

            loanRecord.loanRecordType.processByType(
                decreaseAction = { currentAmountPaid + convertedAmount to currentLoanTotalAmount },
                increaseAction = { currentAmountPaid to currentLoanTotalAmount + convertedAmount }
            )
        }
        return amountPaid to loanTotalAmount
    }

    private fun updatePaidOffLoanVisibility() {
        paidOffLoanVisibility = paidOffLoanVisibility.not()
    }
}
