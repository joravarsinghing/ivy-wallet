package com.ivy.loans.loandetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.LoanRecordType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.read.LoanRecordDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.frp.test.TestIdlingResource
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.datamodel.LoanRecord
import com.ivy.legacy.datamodel.temp.toLegacy
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.legacy.domain.deprecated.logic.AccountCreator
import com.ivy.legacy.utils.computationThread
import com.ivy.legacy.utils.ioThread
import com.ivy.loans.loan.data.DisplayLoanRecord
import com.ivy.loans.loandetails.events.DeleteLoanModalEvent
import com.ivy.loans.loandetails.events.LoanDetailsScreenEvent
import com.ivy.loans.loandetails.events.LoanModalEvent
import com.ivy.loans.loandetails.events.LoanRecordModalEvent
import com.ivy.navigation.LoanDetailsScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.time.impl.DateTimePicker
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.loan.LoanByIdAct
import com.ivy.wallet.domain.deprecated.logic.LoanCreator
import com.ivy.wallet.domain.deprecated.logic.LoanRecordCreator
import com.ivy.wallet.domain.deprecated.logic.loantrasactions.LoanTransactionsLogic
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanRecordData
import com.ivy.wallet.domain.deprecated.logic.model.EditLoanRecordData
import com.ivy.wallet.ui.theme.modal.LoanModalData
import com.ivy.wallet.ui.theme.modal.LoanRecordModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@Stable
@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    private val loanRecordDao: LoanRecordDao,
    private val loanCreator: LoanCreator,
    private val loanRecordCreator: LoanRecordCreator,
    private val settingsDao: SettingsDao,
    private val transactionRepository: TransactionRepository,
    private val transactionMapper: TransactionMapper,
    private val accountCreator: AccountCreator,
    private val loanTransactionsLogic: LoanTransactionsLogic,
    private val nav: Navigation,
    private val accountsAct: AccountsAct,
    private val loanByIdAct: LoanByIdAct,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
    private val dateTimePicker: DateTimePicker,
) : ComposeViewModel<LoanDetailsScreenState, LoanDetailsScreenEvent>() {

    private val baseCurrency = mutableStateOf("")
    private val loan = mutableStateOf<Loan?>(null)
    private val displayLoanRecords =
        mutableStateOf<ImmutableList<DisplayLoanRecord>>(persistentListOf())
    private val loanTotalAmount = mutableDoubleStateOf(0.0)
    private val amountPaid = mutableDoubleStateOf(0.0)
    private val accounts = mutableStateOf<ImmutableList<Account>>(persistentListOf())
    private val loanInterestAmountPaid = mutableDoubleStateOf(0.0)
    private val selectedLoanAccount = mutableStateOf<Account?>(null)
    private var associatedTransaction: Transaction? = null
    private val createLoanTransaction = mutableStateOf(false)
    private var defaultCurrencyCode = ""
    private val loanModalData = mutableStateOf<LoanModalData?>(null)
    private val loanRecordModalData = mutableStateOf<LoanRecordModalData?>(null)
    private val waitModalVisible = mutableStateOf(false)
    private val operationError = mutableStateOf<String?>(null)
    private var deleting = false
    private val isDeleteModalVisible = mutableStateOf(false)
    private var dateTime = mutableStateOf<Instant>(timeProvider.utcNow())
    lateinit var screen: LoanDetailsScreen
    private var startedEntry: UUID? = null
    private var startedLoan: UUID? = null
    private var loadGeneration = 0L
    private var loadJob: kotlinx.coroutines.Job? = null
    private val loading = mutableStateOf(false)
    val modalRestorationReady get() = loan.value != null && !loading.value && startedEntry == nav.currentEntryId && startedLoan == screen.loanId
    fun saveModal(): List<String>? = loan.value?.let { current ->
        com.ivy.loans.LoanEditorRestoration.save(loanModalData.value, current, loanRecordModalData.value,
            displayLoanRecords.value.firstOrNull { it.loanRecord.id == loanRecordModalData.value?.loanRecord?.id }?.loanRecord,
            dateTime.value)
    }
    fun restoreModal(payload: List<String>): Boolean {
        if (loanModalData.value != null || loanRecordModalData.value != null) return true
        val current = loan.value ?: return false
        val recovered = com.ivy.loans.LoanEditorRestoration.restore(payload, baseCurrency.value, listOf(current),
            displayLoanRecords.value.map { it.loanRecord }, accounts.value) ?: return false
        loanModalData.value = recovered.loan
        loanRecordModalData.value = recovered.record
        dateTime.value = recovered.date
        return true
    }

    @Composable
    override fun uiState(): LoanDetailsScreenState {
        LaunchedEffect(Unit) {
            start()
        }

        return LoanDetailsScreenState(
            baseCurrency = baseCurrency.value,
            loan = loan.value,
            displayLoanRecords = displayLoanRecords.value,
            loanTotalAmount = loanTotalAmount.doubleValue,
            amountPaid = amountPaid.doubleValue,
            loanAmountPaid = loanInterestAmountPaid.doubleValue,
            accounts = accounts.value,
            selectedLoanAccount = selectedLoanAccount.value,
            createLoanTransaction = createLoanTransaction.value,
            loanModalData = loanModalData.value,
            loanRecordModalData = loanRecordModalData.value,
            waitModalVisible = waitModalVisible.value,
            operationError = operationError.value,
            loading = loading.value,
            isDeleteModalVisible = isDeleteModalVisible.value,
            dateTime = dateTime.value
        )
    }

    override fun onEvent(event: LoanDetailsScreenEvent) {
        when (event) {
            is LoanRecordModalEvent -> handleLoanRecordModalEvents(event)
            is LoanModalEvent -> handleLoanModalEvents(event)
            is DeleteLoanModalEvent -> handleDeleteLoanModalEvents(event)
            is LoanDetailsScreenEvent -> handleLoanDetailsScreenEvents(event)
        }
    }

    private fun handleLoanRecordModalEvents(event: LoanDetailsScreenEvent) {
        when (event) {
            is LoanRecordModalEvent.OnClickLoanRecord -> {
                loanRecordModalData.value = LoanRecordModalData(
                    loanRecord = event.displayLoanRecord.loanRecord,
                    baseCurrency = event.displayLoanRecord.loanRecordCurrencyCode,
                    selectedAccount = event.displayLoanRecord.account,
                    createLoanRecordTransaction = event.displayLoanRecord.loanRecordTransaction,
                    isLoanInterest = event.displayLoanRecord.loanRecord.interest,
                    loanAccountCurrencyCode = event.displayLoanRecord.loanCurrencyCode
                )
            }

            is LoanRecordModalEvent.OnCreateLoanRecord -> {
                createLoanRecord(event.loanRecordData, event.onResult)
            }

            is LoanRecordModalEvent.OnDeleteLoanRecord -> {
                deleteLoanRecord(event.loanRecord, event.onResult)
            }

            LoanRecordModalEvent.OnDismissLoanRecord -> {
                loanRecordModalData.value = null
                dateTime.value = timeProvider.utcNow()
            }

            is LoanRecordModalEvent.OnEditLoanRecord -> {
                editLoanRecord(event.loanRecordData, event.onResult)
            }

            is LoanRecordModalEvent.OnChangeDate -> {
                handleChangeDate()
            }
            is LoanRecordModalEvent.OnChangeTime -> {
                handleChangeTime()
            }
            else -> {}
        }
    }

    private fun handleLoanModalEvents(event: LoanDetailsScreenEvent) {
        when (event) {
            LoanModalEvent.OnDismissLoanModal -> {
                loanModalData.value = null
                dateTime.value = timeProvider.utcNow()
            }

            is LoanModalEvent.OnEditLoanModal -> {
                editLoan(event.loan, event.createLoanTransaction, event.onResult)
            }

            LoanModalEvent.PerformCalculation -> {
                waitModalVisible.value = true
            }

            LoanModalEvent.OnChangeDate -> {
                handleLoanChangeDate()
            }

            LoanModalEvent.OnChangeTime -> {
                handleLoanChangeTime()
            }

            else -> {}
        }
    }

    private fun handleDeleteLoanModalEvents(event: LoanDetailsScreenEvent) {
        when (event) {
            DeleteLoanModalEvent.OnDeleteLoan -> {
                deleteLoan()
                isDeleteModalVisible.value = false
            }

            is DeleteLoanModalEvent.OnDismissDeleteLoan -> {
                isDeleteModalVisible.value = event.isDeleteModalVisible
            }

            else -> {}
        }
    }

    private fun handleLoanDetailsScreenEvents(event: LoanDetailsScreenEvent) {
        when (event) {
            LoanDetailsScreenEvent.Retry -> load(screen.loanId)
            LoanDetailsScreenEvent.OnAmountClick -> {
                loanModalData.value = LoanModalData(
                    loan = loan.value,
                    baseCurrency = baseCurrency.value,
                    autoFocusKeyboard = false,
                    autoOpenAmountModal = true,
                    selectedAccount = selectedLoanAccount.value,
                    createLoanTransaction = createLoanTransaction.value
                )
            }

            LoanDetailsScreenEvent.OnEditLoanClick -> {
                loanModalData.value = LoanModalData(
                    loan = loan.value,
                    baseCurrency = baseCurrency.value,
                    autoFocusKeyboard = false,
                    selectedAccount = selectedLoanAccount.value,
                    createLoanTransaction = createLoanTransaction.value
                )
            }

            LoanDetailsScreenEvent.OnAddRecord -> {
                loanRecordModalData.value = LoanRecordModalData(
                    loanRecord = null,
                    baseCurrency = baseCurrency.value,
                    selectedAccount = selectedLoanAccount.value
                )
            }

            is LoanDetailsScreenEvent.OnCreateAccount -> {
                createAccount(event.data, event.onResult)
            }

            else -> {}
        }
    }

    private fun start() {
        val entry = nav.currentEntryId
        if (startedEntry == entry && startedLoan == screen.loanId && loadJob?.isActive == true) return
        if (startedEntry != entry || startedLoan != screen.loanId) {
            loanModalData.value = null
            loanRecordModalData.value = null
            loan.value = null
            dateTime.value = timeProvider.utcNow()
        }
        startedEntry = entry
        startedLoan = screen.loanId
        load(screen.loanId)
    }

    private fun load(loanId: UUID) {
        val generation = ++loadGeneration
        val entry = nav.currentEntryId
        loadJob?.cancel()
        loading.value = true
        operationError.value = null
        loadJob = viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                val (refreshed, linked) = computationThread {
                    val walletCurrency = ioThread { settingsDao.findFirst().currency }
                    val refreshedAccounts = accountsAct(Unit)
                    val current = loanByIdAct(loanId) ?: throw com.ivy.wallet.domain.deprecated.logic.LoanWriteConflict()
                    val selected = refreshedAccounts.find { it.id == current.accountId }
                    val currency = selected?.currency ?: walletCurrency
                    val records = ioThread { loanRecordDao.findAllByLoanId(loanId) }.map { record ->
                        val account = refreshedAccounts.find { it.id == record.accountId }
                        val transaction = ioThread { transactionRepository.findLoanRecordTransaction(record.id) }
                        DisplayLoanRecord(loanRecord = record.toLegacyDomain(), account = account,
                            loanRecordTransaction = transaction != null,
                            loanRecordCurrencyCode = account?.currency ?: walletCurrency, loanCurrencyCode = currency)
                    }.toImmutableList()
                    var paid = 0.0
                    var interest = 0.0
                    var total = current.amount
                    records.forEach {
                        val amount = it.loanRecord.convertedAmount ?: it.loanRecord.amount
                        if (it.loanRecord.loanRecordType == LoanRecordType.INCREASE) total += amount
                        else if (it.loanRecord.interest) interest += amount else paid += amount
                    }
                    val transaction = ioThread { transactionRepository.findLoanTransaction(current.id)?.toLegacy(transactionMapper) }
                    LoanDetailsScreenState(currency, current, records, total, paid, interest, refreshedAccounts,
                        selected, transaction != null, null, null, false, false, dateTime.value) to transaction
                }
                if (generation != loadGeneration || nav.currentEntryId != entry) return@launch
                baseCurrency.value = refreshed.baseCurrency
                loan.value = refreshed.loan
                displayLoanRecords.value = refreshed.displayLoanRecords
                loanTotalAmount.doubleValue = refreshed.loanTotalAmount
                amountPaid.doubleValue = refreshed.amountPaid
                loanInterestAmountPaid.doubleValue = refreshed.loanAmountPaid
                accounts.value = refreshed.accounts
                selectedLoanAccount.value = refreshed.selectedLoanAccount
                associatedTransaction = linked
                createLoanTransaction.value = linked != null
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (failure: Exception) {
                if (generation == loadGeneration && nav.currentEntryId == entry) operationError.value =
                    if (failure is com.ivy.wallet.domain.deprecated.logic.LoanWriteConflict)
                        "This loan is no longer available. Go back to Loans to review your list."
                    else "Could not refresh loan details. Your saved data is unchanged. Try again."
            } finally {
                if (generation == loadGeneration) { loading.value = false; waitModalVisible.value = false }
                TestIdlingResource.decrement()
            }
        }
    }

    fun editLoan(loan: Loan, createLoanTransaction: Boolean = false, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                loanCreator.edit(loan, onResult, relatedWrite = { existing ->
                    loanTransactionsLogic.Loan.recalculateLoanRecords(existing.accountId, loan.accountId, loan.id)
                    loanTransactionsLogic.Loan.editAssociatedLoanTransaction(loan, createLoanTransaction, associatedTransaction)
                }) { if (nav.currentEntryId == entry) load(it.id) }
            } finally { TestIdlingResource.decrement() }
        }
    }

    private fun deleteLoan() {
        if (deleting) return
        val current = loan.value ?: return
        val entry = nav.currentEntryId
        deleting = true
        operationError.value = null
        viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                loanCreator.delete(current, onResult = { result ->
                    if (result.isFailure && nav.currentEntryId == entry) operationError.value = "Could not delete. Your loan is still here. Try again."
                }, relatedWrite = { loanTransactionsLogic.Loan.deleteAssociatedLoanTransactions(current.id) }) {
                    if (nav.currentEntryId == entry) nav.back()
                }
            } finally { deleting = false; TestIdlingResource.decrement() }
        }
    }

    private fun createLoanRecord(data: CreateLoanRecordData, onResult: (Result<Unit>) -> Unit) {
        val current = loan.value
        if (current == null) { onResult(Result.failure(com.ivy.wallet.domain.deprecated.logic.LoanWriteConflict())); return }
        val entry = nav.currentEntryId
        viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                loanRecordCreator.create(current.id, data, onResult, prepare = {
                    it.copy(convertedAmount = loanTransactionsLogic.LoanRecord.calculateConvertedAmount(it, current.accountId))
                }, relatedWrite = {
                    loanTransactionsLogic.LoanRecord.createAssociatedLoanRecordTransaction(current, it.id, data)
                }) { if (nav.currentEntryId == entry) load(current.id) }
            } finally { TestIdlingResource.decrement() }
        }
    }

    private fun editLoanRecord(data: EditLoanRecordData, onResult: (Result<Unit>) -> Unit) {
        val current = loan.value
        if (current == null) { onResult(Result.failure(com.ivy.wallet.domain.deprecated.logic.LoanWriteConflict())); return }
        val entry = nav.currentEntryId
        viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                val converted = try {
                    loanTransactionsLogic.LoanRecord.calculateConvertedAmount(current.accountId, data.newLoanRecord,
                        data.originalLoanRecord, data.reCalculateLoanAmount)
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
                } catch (failure: Exception) { onResult(Result.failure(failure)); return@launch }
                val modified = data.newLoanRecord.copy(convertedAmount = converted)
                loanRecordCreator.edit(modified, onResult, relatedWrite = {
                    loanTransactionsLogic.LoanRecord.editAssociatedLoanRecordTransaction(current, modified, data.createLoanRecordTransaction)
                }) { if (nav.currentEntryId == entry) load(current.id) }
            } finally { TestIdlingResource.decrement() }
        }
    }

    private fun deleteLoanRecord(record: LoanRecord, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                loanRecordCreator.delete(record, onResult, relatedWrite = {
                    loanTransactionsLogic.LoanRecord.deleteAssociatedLoanRecordTransaction(record.id)
                }) { if (nav.currentEntryId == entry) load(record.loanId) }
            } finally { TestIdlingResource.decrement() }
        }
    }

    private fun handleChangeDate() {
        val id = loanRecordModalData.value?.id ?: return
        dateTimePicker.pickDate(
            initialDate = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalDateTime().toUTC() }
            } ?: dateTime.value
        ) { localDate ->
            if (loanRecordModalData.value?.id != id) return@pickDate

            val localTime = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.value.toLocalTime() }

            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleChangeTime() {
        val id = loanRecordModalData.value?.id ?: return
        dateTimePicker.pickTime(
            initialTime = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.value.toLocalTime() }
        ) { localTime ->
            if (loanRecordModalData.value?.id != id) return@pickTime
            val localDate = loanRecordModalData.value?.loanRecord?.dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: with(timeConverter) { dateTime.value.toLocalDate() }

            updateDateTime(localDate.atTime(localTime))
        }
    }

    private fun updateDateTime(newDateTime: LocalDateTime) {
        val newDateTimeUtc = with(timeConverter) { newDateTime.toUTC() }
        loanRecordModalData.value?.let { currentData ->
            loanRecordModalData.value = currentData.copy(
                loanRecord = currentData.loanRecord?.copy(
                    dateTime = newDateTimeUtc
                )
            )
            dateTime.value = newDateTimeUtc
        }
    }

    private fun handleLoanChangeDate() {
        val id = loanModalData.value?.id ?: return
        dateTimePicker.pickDate(
            initialDate = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toUTC() }
            } ?: dateTime.value
        ) { localDate ->
            if (loanModalData.value?.id != id) return@pickDate

            val localTime = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.value.toLocalTime() }

            updateLoanDateTime(localDate.atTime(localTime))
        }
    }

    private fun handleLoanChangeTime() {
        val id = loanModalData.value?.id ?: return
        dateTimePicker.pickTime(
            initialTime = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: with(timeConverter) { dateTime.value.toLocalTime() }
        ) { localTime ->
            if (loanModalData.value?.id != id) return@pickTime
            val localDate = loanModalData.value?.loan?.dateTime?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: with(timeConverter) { dateTime.value.toLocalDate() }

            updateLoanDateTime(localDate.atTime(localTime))
        }
    }

    private fun updateLoanDateTime(newDateTime: LocalDateTime) {
        val newDateTimeUtc = with(timeConverter) { newDateTime.toUTC() }
        loanModalData.value?.let { currentData ->
            loanModalData.value = currentData.copy(
                loan = currentData.loan?.copy(
                    dateTime = newDateTime
                )
            )
            dateTime.value = newDateTimeUtc
        }
    }

    fun onLoanTransactionChecked(boolean: Boolean) {
        createLoanTransaction.value = boolean
    }

    private fun createAccount(data: CreateAccountData, onResult: (Result<Unit>) -> Unit) {
        val entry = nav.currentEntryId
        viewModelScope.launch {
            TestIdlingResource.increment()
            try {
                accountCreator.createAccount(data, onResult) {
                    val refreshed = accountsAct(Unit)
                    if (nav.currentEntryId == entry) accounts.value = refreshed
                }
            } finally { TestIdlingResource.decrement() }
        }
    }

    private fun findAccount(
        accounts: List<Account>,
        accountId: UUID?,
    ): Account? {
        return accountId?.let { uuid ->
            accounts.find { acc ->
                acc.id == uuid
            }
        }
    }
}
