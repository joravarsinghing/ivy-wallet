package com.ivy.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.legacy.data.model.MainTab
import kotlin.math.abs

internal data class MainDestination(val tab: MainTab, val label: String, val icon: ImageVector)
internal val mainDestinations = listOf(
    MainDestination(MainTab.HOME, "Home", Icons.Outlined.Home),
    MainDestination(MainTab.ACCOUNTS, "Accounts", Icons.Outlined.AccountBalanceWallet),
    MainDestination(MainTab.PLAN, "Plan", Icons.Outlined.CalendarMonth),
    MainDestination(MainTab.NET_WORTH, "Net worth", Icons.Outlined.AccountBalance),
    MainDestination(MainTab.MORE, "More", Icons.Outlined.GridView),
)

internal fun mainNavigationRailWidth(width: Dp): Dp = when {
    width < 600.dp -> 0.dp
    width < 1100.dp -> 96.dp
    else -> 200.dp
}
internal fun mainBottomNavigationHeight(fontScale: Float): Dp = if (fontScale > 1.3f) 112.dp else 80.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.BottomBar(
    tab: MainTab, selectTab: (MainTab) -> Unit,
    onAddIncome: () -> Unit, onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit, onAddPlannedPayment: () -> Unit,
    showAddAccountModal: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rail = mainNavigationRailWidth(maxWidth)
    val bottomHeight = mainBottomNavigationHeight(LocalDensity.current.fontScale)
    MainNavigation(tab, selectTab, rail, Modifier.align(if (rail == 0.dp) Alignment.BottomCenter else Alignment.CenterStart))
    val expense by rememberUpdatedState(onAddExpense)
    val income by rememberUpdatedState(onAddIncome)
    val transfer by rememberUpdatedState(onAddTransfer)
    if (tab == MainTab.HOME || tab == MainTab.ACCOUNTS) {
        FloatingActionButton(
            onClick = { if (tab == MainTab.ACCOUNTS) showAddAccountModal() else expanded = true },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = if (rail == 0.dp) bottomHeight + 16.dp else 24.dp)
                .testTag("fab_add").pointerInput(tab) {
                    if (tab == MainTab.HOME) {
                        var offset = Offset.Zero
                        var triggered = false
                        detectDragGestures(onDragStart = { offset = Offset.Zero; triggered = false }, onDragEnd = { offset = Offset.Zero }, onDragCancel = { offset = Offset.Zero }) { change, movement ->
                            change.consume(); offset += movement
                            if (!triggered && offset.y < -60) {
                                triggered = true
                                when { abs(offset.x) < 40 -> expense(); offset.x < 0 -> income(); else -> transfer() }
                            }
                        }
                    }
                },
            containerColor = MaterialTheme.colorScheme.primary,
        ) { Icon(Icons.Outlined.Add, if (tab == MainTab.ACCOUNTS) "Add account" else "Add income, expense, transfer or planned payment") }
    }
    if (expanded) ModalBottomSheet(onDismissRequest = { expanded = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add to your wallet", style = MaterialTheme.typography.headlineSmall)
            Text("Choose what happened. Transfers move money between your accounts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(
                Triple("Expense", "Money you spent", onAddExpense),
                Triple("Income", "Money you received", onAddIncome),
                Triple("Transfer", "Move money between your accounts", onAddTransfer),
                Triple("Planned payment", "Set up a bill or a future payment", onAddPlannedPayment),
            ).forEach { (title, description, action) ->
                OutlinedCard(onClick = { expanded = false; action() }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

@Composable
internal fun MainNavigation(tab: MainTab, selectTab: (MainTab) -> Unit, railWidth: Dp, modifier: Modifier = Modifier) {
    if (railWidth == 0.dp) {
        if (LocalDensity.current.fontScale > 1.3f) {
            // Shifting navigation gives the selected label real width at accessibility
            // font sizes. All five targets remain visible and have spoken labels.
            Surface(modifier.fillMaxWidth(), tonalElevation = 1.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 112.dp), verticalAlignment = Alignment.CenterVertically) {
                    mainDestinations.forEach { item ->
                        val active = tab == item.tab
                        Column(Modifier.then(if (active) Modifier.weight(1f) else Modifier.width(48.dp)).heightIn(min = 104.dp)
                            .selectable(selected = active, role = Role.Tab, onClick = { selectTab(item.tab) })
                            .testTag("nav_${item.tab.name.lowercase()}").padding(horizontal = 4.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                            Icon(item.icon, if (active) null else item.label, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            if (active) Text(item.label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        } else NavigationBar(modifier.fillMaxWidth().heightIn(min = 80.dp), containerColor = MaterialTheme.colorScheme.surface) {
            mainDestinations.forEach { item ->
                NavigationBarItem(selected = tab == item.tab, onClick = { selectTab(item.tab) },
                    // Material's inner padding otherwise breaks "Accounts" into letters
                    // on 320dp phones. Labels can use the whole destination slot.
                    icon = { Icon(item.icon, null) }, label = { Text(item.label, Modifier.wrapContentWidth(unbounded = true), textAlign = TextAlign.Center) }, modifier = Modifier.testTag("nav_${item.tab.name.lowercase()}"),
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.onSurface))
            }
        }
    } else {
        Surface(modifier.width(railWidth).fillMaxHeight(), tonalElevation = 1.dp) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(com.ivy.ui.R.drawable.jvy_wallet_logo), "Jvy Wallet", Modifier.padding(vertical = 12.dp).size(44.dp))
                if (railWidth >= 200.dp) Text("Jvy Wallet", style = MaterialTheme.typography.titleMedium)
                mainDestinations.forEach { item ->
                    Surface(onClick = { selectTab(item.tab) }, shape = MaterialTheme.shapes.large,
                        color = if (tab == item.tab) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        contentColor = if (tab == item.tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("nav_${item.tab.name.lowercase()}").semantics { role = Role.Tab; selected = tab == item.tab }) {
                        if (railWidth >= 200.dp) Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(item.icon, null); Text(item.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        } else Column(Modifier.padding(vertical = 8.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(item.icon, null); Text(item.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
