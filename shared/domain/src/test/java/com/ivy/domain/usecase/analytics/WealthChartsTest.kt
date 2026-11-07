package com.ivy.domain.usecase.analytics

import com.ivy.data.model.wealth.*
import com.ivy.domain.usecase.networth.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class WealthChartsTest {
    private val day = LocalDate.parse("2026-11-07")
    private val parent = WealthItem("parent", "Portfolio", WealthKind.INVESTMENT, "AED", createdDate = "2026-01-01")
    private val child = parent.copy(id = "child", parentId = parent.id)
    private val result = WealthResult(day, "AED", listOf(WealthLine(parent, BigDecimal(100), BigDecimal(100), null, null),
        WealthLine(child, BigDecimal(100), BigDecimal(100), null, null)), BigDecimal(100), BigDecimal.ZERO, emptyList())
    private fun workspace(result: WealthResult = this.result) = WealthWorkspace(WealthBackup(items = listOf(parent, child)), result,
        emptyList(), emptyList(), emptyList(), null)
    @Test fun `allocation does not double count a portfolio and its holdings`() {
        val chart = WealthCharts.allocation(workspace())!!
        assertEquals(BigDecimal(100), chart.series.single().values.single().value)
    }
    @Test fun `incomplete picture has no misleading allocation share`() {
        assertNull(WealthCharts.allocation(workspace(result.copy(issues = listOf("Missing rate")))))
    }
    @Test fun `history is bounded and never bridges unknown observations inside a bucket`() {
        val history = (0 until 60).map { i ->
            val date = day.minusMonths((59 - i).toLong())
            val amount = BigDecimal(i + 1)
            WealthHistoryPoint(date, result.copy(asOf = date, assets = amount,
                lines = listOf(result.lines.first().copy(reportingAmount = amount)),
                issues = if (i == 0) listOf("Unavailable old value") else emptyList()))
        }
        val values = WealthCharts.history(history, "AED").series.single().values
        assertEquals(30, values.size)
        assertNull(values.first().value)
        assertEquals(BigDecimal(60), values.last().value)
        assertEquals(day, values.last().end)
    }
    @Test fun `empty history remains empty and has no invented observation`() {
        assertTrue(WealthCharts.history(emptyList(), "AED").series.single().values.isEmpty())
    }
    @Test fun `individual unknown positions are gaps even when other wealth exists`() {
        val history = listOf(WealthHistoryPoint(day.minusMonths(1), result.copy(lines = emptyList())), WealthHistoryPoint(day, result))
        val values = WealthCharts.history(history, "AED", parent.id).series.single().values
        assertNull(values.first().value)
        assertEquals(BigDecimal(100), values.last().value)
    }
}
