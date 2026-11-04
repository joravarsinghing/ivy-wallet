package com.ivy.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Stack
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Stable
@Singleton
class Navigation @Inject constructor() {
  var currentScreen: Screen? by mutableStateOf(null)
    private set
  var currentEntryId: UUID? by mutableStateOf(null)
    private set

  @Deprecated("Legacy code. Don't use it, please.")
  val modalBackHandling: Stack<ModalBackHandler> = Stack()

  @Deprecated("Legacy code. Use Compose `BackHandler {}` instead.")
  val onBackPressed: MutableMap<Screen, () -> Boolean> = mutableMapOf()

  private data class Entry(val screen: Screen, val id: UUID)
  private val backStack: Stack<Entry> = Stack()
  var lastScreen: Screen? = null
    private set

  @Deprecated("Legacy code. Don't use it, please.")
  data class ModalBackHandler(
    val id: UUID,
    val onBackPressed: () -> Boolean
  )

  @Deprecated("Legacy code. Don't use it, please.")
  fun lastModalBackHandlerId(): UUID? {
    return if (modalBackHandling.isEmpty()) {
      null
    } else {
      modalBackHandling.peek().id
    }
  }

  /** Legacy-host bridge. Disposal removes only this registration, even out of order. */
  @Suppress("DEPRECATION")
  fun registerModalBackHandler(id: UUID, onBackPressed: () -> Boolean): () -> Unit {
    val handler = ModalBackHandler(id, onBackPressed)
    modalBackHandling.removeAll { it.id == id }
    modalBackHandling.push(handler)
    return { modalBackHandling.removeAll { it === handler }; Unit }
  }

  @Suppress("DEPRECATION")
  fun registerScreenBackHandler(screen: Screen, handler: () -> Boolean): () -> Unit {
    onBackPressed[screen] = handler
    return { if (onBackPressed[screen] === handler) onBackPressed.remove(screen); Unit }
  }

  fun retainsEntry(id: UUID): Boolean = currentEntryId == id || backStack.any { it.id == id }

  /** Activity saved state holds primitive keys only, never handlers, credentials or records. */
  fun saveState(): ArrayList<String>? {
    val current = currentScreen ?: return null
    val id = currentEntryId ?: return null
    return NavigationSavedState.encode(backStack.map { it.id to it.screen } + (id to current))
  }

  fun savedDestinations(saved: List<String>): List<Screen> = NavigationSavedState.decode(saved)?.map { it.second }.orEmpty()

  fun restoreState(saved: List<String>, usable: (Screen) -> Boolean = { true }): Boolean {
    if (currentScreen != null) return false // A retained live session always wins on rotation.
    val entries = NavigationSavedState.decode(saved)?.filter { usable(it.second) }?.takeIf { it.isNotEmpty() } ?: return false
    backStack.clear()
    modalBackHandling.clear()
    onBackPressed.clear()
    entries.dropLast(1).forEach { (id, screen) -> backStack.push(Entry(screen, id)) }
    val (id, screen) = entries.last()
    switchScreen(screen, id)
    return true
  }

  fun navigateTo(screen: Screen) {
    val previous = lastScreen
    val previousId = currentEntryId
    if (previous != null && previousId != null) {
      backStack.push(Entry(previous, previousId))
    }
    switchScreen(screen, UUID.randomUUID())
  }

  fun backStackEmpty() = backStack.empty()

  private fun popBackStack() {
    backStack.pop()
  }

  @Deprecated("Legacy code. Don't use it, please.")
  fun onBackPressed(): Boolean {
    if (modalBackHandling.isNotEmpty()) {
      return modalBackHandling.peek().onBackPressed()
    }
    val specialHandling = onBackPressed.getOrDefault(currentScreen) { false }.invoke()
    return specialHandling || back()
  }

  fun back(): Boolean {
    if (!backStack.empty()) {
      val entry = backStack.pop()
      switchScreen(entry.screen, entry.id)
      return true
    }
    return false
  }

  private fun switchScreen(screen: Screen, entryId: UUID) {
    this.currentScreen = screen
    currentEntryId = entryId
    lastScreen = screen
  }

  fun resetBackStack() {
    while (!backStackEmpty()) {
      popBackStack()
    }
    lastScreen = null
  }
}
