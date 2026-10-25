package com.ivy.networth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ivy.data.model.wealth.WealthSource
import com.ivy.domain.usecase.networth.WealthCandidate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Presentation only: the route still owns coverage decisions and dated persistence. */
@Composable
internal fun ColumnScope.WealthReviewBody(candidates: List<WealthCandidate>, date: LocalDate,
    ownReviewed: Boolean, oweReviewed: Boolean, onOwn: (Boolean) -> Unit, onOwe: (Boolean) -> Unit,
    onSelect: (WealthCandidate) -> Unit) {
    Text("Start with what you already track", style = MaterialTheme.typography.titleLarge)
    Text("Choose which accounts and loans belong in your total. Their balances update from your wallet entries, so you won't need to type them in again.")
    HelpHeading("Count every item once", WealthHelp.DUPLICATES)
    Text("This review is for ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}. To review an earlier date, choose it from Net worth tools first.",
        style = MaterialTheme.typography.bodySmall)
    candidates.forEach { source ->
        OutlinedCard(onClick = { onSelect(source) }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(source.name, style = MaterialTheme.typography.titleSmall)
                Text(if (source.source == WealthSource.ACCOUNT) "Wallet account" else "Loan", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Choose how to count this", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    if (candidates.isEmpty()) Text("You've checked every existing account and loan. Add anything else you own or owe using the buttons on the main page.")
    Row(Modifier.fillMaxWidth().toggleable(ownReviewed, role = Role.Checkbox, onValueChange = onOwn), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(ownReviewed, null); Text("I reviewed what I own: savings, investments, property and other assets.", Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth().toggleable(oweReviewed, role = Role.Checkbox, onValueChange = onOwe), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(oweReviewed, null); Text("I reviewed what I owe: credit cards, loans and other debts.", Modifier.weight(1f))
    }
}
