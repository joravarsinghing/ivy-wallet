package com.ivy.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.design.system.JvyTextButton
import com.ivy.design.system.JvyTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardToolbar(onOpenEditor: () -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Your dashboard", Modifier.padding(vertical = 14.dp), style = JvyTypography.Supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        JvyTextButton(onOpenEditor) { Text("Customize") }
    }
}
