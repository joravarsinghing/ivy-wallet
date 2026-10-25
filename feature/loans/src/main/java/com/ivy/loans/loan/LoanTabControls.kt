package com.ivy.loans.loan

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Local filters sit by the content, not in a second app-like bottom navigation bar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoanTabControls(tab: LoanTab, select: (LoanTab) -> Unit) {
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LoanTab.entries.forEach { item ->
            val label = if (item == LoanTab.PENDING) "Pending" else "Completed"
            FilterChip(tab == item, { select(item) }, label = { Text(label) }, modifier = Modifier.testTag(label.lowercase()))
        }
    }
}
