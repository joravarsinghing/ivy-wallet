package com.ivy.domain.usecase.networth

import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.model.LoanRecordType
import com.ivy.base.model.TransactionType
import com.ivy.data.db.dao.read.*
import com.ivy.data.db.entity.*
import com.ivy.data.model.LoanType
import com.ivy.data.model.wealth.*
import com.ivy.data.repository.WealthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.*
import java.util.UUID
import javax.inject.Inject

data class WealthCandidate(val id: String, val name: String, val currency: String, val source: WealthSource, val kind: WealthKind, val amount: BigDecimal?, val convention: BalanceConvention)
data class WealthHistoryPoint(val date: LocalDate, val result: WealthResult)
data class WealthWorkspace(val data: WealthBackup, val current: WealthResult, val candidates: List<WealthCandidate>, val history: List<WealthHistoryPoint>, val changes: List<WealthChange>, val legacyHistory: String?)

class WealthWorkspaceUseCase @Inject constructor(
    private val repository: WealthRepository,
    private val accounts: AccountDao,
    private val transactions: TransactionDao,
    private val loans: LoanDao,
    private val records: LoanRecordDao,
    private val settings: SettingsDao,
    private val preferences: SharedPrefs,
    private val exchangeRates: ExchangeRatesDao,
) {
    suspend fun captureWalletRates() {
        val existing = repository.read().rates
        val observed = LocalDate.now().toString()
        val now = Instant.now().toString()
        val rows = exchangeRates.findAll().first()
        val rates = rows.filter { it.rate.isFinite() && it.rate > 0 && it.baseCurrency != it.currency }
            .filter { row -> existing.none { it.from == row.baseCurrency && it.to == row.currency && it.rate.toBigDecimal().compareTo(BigDecimal.valueOf(row.rate)) == 0 && it.source.startsWith("Wallet cache") } }
            .map { WealthFx(UUID.randomUUID().toString(), it.baseCurrency, it.currency, BigDecimal.valueOf(it.rate).toPlainString(), observed,
                "Wallet cache observed $observed; original rate date unknown", now) }
        if (rates.isNotEmpty()) repository.merge(WealthBackup(rates = rates))
    }
    suspend fun load(date: LocalDate, reportingCurrency: String? = null, months: Int = 6): WealthWorkspace = withContext(Dispatchers.Default) {
        val base = settings.findFirstOrNull()?.currency ?: "USD"
        val currency = reportingCurrency ?: base
        val accountRows = accounts.findAll()
        val transactionRows = transactions.findAll()
        val loanRows = loans.findAll()
        val recordRows = records.findAll()
        val balancesByDate = mutableMapOf<LocalDate, Map<String, BigDecimal>>()
        fun balance(id: String, day: LocalDate): BigDecimal = balancesByDate.getOrPut(day) { WealthLedgerAdapter.balances(transactionRows, day) }[id] ?: BigDecimal.ZERO
        var data = repository.read()
        if (data.items.isEmpty()) {
            val assetIds = preferences.getString(SharedPrefs.NET_WORTH_ASSET_ACCOUNT_IDS, "").orEmpty().split(',').toSet()
            val liabilityIds = preferences.getString(SharedPrefs.NET_WORTH_LIABILITY_ACCOUNT_IDS, "").orEmpty().split(',').toSet()
            val today = LocalDate.now().toString()
            val now = Instant.now().toString()
            val migrated = accountRows.filter { it.id.toString() in assetIds || it.id.toString() in liabilityIds }.map { account ->
                val debt = account.id.toString() in liabilityIds
                val balance = balance(account.id.toString(), LocalDate.now())
                WealthItem(UUID.randomUUID().toString(), account.name, if (debt) WealthKind.CREDIT_CARD else WealthKind.CASH, account.currency ?: base,
                    WealthSource.ACCOUNT, account.id.toString(), convention = if (!debt) BalanceConvention.ASSET else if (balance.signum() < 0) BalanceConvention.NEGATIVE_OWED else BalanceConvention.POSITIVE_OWED,
                    createdDate = today, note = "Migrated from account classification. Review debt balance convention.")
            }
            if (migrated.isNotEmpty()) {
                repository.merge(WealthBackup(items = migrated, policies = migrated.map { WealthPolicy(UUID.randomUUID().toString(), it.id, today, recordedAt = now) }))
                data = repository.read()
            }
        }
        fun sourceValues(day: LocalDate): List<WealthSourceValue> = data.items.mapNotNull { item ->
            when (item.source) {
                WealthSource.ACCOUNT -> {
                    val account = accountRows.firstOrNull { it.id.toString() == item.sourceId }
                    val changed = account != null && (account.currency ?: base) != item.currency
                    WealthSourceValue(item.id, account?.takeUnless { changed }?.let { balance(it.id.toString(), day) },
                        if (account == null) "Linked account is unavailable" else if (changed) "Review the linked currency: update it to ${account.currency ?: base}" else null)
                }
                WealthSource.LOAN -> {
                    val loan = loanRows.firstOrNull { it.id.toString() == item.sourceId }
                    val account = loan?.accountId?.let { id -> accountRows.firstOrNull { it.id == id } }
                    val changed = account != null && (account.currency ?: base) != item.currency
                    val amount = loan?.takeUnless { changed }?.let { loanBalance(it, recordRows, accountRows, day, item.currency) }
                    WealthSourceValue(item.id, amount, if (changed) "Review the loan currency: update it to ${account?.currency ?: base}" else if (amount == null) "Loan history or currency conversion is unavailable" else null)
                }
                else -> null
            }
        }
        val candidates = accountRows.map { account ->
            val amount = balance(account.id.toString(), date)
            WealthCandidate(account.id.toString(), account.name, account.currency ?: base, WealthSource.ACCOUNT, WealthKind.CASH, amount, BalanceConvention.ASSET)
        } + loanRows.map { loan ->
            val account = accountRows.firstOrNull { it.id == loan.accountId }
            WealthCandidate(loan.id.toString(), loan.name, account?.currency ?: base, WealthSource.LOAN,
                if (loan.type == LoanType.LEND) WealthKind.RECEIVABLE else WealthKind.BORROWED,
                loanBalance(loan, recordRows, accountRows, date, base), if (loan.type == LoanType.LEND) BalanceConvention.ASSET else BalanceConvention.POSITIVE_OWED)
        }
        val undecided = candidates.filter { candidate -> data.items.none { it.source == candidate.source && it.sourceId == candidate.id } }
        fun result(day: LocalDate): WealthResult {
            val calculated = WealthCalculator.calculate(data, sourceValues(day), day, currency)
            val review = data.reviews.reviewOn(day.toString())
            val coverage = if (review?.assetsReviewed == true && review.debtsReviewed) emptyList() else listOf("Confirm that assets and debts have been reviewed")
            return calculated.copy(issues = calculated.issues + undecided.map { "Review inclusion: ${it.name}" } + coverage)
        }
        val current = result(date)
        val firstDate = data.items.minOfOrNull { it.createdDate }?.let(LocalDate::parse) ?: date
        val length = if (months == 0) (java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(firstDate), YearMonth.from(date)) + 1).toInt().coerceAtLeast(1) else months
        val history = (length - 1 downTo 0).map { offset ->
            val day = if (offset == 0) date else YearMonth.from(date).minusMonths(offset.toLong()).atEndOfMonth()
            WealthHistoryPoint(day, result(day))
        }
        WealthWorkspace(data, current, undecided, history,
            history.getOrNull(history.lastIndex - 1)?.takeIf { it.result.complete && current.complete }?.let { WealthCalculator.changes(it.result, current) }.orEmpty(),
            preferences.getString(SharedPrefs.NET_WORTH_HISTORY, null))
    }

    private fun loanBalance(loan: LoanEntity, rows: List<LoanRecordEntity>, accounts: List<AccountEntity>, day: LocalDate, base: String): BigDecimal? {
        val start = loan.dateTime?.toLocalDate()
        if (start == null && day != LocalDate.now()) return null
        if (start != null && start > day) return null
        val currency = accounts.firstOrNull { it.id == loan.accountId }?.currency ?: base
        var amount = BigDecimal.valueOf(loan.amount)
        for (record in rows.filter { it.loanId == loan.id && it.dateTime.atZone(ZoneId.systemDefault()).toLocalDate() <= day && !it.interest }) {
            val nativeCurrency = if (record.accountId == null) currency else accounts.firstOrNull { it.id == record.accountId }?.currency ?: base
            val value = record.convertedAmount ?: if (nativeCurrency == currency) record.amount else return null
            amount += BigDecimal.valueOf(value).let { if (record.loanRecordType == LoanRecordType.INCREASE) it else it.negate() }
        }
        return amount
    }
}
