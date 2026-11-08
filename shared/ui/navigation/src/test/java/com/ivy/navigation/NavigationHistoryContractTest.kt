package com.ivy.navigation

import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class NavigationHistoryContractTest {
    @Test fun `equal destinations have independent visit identities and Back restores the previous one`() {
        val nav = Navigation()
        val editor = EditTransactionScreen(null, com.ivy.base.model.TransactionType.EXPENSE)
        nav.navigateTo(editor)
        val first = nav.currentEntryId
        nav.navigateTo(editor.copy())
        val second = nav.currentEntryId
        assertNotEquals(first, second)
        assertTrue(nav.retainsEntry(first!!))
        assertTrue(nav.retainsEntry(second!!))
        assertTrue(nav.back())
        assertEquals(first, nav.currentEntryId)
        assertFalse(nav.retainsEntry(second))
    }

    @Test fun `returning to Add after leaving the previous editor starts a new visit`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        val editor = EditPlannedScreen(null, com.ivy.base.model.TransactionType.EXPENSE)
        nav.navigateTo(editor)
        val previous = nav.currentEntryId
        nav.back()
        nav.navigateTo(editor)
        assertNotEquals(previous, nav.currentEntryId)
        assertFalse(nav.retainsEntry(previous!!))
    }

    @Test fun `pending editor save consumes system Back without discarding its page`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        val editor = EditTransactionScreen(null, com.ivy.base.model.TransactionType.INCOME)
        nav.navigateTo(editor)
        var pending = true
        val unregister = nav.registerScreenBackHandler(editor) { pending }
        assertTrue(nav.onBackPressed())
        assertEquals(editor, nav.currentScreen)
        pending = false
        assertTrue(nav.onBackPressed())
        assertEquals(MainScreen, nav.currentScreen)
        unregister()
    }

    @Test fun `a stale screen handler disposer cannot remove the current registration`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        val stale = nav.registerScreenBackHandler(MainScreen) { fail("Replaced"); true }
        val current = nav.registerScreenBackHandler(MainScreen) { true }
        stale()
        assertTrue(nav.onBackPressed())
        current()
        assertFalse(nav.onBackPressed())
    }

    @Test fun `resetting history does not retain a discarded earlier visit`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        val previous = nav.currentEntryId
        nav.navigateTo(GoalsScreen)
        nav.resetBackStack()
        nav.navigateTo(MainScreen)
        assertFalse(nav.retainsEntry(previous!!))
        assertTrue(nav.backStackEmpty())
    }
    @Test fun `disposing a parent before its nested dialog cannot leave a ghost back handler`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        nav.navigateTo(GoalsScreen)
        var parentCalls = 0
        var childCalls = 0
        val closeParent = nav.registerModalBackHandler(UUID.randomUUID()) { parentCalls++; true }
        val closeChild = nav.registerModalBackHandler(UUID.randomUUID()) { childCalls++; true }
        closeParent()
        assertTrue(nav.onBackPressed())
        assertEquals(0, parentCalls)
        assertEquals(1, childCalls)
        closeChild()
        assertTrue(nav.onBackPressed())
        assertEquals(MainScreen, nav.currentScreen)
    }
    @Test fun `a stale disposer cannot unregister a replacement with the same modal id`() {
        val nav = Navigation()
        val id = UUID.randomUUID()
        var calls = 0
        val stale = nav.registerModalBackHandler(id) { fail("Replaced handler"); true }
        val current = nav.registerModalBackHandler(id) { calls++; true }
        stale()
        assertTrue(nav.onBackPressed())
        assertEquals(1, calls)
        current()
        assertFalse(nav.onBackPressed())
    }
    @Test fun `disposing a child restores its live parent and disposal is idempotent`() {
        val nav = Navigation()
        var calls = 0
        val closeParent = nav.registerModalBackHandler(UUID.randomUUID()) { calls++; true }
        val closeChild = nav.registerModalBackHandler(UUID.randomUUID()) { fail("Disposed child"); true }
        closeChild()
        closeChild()
        assertTrue(nav.onBackPressed())
        assertEquals(1, calls)
        closeParent()
        assertFalse(nav.onBackPressed())
    }
    @Test fun `subsections return through the existing stack without jumping to Home`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        nav.navigateTo(BudgetScreen)
        nav.navigateTo(PlannedPaymentsScreen)
        assertTrue(nav.back())
        assertEquals(BudgetScreen, nav.currentScreen)
        assertTrue(nav.back())
        assertEquals(MainScreen, nav.currentScreen)
        assertFalse(nav.back())
    }
    @Test fun `flow back handlers retain ownership of substeps`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        val screen = ImportScreen(false)
        nav.navigateTo(screen)
        var substep = 2
        nav.onBackPressed[screen] = { if (substep > 0) { substep--; true } else false }
        assertTrue(nav.onBackPressed())
        assertEquals(1, substep)
        assertEquals(screen, nav.currentScreen)
        nav.onBackPressed()
        nav.onBackPressed()
        assertEquals(MainScreen, nav.currentScreen)
    }
    @Test fun `dialog dismissal takes precedence over page navigation`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        nav.navigateTo(GoalsScreen)
        var dismissals = 0
        nav.modalBackHandling.push(Navigation.ModalBackHandler(UUID.randomUUID()) { dismissals++; true })
        assertTrue(nav.onBackPressed())
        assertEquals(1, dismissals)
        assertEquals(GoalsScreen, nav.currentScreen)
        nav.modalBackHandling.pop()
        assertTrue(nav.onBackPressed())
        assertEquals(MainScreen, nav.currentScreen)
    }
}
