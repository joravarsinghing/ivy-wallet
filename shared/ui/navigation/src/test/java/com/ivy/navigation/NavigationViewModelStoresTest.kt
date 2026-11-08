package com.ivy.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Test

class NavigationViewModelStoresTest {
    class TrackingModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }
    @Test fun `leaving one destination clears only its own store`() {
        val stores = NavigationViewModelStores()
        val factory = ViewModelProvider.NewInstanceFactory()
        val wealth = ViewModelProvider(stores.forScreen(NetWorthScreen), factory)[TrackingModel::class.java]
        val other = ViewModelProvider(stores.forScreen(GoalsScreen), factory)[TrackingModel::class.java]
        assertSame(stores.forScreen(NetWorthScreen), stores.forScreen(NetWorthScreen))
        stores.clear(NetWorthScreen)
        assertTrue(wealth.cleared)
        assertFalse(other.cleared)
    }
}
