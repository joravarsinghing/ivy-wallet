package com.ivy.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.design.system.JvyOutlinedButton
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Suggestions(
    suggestions: List<Any>,
    onAddSuggestion: (Any) -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FlowRow(modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { suggestion ->
            val name = when (suggestion) {
                is CreateAccountData -> suggestion.name
                is CreateCategoryData -> suggestion.name
                else -> return@forEach
            }
            JvyOutlinedButton({ onAddSuggestion(suggestion) }, enabled = enabled) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(name, Modifier.weight(1f, fill = false))
            }
        }
        JvyOutlinedButton(onAddNew, enabled = enabled) {
            Icon(Icons.Outlined.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_new))
        }
    }
}
