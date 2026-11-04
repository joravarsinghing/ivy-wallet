package com.ivy.legacy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.ivy.design.l0_system.Gray
import com.ivy.design.l0_system.UI
import com.ivy.design.l1_buildingBlocks.IvyIcon
import com.ivy.legacy.utils.onScreenStart
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyBasicTextField

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("MagicNumber")
@Composable
fun SearchInput(
    searchQueryTextFieldValue: TextFieldValue,
    hint: String,
    modifier: Modifier = Modifier,
    focus: Boolean = true,
    showClearIcon: Boolean = true,
    horizontalPadding: Dp = 16.dp,
    onSetSearchQueryTextField: (TextFieldValue) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .heightIn(min = com.ivy.design.system.JvySpacing.TouchTarget)
            .clip(UI.shapes.rFull)
            .background(UI.colors.pure)
            .border(1.dp, Gray, UI.shapes.rFull),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_search), contentDescription = null,
            modifier = Modifier.padding(horizontal = 12.dp).size(24.dp), tint = UI.colors.pureInverse)

        val searchFocus = FocusRequester()
        IvyBasicTextField(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
                .focusRequester(searchFocus),
            value = searchQueryTextFieldValue,
            hint = hint,
            onValueChanged = {
                onSetSearchQueryTextField(it)
            }
        )

        if (focus) {
            onScreenStart {
                searchFocus.requestFocus()
            }
        }

        if (showClearIcon) {
            IconButton(onClick = { onSetSearchQueryTextField(selectEndTextFieldValue("")) }) {
                IvyIcon(Modifier.size(24.dp), R.drawable.ic_outline_clear_24,
                    contentDescription = stringResource(R.string.clear_all))
            }
        }
    }
}
