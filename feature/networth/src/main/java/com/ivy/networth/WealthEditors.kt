package com.ivy.networth

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ivy.data.model.wealth.*
import com.ivy.domain.usecase.networth.WealthCandidate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Composable
internal fun WealthDialog(close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 640.dp).fillMaxWidth().heightIn(max = 720.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
internal fun WealthItemEditor(vm: NetWorthViewModel, existing: WealthItem?, candidate: WealthCandidate?, debt: Boolean, onClose: () -> Unit) {
    val data = vm.workspace?.data ?: WealthBackup()
    val last = data.valuations.filter { it.itemId == existing?.id && it.effectiveDate <= vm.date.toString() }.maxWithOrNull(compareBy<WealthValuation> { it.effectiveDate }.thenBy { Instant.parse(it.recordedAt) })
    val policy = data.policies.filter { it.itemId == existing?.id && it.effectiveDate <= vm.date.toString() }.maxWithOrNull(compareBy<WealthPolicy> { it.effectiveDate }.thenBy { Instant.parse(it.recordedAt) })
    var name by rememberSaveable { mutableStateOf(existing?.name ?: candidate?.name ?: "") }
    var currency by rememberSaveable { mutableStateOf(existing?.currency ?: candidate?.currency ?: vm.workspace?.current?.currency ?: "USD") }
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: candidate?.kind ?: if (debt) WealthKind.OTHER_DEBT else WealthKind.OTHER_ASSET) }
    var source by rememberSaveable { mutableStateOf(existing?.source ?: candidate?.source ?: WealthSource.MANUAL) }
    var amount by rememberSaveable { mutableStateOf(last?.amount ?: "") }
    var quantity by rememberSaveable { mutableStateOf(last?.quantity ?: "") }
    var price by rememberSaveable { mutableStateOf(last?.unitPrice ?: "") }
    var unit by rememberSaveable { mutableStateOf(existing?.unit ?: "units") }
    var date by rememberSaveable { mutableStateOf(vm.date.toString()) }
    var ownership by rememberSaveable { mutableStateOf(policy?.ownership ?: "100") }
    var included by rememberSaveable { mutableStateOf(policy?.included ?: true) }
    var convention by rememberSaveable { mutableStateOf(existing?.convention ?: candidate?.convention ?: if (debt) BalanceConvention.POSITIVE_OWED else BalanceConvention.ASSET) }
    var parentId by rememberSaveable { mutableStateOf(existing?.parentId) }
    var securedBy by rememberSaveable { mutableStateOf(existing?.securedById) }
    var duplicateId by rememberSaveable { mutableStateOf(existing?.duplicateAccountId) }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var days by rememberSaveable { mutableStateOf((existing?.reviewDays ?: 90).toString()) }
    var apr by rememberSaveable { mutableStateOf(existing?.apr ?: "") }
    var minimum by rememberSaveable { mutableStateOf(existing?.minimumPayment ?: "") }
    var due by rememberSaveable { mutableStateOf(existing?.dueDate ?: "") }
    var contribution by rememberSaveable { mutableStateOf("") }
    var withdrawal by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    WealthDialog(onClose) {
        Text(if (existing != null) "Update asset or liability" else "Add asset or liability", style = MaterialTheme.typography.titleLarge)
        Text("Amounts are valuations, not spending or income. Existing wallet and loan records remain the source for linked items.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WealthKind.entries.forEach { choice -> FilterChip(selected = kind == choice, onClick = {
                kind = choice
                if (choice == WealthKind.GOLD && unit !in listOf("grams", "troy ounces")) unit = "grams"
                if (existing == null && candidate == null) convention = if (choice.isDebt) BalanceConvention.POSITIVE_OWED else BalanceConvention.ASSET
            }, label = { Text(choice.title()) }) }
        }
        val canReviewCurrency = existing?.source in listOf(WealthSource.ACCOUNT, WealthSource.LOAN)
        OutlinedTextField(currency, { if (canReviewCurrency || existing == null && candidate?.source != WealthSource.ACCOUNT) currency = it.uppercase().take(12) }, label = { Text("Native currency") }, readOnly = !canReviewCurrency && (existing != null || candidate?.source == WealthSource.ACCOUNT), singleLine = true, modifier = Modifier.fillMaxWidth())
        if (candidate?.source == WealthSource.LOAN) Text("Confirm the loan's currency. Loans without a linked account inherit the wallet currency unless you choose another.", style = MaterialTheme.typography.bodySmall)
        if (candidate == null && (existing == null || existing.source == WealthSource.MANUAL || existing.source == WealthSource.HOLDINGS)) {
            Text("Valuation method")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = source == WealthSource.MANUAL, onClick = { source = WealthSource.MANUAL }, label = { Text("Total / holding value") })
                FilterChip(selected = source == WealthSource.HOLDINGS, onClick = { source = WealthSource.HOLDINGS }, label = { Text("Sum holdings") })
            }
            if (source == WealthSource.MANUAL && existing == null) {
                Text("Portfolio (optional)")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = parentId == null, onClick = { parentId = null }, label = { Text("Standalone") })
                    data.items.filter { it.source == WealthSource.HOLDINGS }.forEach { portfolio -> FilterChip(selected = parentId == portfolio.id, onClick = { parentId = portfolio.id }, label = { Text(portfolio.name) }) }
                }
            }
            if (source == WealthSource.MANUAL) {
                DecimalField("Current market value / amount owed", amount, { amount = it })
                DecimalField("Quantity (optional)", quantity, { quantity = it }, 8)
                DecimalField("Price per unit (required with quantity)", price, { price = it }, 8)
                if (kind == WealthKind.GOLD) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("grams", "troy ounces").forEach { u -> FilterChip(selected = unit == u, enabled = existing == null, onClick = {
                            // Changing the unit preserves the value by converting quantity and its price.
                            val factor = BigDecimal("31.1034768")
                            if (unit in listOf("grams", "troy ounces") && unit != u) {
                                quantity.toBigDecimalOrNull()?.let { quantity = (if (u == "grams") it.multiply(factor) else it.divide(factor, java.math.MathContext.DECIMAL128)).stripTrailingZeros().toPlainString() }
                                price.toBigDecimalOrNull()?.let { price = (if (u == "grams") it.divide(factor, java.math.MathContext.DECIMAL128) else it.multiply(factor)).stripTrailingZeros().toPlainString() }
                            }
                            unit = u
                        }, label = { Text(u) }) }
                    }
                    if (existing != null) Text("The unit is fixed for this holding so its earlier quantities remain accurate. Add a new holding to use another unit.", style = MaterialTheme.typography.bodySmall)
                }
                Text("Quantity × unit price takes precedence when both are provided.", style = MaterialTheme.typography.bodySmall)
                if (kind in listOf(WealthKind.INVESTMENT, WealthKind.RETIREMENT, WealthKind.CRYPTO, WealthKind.GOLD)) {
                    DecimalField("Contribution since previous value (optional)", contribution, { contribution = it })
                    DecimalField("Withdrawal since previous value (optional)", withdrawal, { withdrawal = it })
                }
            }
        }
        OutlinedTextField(date, { date = it }, label = { Text("Effective date · YYYY-MM-DD") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        DecimalField("Ownership %", ownership, { ownership = it }, 4)
        if (source == WealthSource.ACCOUNT) {
            Text("Account balance convention")
            listOf(BalanceConvention.ASSET to "Normal asset (negative balance is an overdraft)", BalanceConvention.NEGATIVE_OWED to "Debt is shown as a negative account balance", BalanceConvention.POSITIVE_OWED to "Debt is shown as a positive amount owed").forEach { (c, label) -> FilterChip(selected = convention == c, onClick = { convention = c }, label = { Text(label) }) }
        }
        if (kind.isDebt) {
            DecimalField("APR % (optional)", apr, { apr = it }, 4)
            DecimalField("Minimum payment (optional)", minimum, { minimum = it })
            OutlinedTextField(due, { due = it }, label = { Text("Next due date (optional) · YYYY-MM-DD") }, singleLine = true)
            Text("Secured against (optional)")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = securedBy == null, onClick = { securedBy = null }, label = { Text("None") })
                data.items.filter { it.kind == WealthKind.PROPERTY || it.kind == WealthKind.VEHICLE }.forEach { property -> FilterChip(selected = securedBy == property.id, onClick = { securedBy = property.id }, label = { Text(property.name) }) }
            }
        }
        if (source == WealthSource.LOAN) {
            Text("Same debt also represented by a wallet account? Select only if these are duplicate records, not the account that paid or received the loan.")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = duplicateId == null, onClick = { duplicateId = null }, label = { Text("No duplicate") })
                data.items.filter { it.source == WealthSource.ACCOUNT }.forEach { account -> FilterChip(selected = duplicateId == account.sourceId, onClick = { duplicateId = account.sourceId }, label = { Text(account.name) }) }
            }
        }
        OutlinedTextField(days, { days = it.filter(Char::isDigit).take(4) }, label = { Text("Review valuation every N days") }, singleLine = true)
        OutlinedTextField(note, { note = it }, label = { Text("Notes / valuation source") }, modifier = Modifier.fillMaxWidth())
        Row { Checkbox(included, { included = it }); Text("Include in net worth", Modifier.padding(top = 12.dp)) }
        (localError ?: vm.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            try {
                val day = LocalDate.parse(date)
                require(day <= LocalDate.now()) { "Effective date cannot be in the future." }
                require(name.isNotBlank()) { "Enter a name." }
                val id = existing?.id ?: UUID.randomUUID().toString()
                val now = Instant.now().toString()
                val ownershipNumber = BigDecimal(ownership)
                require(ownershipNumber >= BigDecimal.ZERO && ownershipNumber <= BigDecimal("100")) { "Ownership must be between 0 and 100." }
                require(quantity.isBlank() == price.isBlank()) { "Enter both quantity and price, or neither." }
                if (due.isNotBlank()) LocalDate.parse(due)
                val value = if (quantity.isNotBlank()) BigDecimal(quantity).multiply(BigDecimal(price)) else amount.toBigDecimalOrNull()
                require(!included || source != WealthSource.MANUAL || value != null) { "Enter a value for this asset or debt." }
                val item = WealthItem(id, name.trim(), kind, currency, source, existing?.sourceId ?: candidate?.id, parentId,
                    convention, minOf(existing?.createdDate ?: day.toString(), day.toString()), note, days.toInt(), apr.takeIf(String::isNotBlank), minimum.takeIf(String::isNotBlank), due.takeIf(String::isNotBlank), unit, securedBy, duplicateId)
                val newPolicy = WealthPolicy(UUID.randomUUID().toString(), id, day.toString(), included, ownership, false, now)
                val corrected = data.valuations.filter { it.itemId == id && it.effectiveDate == day.toString() }
                    .maxWithOrNull(compareBy<WealthValuation> { Instant.parse(it.recordedAt) }.thenBy { it.id })
                val valuation = if (source == WealthSource.MANUAL && value != null) WealthValuation(UUID.randomUUID().toString(), id, day.toString(), value.toPlainString(), quantity.takeIf(String::isNotBlank), price.takeIf(String::isNotBlank), contribution.takeIf(String::isNotBlank), withdrawal.takeIf(String::isNotBlank), note, now,
                    corrected?.id) else null
                vm.save(item, newPolicy, valuation, onClose)
            } catch (e: Exception) { localError = e.message ?: "Check the entered values." }
        }, enabled = !vm.loading && !vm.saving) { Text(if (vm.saving) "Saving…" else "Save") }
        TextButton(onClick = onClose) { Text("Cancel") }
    }
}

@Composable
internal fun FxEditor(vm: NetWorthViewModel, currentCurrency: String, close: () -> Unit) {
    var reporting by rememberSaveable { mutableStateOf(currentCurrency) }
    var from by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(vm.date.toString()) }
    WealthDialog(close) {
        Text("Reporting currency and FX", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(reporting, { reporting = it.uppercase().take(12) }, label = { Text("Reporting currency") }, singleLine = true)
        TextButton(onClick = { vm.chooseCurrency(reporting) }) { Text("Use reporting currency") }
        Text("Manual rates stay offline. Enter how much one unit of the native currency is worth in the reporting currency.")
        TextButton(onClick = vm::useWalletRates) { Text("Use existing wallet rates (original dates unknown)") }
        OutlinedTextField(from, { from = it.uppercase().take(12) }, label = { Text("Native currency") }, singleLine = true)
        DecimalField("1 native unit = reporting units", rate, { rate = it }, 12)
        OutlinedTextField(date, { date = it }, label = { Text("Rate date · YYYY-MM-DD") }, singleLine = true)
        TextButton(onClick = { vm.addRate(from, reporting, rate, date) { rate = "" } }) { Text("Save exchange rate") }
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        vm.workspace?.data?.rates?.sortedByDescending { it.effectiveDate }?.take(12)?.forEach { Text("${it.effectiveDate} · 1 ${it.from} = ${if (vm.hidden) "••••" else it.rate} ${it.to}") }
        TextButton(onClick = close) { Text("Done") }
    }
}

@Composable
private fun DecimalField(label: String, value: String, change: (String) -> Unit, precision: Int = 2) {
    OutlinedTextField(value, { candidate ->
        val raw = candidate.replace(",", "")
        if (raw.matches(Regex("[0-9]{0,24}(\\.[0-9]{0,$precision})?"))) change(raw)
    }, label = { Text(label) }, visualTransformation = com.ivy.ui.money.GroupedDecimalInput,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
}
