package com.ivy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style

@Composable
fun HomeDashboardToolbar(
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Your dashboard",
            style = UI.typo.c.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenEditor)
                .semantics {
                    role = Role.Button
                    contentDescription = "Customize Home dashboard"
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .background(UI.colors.pure, RoundedCornerShape(12.dp))
                .padding(horizontal = 2.dp),
            text = "Customize",
            style = UI.typo.c.style(
                color = UI.colors.primary,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}
