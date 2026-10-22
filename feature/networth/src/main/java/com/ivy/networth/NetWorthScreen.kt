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
import androidx.compose.material3.*
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

internal fun money(value: BigDecimal?, currency: String, hidden: Boolean): String = when {
    hidden -> "••••"
    value == null -> "Value unavailable"
    else -> com.ivy.ui.money.formatDecimalMoney(value, currency)
}
internal fun WealthKind.title() = name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
internal val WealthKind.isDebt get() = this in setOf(WealthKind.CREDIT_CARD, WealthKind.MORTGAGE, WealthKind.PERSONAL_LOAN, WealthKind.VEHICLE_LOAN, WealthKind.STUDENT_LOAN, WealthKind.BORROWED, WealthKind.OTHER_DEBT)

@Composable
fun NetWorthScreen() {
    val vm: NetWorthViewModel = com.ivy.navigation.screenScopedViewModel()
    val nav = navigation()
    Surface(Modifier.fillMaxSize()) { NetWorthScreenContent(vm, { nav.back() }) }
}

@Composable
internal fun NetWorthScreenContent(vm: NetWorthViewModel, onBack: () -> Unit, initialTab: Int = 0) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var addDebt by rememberSaveable { mutableStateOf(false) }
    var candidateKey by rememberSaveable { mutableStateOf<String?>(null) }
    var review by rememberSaveable { mutableStateOf(false) }
    var rates by rememberSaveable { mutableStateOf(false) }
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
            LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onBack) { Text("Back") }
                        Text("Net worth", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Overview", "Assets", "Liabilities", "History").forEachIndexed { index, name -> FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(name) }) }
                } }
                item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { adding = true; addDebt = false }) { Text("Add asset") }
                    FilledTonalButton(onClick = { adding = true; addDebt = true }) { Text("Add debt") }
                } }
                item { OutlinedButton(onClick = { review = true }) { Text("Review and link accounts / loans") } }
                if (vm.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                (vm.error ?: exportError)?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::load) { Text("Retry") } } }
                if (workspace == null && !vm.loading) item { Text("Add what you own and what you owe, or link existing accounts and loans.") }
                if (workspace != null) {
                    item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(dateText, { dateText = it }, label = { Text("As of YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.setDate(dateText) }) { Text("View") }
                    } }
                    item { Text("Net worth = assets − liabilities. Valuations never create spending transactions.", style = MaterialTheme.typography.bodySmall) }
                    when (tab) {
                        0 -> {
                            item { Summary(workspace.current, vm.hidden) }
                            item { TextButton(onClick = { rates = true }) { Text("Currency and exchange rates · ${workspace.current.currency}") } }
                            if (workspace.current.issues.isNotEmpty()) item {
                                Text("${workspace.current.issues.size} item(s) need review", style = MaterialTheme.typography.titleMedium)
                                workspace.current.issues.take(5).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }
                            item { Text("Asset allocation", style = MaterialTheme.typography.titleMedium) }
                            val allocation = workspace.current.lines.filter { it.item.parentId == null && it.included && (it.reportingAmount?.signum() ?: -1) >= 0 }.groupBy { it.item.kind }
                            items(allocation.entries.toList(), key = { it.key.name }) { entry ->
                                val amount = entry.value.mapNotNull { it.reportingAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.key.title()); Text(money(amount, workspace.current.currency, vm.hidden)) }
                            }
                            item { Text("What changed since last month", style = MaterialTheme.typography.titleMedium) }
                            if (workspace.changes.isEmpty()) item { Text("Two complete observations are needed to explain a change.", style = MaterialTheme.typography.bodySmall) }
                            items(workspace.changes) { change ->
                                Column { Text(change.label); Text(money(change.amount, workspace.current.currency, vm.hidden)) }
                            }
                            item { Text("Cash used to repay principal reduces both cash and debt. These movements offset; only interest, fees or other changes reduce net worth.", style = MaterialTheme.typography.bodySmall) }
                        }
                        1, 2 -> {
                            val lines = workspace.current.lines.filter { it.item.parentId == null && (if (tab == 2) (it.reportingAmount?.signum() ?: if (it.item.kind.isDebt) -1 else 1) < 0 else (it.reportingAmount?.signum() ?: if (it.item.kind.isDebt) -1 else 1) >= 0) }
                            if (lines.isEmpty()) item { Text(if (tab == 1) "No assets yet. Add an asset or review your accounts." else "No liabilities yet. Review loans and credit cards before confirming your total.") }
                            items(lines, key = { it.item.id }) { line -> ItemRow(line, workspace.current.currency, vm.hidden) { selected = line.item.id } }
                        }
                        3 -> {
                            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(3, 6, 12, 24, 0).forEach { m -> FilterChip(selected = vm.months == m, onClick = { vm.range(m) }, label = { Text(if (m == 0) "All" else "${m}M") }) } } }
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
                    item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportHistory = false; export.launch("Jvy_wealth_${vm.date}.csv") }) { Text("Export summary") }
                        OutlinedButton(onClick = { exportHistory = true; export.launch("Jvy_wealth_history_${vm.date}.csv") }) { Text("Export history") }
                    } }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
            if (expanded) {
                VerticalDivider()
                Box(Modifier.widthIn(min = 300.dp, max = 440.dp).weight(0.65f).fillMaxHeight().padding(24.dp)) {
                    val line = workspace?.current?.lines?.firstOrNull { it.item.id == selected }
                    if (line == null) Text("Select an asset or liability to review its value and history.")
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
        Text("Review net worth setup", style = MaterialTheme.typography.titleLarge)
        Text("Link or explicitly exclude each account and loan. Also review property, investments and other debts; the app cannot know about items you have not entered.")
        Text("This review applies from ${vm.date}. Select an earlier date first if you are confirming historical coverage.", style = MaterialTheme.typography.bodySmall)
        workspace.candidates.forEach { source ->
            Text(source.name + " · " + source.source.name.lowercase())
            TextButton(onClick = { candidateKey = "${source.source}:${source.id}"; review = false }) { Text("Review inclusion") }
        }
        if (workspace.candidates.isEmpty()) Text("All existing accounts and loans have an explicit inclusion decision.")
        Row { Checkbox(ownReviewed, { ownReviewed = it }); Text("I reviewed savings, investments, property and other assets.", Modifier.padding(top = 12.dp)) }
        Row { Checkbox(oweReviewed, { oweReviewed = it }); Text("I reviewed credit cards, loans and other debts.", Modifier.padding(top = 12.dp)) }
        Button(onClick = { vm.confirmCoverage(ownReviewed, oweReviewed); review = false }) { Text("Save review") }
        TextButton(onClick = { review = false }) { Text("Done") }
    }
    if (workspace != null && (adding || editor != null || candidate != null)) WealthItemEditor(vm, editor, candidate, addDebt, onClose = { adding = false; editorId = null; candidateKey = null })
    if (rates && workspace != null) FxEditor(vm, workspace.current.currency) { rates = false }
}

@Composable
internal fun Summary(result: WealthResult, hidden: Boolean) {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (result.complete) "Net worth" else "Known net worth subtotal", style = MaterialTheme.typography.titleMedium)
        Text(money(result.netWorth, result.currency, hidden), style = MaterialTheme.typography.headlineMedium)
        Text("Assets  ${money(result.assets, result.currency, hidden)}")
        Text("Liabilities  ${money(result.liabilities, result.currency, hidden)}")
        Text("As of ${result.asOf} · ${if (result.complete) "linked sources reviewed" else "setup or valuations incomplete"}", style = MaterialTheme.typography.bodySmall)
        val needsReview = result.lines.count { it.included && it.warning != null }
        if (needsReview > 0) Text("$needsReview valuation(s) or rates need review", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    } }
}

@Composable
private fun ItemRow(line: WealthLine, currency: String, hidden: Boolean, click: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = click)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(line.item.name, style = MaterialTheme.typography.titleMedium)
        Text("${line.item.kind.title()} · ${line.item.source.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
        Text(if (!line.included) "Excluded / archived" else money(line.reportingAmount?.abs(), currency, hidden))
        (line.issue ?: line.warning)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    } }
}

@Composable
private fun ItemDetail(line: WealthLine, workspace: WealthWorkspace, vm: NetWorthViewModel, onEdit: () -> Unit, onSelect: (String) -> Unit) {
    val nav = navigation()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(line.item.name, style = MaterialTheme.typography.titleLarge)
        Text(line.item.kind.title())
        Text(money(line.reportingAmount, workspace.current.currency, vm.hidden), style = MaterialTheme.typography.headlineSmall)
        Text("Owned native value: ${money(line.nativeAmount, line.item.currency, vm.hidden)}")
        val policy = workspace.data.policies.filter { it.itemId == line.item.id && it.effectiveDate <= vm.date.toString() }
            .maxWithOrNull(compareBy<WealthPolicy> { it.effectiveDate }.thenBy { java.time.Instant.parse(it.recordedAt) }.thenBy { it.id })
        Text("Ownership: ${if (vm.hidden) "••••" else policy?.ownership ?: "Not reviewed"}%")
        Text("Valued on: ${line.valuationDate ?: "Unknown"}")
        line.rateId?.let { id ->
            Text("Applied FX multiplier: ${if (vm.hidden) "••••" else line.exchangeRate?.stripTrailingZeros()?.toPlainString()}")
            id.split('+').forEach { rateId -> workspace.data.rates.firstOrNull { it.id == rateId }?.let { rate -> Text("FX source: ${if (vm.hidden) "••••" else rate.rate} ${rate.from}/${rate.to} · ${rate.effectiveDate} · ${rate.source}") } }
        }
        (line.issue ?: line.warning)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (line.item.note.isNotBlank()) Text(line.item.note)
        val linkedId = line.item.sourceId?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
        if (linkedId != null && line.item.source == WealthSource.ACCOUNT) {
            TextButton(onClick = { nav.navigateTo(com.ivy.navigation.TransactionsScreen(accountId = linkedId)) }) { Text("Open wallet account") }
        }
        if (linkedId != null && line.item.source == WealthSource.LOAN) {
            Text("Linked loans use recorded principal. Paid interest is not counted as principal reduction.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { nav.navigateTo(com.ivy.navigation.LoanDetailsScreen(linkedId)) }) { Text("Open loan record") }
        }
        line.item.minimumPayment?.let { Text("Minimum payment: ${money(it.toBigDecimal(), line.item.currency, vm.hidden)}") }
        line.item.apr?.let { Text("APR: ${if (vm.hidden) "••••" else it}% · informational; future interest is not a current liability") }
        Button(onClick = onEdit) { Text("Edit / update valuation") }
        OutlinedButton(onClick = { vm.policy(line.item, !line.included) }) { Text(if (line.included) "Exclude from net worth" else "Include in net worth") }
        var archive by remember { mutableStateOf(false) }
        TextButton(onClick = { archive = true }) { Text("Archive item") }
        if (archive) AlertDialog(onDismissRequest = { archive = false }, title = { Text("Archive this item?") }, text = { Text("This removes it from net worth from the selected date. Earlier history is preserved.") }, confirmButton = { TextButton(onClick = { vm.policy(line.item, false, true); archive = false }) { Text("Archive") } }, dismissButton = { TextButton(onClick = { archive = false }) { Text("Cancel") } })
        if (line.item.source == WealthSource.HOLDINGS) {
            Text("Holdings", style = MaterialTheme.typography.titleMedium)
            workspace.current.lines.filter { it.item.parentId == line.item.id }.forEach { child -> TextButton(onClick = { onSelect(child.item.id) }) { Text(child.item.name + " · " + money(child.reportingAmount, workspace.current.currency, vm.hidden)) } }
            Text("Use Add asset and select this portfolio to add a holding.", style = MaterialTheme.typography.bodySmall)
        }
        if (line.item.kind == WealthKind.PROPERTY) {
            val debts = workspace.current.lines.filter { it.item.securedById == line.item.id && it.included }.mapNotNull { it.reportingAmount }
            val propertyValue = line.reportingAmount
            if (debts.isNotEmpty() && propertyValue != null) Text("Property equity: ${money(debts.fold(propertyValue, BigDecimal::add), workspace.current.currency, vm.hidden)}")
        }
        Text("Valuation history · before ownership", style = MaterialTheme.typography.titleMedium)
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
                Text("Valuation change excluding declared flows: ${money(movement, line.item.currency, vm.hidden)}")
                Text("Before ownership and FX. This is not an annualized investment return.", style = MaterialTheme.typography.bodySmall)
            } else Text("Portfolio value changes include contributions and withdrawals. Enter both flows to separate them from valuation movement.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WealthChart(history: List<WealthHistoryPoint>, currency: String, hidden: Boolean) {
    val observed = history.filter { it.result.complete }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Net worth history", style = MaterialTheme.typography.titleMedium)
        if (hidden) { Text("Values hidden", Modifier.height(180.dp)); return }
        if (observed.isEmpty()) { Text("Complete valuations and exchange rates to plot reliable history.", Modifier.height(180.dp)); return }
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
