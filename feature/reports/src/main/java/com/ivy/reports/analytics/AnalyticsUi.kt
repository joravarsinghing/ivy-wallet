package com.ivy.reports.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.design.system.*
import com.ivy.reports.ReportScreenState
import com.ivy.ui.money.formatDecimalMoney
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

internal sealed interface ExploreLoad {
    data object Loading : ExploreLoad
    data object Failed : ExploreLoad
    data class Ready(val board: AnalyticsBoard) : ExploreLoad
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReportModeControls(explore: Boolean, onExplore: (Boolean) -> Unit) {
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !explore, onClick = { onExplore(false) }, label = { Text("Transactions") },
            modifier = Modifier.heightIn(min = 48.dp), shape = JvyShapes.Control)
        FilterChip(selected = explore, onClick = { onExplore(true) }, label = { Text("Explore") },
            modifier = Modifier.heightIn(min = 48.dp), shape = JvyShapes.Control)
    }
}

@Composable
internal fun rememberExploreLoad(state: ReportScreenState, section: ExploreSection, retry: Int): ExploreLoad {
    val vm: AnalyticsViewModel = viewModel()
    // Applied report identity and immutable result references, never pending filter-sheet drafts.
    return key(state.transactions, state.filter, state.baseCurrency, state.accounts, state.categories, section, retry, state.loading) {
        // A new request owns a fresh slot immediately: old amounts cannot flash under a new caption/currency.
        val result by produceState<ExploreLoad>(ExploreLoad.Loading) {
            if (!state.loading && state.filter != null) {
                value = try { ExploreLoad.Ready(vm.load(state, section)) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { ExploreLoad.Failed }
            }
        }
        result
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExploreControls(section: ExploreSection, onSection: (ExploreSection) -> Unit,
                             advanced: Boolean, onAdvanced: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Explore your money", style = JvyTypography.SectionTitle, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExploreSection.entries.forEach { item ->
                FilterChip(selected = section == item, onClick = { onSection(item) },
                    label = { Text(item.label()) }, modifier = Modifier.heightIn(min = 48.dp), shape = JvyShapes.Control)
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = advanced, role = Role.Checkbox, onValueChange = onAdvanced),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = advanced, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
            Text("Show advanced analysis", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

internal fun ExploreSection.label(): String = when (this) {
    ExploreSection.SPENDING -> "Spending"
    ExploreSection.CASH_FLOW -> "Cash flow"
    ExploreSection.WEALTH -> "Wealth"
    ExploreSection.DEBT -> "Debt"
    ExploreSection.GOALS -> "Goals"
}

internal fun LazyListScope.exploreItems(load: ExploreLoad, advanced: Boolean, onRetry: () -> Unit,
                                      onRecords: (AnalyticsCard, ChartValue) -> Unit,
                                      onWorkspace: (() -> Unit)? = null) {
    when (load) {
        ExploreLoad.Loading -> item(key = "explore-loading", contentType = "status") {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Preparing analysis…", style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        ExploreLoad.Failed -> item(key = "explore-failed", contentType = "status") {
            Column(Modifier.padding(24.dp)) {
                Text("Analysis couldn't load. Your records are unchanged.", color = MaterialTheme.colorScheme.onBackground)
                JvyTextButton(onRetry) { Text("Retry") }
            }
        }
        is ExploreLoad.Ready -> {
            item(key = "explore-caption", contentType = "scope") {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(load.board.caption, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onBackground)
                    load.board.notices.forEach { Text(it, style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    JvyTextButton(onRetry) { Text("Refresh analysis") }
                    if (onWorkspace != null) JvyTextButton(onWorkspace) { Text("Open workspace") }
                    if (load.board.cards.isEmpty()) Text("No chartable records in this view. Try another period or review the relevant workspace.",
                        style = JvyTypography.Body, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            items(load.board.cards.filter { advanced || !it.advanced }, key = { "explore-${it.id}" }, contentType = { "chart" }) { card ->
                AnalyticsChartCard(card, onRecords = { onRecords(card, it) })
            }
            item(key = "explore-bottom", contentType = "space") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
internal fun AnalyticsChartCard(card: AnalyticsCard, onRecords: (ChartValue) -> Unit = {}) =
    com.ivy.design.system.chart.JvyAnalyticsCard(card, onRecords = onRecords)
