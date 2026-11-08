package com.ivy.wallet.ui.edit.core

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.ivy.base.model.TransactionType
import com.ivy.design.system.JvyScreenHeader
import com.ivy.navigation.navigation
import com.ivy.ui.R
import java.util.UUID

/** Transaction editors share subsection chrome; secondary/destructive work is contextual. */
@Deprecated("Old design system adapter. Uses the shared Jvy header.")
@Composable
fun Toolbar(
    type: TransactionType,
    initialTransactionId: UUID?,
    onDeleteTrnModal: () -> Unit,
    onChangeTransactionTypeModal: () -> Unit,
    showDuplicateButton: Boolean,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
    allowTypeChange: Boolean = true,
    enabled: Boolean = true,
) {
    val nav = navigation()
    var menuOpen by remember { mutableStateOf(false) }
    val canChangeType = allowTypeChange && type != TransactionType.TRANSFER
    val title = stringResource(when (type) {
        TransactionType.INCOME -> R.string.income
        TransactionType.EXPENSE -> R.string.expense
        TransactionType.TRANSFER -> R.string.transfer
    })
    Box(modifier) {
        JvyScreenHeader(title, { nav.onBackPressed() }, backEnabled = enabled, actions = {
            if (canChangeType || initialTransactionId != null) Box {
                IconButton({ menuOpen = true }, enabled = enabled,
                    modifier = Modifier.testTag("editor_actions")) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.jvy_navigation_actions))
                }
                DropdownMenu(menuOpen && enabled, { menuOpen = false }) {
                    if (canChangeType) DropdownMenuItem(
                        text = { Text(stringResource(R.string.jvy_change_transaction_type)) },
                        onClick = { menuOpen = false; onChangeTransactionTypeModal() },
                    )
                    if (initialTransactionId != null) {
                        if (showDuplicateButton) DropdownMenuItem(
                            text = { Text(stringResource(R.string.jvy_duplicate_transaction)) },
                            modifier = Modifier.testTag("duplicate_button"),
                            onClick = { menuOpen = false; onDuplicate() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.testTag("delete_button"),
                            onClick = { menuOpen = false; onDeleteTrnModal() },
                        )
                    }
                }
            }
        })
    }
}
