package com.ivy.legacy.ui.chart

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.wallet.domain.action.transaction.LegacyTrnsWithDateDivsAct
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class AccountDaySummaryTest {
    @Test fun `account day header uses transfer setting and native receiving amount`() = runTest {
        val source = UUID.randomUUID()
        val target = UUID.randomUUID()
        val entry = Transaction(source, TransactionType.TRANSFER, BigDecimal("367.25"), target,
            BigDecimal("100.00"), dateTime = Instant.parse("2026-11-07T12:00:00Z"))
        val action = LegacyTrnsWithDateDivsAct(mockk(), mockk(), TestTimeConverter())
        val enabled = action(LegacyTrnsWithDateDivsAct.Input("USD", listOf(entry), target, true))
            .filterIsInstance<TransactionHistoryDateDivider>().single()
        assertEquals(100.0, enabled.income, .00001)
        assertEquals(0.0, enabled.expenses, .00001)
        assertEquals("USD", enabled.currency)
        val disabled = action(LegacyTrnsWithDateDivsAct.Input("USD", listOf(entry), target, false))
            .filterIsInstance<TransactionHistoryDateDivider>().single()
        assertEquals(0.0, disabled.income, .00001)
    }
}
