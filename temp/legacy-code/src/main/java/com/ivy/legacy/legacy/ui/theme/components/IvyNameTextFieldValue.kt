package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.isNotNullOrBlank

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
// Preserve this established positional adapter; migrate callers before reordering its API.
@Suppress("ParameterNaming", "ComposeParameterOrder")
fun IvyNameTextField(
    modifier: Modifier = Modifier,
    underlineModifier: Modifier = Modifier,
    value: TextFieldValue,
    textColor: Color = UI.colors.pureInverse,
    hint: String?,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        autoCorrect = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done,
        capitalization = KeyboardCapitalization.Sentences
    ),
    focusRequester: FocusRequester = remember { FocusRequester() },
    keyboardActions: KeyboardActions? = null,
    inputTestTag: String = "base_input",
    enabled: Boolean = true,
    onValueChanged: (TextFieldValue) -> Unit
) {
    // Preserve the legacy API while sharing labelled Material controls and Jvy tokens.
    val view = LocalView.current
    androidx.compose.material3.OutlinedTextField(
        value = value, onValueChange = onValueChanged, enabled = enabled,
        modifier = modifier.fillMaxWidth().testTag(inputTestTag).focusRequester(focusRequester),
        label = { if (!hint.isNullOrBlank()) Text(hint) },
        textStyle = com.ivy.design.system.JvyTypography.Body,
        shape = com.ivy.design.system.JvyShapes.Control,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor, unfocusedTextColor = textColor),
        visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions ?: KeyboardActions(onDone = { hideKeyboard(view) }),
    )
}

@Preview
@Composable
private fun PreviewIvyNameTextField() {
    IvyWalletComponentPreview {
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            IvyNameTextField(
                modifier = Modifier.padding(horizontal = 32.dp),
                underlineModifier = Modifier.padding(horizontal = 24.dp),
                value = TextFieldValue("Title"),
                hint = "Title",
                onValueChanged = {}
            )
        }
    }
}
