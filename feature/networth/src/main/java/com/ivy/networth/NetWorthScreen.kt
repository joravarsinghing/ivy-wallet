package com.ivy.networth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.data.model.wealth.*
import com.ivy.domain.usecase.networth.*
import com.ivy.navigation.navigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun money(value: BigDecimal?, currency: String, hidden: Boolean): String = when {
    hidden -> "••••"
    value == null -> "Value unavailable"
    else -> com.ivy.ui.money.formatDecimalMoney(value, currency)
}
internal fun WealthKind.title() = when (this) {
    WealthKind.CASH -> "Cash & bank balances"
    WealthKind.RECEIVABLE -> "Money someone owes me"
    WealthKind.OTHER_ASSET -> "Something else I own"
    WealthKind.BORROWED -> "Money I borrowed"
    WealthKind.OTHER_DEBT -> "Another debt"
    else -> name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}
internal val WealthKind.isDebt get() = this in setOf(WealthKind.CREDIT_CARD, WealthKind.MORTGAGE, WealthKind.PERSONAL_LOAN, WealthKind.VEHICLE_LOAN, WealthKind.STUDENT_LOAN, WealthKind.BORROWED, WealthKind.OTHER_DEBT)

/** The legacy mint container uses white text; use the higher-contrast brand pair. */
@Composable
internal fun wealthChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
fun NetWorthScreen() {
    val vm: NetWorthViewModel = com.ivy.navigation.screenScopedViewModel()
    val nav = navigation()
    Surface(Modifier.fillMaxSize()) { NetWorthScreenContent(vm, { nav.back() }) }
}

/** The main-shell tab reuses this feature rather than creating a second calculator. */
@Composable
fun NetWorthTab() {
    val vm: NetWorthViewModel = viewModel(key = "main-net-worth")
    Surface(Modifier.fillMaxSize()) { NetWorthScreenContent(vm, {}, embedded = true) }
}

@Composable
internal fun NetWorthScreenContent(vm: NetWorthViewModel, onBack: () -> Unit, initialTab: Int = 0, embedded: Boolean = false) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var addDebt by rememberSaveable { mutableStateOf(false) }
    var candidateKey by rememberSaveable { mutableStateOf<String?>(null) }
    var review by rememberSaveable { mutableStateOf(false) }
    var rates by rememberSaveable { mutableStateOf(false) }
    var guide by rememberSaveable { mutableStateOf(false) }
    var tools by remember { mutableStateOf(false) }
    var dateDialog by rememberSaveable { mutableStateOf(false) }
    var dateText by rememberSaveable { mutableStateOf(vm.date.toString()) }
    var exportHistory by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            try { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(vm.csv(exportHistory)) } ?: error("Cannot open file") } }
            catch (_: Exception) { exportError = "Could not export. Please choose another file location." }
        }
    }
    LaunchedEffect(Unit) { vm.load() }
    val workspace = vm.workspace
    val editor = workspace?.data?.items?.firstOrNull { it.id == editorId }
    val candidate = workspace?.candidates?.firstOrNull { "${it.source}:${it.id}" == candidateKey }
    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        val expanded = maxWidth >= 840.dp
        Row(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = if (embedded) 144.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    if (!embedded) TextButton(onClick = onBack) { Text("Back") }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Net worth", style = MaterialTheme.typography.headlineMedium)
                            Text("Your bigger financial picture", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { guide = true }) { Icon(Icons.Outlined.HelpOutline, "Open the net worth guide") }
                        Box {
                            IconButton(onClick = { tools = true }) { Icon(Icons.Outlined.MoreVert, "Net worth tools") }
                            DropdownMenu(expanded = tools, onDismissRequest = { tools = false }) {
                                DropdownMenuItem(text = { Text("How net worth works") }, onClick = { tools = false; guide = true })
                                DropdownMenuItem(text = { Text("View another date") }, onClick = { tools = false; dateDialog = true })
                                DropdownMenuItem(text = { Text("Currency & exchange rates") }, onClick = { tools = false; rates = true })
                                DropdownMenuItem(text = { Text("Export summary") }, onClick = { tools = false; exportHistory = false; export.launch("Jvy_wealth_${vm.date}.csv") })
                                DropdownMenuItem(text = { Text("Export history") }, onClick = { tools = false; exportHistory = true; export.launch("Jvy_wealth_history_${vm.date}.csv") })
                            }
                        }
                    }
                }
                item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Overview", "What I own", "What I owe", "Progress").forEachIndexed { index, name -> FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(name) }, colors = wealthChipColors()) }
                } }
                if (tab == 0 && workspace != null) item { Summary(workspace.current, vm.hidden) }
                if (tab == 0 && !vm.guideSeen) item {
                    TextButton(onClick = { guide = true }, modifier = Modifier.fillMaxWidth()) { Text("New to net worth? Let me walk you through it") }
                }
                if (workspace?.candidates?.isNotEmpty() == true) item {
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Start with accounts already in Jvy", style = MaterialTheme.typography.titleMedium)
                        Text("Use your existing balances instead of typing the same money or loan twice.")
                        Button(onClick = { review = true }, modifier = Modifier.fillMaxWidth()) { Text("Check my existing accounts & loans") }
                    } }
                }
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { adding = true; addDebt = false }, enabled = workspace != null && !vm.loading, modifier = Modifier.weight(1f)) { Text("Add what I own") }
                    Button(onClick = { adding = true; addDebt = true }, enabled = workspace != null && !vm.loading, modifier = Modifier.weight(1f)) { Text("Add what I owe") }
                } }
                item { OutlinedButton(onClick = { review = true }) { Text("Use accounts & loans I already track") } }
                if (vm.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                (vm.error ?: exportError)?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::load) { Text("Retry") } } }
                if (workspace == null && !vm.loading) item { Text("Add what you own and what you owe, or link existing accounts and loans.") }
                if (workspace != null) {
                    item { Text("Your picture on ${vm.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} · ${workspace.current.currency}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    when (tab) {
                        0 -> {
                            item { TextButton(onClick = { rates = true }) { Text("Currency and exchange rates · ${workspace.current.currency}") } }
                            if (workspace.current.issues.isNotEmpty()) item {
                                Text("Let's finish your picture", style = MaterialTheme.typography.titleMedium)
                                Text("${workspace.current.issues.size} thing(s) still need a value, currency rate or review. Your total is only an estimate so far.")
                                workspace.current.issues.take(5).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }
                            item { HelpHeading("Where your value comes from", WealthHelp.ASSETS) }
                            val allocation = workspace.current.lines.filter { it.item.parentId == null && it.included && (it.reportingAmount?.signum() ?: -1) >= 0 }.groupBy { it.item.kind }
                            items(allocation.entries.toList(), key = { it.key.name }) { entry ->
                                val amount = entry.value.mapNotNull { it.reportingAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.key.title()); Text(money(amount, workspace.current.currency, vm.hidden)) }
                            }
                            item { HelpHeading("What changed since last month", WealthHelp.HISTORY) }
                            if (workspace.changes.isEmpty()) item { Text("Keep your values up to date. Once we have a complete picture for this month and last month, you'll see what changed.", style = MaterialTheme.typography.bodySmall) }
                            items(workspace.changes) { change ->
                                Column { Text(change.label); Text(money(change.amount, workspace.current.currency, vm.hidden)) }
                            }
                            item { Text("Cash used to repay principal reduces both cash and debt. These movements offset; only interest, fees or other changes reduce net worth.", style = MaterialTheme.typography.bodySmall) }
                        }
                        1, 2 -> {
                            val lines = workspace.current.lines.filter { it.item.parentId == null && (if (tab == 2) (it.reportingAmount?.signum() ?: if (it.item.kind.isDebt) -1 else 1) < 0 else (it.reportingAmount?.signum() ?: if (it.item.kind.isDebt) -1 else 1) >= 0) }
                            item { HelpHeading(if (tab == 1) "Money and things you own" else "Debts still left to repay", if (tab == 1) WealthHelp.ASSETS else WealthHelp.DEBTS) }
                            if (lines.isEmpty()) item { Text(if (tab == 1) "Start with cash or savings, then add investments, your home, a car or gold. Use an existing account instead of adding the same money twice." else "Add the amount left on credit cards, loans or a mortgage — not the monthly payment. No debts? Confirm that in your account review.") }
                            items(lines, key = { it.item.id }) { line -> ItemRow(line, workspace.current.currency, vm.hidden) { selected = line.item.id } }
                        }
                        3 -> {
                            item { HelpHeading("Your progress over time", WealthHelp.HISTORY) }
                            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(3, 6, 12, 24, 0).forEach { m -> FilterChip(selected = vm.months == m, onClick = { vm.range(m) }, label = { Text(if (m == 0) "All" else "$m months") }, colors = wealthChipColors()) } } }
                            item { WealthChart(workspace.history, workspace.current.currency, vm.hidden) }
                            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Date"); Text("Net worth") } }
                            items(workspace.history, key = { it.date.toString() }) { point ->
                                Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(point.date.toString()); Text(if (point.result.lines.isEmpty()) "No observations" else money(point.result.netWorth, point.result.currency, vm.hidden)) }
                                    if (!point.result.complete && point.result.lines.isNotEmpty()) Text("Known subtotal · incomplete", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (!workspace.legacyHistory.isNullOrBlank()) item { Text("Earlier account-only history is retained in your backup. Its original currency and valuation sources were not recorded, so it is not mixed into this chart.", style = MaterialTheme.typography.bodySmall) }
                            val revisions = workspace.data.snapshots.count { it.supersedesId != null }
                            if (revisions > 0) item { Text("$revisions historical calculation revision(s) retained with their original inputs.") }
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
            if (expanded) {
                VerticalDivider()
                Box(Modifier.widthIn(min = 300.dp, max = 440.dp).weight(0.65f).fillMaxHeight().padding(24.dp).verticalScroll(rememberScrollState())) {
                    val line = workspace?.current?.lines?.firstOrNull { it.item.id == selected }
                    if (line == null) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Make sense of your money", style = MaterialTheme.typography.titleLarge)
                        Text("Select something you own or owe to see its value and update it. Your wallet transactions stay separate.")
                        TextButton(onClick = { guide = true }) { Text("Walk me through net worth") }
                        HelpHeading("Avoid counting things twice", WealthHelp.DUPLICATES)
                    }
                    else ItemDetail(line, workspace, vm, onEdit = { editorId = line.item.id }, onSelect = { selected = it })
                }
            }
        }
        if (!expanded && selected != null && workspace != null) {
            workspace.current.lines.firstOrNull { it.item.id == selected }?.let { line -> WealthDialog({ selected = null }) { ItemDetail(line, workspace, vm, { editorId = line.item.id }, { selected = it }); TextButton(onClick = { selected = null }) { Text("Close") } } }
        }
    }
    if (review && workspace != null) WealthDialog({ review = false }) {
        val previous = workspace.data.reviews.filter { it.effectiveDate <= vm.date.toString() }.maxByOrNull { java.time.Instant.parse(it.recordedAt) }
        var ownReviewed by rememberSaveable { mutableStateOf(previous?.assetsReviewed == true) }
        var oweReviewed by rememberSaveable { mutableStateOf(previous?.debtsReviewed == true) }
        Text("Start with what you already track", style = MaterialTheme.typography.titleLarge)
        Text("Choose which accounts and loans belong in your total. Jvy keeps their balances up to date from your wallet entries. You won't need to type them in again.")
        HelpHeading("Count every item once", WealthHelp.DUPLICATES)
        Text("This review applies from ${vm.date}. Select an earlier date first if you are confirming historical coverage.", style = MaterialTheme.typography.bodySmall)
        workspace.candidates.forEach { source ->
            Text(source.name + if (source.source == WealthSource.ACCOUNT) " · wallet account" else " · loan")
            TextButton(onClick = { candidateKey = "${source.source}:${source.id}"; review = false }) { Text("Choose how to count this") }
        }
        if (workspace.candidates.isEmpty()) Text("You've checked every existing account and loan. Add anything else you own or owe using the buttons on the main page.")
        Row { Checkbox(ownReviewed, { ownReviewed = it }); Text("I reviewed savings, investments, property and other assets.", Modifier.padding(top = 12.dp)) }
        Row { Checkbox(oweReviewed, { oweReviewed = it }); Text("I reviewed credit cards, loans and other debts.", Modifier.padding(top = 12.dp)) }
        Button(onClick = { vm.confirmCoverage(ownReviewed, oweReviewed); review = false }) { Text("Save review") }
        TextButton(onClick = { review = false }) { Text("Done") }
    }
    if (workspace != null && (adding || editor != null || candidate != null)) WealthItemEditor(vm, editor, candidate, addDebt, onClose = { adding = false; editorId = null; candidateKey = null })
    if (rates && workspace != null) FxEditor(vm, workspace.current.currency) { rates = false }
    if (guide) WealthGuide(close = { guide = false }, finish = { vm.finishGuide(); guide = false }, linkAccounts = { guide = false; review = true })
    if (dateDialog) WealthDialog({ dateDialog = false }) {
        Text("View your picture on another date", style = MaterialTheme.typography.titleLarge)
        Text("Jvy uses the balances and estimates recorded for that date. A missing older value stays unknown — it isn't assumed to be zero.")
        WealthDateField("Date", dateText, { dateText = it })
        Button(onClick = { vm.setDate(dateText); dateDialog = false }) { Text("View this date") }
        TextButton(onClick = { dateDialog = false }) { Text("Cancel") }
    }
}

@Composable
internal fun Summary(result: WealthResult, hidden: Boolean) {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HelpHeading(if (result.complete) "Your net worth" else "Your estimate so far", WealthHelp.NET_WORTH)
        Text(if (result.lines.isEmpty()) "Let's find your starting point" else money(result.netWorth, result.currency, hidden), style = MaterialTheme.typography.headlineMedium)
        if (result.lines.isNotEmpty()) {
            Text("What you own  ${money(result.assets, result.currency, hidden)}")
            Text("Minus what you owe  ${money(result.liabilities, result.currency, hidden)}")
        } else Text("Start with an account, an estimate of something you own, or a debt. Nothing added yet does not mean you own nothing.")
        Text(if (result.complete) "Your reviewed picture on ${result.asOf.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}. This is not your available spending money." else "Not finished yet. Add missing items, check their values and review your accounts. Unknown amounts are not treated as zero.", style = MaterialTheme.typography.bodySmall)
        val needsReview = result.lines.count { it.included && it.warning != null }
        if (needsReview > 0) Text("$needsReview valuation(s) or rates need review", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    } }
}

@Composable
private fun ItemRow(line: WealthLine, currency: String, hidden: Boolean, click: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = click)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(line.item.name, style = MaterialTheme.typography.titleMedium)
        val source = when (line.item.source) { WealthSource.ACCOUNT -> "Linked wallet balance"; WealthSource.LOAN -> "Linked loan"; WealthSource.MANUAL -> "Your estimate"; WealthSource.HOLDINGS -> "Investment group" }
        Text("${line.item.kind.title()} · $source", style = MaterialTheme.typography.bodySmall)
        Text(if (!line.included) "Excluded / archived" else money(line.reportingAmount?.abs(), currency, hidden))
        (line.issue ?: line.warning)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    } }
}

@Composable
private fun ItemDetail(line: WealthLine, workspace: WealthWorkspace, vm: NetWorthViewModel, onEdit: () -> Unit, onSelect: (String) -> Unit) {
    val nav = navigation()
    var calculationDetails by rememberSaveable(line.item.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(line.item.name, style = MaterialTheme.typography.titleLarge)
        Text(line.item.kind.title())
        Text(money(line.reportingAmount, workspace.current.currency, vm.hidden), style = MaterialTheme.typography.headlineSmall)
        Text(if ((line.reportingAmount?.signum() ?: 0) < 0) "This debt is subtracted from your total." else "This is the amount counted in your total.")
        val policy = workspace.data.policies.filter { it.itemId == line.item.id && it.effectiveDate <= vm.date.toString() }
            .maxWithOrNull(compareBy<WealthPolicy> { it.effectiveDate }.thenBy { java.time.Instant.parse(it.recordedAt) }.thenBy { it.id })
        Text("Last value recorded: ${line.valuationDate ?: "Not entered yet"}")
        TextButton(onClick = { calculationDetails = !calculationDetails }) { Text(if (calculationDetails) "Hide calculation details" else "How was this amount worked out?") }
        if (calculationDetails) {
        Text("Your share before currency conversion: ${money(line.nativeAmount?.abs(), line.item.currency, vm.hidden)}")
        HelpHeading("Your share: ${if (vm.hidden) "••••" else policy?.ownership ?: "Not reviewed"}%", WealthHelp.OWNERSHIP)
        line.rateId?.let { id ->
            HelpHeading("Currency conversion used", WealthHelp.CURRENCY)
            Text("Conversion multiplier: ${if (vm.hidden) "••••" else line.exchangeRate?.stripTrailingZeros()?.toPlainString()}")
            id.split('+').forEach { rateId -> workspace.data.rates.firstOrNull { it.id == rateId }?.let { rate -> Text("1 ${rate.from} = ${if (vm.hidden) "••••" else rate.rate} ${rate.to} · checked ${rate.effectiveDate} · ${rate.source}") } }
        }
        }
        (line.issue ?: line.warning)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (line.item.note.isNotBlank()) Text(line.item.note)
        val linkedId = line.item.sourceId?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
        if (linkedId != null && line.item.source == WealthSource.ACCOUNT) {
            TextButton(onClick = { nav.navigateTo(com.ivy.navigation.TransactionsScreen(accountId = linkedId)) }) { Text("Open wallet account") }
        }
        if (linkedId != null && line.item.source == WealthSource.LOAN) {
            Text("Jvy uses the loan balance still left to repay. Interest you've paid is a cost, not money paid off the original loan balance.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { nav.navigateTo(com.ivy.navigation.LoanDetailsScreen(linkedId)) }) { Text("Open loan record") }
        }
        line.item.minimumPayment?.let { Text("Minimum payment: ${money(it.toBigDecimal(), line.item.currency, vm.hidden)}") }
        line.item.apr?.let { Text("Yearly interest rate: ${if (vm.hidden) "••••" else it}% · future interest is not added to today's debt") }
        Button(onClick = onEdit) { Text("Update value or details") }
        OutlinedButton(onClick = { vm.policy(line.item, !line.included) }) { Text(if (line.included) "Exclude from net worth" else "Include in net worth") }
        var archive by remember { mutableStateOf(false) }
        TextButton(onClick = { archive = true }) { Text("Archive item") }
        if (archive) AlertDialog(onDismissRequest = { archive = false }, title = { Text("Archive this item?") }, text = { Text("This removes it from net worth from the selected date. Earlier history is preserved.") }, confirmButton = { TextButton(onClick = { vm.policy(line.item, false, true); archive = false }) { Text("Archive") } }, dismissButton = { TextButton(onClick = { archive = false }) { Text("Cancel") } })
        if (line.item.source == WealthSource.HOLDINGS) {
            Text("Holdings", style = MaterialTheme.typography.titleMedium)
            workspace.current.lines.filter { it.item.parentId == line.item.id }.forEach { child -> TextButton(onClick = { onSelect(child.item.id) }) { Text(child.item.name + " · " + money(child.reportingAmount, workspace.current.currency, vm.hidden)) } }
            Text("Use Add what I own, then More options, to add an investment to this group.", style = MaterialTheme.typography.bodySmall)
        }
        if (line.item.kind == WealthKind.PROPERTY) {
            val debts = workspace.current.lines.filter { it.item.securedById == line.item.id && it.included }.mapNotNull { it.reportingAmount }
            val propertyValue = line.reportingAmount
            if (debts.isNotEmpty() && propertyValue != null) Text("Home value left after linked debts (equity): ${money(debts.fold(propertyValue, BigDecimal::add), workspace.current.currency, vm.hidden)}")
        }
        HelpHeading("Previous estimates · full value before your share", WealthHelp.VALUE)
        workspace.data.valuations.filter { it.itemId == line.item.id }.sortedByDescending { it.effectiveDate }.take(12).forEach {
            Text("${it.effectiveDate} · ${money(it.amount.toBigDecimal(), line.item.currency, vm.hidden)}${if (it.supersedesId != null) " · revised" else ""}")
        }
        if (line.item.kind in listOf(WealthKind.INVESTMENT, WealthKind.RETIREMENT, WealthKind.CRYPTO, WealthKind.GOLD)) {
            val superseded = workspace.data.valuations.mapNotNull { it.supersedesId }.toSet()
            val values = workspace.data.valuations.filter { it.itemId == line.item.id && it.effectiveDate <= vm.date.toString() && it.id !in superseded }.sortedWith(compareBy<WealthValuation> { it.effectiveDate }.thenBy { java.time.Instant.parse(it.recordedAt) })
            val last = values.lastOrNull(); val previous = values.getOrNull(values.lastIndex - 1)
            val contribution = last?.contribution; val withdrawal = last?.withdrawal
            if (last != null && contribution != null && withdrawal != null && previous != null) {
                val movement = last.amount.toBigDecimal() - previous.amount.toBigDecimal() - contribution.toBigDecimal() + withdrawal.toBigDecimal()
                Text("Change in value, excluding money added or taken out: ${money(movement, line.item.currency, vm.hidden)}")
                Text("Before adjusting for your share or currency conversion. This is not a yearly investment return.", style = MaterialTheme.typography.bodySmall)
            } else Text("Adding or withdrawing money also changes an investment's total. Enter both in More options to separate them from changes in market value.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WealthChart(history: List<WealthHistoryPoint>, currency: String, hidden: Boolean) {
    val observed = history.filter { it.result.complete }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Net worth history", style = MaterialTheme.typography.titleMedium)
        if (hidden) { Text("Values hidden", Modifier.height(180.dp)); return }
        if (observed.isEmpty()) { Text("Your progress will appear here after you've added values and checked your account list and currency rates. Missing earlier values stay blank.", Modifier.height(180.dp)); return }
        val min = minOf(BigDecimal.ZERO, observed.minOf { it.result.netWorth })
        val max = maxOf(BigDecimal.ZERO, observed.maxOf { it.result.netWorth })
        val span = (max - min).takeUnless { it.signum() == 0 } ?: BigDecimal.ONE
        Text(money(max, currency, false), style = MaterialTheme.typography.bodySmall)
        val color = MaterialTheme.colorScheme.primary
        val axis = MaterialTheme.colorScheme.outline
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            fun y(value: BigDecimal) = size.height - ((value - min).divide(span, 12, RoundingMode.HALF_UP).toFloat() * size.height)
            drawLine(axis, Offset(0f, y(BigDecimal.ZERO)), Offset(size.width, y(BigDecimal.ZERO)))
            var previous: Offset? = null
            history.forEachIndexed { index, point ->
                if (!point.result.complete) { previous = null; return@forEachIndexed }
                val p = Offset(if (history.size == 1) size.width / 2 else index.toFloat() / (history.size - 1) * size.width, y(point.result.netWorth))
                previous?.let { drawLine(color, it, p, 3.dp.toPx()) }; drawCircle(color, 4.dp.toPx(), p); previous = p
            }
        }
        Text(money(min, currency, false), style = MaterialTheme.typography.bodySmall)
        Text("${history.first().date} — ${history.last().date} · missing observations remain gaps", style = MaterialTheme.typography.bodySmall)
        Text("Manual valuations carry forward from their recorded dates. Review stale values before relying on a trend.", style = MaterialTheme.typography.bodySmall)
    }
}
