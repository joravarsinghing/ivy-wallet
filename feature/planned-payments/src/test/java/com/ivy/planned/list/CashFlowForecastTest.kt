package com.ivy.planned.list

import org.junit.Assert.assertEquals
import org.junit.Test

class CashFlowForecastTest {
    @Test
    fun `combines recurring and upcoming planned payments`() {
        val forecast = calculateCashFlowForecast(
            currentBalance = 10000.0,
            recurringIncome = 5000.0,
            recurringExpenses = 2800.0,
            oneTimeIncome = 300.0,
            oneTimeExpenses = 450.0,
        )

        assertEquals(2200.0, forecast.recurringNet, 0.001)
        assertEquals(2050.0, forecast.nextPeriodNet, 0.001)
        assertEquals(12050.0, forecast.projectedBalance, 0.001)
    }
}
