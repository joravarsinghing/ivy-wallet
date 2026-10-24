package com.ivy.networth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ivy.data.model.wealth.WealthKind
import com.ivy.data.model.wealth.WealthSource
import com.ivy.data.model.wealth.isDebt
import com.ivy.design.system.JvyFinanceColors
import com.ivy.design.system.JvyShapes
import com.ivy.design.system.JvyTypography
import com.ivy.domain.usecase.networth.WealthLine
import com.ivy.domain.usecase.networth.WealthResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun WealthSummary(result: WealthResult, hidden: Boolean) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = JvyShapes.Card) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HelpHeading(if (result.complete) "Your net worth" else "Your starting picture", WealthHelp.NET_WORTH)
            if (result.lines.isEmpty()) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, Modifier.size(32.dp), tint = JvyFinanceColors.asset())
                Text("A clearer picture starts here", style = MaterialTheme.typography.headlineSmall)
                Text("Bring together your savings, things you own and debts. Start with what you know — you can add more later.")
                Text("Nothing entered yet is not the same as a net worth of zero.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(money(result.netWorth, result.currency, hidden), style = JvyTypography.Amount)
                Text("What you own − what you owe", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                WealthSummaryMetric("What I own", result.assets, result.currency, hidden, false)
                WealthSummaryMetric("What I owe", result.liabilities, result.currency, hidden, true)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(if (result.complete) "Your picture on ${result.asOf.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}. Not the amount available to spend."
                else "An estimate, not a finished total. Review your accounts and add missing values or exchange rates. Unknown values stay unknown.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val needsReview = result.lines.count { it.included && it.warning != null }
            if (needsReview > 0) Text("$needsReview estimate(s) or exchange rates need checking", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WealthSummaryMetric(label: String, value: BigDecimal, currency: String, hidden: Boolean, debt: Boolean) {
    val ink = if (debt) JvyFinanceColors.debt() else JvyFinanceColors.asset()
    Surface(shape = JvyShapes.Control, color = if (debt) JvyFinanceColors.debtSurface() else JvyFinanceColors.assetSurface()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (debt) Icons.Outlined.RemoveCircleOutline else Icons.Outlined.AddCircleOutline, null, Modifier.size(20.dp), tint = ink)
                Text(label, style = MaterialTheme.typography.labelLarge, color = ink)
            }
            Text(money(value, currency, hidden), style = JvyTypography.AmountSmall, color = ink)
        }
    }
}

internal fun WealthKind.symbol(): ImageVector = when (this) {
    WealthKind.PROPERTY -> Icons.Outlined.Home
    WealthKind.VEHICLE -> Icons.Outlined.DirectionsCar
    WealthKind.INVESTMENT, WealthKind.RETIREMENT, WealthKind.CRYPTO -> Icons.Outlined.ShowChart
    WealthKind.BUSINESS -> Icons.Outlined.Storefront
    WealthKind.GOLD -> Icons.Outlined.Diamond
    else -> if (isDebt) Icons.Outlined.CreditCard else Icons.Outlined.AccountBalanceWallet
}

@Composable
internal fun WealthPositionRow(line: WealthLine, currency: String, hidden: Boolean, onClick: () -> Unit) {
    val debt = (line.reportingAmount?.signum() ?: if (line.item.kind.isDebt) -1 else 1) < 0
    val ink = if (debt) JvyFinanceColors.debt() else JvyFinanceColors.asset()
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = JvyShapes.Control, color = if (debt) JvyFinanceColors.debtSurface() else JvyFinanceColors.assetSurface()) {
                    Icon(line.item.kind.symbol(), null, Modifier.padding(12.dp).size(24.dp), tint = ink)
                }
                Column(Modifier.weight(1f)) {
                    Text(line.item.name, style = MaterialTheme.typography.titleMedium)
                    Text(line.item.kind.title(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (!line.included) "Not counted in your total" else money(line.reportingAmount?.abs(), currency, hidden), style = JvyTypography.AmountSmall, color = ink)
            Text(when (line.item.source) {
                WealthSource.ACCOUNT -> "Linked to your wallet balance"
                WealthSource.LOAN -> "Linked to your loan record"
                WealthSource.MANUAL -> "Your own estimate"
                WealthSource.HOLDINGS -> "An investment group"
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            (line.issue ?: line.warning)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
internal fun WealthAllocationRow(kind: WealthKind, amount: BigDecimal, assets: BigDecimal, currency: String, hidden: Boolean) {
    val fraction = if (hidden || assets.signum() <= 0) 0f else amount.divide(assets, 6, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(kind.title(), style = MaterialTheme.typography.titleSmall)
        Text(money(amount, currency, hidden), style = JvyTypography.AmountSmall)
        LinearProgressIndicator(progress = fraction, modifier = Modifier.fillMaxWidth().height(6.dp), color = JvyFinanceColors.asset(), trackColor = JvyFinanceColors.assetSurface())
    }
}
