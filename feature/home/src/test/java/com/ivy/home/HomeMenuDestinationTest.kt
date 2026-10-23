package com.ivy.home

import com.ivy.navigation.*
import org.junit.Assert.*
import org.junit.Test

class HomeMenuDestinationTest {
    @Test fun `net worth is near the top of the dropdown and all tools stay reachable`() {
        assertEquals(NetWorthScreen, homeMenuDestinations[1].second)
        assertEquals(homeMenuDestinations.size, homeMenuDestinations.map { it.second }.toSet().size)
        assertTrue(homeMenuDestinations.any { it.second == BudgetScreen })
        assertTrue(homeMenuDestinations.any { it.second == GoalsScreen })
        assertTrue(homeMenuDestinations.any { it.second == PlannedPaymentsScreen })
        assertTrue(homeMenuDestinations.any { it.second == SecurityScreen })
        assertTrue(homeMenuDestinations.any { it.second == SettingsScreen })
    }
}
