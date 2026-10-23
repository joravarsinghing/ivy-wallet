package com.ivy.main

import androidx.compose.ui.unit.dp
import com.ivy.legacy.data.model.MainTab
import com.ivy.navigation.*
import org.junit.Assert.*
import org.junit.Test

class MainDestinationTest {
    @Test fun `every primary section is directly accessible and unique`() {
        assertEquals(MainTab.entries.toSet(), mainDestinations.map { it.tab }.toSet())
        assertEquals(5, mainDestinations.size)
        assertEquals("Net worth", mainDestinations.single { it.tab == MainTab.NET_WORTH }.label)
    }
    @Test fun `planning and more retain existing destinations`() {
        assertEquals(setOf(BudgetScreen, PlannedPaymentsScreen, GoalsScreen, LoansScreen), planningDestinations.map { it.screen }.toSet())
        assertTrue(moreDestinations.any { it.screen == NetWorthScreen })
        assertTrue(moreDestinations.any { it.screen == SecurityScreen })
        assertTrue(moreDestinations.any { it.screen == ReportScreen })
        assertTrue(moreDestinations.any { it.screen == SettingsScreen })
    }
    @Test fun `rails reserve space only where the window can support them`() {
        assertEquals(0.dp, mainNavigationRailWidth(359.dp))
        assertEquals(0.dp, mainNavigationRailWidth(599.dp))
        assertEquals(96.dp, mainNavigationRailWidth(600.dp))
        assertEquals(96.dp, mainNavigationRailWidth(720.dp))
        assertEquals(96.dp, mainNavigationRailWidth(1099.dp))
        assertEquals(200.dp, mainNavigationRailWidth(1100.dp))
        assertEquals(200.dp, mainNavigationRailWidth(1280.dp))
    }
    @Test fun `larger text reserves more room for navigation labels`() {
        assertEquals(80.dp, mainBottomNavigationHeight(1f))
        assertEquals(112.dp, mainBottomNavigationHeight(2f))
    }
}
