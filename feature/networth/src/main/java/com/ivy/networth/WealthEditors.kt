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
import com.ivy.design.system.JvyTypography
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import com.ivy.design.system.JvyButton as Button
import com.ivy.design.system.JvyOutlinedButton as OutlinedButton
import com.ivy.design.system.JvyTextButton as TextButton

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
    var advanced by rememberSaveable { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var useQuantity by rememberSaveable { mutableStateOf(last?.quantity != null) }
    val isDebtEntry = kind.isDebt
    WealthDialog(onClose) {
        Text(if (existing != null) "Update ${existing.name}" else if (isDebtEntry) "Add something I owe" else "Add something I own", style = MaterialTheme.typography.titleLarge)
        Text(if (candidate != null) "This stays linked to your wallet. Jvy uses the balance you already track — you won't enter the same money twice." else "This is a value for your bigger financial picture. It won't add income, spending or a transaction to your wallet.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(name, { name = it }, label = { Text("Give it a name") }, placeholder = { Text(if (isDebtEntry) "e.g. Car loan" else "e.g. My savings or family home") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Box {
            OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Type: ${kind.title()}") }
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                // New entries stay focused; existing entries can still correct a wrong type.
                WealthKind.entries.filter { existing != null || it.isDebt == isDebtEntry || it == kind }.forEach { choice ->
                    DropdownMenuItem(text = { Text(choice.title()) }, onClick = {
                        kind = choice; categoryMenu = false
                        if (choice == WealthKind.GOLD && unit !in listOf("grams", "troy ounces")) unit = "grams"
                        if (source == WealthSource.MANUAL || source == WealthSource.HOLDINGS) {
                            convention = if (choice.isDebt) BalanceConvention.POSITIVE_OWED else BalanceConvention.ASSET
                            if (choice.isDebt && source == WealthSource.HOLDINGS) source = WealthSource.MANUAL
                        }
                    })
                }
            }
        }
        val canReviewCurrency = existing?.source in listOf(WealthSource.ACCOUNT, WealthSource.LOAN)
        OutlinedTextField(currency, { if (canReviewCurrency || existing == null && candidate?.source != WealthSource.ACCOUNT) currency = it.uppercase().take(12) }, label = { Text("Currency of this amount") }, supportingText = { Text("Use the currency on the statement or estimate, such as AED, USD or INR.") }, readOnly = !canReviewCurrency && (existing != null || candidate?.source == WealthSource.ACCOUNT), singleLine = true, modifier = Modifier.fillMaxWidth())
        if (candidate?.source == WealthSource.LOAN) Text("Check your loan statement's currency before saving.", style = MaterialTheme.typography.bodySmall)
        if (source == WealthSource.MANUAL) {
            HelpHeading(if (kind.isDebt) "How much is left to repay?" else "What is it worth today?", if (kind.isDebt) WealthHelp.DEBTS else WealthHelp.VALUE)
            if (kind in listOf(WealthKind.INVESTMENT, WealthKind.RETIREMENT, WealthKind.CRYPTO, WealthKind.GOLD) || useQuantity) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !useQuantity, onClick = { useQuantity = false; quantity = ""; price = "" }, label = { Text("Enter a total") }, colors = wealthChipColors())
                    FilterChip(selected = useQuantity, onClick = { useQuantity = true }, label = { Text("Quantity × price") }, colors = wealthChipColors())
                }
            }
            if (!useQuantity) DecimalField(if (kind.isDebt) "Remaining balance" else "My best estimate", amount, { amount = it })
            else {
                DecimalField("How many units do you own?", quantity, { quantity = it }, 8)
                DecimalField("Price of one unit", price, { price = it }, 8)
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
                        }, label = { Text(u) }, colors = wealthChipColors()) }
                    }
                    if (existing != null) Text("The unit is fixed for this holding so its earlier quantities remain accurate. Add a new holding to use another unit.", style = MaterialTheme.typography.bodySmall)
                }
                Text("Jvy multiplies your quantity by the price. You don't need to enter a total as well.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (source == WealthSource.HOLDINGS) Text("Jvy adds the items inside this investment group. It does not count a separate total on top.")
        WealthDateField("This value is from", date, { date = it })
        if (ownership != "100") Text("Your share: $ownership%. Jvy counts only your share of the full value.")
        if (source == WealthSource.ACCOUNT) {
            HelpHeading("How does this account show a debt?", WealthHelp.DEBTS)
            listOf(BalanceConvention.ASSET to "Cash or savings · a minus balance means overdraft", BalanceConvention.NEGATIVE_OWED to "A minus balance is what I owe · e.g. −500", BalanceConvention.POSITIVE_OWED to "A plus balance is what I owe · e.g. 500 on a credit card").forEach { (c, label) ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(selected = convention == c, onClick = { convention = c }); Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium) }
            }
        }
        TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Hide optional details" else "More options · shared ownership, investments & notes") }
        if (advanced) {
        HelpHeading("Your share of the full value", WealthHelp.OWNERSHIP)
        DecimalField("My ownership share % · usually 100", ownership, { ownership = it }, 4)
        if (candidate == null && (existing == null || existing.source in listOf(WealthSource.MANUAL, WealthSource.HOLDINGS))) {
            HelpHeading("Investment groups (optional)", WealthHelp.DUPLICATES)
            if (!kind.isDebt && parentId == null) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = source == WealthSource.MANUAL, onClick = { source = WealthSource.MANUAL }, label = { Text("Use this item's value") }, colors = wealthChipColors())
                FilterChip(selected = source == WealthSource.HOLDINGS, onClick = { source = WealthSource.HOLDINGS }, label = { Text("Add up items in a group") }, colors = wealthChipColors())
            }
            if (source == WealthSource.MANUAL && existing == null) {
                Text("Add to an investment group")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = parentId == null, onClick = { parentId = null }, label = { Text("Keep separate") }, colors = wealthChipColors())
                    data.items.filter { it.source == WealthSource.HOLDINGS }.forEach { portfolio -> FilterChip(selected = parentId == portfolio.id, onClick = { parentId = portfolio.id }, label = { Text(portfolio.name) }, colors = wealthChipColors()) }
                }
            }
            if (kind in listOf(WealthKind.INVESTMENT, WealthKind.RETIREMENT, WealthKind.CRYPTO, WealthKind.GOLD)) {
                Text("Help explain changes in value (optional)")
                DecimalField("Money added since the previous value", contribution, { contribution = it })
                DecimalField("Money taken out since the previous value", withdrawal, { withdrawal = it })
            }
        }
        if (kind.isDebt) {
            Text("Loan details are for reference. They do not add future interest to what you owe today.", style = MaterialTheme.typography.bodySmall)
            DecimalField("Yearly interest rate (APR %) · optional", apr, { apr = it }, 4)
            DecimalField("Minimum payment (optional)", minimum, { minimum = it })
            if (due.isBlank()) TextButton(onClick = { due = LocalDate.now().toString() }) { Text("Add next payment date (optional)") }
            else { WealthDateField("Next payment date", due, { due = it }, allowFuture = true); TextButton(onClick = { due = "" }) { Text("Remove payment date") } }
            Text("Secured against (optional)")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = securedBy == null, onClick = { securedBy = null }, label = { Text("None") }, colors = wealthChipColors())
                data.items.filter { it.kind == WealthKind.PROPERTY || it.kind == WealthKind.VEHICLE }.forEach { property -> FilterChip(selected = securedBy == property.id, onClick = { securedBy = property.id }, label = { Text(property.name) }, colors = wealthChipColors()) }
            }
        }
        if (source == WealthSource.LOAN) {
            Text("Same debt also represented by a wallet account? Select only if these are duplicate records, not the account that paid or received the loan.")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = duplicateId == null, onClick = { duplicateId = null }, label = { Text("No duplicate") }, colors = wealthChipColors())
                data.items.filter { it.source == WealthSource.ACCOUNT }.forEach { account -> FilterChip(selected = duplicateId == account.sourceId, onClick = { duplicateId = account.sourceId }, label = { Text(account.name) }, colors = wealthChipColors()) }
            }
        }
        OutlinedTextField(days, { days = it.filter(Char::isDigit).take(4) }, label = { Text("Flag this estimate for review after (days)") }, singleLine = true)
        OutlinedTextField(note, { note = it }, label = { Text("Notes / valuation source") }, modifier = Modifier.fillMaxWidth())
        }
        Row { Checkbox(included, { included = it }); Text("Count this in my net worth", Modifier.padding(top = 12.dp)) }
        (localError ?: vm.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (existing != null) Text("Changes to type or valuation method start on the chosen date. The earlier setup is kept in your history.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = {
            try {
                val day = LocalDate.parse(date)
                require(day <= LocalDate.now()) { "Effective date cannot be in the future." }
                require(name.isNotBlank()) { "Enter a name." }
                val id = existing?.id ?: UUID.randomUUID().toString()
                val now = Instant.now().toString()
                val ownershipNumber = BigDecimal(ownership)
                require(ownershipNumber >= BigDecimal.ZERO && ownershipNumber <= BigDecimal("100")) { "Ownership must be between 0 and 100." }
                require(!useQuantity || quantity.isNotBlank() && price.isNotBlank()) { "Enter both the quantity and the price of one unit, or choose Enter a total." }
                require(quantity.isBlank() == price.isBlank()) { "Enter both quantity and price, or neither." }
                if (due.isNotBlank()) LocalDate.parse(due)
                val value = if (quantity.isNotBlank()) BigDecimal(quantity).multiply(BigDecimal(price)) else amount.toBigDecimalOrNull()
                require(!included || source != WealthSource.MANUAL || value != null) { "Enter a value, even if it is an estimate." }
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
        Text("Currency for your total", style = MaterialTheme.typography.titleLarge)
        HelpHeading("Add different currencies fairly", WealthHelp.CURRENCY)
        OutlinedTextField(reporting, { reporting = it.uppercase().take(12) }, label = { Text("Show my total in · e.g. AED") }, singleLine = true)
        TextButton(onClick = { vm.chooseCurrency(reporting) }) { Text("Use this currency for my total") }
        Text("If an item uses another currency, add an exchange rate below. Rates stay offline and won't update automatically.")
        TextButton(onClick = vm::useWalletRates) { Text("Use existing wallet rates (original dates unknown)") }
        OutlinedTextField(from, { from = it.uppercase().take(12) }, label = { Text("Currency the item is in · e.g. USD") }, singleLine = true)
        DecimalField("1 ${from.ifBlank { "item currency" }} = how many ${reporting.ifBlank { "total currency" }}?", rate, { rate = it }, 12)
        WealthDateField("Rate checked on", date, { date = it })
        TextButton(onClick = { vm.addRate(from, reporting, rate, date) { rate = "" } }) { Text("Save exchange rate") }
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        vm.workspace?.data?.rates?.sortedByDescending { it.effectiveDate }?.take(12)?.forEach { Text("${it.effectiveDate} · 1 ${it.from} = ${if (vm.hidden) "••••" else it.rate} ${it.to}") }
        TextButton(onClick = close) { Text("Done") }
    }
}

@Composable
private fun DecimalField(label: String, value: String, change: (String) -> Unit, precision: Int = 2) {
    com.ivy.ui.money.JvyDecimalField(label, value, change, precision = precision)
}
