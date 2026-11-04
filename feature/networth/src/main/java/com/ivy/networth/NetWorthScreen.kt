package com.ivy.networth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.platform.LocalDensity
import com.ivy.design.system.JvyFinanceColors
import com.ivy.design.system.JvyTypography
import com.ivy.design.system.JvyButton as Button
import com.ivy.design.system.JvyOutlinedButton as OutlinedButton
import com.ivy.design.system.JvyTextButton as TextButton

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

/** Semantic mint selection with a readable light/dark foreground. */
@Composable
internal fun wealthChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
)

@Composable
fun NetWorthScreen(modifier: Modifier = Modifier) {
    val vm: NetWorthViewModel = com.ivy.navigation.screenScopedViewModel()
    val nav = navigation()
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { NetWorthScreenContent(vm, { nav.back() }) }
}

/** The main-shell tab reuses this feature rather than creating a second calculator. */
@Composable
fun NetWorthTab(modifier: Modifier = Modifier) {
    val vm: NetWorthViewModel = viewModel(key = "main-net-worth")
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { NetWorthScreenContent(vm, {}, embedded = true) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
fun NetWorthScreenContent(vm: NetWorthViewModel, onBack: () -> Unit, modifier: Modifier = Modifier, initialTab: Int = 0, embedded: Boolean = false) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var addDebt by rememberSaveable { mutableStateOf(false) }
    var candidateKey by rememberSaveable { mutableStateOf<String?>(null) }
    var review by rememberSaveable { mutableStateOf(false) }
    var ownReviewDraft by rememberSaveable(vm.date.toString()) { mutableStateOf<Boolean?>(null) }
    var oweReviewDraft by rememberSaveable(vm.date.toString()) { mutableStateOf<Boolean?>(null) }
    var repaymentsHelp by rememberSaveable { mutableStateOf(false) }
    var rates by rememberSaveable { mutableStateOf(false) }
    var guide by rememberSaveable { mutableStateOf(false) }
    var tools by remember { mutableStateOf(false) }
    var dateDialog by rememberSaveable { mutableStateOf(false) }
    var dateText by rememberSaveable { mutableStateOf(vm.date.toString()) }
    var exportConfirmation by remember { mutableStateOf<Boolean?>(null) }
    var preparingExport by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val largeText = LocalDensity.current.fontScale > 1.3f
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val request = vm.pendingExport
        vm.pendingExport = null
        if (uri != null && request == null) exportError = "Export was interrupted. No financial data was written. Choose Export again."
        else if (uri != null && request != null) scope.launch {
            try { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(request.content) } ?: error("Cannot open file") } }
            catch (_: Exception) { exportError = "Could not export. Please choose another file location." }
        }
    }
    val headerActions: @Composable RowScope.() -> Unit = {
        IconButton(onClick = { guide = true }) { Icon(Icons.Outlined.HelpOutline, "Open the net worth guide") }
        Box {
            IconButton(onClick = { tools = true }) { Icon(Icons.Outlined.MoreVert, "Net worth tools") }
            DropdownMenu(expanded = tools, onDismissRequest = { tools = false }) {
                DropdownMenuItem(text = { Text("How net worth works") }, onClick = { tools = false; guide = true })
                DropdownMenuItem(text = { Text("View another date") }, onClick = { tools = false; dateDialog = true })
                DropdownMenuItem(text = { Text("Currency & exchange rates") }, onClick = { tools = false; rates = true })
                DropdownMenuItem(text = { Text("Export summary") }, enabled = !vm.loading && vm.workspace != null && !preparingExport && vm.pendingExport == null,
                    onClick = { tools = false; exportConfirmation = false })
                DropdownMenuItem(text = { Text("Export history") }, enabled = !vm.loading && vm.workspace != null && !preparingExport && vm.pendingExport == null,
                    onClick = { tools = false; exportConfirmation = true })
            }
        }
    }
    val restoration = com.ivy.design.system.rememberProcessRestoration("wealth-view", vm::saveView)
    LaunchedEffect(Unit) { vm.restoreView(restoration.payload); restoration.consumed = true; vm.load() }
    val workspace = vm.workspace
    exportConfirmation?.let { history ->
        AlertDialog(onDismissRequest = { exportConfirmation = null },
            title = { Text(stringResource(R.string.jvy_export_private_title)) },
            text = { Text(stringResource(R.string.jvy_export_private_message)) },
            confirmButton = { TextButton(onClick = {
                exportConfirmation = null
                preparingExport = true
                scope.launch {
                    try {
                        val request = vm.prepareExport(history)
                        vm.pendingExport = request
                        export.launch(request.fileName)
                    } catch (error: kotlinx.coroutines.CancellationException) { throw error }
                    catch (_: Exception) { vm.pendingExport = null; exportError = "Could not prepare export. Wait for the current view to load, then try again." }
                    finally { preparingExport = false }
                }
            }) { Text(stringResource(R.string.jvy_export_continue)) } },
            dismissButton = { TextButton(onClick = { exportConfirmation = null }) { Text(stringResource(R.string.cancel)) } })
    }
    val editor = workspace?.data?.items?.firstOrNull { it.id == editorId }
    val candidate = workspace?.candidates?.firstOrNull { "${it.source}:${it.id}" == candidateKey }
    BoxWithConstraints(modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        val expanded = maxWidth >= 840.dp
        val wrapControls = largeText || maxWidth < 360.dp
        Row(Modifier.widthIn(max = 1280.dp).fillMaxSize().align(Alignment.TopCenter)) {
            LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = if (embedded) 144.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item(key = "heading", contentType = "heading") {
                    if (!embedded) com.ivy.design.system.JvyScreenHeader("Net worth", onBack,
                        "Your bigger financial picture", horizontalPadding = 0.dp, actions = headerActions)
                    if (embedded) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if (embedded) {
                                Text("Net worth", style = MaterialTheme.typography.headlineMedium)
                                Text("Your bigger financial picture", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        headerActions()
                    }
                }
                item(key = "tabs", contentType = "tabs") { FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (wrapControls) 2 else 4, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Overview", "I own", "I owe", "Progress").forEachIndexed { index, name -> FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(name) }, colors = wealthChipColors()) }
                } }
                if (tab == 0 && workspace != null) item(key = "summary", contentType = "summary") { Summary(workspace.current, vm.hidden) }
                if (tab == 0 && !vm.guideSeen) item(key = "guide", contentType = "action") {
                    TextButton(onClick = { guide = true }, modifier = Modifier.fillMaxWidth()) { Text("New to net worth? Let me walk you through it") }
                }
                if (workspace?.candidates?.isNotEmpty() == true) item(key = "candidates", contentType = "review") {
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Start with accounts already in Jvy", style = MaterialTheme.typography.titleMedium)
                        Text("Use your existing balances instead of typing the same money or loan twice.")
                        Button(onClick = { review = true }, modifier = Modifier.fillMaxWidth()) { Text("Check my existing accounts & loans") }
                    } }
                }
                item(key = "add", contentType = "actions") { FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (wrapControls) 1 else 2, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { adding = true; addDebt = false }, enabled = workspace != null && !vm.loading, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)) { Text("Add what I own") }
                    OutlinedButton(onClick = { adding = true; addDebt = true }, enabled = workspace != null && !vm.loading, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)) { Text("Add what I owe") }
                } }
                if (workspace?.candidates?.isEmpty() == true && (tab != 0 || workspace.current.complete)) item(key = "review-linked", contentType = "action") { TextButton(onClick = { review = true }) { Text("Review accounts & loans already linked") } }
                if (vm.loading) item(key = "loading", contentType = "loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                (vm.error ?: exportError)?.let { message -> item(key = "error", contentType = "message") { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::load) { Text("Retry") } } }
                if (workspace == null && !vm.loading) item(key = "empty", contentType = "message") { Text("Add what you own and what you owe, or link existing accounts and loans.") }
                if (workspace != null) {
                    item(key = "as-of", contentType = "caption") { Text("Your picture on ${vm.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} · ${workspace.current.currency}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    when (tab) {
                        0 -> {
                            if (workspace.current.issues.isNotEmpty()) item(key = "issues", contentType = "message") {
                                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Let's finish your picture", style = MaterialTheme.typography.titleMedium)
                                Text(if (workspace.current.issues.size == 1) "Your picture needs one more check. This total only includes what's entered so far."
                                    else "Your picture needs ${workspace.current.issues.size} more checks. This total only includes what's entered so far.", style = MaterialTheme.typography.bodySmall)
                                workspace.current.issues.take(5).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                                if (workspace.candidates.isEmpty()) TextButton(onClick = { review = true }) { Text("Confirm what I've reviewed") }
                                } }
                            }
                            val allocation = workspace.current.lines.filter { it.item.parentId == null && it.included && (it.reportingAmount?.signum() ?: -1) >= 0 }.groupBy { it.item.kind }
                            if (allocation.isNotEmpty()) item(key = "allocation", contentType = "allocation") {
                                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                HelpHeading("Where your value comes from", WealthHelp.ASSETS)
                                allocation.entries.forEach { entry ->
                                val amount = entry.value.mapNotNull { it.reportingAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
                                WealthAllocationRow(entry.key, amount, workspace.current.assets, workspace.current.currency, vm.hidden)
                                }
                                } }
                            }
                            item(key = "changes-heading", contentType = "help-heading") { HelpHeading("What changed since last month", WealthHelp.HISTORY) }
                            if (workspace.changes.isEmpty()) item(key = "changes-empty", contentType = "message") { Text("Keep your values up to date. Once we have a complete picture for this month and last month, you'll see what changed.", style = MaterialTheme.typography.bodySmall) }
                            items(workspace.changes, key = { "change-${it.label}" }, contentType = { "change" }) { change ->
                                Column { Text(change.label); Text(money(change.amount, workspace.current.currency, vm.hidden), style = JvyTypography.AmountSmall) }
                            }
                            item(key = "principal-explainer", contentType = "action") { TextButton(onClick = { repaymentsHelp = true }) { Text("How loan repayments affect net worth") } }
                        }
                        1, 2 -> {
                            val lines = workspace.current.lines.filter { it.item.parentId == null && (if (tab == 2) (it.reportingAmount?.signum() ?: if (it.item.kind.isDebt) -1 else 1) < 0 else (it.reportingAmount?.signum() ?: if (it.item.kind.isDebt) -1 else 1) >= 0) }
                            item(key = "positions-heading-$tab", contentType = "help-heading") { HelpHeading(if (tab == 1) "Money and things you own" else "Debts still left to repay", if (tab == 1) WealthHelp.ASSETS else WealthHelp.DEBTS) }
                            if (lines.isEmpty()) item(key = "positions-empty-$tab", contentType = "message") { Text(if (tab == 1) "Start with cash or savings, then add investments, your home, a car or gold. Use an existing account instead of adding the same money twice." else "Add the amount left on credit cards, loans or a mortgage — not the monthly payment. No debts? Confirm that in your account review.") }
                            items(lines, key = { "position-${it.item.id}" }, contentType = { "position" }) { line -> ItemRow(line, workspace.current.currency, vm.hidden) { selected = line.item.id } }
                        }
                        3 -> {
                            item(key = "history-heading", contentType = "help-heading") { HelpHeading("Your progress over time", WealthHelp.HISTORY) }
                            item(key = "history-range", contentType = "tabs") { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(3, 6, 12, 24, 0).forEach { m -> FilterChip(selected = vm.months == m, onClick = { vm.range(m) }, label = { Text(if (m == 0) "All" else "$m months") }, colors = wealthChipColors()) } } }
                            item(key = "history-chart", contentType = "chart") { WealthChart(workspace.history, workspace.current.currency, vm.hidden) }
                            items(workspace.history, key = { "history-${it.date}" }, contentType = { "history" }) { point ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(point.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (point.result.lines.isEmpty()) "No observations" else money(point.result.netWorth, point.result.currency, vm.hidden), style = JvyTypography.AmountSmall)
                                    if (!point.result.complete && point.result.lines.isNotEmpty()) Text("Known subtotal · incomplete", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (!workspace.legacyHistory.isNullOrBlank()) item(key = "legacy-history", contentType = "message") { Text("Earlier account-only history is retained in your backup. Its original currency and valuation sources were not recorded, so it is not mixed into this chart.", style = MaterialTheme.typography.bodySmall) }
                            val revisions = workspace.data.snapshots.count { it.supersedesId != null }
                            if (revisions > 0) item(key = "revisions", contentType = "message") { Text("$revisions historical calculation revision(s) retained with their original inputs.") }
                        }
                    }
                    item(key = "footer", contentType = "spacer") { Spacer(Modifier.height(32.dp)) }
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
    if (review && workspace != null) {
        val previous = workspace.data.reviews.reviewOn(vm.date.toString())
        val ownReviewed = ownReviewDraft ?: (previous?.assetsReviewed == true)
        val oweReviewed = oweReviewDraft ?: (previous?.debtsReviewed == true)
        WealthDialog({ review = false }, footer = {
            Button(onClick = { vm.confirmCoverage(ownReviewed, oweReviewed); ownReviewDraft = null; oweReviewDraft = null; review = false }, modifier = Modifier.fillMaxWidth()) { Text("Save review") }
        }) {
        WealthReviewBody(workspace.candidates, vm.date, ownReviewed, oweReviewed,
            { ownReviewDraft = it }, { oweReviewDraft = it }, { source -> candidateKey = "${source.source}:${source.id}"; review = false })
        }
    }
    if (workspace != null && (adding || editor != null || candidate != null)) WealthItemEditor(vm, editor, candidate, addDebt, onClose = { review = candidateKey != null; adding = false; editorId = null; candidateKey = null })
    if (repaymentsHelp) WealthDialog({ repaymentsHelp = false }) {
        Text("Why a repayment may not change net worth", style = MaterialTheme.typography.titleLarge)
        Text("Cash used to repay principal reduces both cash and debt. These movements offset; only interest, fees or other changes reduce net worth.")
    }
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
    WealthSummary(result, hidden)
}

@Composable
private fun ItemRow(line: WealthLine, currency: String, hidden: Boolean, click: () -> Unit) {
    WealthPositionRow(line, currency, hidden, click)
}

@Composable
private fun ItemDetail(line: WealthLine, workspace: WealthWorkspace, vm: NetWorthViewModel, onEdit: () -> Unit, onSelect: (String) -> Unit) {
    val nav = navigation()
    var calculationDetails by rememberSaveable(line.item.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(line.item.name, style = MaterialTheme.typography.titleLarge)
        Text(line.item.kind.title())
        Text(money(line.reportingAmount, workspace.current.currency, vm.hidden), style = JvyTypography.Amount)
        Text(if ((line.reportingAmount?.signum() ?: 0) < 0) "This debt is subtracted from your total." else "This is the amount counted in your total.")
        val policy = workspace.data.policies.filter { it.itemId == line.item.id && it.effectiveDate <= vm.date.toString() }
            .maxWithOrNull(compareBy<WealthPolicy> { it.effectiveDate }.thenBy { java.time.Instant.parse(it.recordedAt) }.thenBy { it.id })
        Text("Last value recorded: ${line.valuationDate ?: "Not entered yet"}")
        TextButton(onClick = { calculationDetails = !calculationDetails }) { Text(if (calculationDetails) "Hide calculation details" else "How was this amount worked out?") }
        if (calculationDetails) {
        Text("Your share before currency conversion", style = MaterialTheme.typography.bodySmall)
        Text(money(line.nativeAmount?.abs(), line.item.currency, vm.hidden), style = JvyTypography.AmountSmall)
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
        line.item.minimumPayment?.let {
            Text("Minimum payment", style = MaterialTheme.typography.bodySmall)
            Text(money(it.toBigDecimal(), line.item.currency, vm.hidden), style = JvyTypography.AmountSmall)
        }
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
            if (debts.isNotEmpty() && propertyValue != null) {
                Text("Home value left after linked debts (equity)", style = MaterialTheme.typography.bodySmall)
                Text(money(debts.fold(propertyValue, BigDecimal::add), workspace.current.currency, vm.hidden), style = JvyTypography.AmountSmall)
            }
        }
        HelpHeading("Previous estimates · full value before your share", WealthHelp.VALUE)
        workspace.data.valuations.filter { it.itemId == line.item.id }.sortedByDescending { it.effectiveDate }.take(12).forEach {
            Text("${it.effectiveDate}${if (it.supersedesId != null) " · revised" else ""}", style = MaterialTheme.typography.bodySmall)
            Text(money(it.amount.toBigDecimal(), line.item.currency, vm.hidden), style = JvyTypography.AmountSmall)
        }
        if (line.item.kind in listOf(WealthKind.INVESTMENT, WealthKind.RETIREMENT, WealthKind.CRYPTO, WealthKind.GOLD)) {
            val superseded = workspace.data.valuations.mapNotNull { it.supersedesId }.toSet()
            val values = workspace.data.valuations.filter { it.itemId == line.item.id && it.effectiveDate <= vm.date.toString() && it.id !in superseded }.sortedWith(compareBy<WealthValuation> { it.effectiveDate }.thenBy { java.time.Instant.parse(it.recordedAt) })
            val last = values.lastOrNull(); val previous = values.getOrNull(values.lastIndex - 1)
            val contribution = last?.contribution; val withdrawal = last?.withdrawal
            if (last != null && contribution != null && withdrawal != null && previous != null) {
                val movement = last.amount.toBigDecimal() - previous.amount.toBigDecimal() - contribution.toBigDecimal() + withdrawal.toBigDecimal()
                Text("Change in value, excluding money added or taken out", style = MaterialTheme.typography.bodySmall)
                Text(money(movement, line.item.currency, vm.hidden), style = JvyTypography.AmountSmall)
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
        if (hidden || com.ivy.design.system.LocalFinancialVisibility.current.hidden) { Text("Values hidden", Modifier.height(180.dp)); return }
        if (observed.isEmpty()) { Text("Your progress will appear here after you've added values and checked your account list and currency rates. Missing earlier values stay blank.", Modifier.height(180.dp)); return }
        val min = minOf(BigDecimal.ZERO, observed.minOf { it.result.netWorth })
        val max = maxOf(BigDecimal.ZERO, observed.maxOf { it.result.netWorth })
        val span = (max - min).takeUnless { it.signum() == 0 } ?: BigDecimal.ONE
        Text(money(max, currency, false), style = JvyTypography.AmountSmall)
        val color = JvyFinanceColors.asset()
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
        Text(money(min, currency, false), style = JvyTypography.AmountSmall)
        Text("${history.first().date} — ${history.last().date} · missing observations remain gaps", style = MaterialTheme.typography.bodySmall)
        Text("Manual valuations carry forward from their recorded dates. Review stale values before relying on a trend.", style = MaterialTheme.typography.bodySmall)
    }
}
