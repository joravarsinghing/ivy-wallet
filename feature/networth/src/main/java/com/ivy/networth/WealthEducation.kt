package com.ivy.networth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import com.ivy.design.system.JvyButton as Button
import com.ivy.design.system.JvyOutlinedButton as OutlinedButton
import com.ivy.design.system.JvyTextButton as TextButton

internal enum class WealthHelp(val title: String, val explanation: String, val example: String) {
    NET_WORTH("What is net worth?", "Add up the money and things you own. Then subtract the debts you still need to repay. The result is your net worth — a snapshot, not money you can spend today.", "Example only: you own 30,000.00 and owe 8,000.00 in the same currency. Your net worth is 22,000.00. A negative total is not a score or a failure; it is a starting point."),
    ASSETS("What do I own?", "Assets are money and things with a resale value: cash, savings, investments, a home, a car or gold. Use a reasonable value today, not the price you originally paid.", "Example only: a car bought for 20,000.00 might sell for 12,000.00 today. Enter 12,000.00. Do not count future salary or money reserved for a goal a second time."),
    DEBTS("What do I owe?", "Debts (also called liabilities) are amounts still left to repay: credit cards, a mortgage, loans, or money borrowed from someone. Enter the remaining balance, not the original loan or the monthly payment.", "Example only: a 10,000.00 loan has 6,000.00 left. Enter 6,000.00, not your 300.00 monthly payment. Add your home at its full current value and its mortgage separately; Jvy does the subtraction."),
    OWNERSHIP("My share of an item", "Start with the full value of the item, then tell Jvy what percentage belongs to you. Leave 100% if it is entirely yours. Do not reduce both the amount and the percentage.", "Example only: a shared home is worth 200,000.00 and you own half. Enter 200,000.00 and 50%. Jvy counts 100,000.00. For a debt, use the full balance with your share %, or only your own balance at 100% — not both reductions."),
    VALUE("How do I choose a value?", "Use a recent account statement, loan balance or realistic selling price. An estimate is fine to start; you can update it later. Adding or updating a value does not create income or spending in your wallet.", "Example only: use today's bank balance for savings and today's resale estimate for a car. Give the date that value was true, so earlier months are not rewritten by accident."),
    CURRENCY("One currency for your total", "Items can have different currencies. Jvy converts them into one currency so they can be added fairly. A missing exchange rate leaves the total unfinished rather than guessing.", "Example only: if 1 unit of an item's currency is worth 0.25 units of your total's currency, enter 0.25. A rate changes over time; use the date you checked it."),
    DUPLICATES("Count each thing only once", "An account you already track can be linked instead of typed in again. The same investment or debt should not appear both as an account total and as another manual item.", "Example only: if your investment account already includes all your shares, link that total OR add the individual shares — not both. Moving money into a goal is not a new asset."),
    HISTORY("Why does my net worth change?", "Your total changes when you earn or spend, repay interest, change an item's value, or use a different exchange rate. Jvy needs observations from more than one date to show a meaningful change.", "Example only: paying 500.00 of loan principal from cash reduces cash and debt by the same amount. Net worth stays the same. Interest is a cost and reduces net worth."),
}

@Composable
internal fun WealthHelpButton(topic: WealthHelp) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Info, "Explain ${topic.title}", Modifier.size(20.dp)) }
    if (open) WealthHelpDialog(topic) { open = false }
}

@Composable
internal fun WealthHelpDialog(topic: WealthHelp, close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text(topic.title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(topic.explanation)
            Text(topic.example, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, confirmButton = { TextButton(onClick = close) { Text("Got it") } })
}

@Composable
internal fun HelpHeading(title: String, topic: WealthHelp) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        WealthHelpButton(topic)
    }
}

internal data class WealthGuideStep(val title: String, val body: String, val example: String)
internal val wealthGuideSteps = listOf(
    WealthGuideStep("Start with what you own", WealthHelp.ASSETS.explanation, "Start small: your cash and savings are enough for a first estimate. Add property and investments later."),
    WealthGuideStep("Add what you still owe", WealthHelp.DEBTS.explanation, "Check your latest statements. Use remaining balances, not monthly payments. If you have no debts, confirm that during the review."),
    WealthGuideStep("Use the accounts you already track", WealthHelp.DUPLICATES.explanation, "Link your wallet accounts and loans first. Only add other items manually, so the same money is not counted twice."),
    WealthGuideStep("Your starting point, not a judgement", WealthHelp.NET_WORTH.explanation, "Example only, in one currency: 5,000.00 in savings + an 8,000.00 car − a 3,000.00 loan = 10,000.00 net worth. You still have 5,000.00 in savings, not 10,000.00 available to spend. A negative total is a starting point, not a failure."),
)

@Composable
internal fun WealthGuide(close: () -> Unit, finish: () -> Unit, linkAccounts: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    WealthDialog(close) {
        WealthGuidePage(step, next = { if (step == wealthGuideSteps.lastIndex) finish() else step++ }, previous = { step-- }, close = close, linkAccounts = linkAccounts)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ColumnScope.WealthGuidePage(step: Int, next: () -> Unit, previous: () -> Unit, close: () -> Unit, linkAccounts: () -> Unit) {
        val page = wealthGuideSteps[step]
        Text("Net worth, made simple", style = MaterialTheme.typography.titleLarge)
        Text("${step + 1} of ${wealthGuideSteps.size}", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = (step + 1f) / wealthGuideSteps.size, modifier = Modifier.fillMaxWidth())
        Text(page.title, style = MaterialTheme.typography.headlineSmall)
        Text(page.body)
        Text(page.example, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (step == 2) OutlinedButton(onClick = linkAccounts, modifier = Modifier.fillMaxWidth()) { Text("Check my existing accounts") }
        Button(onClick = next, modifier = Modifier.fillMaxWidth()) { Text(if (step == wealthGuideSteps.lastIndex) "I'm ready to start" else "Next") }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (step > 0) TextButton(onClick = previous) { Text("Previous") }
            TextButton(onClick = close) { Text("I'll explore first") }
        }
}

/** DatePicker stores UTC dates; wealth persistence remains the same ISO local-date contract. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WealthDateField(label: String, value: String, change: (String) -> Unit, allowFuture: Boolean = false) {
    com.ivy.ui.time.JvyDateField(label, runCatching { LocalDate.parse(value) }.getOrNull(),
        { it?.let { day -> change(day.toString()) } }, latestDate = if (allowFuture) null else LocalDate.now())
}
