package com.ivy.domain.usecase.networth

import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.TransactionEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/** One ledger pass per date, shared by all linked accounts. Planned-only rows have no posted date. */
object WealthLedgerAdapter {
    fun balances(rows: List<TransactionEntity>, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Map<String, BigDecimal> {
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        val balances = mutableMapOf<String, BigDecimal>()
        fun add(id: String, amount: BigDecimal) { balances[id] = (balances[id] ?: BigDecimal.ZERO) + amount }
        rows.asSequence().filter { it.dateTime?.isBefore(end) == true }.forEach { row ->
            val amount = BigDecimal.valueOf(row.amount)
            add(row.accountId.toString(), if (row.type == TransactionType.INCOME) amount else -amount)
            if (row.type == TransactionType.TRANSFER && row.toAccountId != null) add(row.toAccountId.toString(), BigDecimal.valueOf(row.toAmount ?: row.amount))
        }
        return balances
    }
}
