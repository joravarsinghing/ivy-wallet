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
                HomeDashboardCardId.SAFE_TO_SPEND,
                HomeDashboardCardId.MONEY_COMING_UP,
                HomeDashboardCardId.MONEY_TRENDS,
                HomeDashboardCardId.SPENDING_BREAKDOWN,
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
}
