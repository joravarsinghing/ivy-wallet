package com.ivy.wallet.domain.action.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.time.TimeConverter
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TagRepository
import com.ivy.frp.action.FPAction
import com.ivy.frp.then
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.legacy.domain.pure.transaction.LegacyTrnDateDividers
import com.ivy.legacy.domain.pure.transaction.transactionsWithDateDividers
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.action.exchange.actInput
import javax.inject.Inject

class TrnsWithDateDivsAct @Inject constructor(
    private val accountDao: AccountDao,
    private val exchangeAct: ExchangeAct,
    private val tagRepository: TagRepository,
    private val accountRepository: AccountRepository,
) : FPAction<TrnsWithDateDivsAct.Input, List<TransactionHistoryItem>>() {

    override suspend fun Input.compose(): suspend () -> List<TransactionHistoryItem> = suspend {
        transactionsWithDateDividers(
            transactions = transactions,
            baseCurrencyCode = baseCurrency,
            getTags = { tagIds -> tagRepository.findByIds(tagIds) },
            getAccount = accountDao::findById then { it?.toLegacyDomain() },
            accountRepository = accountRepository,
            exchange = ::actInput then exchangeAct
        )
    }

    data class Input(
        val baseCurrency: String,
        val transactions: List<com.ivy.data.model.Transaction>
    )
}

@Deprecated("Uses legacy Transaction")
class LegacyTrnsWithDateDivsAct @Inject constructor(
    private val accountDao: AccountDao,
    private val exchangeAct: ExchangeAct,
    private val timeConverter: TimeConverter,
) : FPAction<LegacyTrnsWithDateDivsAct.Input, List<TransactionHistoryItem>>() {

    override suspend fun Input.compose(): suspend () -> List<TransactionHistoryItem> = suspend {
        // Native account totals share the chart's transfer preference and receiving amounts.
        if (accountId != null) {
            transactions.groupBy { with(timeConverter) { it.dateTime?.toLocalDate() } }
                .filterKeys { it != null }.toList().sortedByDescending { it.first }
                .flatMap { (date, rows) ->
                    val legs = rows.map { com.ivy.wallet.domain.pure.transaction.accountCashFlow(it, accountId, includeTransfers, excludeAdjustments = false) }
                    listOf<TransactionHistoryItem>(com.ivy.wallet.domain.data.TransactionHistoryDateDivider(
                        date!!, legs.fold(java.math.BigDecimal.ZERO) { sum, it -> sum + it.first }.toDouble(),
                        legs.fold(java.math.BigDecimal.ZERO) { sum, it -> sum + it.second }.toDouble(), baseCurrency)) + rows
                }
        } else LegacyTrnDateDividers.transactionsWithDateDividers(
            transactions = transactions,
            baseCurrencyCode = baseCurrency,

            getAccount = accountDao::findById then { it?.toLegacyDomain() },
            exchange = ::actInput then exchangeAct,
            timeConverter = timeConverter,
        )
    }

    data class Input(
        val baseCurrency: String,
        val transactions: List<Transaction>,
        val accountId: java.util.UUID? = null,
        val includeTransfers: Boolean = false,
    )
}
