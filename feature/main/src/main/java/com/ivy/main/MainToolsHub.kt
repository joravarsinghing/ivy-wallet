package com.ivy.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ivy.navigation.*

internal data class ToolDestination(val title: String, val description: String, val icon: ImageVector, val screen: Screen)
internal val planningDestinations = listOf(
    ToolDestination("Budgets", "Give each spending category a sensible limit", Icons.Outlined.Tune, BudgetScreen),
    ToolDestination("Upcoming payments", "See bills and money coming in before they arrive", Icons.Outlined.EventNote, PlannedPaymentsScreen),
    ToolDestination("Savings goals", "Reserve money for things that matter to you", Icons.Outlined.Flag, GoalsScreen),
    ToolDestination("Loans & money owed", "Keep track of what you owe or have lent", Icons.Outlined.Handshake, LoansScreen),
)
internal val moreDestinations = listOf(
    ToolDestination("Reports & insights", "Explore your income and spending", Icons.Outlined.BarChart, ReportScreen),
    ToolDestination("Search transactions", "Find a payment by name, amount or category", Icons.Outlined.Search, SearchScreen),
    ToolDestination("Categories", "Organise the way you earn and spend", Icons.Outlined.Category, CategoriesScreen),
    ToolDestination("Security & screen capture", "PIN, fingerprint, recovery and marketing captures", Icons.Outlined.Security, SecurityScreen),
    ToolDestination("Settings & backups", "Appearance, currency, exports and restoring your data", Icons.Outlined.Settings, SettingsScreen),
)

@Composable
internal fun MainToolsHub(planning: Boolean) {
    val nav = navigation()
    MainToolsHubContent(planning, nav::navigateTo)
}

/** Sections index existing features, not new financial/storage implementations. */
@Composable
internal fun MainToolsHubContent(planning: Boolean, open: (Screen) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
        val wide = maxWidth >= 840.dp
        val entries = if (planning) planningDestinations else moreDestinations
        LazyColumn(Modifier.widthIn(max = 1200.dp).fillMaxSize().align(Alignment.TopCenter), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 128.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item(key = "heading", contentType = "heading") {
                Text(if (planning) "Make a plan" else "Your money tools", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(if (planning) "One small step at a time. Choose where you'd like a little more control." else "Everything has a place. Find insights, organise your wallet, and choose your privacy settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item(key = "section", contentType = "section") { Text(if (planning) "LOOK AHEAD" else "EXPLORE & MANAGE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(entries.chunked(if (wide) 2 else 1), key = { it.first().title }, contentType = { "tool-row" }) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { item ->
                        OutlinedCard(onClick = { open(item.screen) }, modifier = Modifier.weight(1f)) {
                            Row(Modifier.padding(16.dp).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp))
                            }
                        }
                    }
                    if (wide && row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (planning) item(key = "outlook", contentType = "outlook") {
                HorizontalDivider(); Spacer(Modifier.height(16.dp))
                Text("Looking further ahead?", style = MaterialTheme.typography.titleMedium)
                Text("Your net worth brings savings, investments, things you own and debts into one picture.")
                Text("Open the Net worth tab when you're ready to see your bigger financial picture.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
