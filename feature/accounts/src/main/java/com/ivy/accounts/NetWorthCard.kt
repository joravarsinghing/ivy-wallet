package com.ivy.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ivy.domain.usecase.networth.NetWorthAccountRole
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.utils.format

@Composable
fun NetWorthCard(
    state: NetWorthUiState,
    currency: String,
    onOpenSetup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(UI.colors.medium)
            .padding(18.dp),
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val hidden = remember(context) { com.ivy.base.legacy.SharedPrefs(context) }
            .getBoolean(com.ivy.base.legacy.SharedPrefs.HIDE_CURRENT_BALANCE, false)
        Text(
            text = "Net worth",
            style = UI.typo.b1.style(color = UI.colors.pureInverse, fontWeight = FontWeight.ExtraBold),
        )
        Spacer(Modifier.height(6.dp))
        if (!state.hasWealthItems && state.accounts.isEmpty() && state.unclassifiedCount == 0) {
            Text("Add or link what you own and what you owe.", style = UI.typo.c.style(color = UI.colors.gray))
        } else if (state.unclassifiedCount > 0) {
            Text(
                text = "Review ${state.unclassifiedCount} source(s) or valuation(s) in your net worth workspace.",
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
            )
        } else {
            Text(
                text = if (hidden) "••••" else state.netWorth.toDouble().format(currency),
                style = UI.typo.h2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.ExtraBold),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NetWorthValue("Assets", if (hidden) "••••" else state.assets.toDouble().format(currency))
                NetWorthValue("Liabilities", if (hidden) "••••" else state.liabilities.toDouble().format(currency))
            }
            if (!hidden && state.history.size >= 2) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "History: " + state.history.joinToString("  ·  ") { point ->
                        "${point.period} ${point.netWorth.format(currency)}"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenSetup)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            text = "Open net worth workspace",
            style = UI.typo.c.style(color = UI.colors.primary, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun NetWorthValue(label: String, value: String) {
    Column {
        Text(label, style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium))
        Text(value, style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun NetWorthSetupDialog(
    state: NetWorthUiState,
    currency: String,
    onRoleChanged: (java.util.UUID, NetWorthAccountRole?) -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(UI.colors.medium)
                .padding(20.dp),
        ) {
            Text("Classify accounts", style = UI.typo.b1.style(color = UI.colors.pureInverse, fontWeight = FontWeight.ExtraBold))
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap an account to cycle through unclassified, asset, and liability.",
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.height(14.dp))
            state.accounts.forEach { account ->
                val nextRole = when (account.role) {
                    null -> NetWorthAccountRole.ASSET
                    NetWorthAccountRole.ASSET -> NetWorthAccountRole.LIABILITY
                    NetWorthAccountRole.LIABILITY -> null
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onRoleChanged(account.id, nextRole) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(account.name, style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold))
                        Text(account.balance.toDouble().format(currency), style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium))
                    }
                    Text(
                        text = account.role?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Unclassified",
                        style = UI.typo.c.style(color = UI.colors.primary, fontWeight = FontWeight.Bold),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onClose)
                    .padding(10.dp),
                text = "Done",
                style = UI.typo.b2.style(color = UI.colors.primary, fontWeight = FontWeight.Bold),
            )
        }
    }
}
