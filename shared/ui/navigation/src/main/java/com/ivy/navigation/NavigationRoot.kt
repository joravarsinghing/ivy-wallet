package com.ivy.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalNavigation = compositionLocalOf<Navigation> { error("No LocalNavigation") }

@Composable
fun NavigationRoot(
    navigation: Navigation,
    navGraph: @Composable (screen: Screen?) -> Unit
) {
    CompositionLocalProvider(
        LocalNavigation provides navigation,
    ) {
        // Pure previews/screenshot hosts may have no Activity owner. They do not
        // instantiate destination ViewModels, so no retained stores are needed.
        val owner = LocalViewModelStoreOwner.current
        val stores: NavigationViewModelStores? = if (owner != null) {
            viewModel(viewModelStoreOwner = owner)
        } else null
        val context = LocalContext.current
        val currentScreen = navigation.currentScreen
        DisposableEffect(navigation.currentScreen) {
            onDispose {
                // Never clear the Activity's store: it contains root/authentication state.
                if (currentScreen?.isLegacy == false && !context.isChangingActivityConfigurations()) stores?.clear(currentScreen)
            }
        }
        navGraph(navigation.currentScreen)
    }
}

@Composable
fun navigation(): Navigation {
    return LocalNavigation.current
}

/**
 * Provides a [ViewModel] instance scoped the screen's life.
 * When the user navigates away from the screen all screen scoped
 * viewModels are destroyed.
 * Does not apply for legacy screens.
 */
@Composable
inline fun <reified T : ViewModel> screenScopedViewModel(
    factory: ViewModelProvider.Factory? = null
): T {
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    requireNotNull(viewModelStoreOwner) { "No ViewModelStoreOwner provided" }
    val screen = navigation().currentScreen
    val stores: NavigationViewModelStores = viewModel()
    val selectedFactory = factory ?: (viewModelStoreOwner as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory
        ?: ViewModelProvider.NewInstanceFactory()
    val extras = (viewModelStoreOwner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
    val store = if (screen?.isLegacy == false) stores.forScreen(screen) else viewModelStoreOwner.viewModelStore
    val viewModelProvider = remember(store, selectedFactory) { ViewModelProvider(store, selectedFactory, extras) }
    return viewModelProvider[T::class.java]
}

/** Stores survive configuration changes with the Activity holder, without owning root state. */
class NavigationViewModelStores : ViewModel() {
    private val stores = mutableMapOf<Screen, ViewModelStore>()
    fun forScreen(screen: Screen): ViewModelStore = stores.getOrPut(screen) { ViewModelStore() }
    fun clear(screen: Screen) { stores.remove(screen)?.clear() }
    override fun onCleared() { stores.values.forEach { it.clear() }; stores.clear() }
}

private tailrec fun android.content.Context.isChangingActivityConfigurations(): Boolean = when (this) {
    is android.app.Activity -> isChangingConfigurations
    is android.content.ContextWrapper -> baseContext.isChangingActivityConfigurations()
    else -> false
}
