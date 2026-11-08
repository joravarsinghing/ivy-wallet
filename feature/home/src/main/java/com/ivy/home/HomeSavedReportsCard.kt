package com.ivy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.data.db.entity.SavedAnalyticsEntity
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style

/** Home entry point; Reports remains the single owner of report behavior. */
@Composable
fun HomeSavedReportsCard(
    reports: List<SavedAnalyticsEntity>,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(UI.colors.medium, UI.shapes.r3)
            .clickable(onClick = onOpenReports)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.weight(1f),
                text = "Saved reports",
                style = UI.typo.b1.style(fontWeight = FontWeight.ExtraBold),
            )
            Text(
                text = "Open reports",
                style = UI.typo.c.style(color = UI.colors.primary, fontWeight = FontWeight.Bold),
            )
        }
        Spacer(Modifier.height(8.dp))
        reports.take(3).forEach { report ->
            Text(
                text = report.name,
                style = UI.typo.b2.style(color = UI.colors.pureInverse),
            )
            Spacer(Modifier.height(3.dp))
        }
    }
}
