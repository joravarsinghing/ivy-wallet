package com.ivy.domain.usecase.networth

import com.ivy.data.model.wealth.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class WealthEditRegressionTest {
    private val jan = "2026-01-01"
    private val oct = "2026-10-01"
    private val stamp = "2026-10-24T12:00:00Z"
    private val item = WealthItem("item", "Investments", WealthKind.INVESTMENT, "AED", createdDate = jan)
    private val policy = WealthPolicy("policy", item.id, jan, recordedAt = stamp)
    private val value = WealthValuation("value", item.id, jan, "500", recordedAt = stamp)
    private val original = WealthBackup(items = listOf(item), policies = listOf(policy), valuations = listOf(value))
    private fun merge(a: WealthBackup, b: WealthBackup) = a.copy(
        items = (a.items + b.items).associateBy { it.id }.values.toList(),
        policies = a.policies + b.policies, valuations = a.valuations + b.valuations)
    private fun net(data: WealthBackup, day: String, sources: List<WealthSourceValue> = emptyList()) =
        WealthCalculator.calculate(data, sources, LocalDate.parse(day), "AED").netWorth.stripTrailingZeros()

    @Test fun `retyping asset as debt subtracts it now without changing January`() {
        val debt = item.copy(kind = WealthKind.CREDIT_CARD) // Old editor left the asset convention behind.
        val saved = merge(original, planWealthSave(original, debt, policy.copy(id = "new", effectiveDate = oct), value.copy(id = "new-value", effectiveDate = oct)))
        assertEquals(BigDecimal("5E+2"), net(saved, "2026-01-31"))
        assertEquals(BigDecimal("-5E+2"), net(saved, "2026-10-24"))
        assertEquals(item, saved.items.first { it.id == item.id })
    }
    @Test fun `legacy inconsistent manual type cannot silently increase a trusted total`() {
        val inconsistent = original.copy(items = listOf(item.copy(kind = WealthKind.CREDIT_CARD)))
        val result = WealthCalculator.calculate(inconsistent, emptyList(), LocalDate.parse(oct), "AED")
        assertFalse(result.complete)
        assertNull(result.lines.single().reportingAmount)
        assertTrue(result.issues.single().contains("Review"))
    }
    @Test fun `switching a manual total to holdings leaves earlier totals intact`() {
        val saved = merge(original, planWealthSave(original, item.copy(source = WealthSource.HOLDINGS), policy.copy(id = "new", effectiveDate = oct), null))
        assertEquals(net(original, "2026-01-31"), net(saved, "2026-01-31"))
        assertTrue(saved.policyOn(item.id, oct)!!.archived)
        assertEquals(oct, saved.items.single { it.id != item.id }.createdDate)
    }
    @Test fun `ordinary value corrections keep the existing position ID`() {
        val update = planWealthSave(original, item.copy(name = "Renamed"), policy.copy(id = "new", effectiveDate = oct), value.copy(id = "new-value", effectiveDate = oct))
        assertEquals(item.id, update.items.single().id)
        assertEquals(1, update.policies.size)
    }
    @Test fun `same day replacement archival wins timestamp ties`() {
        val saved = merge(original, planWealthSave(original, item.copy(source = WealthSource.HOLDINGS), policy.copy(id = "new"), null))
        assertTrue(saved.policyOn(item.id, jan)!!.archived)
    }
    @Test fun `backdated structure changes cannot invalidate later recorded decisions`() {
        val data = original.copy(valuations = listOf(value.copy(effectiveDate = oct)))
        assertThrows(IllegalArgumentException::class.java) {
            planWealthSave(data, item.copy(source = WealthSource.HOLDINGS), policy.copy(effectiveDate = "2026-09-01"), null)
        }
    }
    @Test fun `linked source setup generations cannot overlap or be accidentally reactivated`() {
        val linked = item.copy(source = WealthSource.ACCOUNT, sourceId = "bank")
        val data = original.copy(items = listOf(linked), valuations = emptyList())
        val payload = planWealthSave(data, linked.copy(convention = BalanceConvention.POSITIVE_OWED), policy.copy(id = "new", effectiveDate = oct), null)
        val saved = merge(data, payload)
        validateWealthSourceLinks(saved)
        val sources = saved.items.map { WealthSourceValue(it.id, BigDecimal("500")) }
        assertEquals(BigDecimal("5E+2"), net(saved, "2026-01-31", sources))
        assertEquals(BigDecimal("-5E+2"), net(saved, "2026-10-24", sources))
        assertThrows(IllegalArgumentException::class.java) {
            validateWealthSourceLinks(saved.copy(policies = saved.policies + policy.copy(id = "reactivate", effectiveDate = "2026-11-01")))
        }
    }
    @Test fun `revising a group keeps holdings in both historical and current views`() {
        val group = item.copy(source = WealthSource.HOLDINGS)
        val child = item.copy(id = "child", parentId = group.id)
        val data = original.copy(items = listOf(group, child), policies = listOf(policy, policy.copy(id = "child-policy", itemId = child.id)),
            valuations = listOf(value.copy(itemId = child.id)))
        val saved = merge(data, planWealthSave(data, group.copy(kind = WealthKind.RETIREMENT), policy.copy(id = "new", effectiveDate = oct), null))
        assertEquals(net(data, "2026-01-31"), net(saved, "2026-01-31"))
        assertEquals(net(data, "2026-01-31"), net(saved, "2026-10-24"))
    }
    @Test fun `backdated review cannot supersede a later effective review`() {
        val later = WealthReview("oct", oct, true, true, "2026-10-01T12:00:00Z")
        val backdated = WealthReview("jan", jan, true, false, stamp)
        val reviews = listOf(later, backdated)
        assertEquals(later, reviews.reviewOn("2026-10-24"))
        assertEquals(backdated, reviews.reviewOn("2026-01-31"))
        assertNull(reviews.reviewOn("2025-12-31"))
    }
}
