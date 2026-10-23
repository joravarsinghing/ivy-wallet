package com.ivy.home

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.navigation.*

internal val homeMenuDestinations: List<Pair<String, Screen>> = listOf(
    "Search transactions" to SearchScreen,
    "Net worth" to NetWorthScreen,
    "Reports & insights" to ReportScreen,
    "Budgets" to BudgetScreen,
    "Upcoming payments" to PlannedPaymentsScreen,
    "Savings goals" to GoalsScreen,
    "Loans & money owed" to LoansScreen,
    "Categories" to CategoriesScreen,
    "Security & screen capture" to SecurityScreen,
    "Settings & backups" to SettingsScreen,
)

/** An anchored, scrollable menu replaces the screen-coordinate animated overlay. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun BoxWithConstraintsScope.MoreMenu(
    expanded: Boolean, balance: Double, buffer: Double, currency: String, theme: Theme,
    setExpanded: (Boolean) -> Unit, onSwitchTheme: () -> Unit,
    onBufferClick: () -> Unit, onCurrencyClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    Box(modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 12.dp, end = 16.dp)) {
        IconButton(onClick = { setExpanded(!expanded) }, modifier = Modifier.testTag("home_more_menu_arrow")) {
            Icon(Icons.Outlined.MoreHoriz, "Open all money tools")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { setExpanded(false) }, modifier = Modifier.widthIn(min = 280.dp, max = 340.dp).heightIn(max = 560.dp)) {
            Text("Your money tools", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall)
            homeMenuDestinations.forEach { (label, screen) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { setExpanded(false); nav.navigateTo(screen) })
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Wallet currency · $currency") }, onClick = { setExpanded(false); onCurrencyClick() })
            DropdownMenuItem(text = { Text("Change appearance · ${theme.name.lowercase()}") }, onClick = { setExpanded(false); onSwitchTheme() })
            DropdownMenuItem(text = { Text("Share Jvy Wallet") }, onClick = {
                setExpanded(false)
                runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Jvy Wallet · ${com.ivy.legacy.Constants.URL_JVY_WALLET_REPO}")
                }, "Share Jvy Wallet")) }
            })
            DropdownMenuItem(text = { Text("About & open source") }, onClick = { setExpanded(false); runCatching { uri.openUri(com.ivy.legacy.Constants.URL_JVY_WALLET_REPO) } })
        }
    }
}
