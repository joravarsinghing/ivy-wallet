package com.ivy.wallet.domain.deprecated.logic

import arrow.core.getOrElse
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.data.temp.migration.getValue
import com.ivy.legacy.data.model.filterOverdue
import com.ivy.legacy.data.model.filterUpcoming
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.temp.toDomain
import com.ivy.wallet.domain.action.viewmodel.account.AccountDataAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.absoluteValue
import java.util.UUID

@Deprecated("Migrate to FP Style")
class WalletAccountLogic @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionMapper: TransactionMapper,
    private val accountDataAct: AccountDataAct,
    private val sharedPrefs: SharedPrefs,
    private val currencyRepository: CurrencyRepository,
    private val timeProvider: TimeProvider
) {

    suspend fun adjustBalance(
        account: Account,
        actualBalance: Double? = null,
        newBalance: Double,

        adjustTransactionTitle: String = "Adjust balance",

        isFiat: Boolean? = null,
        trnIsSyncedFlag: Boolean = false, // TODO: Remove this once Bank Integration trn sync is properly implemented
    ) = adjustBalanceWithIdentity(account, actualBalance, newBalance, adjustTransactionTitle, isFiat, trnIsSyncedFlag, UUID.randomUUID())

    suspend fun initializeBalance(account: Account, newBalance: Double, transactionId: UUID) =
        adjustBalanceWithIdentity(account, 0.0, newBalance, "Adjust balance", null, false, transactionId)

    private suspend fun adjustBalanceWithIdentity(
        account: Account, actualBalance: Double?, newBalance: Double,
        adjustTransactionTitle: String, isFiat: Boolean?, trnIsSyncedFlag: Boolean, transactionId: UUID,
    ) {
        require(newBalance.isFinite()) { "Account balance must be finite." }
        val ab = actualBalance ?: calculateAccountBalance(account)
        require(ab.isFinite()) { "Current account balance must be finite." }
        val diff = ab - newBalance
        require(diff.isFinite()) { "Balance adjustment is out of range." }

        val finalDiff = if (isFiat == true && abs(diff) < 0.009) 0.0 else diff
        when {
            finalDiff < 0 -> {
                // add income
                val adjustment = Transaction(
                    id = transactionId,
                    type = TransactionType.INCOME,
                    title = adjustTransactionTitle,
                    amount = diff.absoluteValue.toBigDecimal(),
                    toAmount = diff.absoluteValue.toBigDecimal(),
                    dateTime = timeProvider.utcNow(),
                    accountId = account.id,
                    isSynced = trnIsSyncedFlag
                ).toDomain(transactionMapper)
                checkNotNull(adjustment) { "Could not prepare balance adjustment." }
                transactionRepository.save(adjustment)
            }

            finalDiff > 0 -> {
                // add expense
                val adjustment = Transaction(
                    id = transactionId,
                    type = TransactionType.EXPENSE,
                    title = adjustTransactionTitle,
                    amount = diff.absoluteValue.toBigDecimal(),
                    toAmount = diff.absoluteValue.toBigDecimal(),
                    dateTime = timeProvider.utcNow(),
                    accountId = account.id,
                    isSynced = trnIsSyncedFlag
                ).toDomain(transactionMapper)
                checkNotNull(adjustment) { "Could not prepare balance adjustment." }
                transactionRepository.save(adjustment)
            }
        }
    }

    suspend fun calculateAccountBalance(
        account: Account
    ): Double {
        val accountList = account.toDomainAccount(currencyRepository)
            .map { a -> listOf(a) }
            .getOrElse { emptyList() }

        val includeTransfersInCalc =
            sharedPrefs.getBoolean(SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE, false)

        val accountsDataList = accountDataAct(
            AccountDataAct.Input(
                accounts = accountList.toImmutableList(),
                range = ClosedTimeRange.allTimeIvy(timeProvider),
                baseCurrency = currencyRepository.getBaseCurrency().code,
                includeTransfersInCalc = includeTransfersInCalc
            )
        )

        return accountsDataList.firstOrNull()?.balance ?: 0.0
    }

    suspend fun calculateUpcomingIncome(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): Double =
        upcoming(account, range = range)
            .filterIsInstance<Income>()
            .sumOf { it.getValue().toDouble() }

    suspend fun calculateUpcomingExpenses(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): Double =
        upcoming(account = account, range = range)
            .filterIsInstance<Expense>()
            .sumOf { it.getValue().toDouble() }

    suspend fun calculateOverdueIncome(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): Double =
        overdue(account, range = range)
            .filterIsInstance<Income>()
            .sumOf { it.getValue().toDouble() }

    suspend fun calculateOverdueExpenses(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): Double =
        overdue(account, range = range)
            .filterIsInstance<Expense>()
            .sumOf { it.getValue().toDouble() }

    suspend fun upcoming(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): List<com.ivy.data.model.Transaction> {
        return transactionRepository.findAllDueToBetweenByAccount(
            accountId = AccountId(account.id),
            startDate = range.upcomingFrom(timeProvider),
            endDate = range.to()
        ).filterUpcoming()
    }

    suspend fun overdue(
        account: Account,
        range: com.ivy.legacy.data.model.FromToTimeRange
    ): List<com.ivy.data.model.Transaction> {
        return transactionRepository.findAllDueToBetweenByAccount(
            accountId = AccountId(account.id),
            startDate = range.from(),
            endDate = range.overdueTo(timeProvider)
        ).filterOverdue()
    }
}
