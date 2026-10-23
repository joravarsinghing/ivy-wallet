package com.ivy.networth

import com.ivy.data.model.wealth.*
import com.ivy.domain.usecase.networth.WealthCalculator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class WealthEducationTest {
    @Test fun `guide starts with possessions and debts before discussing a total`() {
        assertEquals(4, wealthGuideSteps.size)
        assertTrue(wealthGuideSteps[0].title.contains("own"))
        assertTrue(wealthGuideSteps[1].title.contains("owe"))
        assertTrue(wealthGuideSteps[2].body.contains("linked"))
        assertTrue(wealthGuideSteps.last().body.contains("not money you can spend"))
    }
    @Test fun `each unfamiliar concept has an explanation and clearly labelled example`() {
        WealthHelp.entries.forEach { topic ->
            assertTrue(topic.explanation.length > 80)
            assertTrue(topic.example.startsWith("Example only:"))
        }
        assertTrue(WealthHelp.DEBTS.explanation.contains("monthly payment"))
        assertTrue(WealthHelp.ASSETS.example.contains("second time"))
        assertTrue(WealthHelp.NET_WORTH.example.contains("negative", ignoreCase = true))
    }
    @Test fun `tutorial example agrees with the real calculator without turning a car into spending money`() {
        val date = "2026-10-21"
        val recordedAt = "2026-10-21T12:00:00Z"
        val items = listOf(
            WealthItem("savings", "Savings", WealthKind.CASH, "USD", createdDate = date),
            WealthItem("car", "Car", WealthKind.VEHICLE, "USD", createdDate = date),
            WealthItem("loan", "Car loan", WealthKind.VEHICLE_LOAN, "USD",
                convention = BalanceConvention.POSITIVE_OWED, createdDate = date),
        )
        val backup = WealthBackup(items = items,
            policies = items.map { WealthPolicy("policy-${it.id}", it.id, date, recordedAt = recordedAt) },
            valuations = items.zip(listOf("5000.00", "8000.00", "3000.00")).map { (item, value) ->
                WealthValuation("value-${item.id}", item.id, date, value, recordedAt = recordedAt)
            })
        val result = WealthCalculator.calculate(backup, emptyList(), LocalDate.parse(date), "USD")
        assertEquals(0, BigDecimal("10000.00").compareTo(result.netWorth))
        assertEquals(0, BigDecimal("5000.00").compareTo(result.lines.single { it.item.id == "savings" }.reportingAmount))
        assertTrue(wealthGuideSteps.last().example.contains("not 10,000.00 available to spend"))
    }
}
