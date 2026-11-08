package com.ivy.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.data.model.IntervalType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.system.*
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.money.formatDecimalMoney
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
internal fun HomeCurrentHeader(name: String, currency: String, balance: BigDecimal, hidden: Boolean, openBalance: () -> Unit) {
    val nav = navigation()
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (name.isBlank()) stringResource(R.string.hi) else stringResource(R.string.hi_name, name),
                Modifier.weight(1f).testTag("home_greeting_text"), style = MaterialTheme.typography.titleLarge)
            IconButton({ nav.navigateTo(SearchScreen) }, Modifier.testTag("home_search")) {
                Icon(Icons.Outlined.Search, stringResource(R.string.jvy_home_search))
            }
        }
        Text(stringResource(R.string.jvy_home_current_balance), style = JvyTypography.Supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (hidden) "••••" else financialText(formatDecimalMoney(balance, currency)),
            Modifier.fillMaxWidth().clip(JvyShapes.Control).clickable(role = Role.Button, onClick = openBalance)
                .heightIn(min = 48.dp).padding(vertical = 8.dp).testTag("home_balance"),
            style = JvyTypography.Amount, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
internal fun homePeriodLabel(selection: HomePeriodSelection): String = when (selection) {
    HomePeriodSelection.CurrentCycle -> stringResource(R.string.jvy_home_this_month)
    is HomePeriodSelection.BudgetMonth -> selection.month.format(DateTimeFormatter.ofPattern("LLLL yyyy"))
    is HomePeriodSelection.Rolling -> when (selection.unit) {
        IntervalType.DAY -> stringResource(R.string.jvy_home_last_days, selection.count)
        IntervalType.WEEK -> stringResource(R.string.jvy_home_last_weeks, selection.count)
        IntervalType.MONTH -> stringResource(R.string.jvy_home_last_months, selection.count)
        IntervalType.YEAR -> stringResource(R.string.jvy_home_last_years, selection.count)
    }
    is HomePeriodSelection.Custom -> stringResource(R.string.jvy_home_custom_dates)
    HomePeriodSelection.AllTime -> stringResource(R.string.jvy_home_all_time)
}

internal val homeQuickPeriods: List<HomePeriodSelection> = listOf(HomePeriodSelection.CurrentCycle,
    HomePeriodSelection.Rolling(7, IntervalType.DAY), HomePeriodSelection.Rolling(30, IntervalType.DAY),
    HomePeriodSelection.Rolling(3, IntervalType.MONTH), HomePeriodSelection.Rolling(6, IntervalType.MONTH),
    HomePeriodSelection.Rolling(12, IntervalType.MONTH))

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeTimelineControls(selection: HomePeriodSelection, applied: HomePeriodSelection,
    window: HomeDateWindow?, loading: Boolean, error: String?, capped: Boolean,
    openChooser: () -> Unit, select: (HomePeriodSelection) -> Unit, retry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("home_period_controls"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JvyTextButton(openChooser, Modifier.testTag("home_period_chooser")) {
            Icon(Icons.Outlined.CalendarMonth, null)
            Spacer(Modifier.width(8.dp))
            Text(homePeriodLabel(selection))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            homeQuickPeriods.forEach { choice ->
                val accessibleLabel = homePeriodLabel(choice)
                FilterChip(selection == choice, { select(choice) }, label = { Text(when (choice) {
                    HomePeriodSelection.CurrentCycle -> homePeriodLabel(choice)
                    is HomePeriodSelection.Rolling -> "${choice.count}${if (choice.unit == IntervalType.DAY) "d" else "m"}"
                    else -> homePeriodLabel(choice)
                }) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("home_period_${choice.encode()}").semantics { contentDescription = accessibleLabel }, shape = JvyShapes.Control,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer))
            }
        }
        Text(window?.let { stringResource(R.string.jvy_home_showing_dates, it.displayLabel()) }
            ?: stringResource(if (loading) R.string.home_chart_range_loading else R.string.jvy_home_no_history),
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (selection != applied && (loading || error != null)) Text(
            stringResource(R.string.jvy_home_previous_period, homePeriodLabel(applied)),
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (capped && !loading && selection == applied) Text(stringResource(R.string.jvy_home_through_today),
            style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = JvyTypography.Supporting)
            JvyTextButton(retry) { Text(stringResource(R.string.jvy_startup_retry)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomePeriodSummary(stats: IncomeExpensePair, currency: String, hidden: Boolean, window: HomeDateWindow?, reveal: () -> Unit = {}) {
    val converter = LocalTimeConverter.current
    val nav = navigation()
    val minWidth = if (LocalDensity.current.fontScale > 1.3f) 240.dp else 144.dp
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(TransactionType.INCOME to stats.income, TransactionType.EXPENSE to stats.expense).forEach { (type, amount) ->
            val incoming = type == TransactionType.INCOME
            Card(onClick = { if (hidden) reveal() else window?.let { nav.navigateTo(PieChartStatisticScreen(type = type,
                dateRange = it.navigationRange(converter), cashFlowOnly = true)) } }, enabled = window != null,
                modifier = Modifier.widthIn(min = minWidth).weight(1f).testTag(if (type == TransactionType.INCOME) "home_card_income" else "home_card_expense"),
                shape = JvyShapes.Card, colors = CardDefaults.cardColors(
                    containerColor = if (incoming) JvySummaryColors.Income else JvySummaryColors.Expenses,
                    contentColor = if (incoming) JvySummaryColors.OnIncome else JvySummaryColors.OnExpenses)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(if (type == TransactionType.INCOME) R.string.income else R.string.expenses), style = MaterialTheme.typography.titleSmall)
                    Text(if (hidden) "••••" else financialText(formatDecimalMoney(amount, currency)), style = JvyTypography.AmountSmall)
                }
            }
        }
    }
}
