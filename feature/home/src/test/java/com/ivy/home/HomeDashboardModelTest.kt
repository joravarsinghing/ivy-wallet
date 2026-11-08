package com.ivy.home

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class HomeDashboardModelTest {
    @Test
    fun defaultsExposeAllSupportedCardsInStableOrder() {
        assertEquals(
            listOf(
                HomeDashboardCardId.MONEY_TRENDS,
                HomeDashboardCardId.SPENDING_BREAKDOWN,
                HomeDashboardCardId.RECENT_ACTIVITY,
                HomeDashboardCardId.SAFE_TO_SPEND,
                HomeDashboardCardId.MONEY_COMING_UP,
                HomeDashboardCardId.SAVED_REPORTS,
            ),
            defaultHomeDashboardCards.map { it.id },
        )
        assertEquals(
            defaultHomeDashboardCards.map { it.id }.toSet(),
            defaultVisibleHomeDashboardCards,
        )
    }

    @Test
    fun toggleRemovesVisibleCardAndAddsHiddenCard() {
        val hidden = toggleHomeDashboardCard(
            visibleCards = defaultVisibleHomeDashboardCards,
            card = HomeDashboardCardId.MONEY_TRENDS,
        )
        assertFalse(HomeDashboardCardId.MONEY_TRENDS in hidden)

        val restored = toggleHomeDashboardCard(hidden, HomeDashboardCardId.MONEY_TRENDS)
        assertTrue(HomeDashboardCardId.MONEY_TRENDS in restored)
        assertEquals(defaultVisibleHomeDashboardCards, restored)
    }

    @Test fun `ordering deduplicates unknown preferences and appends new cards`() {
        val order = normalizedHomeCardOrder(listOf("safe_to_spend", "unknown", "safe_to_spend"))
        assertEquals(HomeDashboardCardId.SAFE_TO_SPEND, order.first())
        assertEquals(HomeDashboardCardId.entries.toSet(), order.toSet())
        assertEquals(order.size, order.distinct().size)
        val moved = moveHomeCard(order, HomeDashboardCardId.MONEY_TRENDS, -1)
        assertEquals(HomeDashboardCardId.MONEY_TRENDS, moved.first())
        assertEquals(moved, moveHomeCard(moved, moved.first(), -1))
    }
    @Test fun `every optional card may be hidden without removing shared controls`() {
        var visible = defaultVisibleHomeDashboardCards
        HomeDashboardCardId.entries.forEach { visible = toggleHomeDashboardCard(visible, it) }
        assertTrue(visible.isEmpty())
    }
}
