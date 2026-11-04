package com.ivy.legacy.domain.deprecated.logic.csv

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType
import com.ivy.wallet.domain.deprecated.logic.csv.CSVMapper
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalStdlibApi::class)
class TransferJoinTest {
    private val mapping = CSVMapper().mapping(ImportType.FINANCISTO, null)
    private val first = Transaction(type = TransactionType.TRANSFER, amount = BigDecimal("100"),
        accountId = UUID.randomUUID(), dateTime = Instant.parse("2026-11-01T09:00:00Z"))
    private val second = first.copy(id = UUID.randomUUID(), accountId = UUID.randomUUID(), amount = BigDecimal("27"))
    private val signs = mapOf(first.id to -100.0, second.id to 27.0)

    @Test fun `matching transfer halves preserve both amounts and account identities`() {
        val result = mapping.joinTransactions(listOf(first, second), signs)
        assertEquals(1, result.mergedCount)
        val transfer = result.transactions.single()
        assertEquals(first.id, transfer.id)
        assertEquals(first.accountId, transfer.accountId)
        assertEquals(second.accountId, transfer.toAccountId)
        assertEquals(first.amount, transfer.amount)
        assertEquals(second.amount, transfer.toAmount)
    }

    @Test fun `an expense after a transfer is never consumed`() {
        val expense = second.copy(type = TransactionType.EXPENSE)
        val result = mapping.joinTransactions(listOf(first, expense), signs)
        assertEquals(0, result.mergedCount)
        assertEquals(listOf(first, expense), result.transactions)
    }

    @Test fun `same-account or different-time transfers are not guessed as a pair`() {
        for (unrelated in listOf(second.copy(accountId = first.accountId),
            second.copy(dateTime = first.dateTime!!.plusSeconds(60)))) {
            val result = mapping.joinTransactions(listOf(first, unrelated), signs)
            assertEquals(0, result.mergedCount)
            assertEquals(listOf(first, unrelated), result.transactions)
        }
    }

    @Test fun `incoming first still debits the outgoing account and credits the receiving account`() {
        val transfer = mapping.joinTransactions(listOf(second, first), signs).transactions.single()
        assertEquals(first.accountId, transfer.accountId)
        assertEquals(second.accountId, transfer.toAccountId)
        assertEquals(first.amount, transfer.amount)
        assertEquals(second.amount, transfer.toAmount)
    }

    @Test fun `two outgoing halves cannot be guessed as a transfer`() {
        val result = mapping.joinTransactions(listOf(first, second), signs + (second.id to -27.0))
        assertEquals(0, result.mergedCount)
        assertEquals(listOf(first, second), result.transactions)
    }
}
