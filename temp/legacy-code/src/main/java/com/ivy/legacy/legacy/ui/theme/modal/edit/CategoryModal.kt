package com.ivy.wallet.ui.theme.modal.edit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import arrow.core.raise.either
import com.ivy.data.model.Category
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.domain.legacy.ui.IvyColorPicker
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.onScreenStart
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyNameTextField
import com.ivy.wallet.ui.theme.dynamicContrast
import com.ivy.wallet.ui.theme.modal.ChooseIconModal
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalAddSave
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
data class CategoryModalData(
    val category: Category?,
    val id: UUID = UUID.randomUUID(),
    val autoFocusKeyboard: Boolean = true,
    // Live UI session state follows a ViewModel-owned modal through recreation.
    // It is never serialized into a Bundle or a financial backup.
    internal val submission: com.ivy.design.system.EditorSubmission = com.ivy.design.system.EditorSubmission(),
)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun BoxWithConstraintsScope.CategoryModal(
    modal: CategoryModalData?,
    onCreateCategory: (CreateCategoryData, (Result<Unit>) -> Unit) -> Unit,
    onEditCategory: (Category, (Result<Unit>) -> Unit) -> Unit,
    dismiss: () -> Unit,
    submission: com.ivy.design.system.EditorSubmission = modal?.submission ?: remember { com.ivy.design.system.EditorSubmission() },
) {
    val initialCategory = modal?.category
    val latestSessionId by rememberUpdatedState(modal?.id)
    var nameTextFieldValue by rememberSaveable(modal?.id?.toString(), stateSaver = androidx.compose.ui.text.input.TextFieldValue.Saver) {
        mutableStateOf(selectEndTextFieldValue(initialCategory?.name?.value))
    }
    var color by rememberSaveable(modal?.id?.toString(), stateSaver = Saver<androidx.compose.ui.graphics.Color, Int>(save = { it.toArgb() }, restore = { androidx.compose.ui.graphics.Color(it) })) {
        mutableStateOf(initialCategory?.color?.let { Color(it.value) } ?: Ivy)
    }
    var icon by rememberSaveable(modal?.id?.toString(), stateSaver = Saver<IconAsset?, String>(save = { it?.id.orEmpty() }, restore = { it.takeIf(String::isNotEmpty)?.let { value -> IconAsset.from(value).getOrNull() } })) {
        mutableStateOf(initialCategory?.icon)
    }

    var chooseIconModalVisible by rememberSaveable(modal?.id?.toString()) {
        mutableStateOf(false)
    }

    IvyModal(
        id = modal?.id,
        visible = modal != null,
        title = stringResource(if (initialCategory != null) R.string.edit_category else R.string.create_category),
        hasUnsavedChanges = nameTextFieldValue.text != initialCategory?.name?.value.orEmpty() ||
            color.toArgb() != (initialCategory?.color?.value ?: Ivy.toArgb()) || icon != initialCategory?.icon,
        dismiss = dismiss,
        dismissEnabled = !submission.saving,
        PrimaryAction = {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                if (submission.saving) {
                    androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
                    androidx.compose.material3.Text(stringResource(R.string.jvy_saving),
                        style = com.ivy.design.system.JvyTypography.Supporting,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }
                submission.error?.let { error ->
                    androidx.compose.material3.Text(error,
                        Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics {
                            liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                        }, color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = com.ivy.design.system.JvyTypography.Supporting)
                }
                ModalAddSave(item = initialCategory,
                    enabled = nameTextFieldValue.text.isNotNullOrBlank() && !submission.saving) {
                    val sessionId = checkNotNull(modal?.id)
                    submission.submit({ complete ->
                        if (initialCategory != null) {
                            val updated = either {
                                initialCategory.copy(
                                    name = NotBlankTrimmedString.from(nameTextFieldValue.text.trim()).bind(),
                                    color = ColorInt(color.toArgb()), icon = icon)
                            }.getOrNull()
                            if (updated == null) complete(Result.failure(IllegalArgumentException()))
                            else onEditCategory(updated, complete)
                        } else {
                            onCreateCategory(CreateCategoryData(
                                name = nameTextFieldValue.text.trim(), color = color,
                                icon = icon?.id, id = sessionId), complete)
                        }
                    }, onSuccess = { if (latestSessionId == sessionId) dismiss() })
                }
            }
        }
    ) {
        IconNameRow(
            hint = stringResource(R.string.category_name),
            defaultIcon = R.drawable.ic_custom_category_m,
            color = color,
            icon = icon?.id,

            autoFocusKeyboard = modal?.autoFocusKeyboard ?: true,
            enabled = !submission.saving,

            nameTextFieldValue = nameTextFieldValue,
            setNameTextFieldValue = { if (!submission.saving) nameTextFieldValue = it },
            showChooseIconModal = {
                if (!submission.saving) chooseIconModalVisible = true
            }
        )

        Spacer(Modifier.height(16.dp))

        IvyColorPicker(
            selectedColor = color,
            enabled = !submission.saving,
            onColorSelected = { if (!submission.saving) color = it }
        )

    }

    ChooseIconModal(
        visible = chooseIconModalVisible,
        initialIcon = icon?.id ?: "category",
        color = color,
        dismiss = { chooseIconModalVisible = false }
    ) {
        icon = it?.let { iconId -> IconAsset.unsafe(iconId) }
    }
}

@Composable
fun IconNameRow(
    hint: String,
    @DrawableRes defaultIcon: Int,
    color: Color,
    icon: String?,

    autoFocusKeyboard: Boolean,

    nameTextFieldValue: TextFieldValue,
    setNameTextFieldValue: (TextFieldValue) -> Unit,

    showChooseIconModal: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = com.ivy.design.system.JvySpacing.PageGutter),
        horizontalArrangement = Arrangement.spacedBy(com.ivy.design.system.JvySpacing.Group),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val nameFocus = remember { FocusRequester() }

        onScreenStart {
            if (autoFocusKeyboard && enabled) {
                nameFocus.requestFocus()
            }
        }

        ItemIconMDefaultIcon(
            modifier = Modifier
                .clip(CircleShape)
                .background(color, CircleShape)
                .clickable(enabled = enabled) {
                    showChooseIconModal()
                }
                .testTag("modal_item_icon"),
            iconName = icon,
            tint = com.ivy.wallet.ui.theme.findContrastTextColor(color),
            defaultIcon = defaultIcon
        )

        val view = LocalView.current
        IvyNameTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(nameFocus),
            value = nameTextFieldValue,
            enabled = enabled,
            hint = hint,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
                autoCorrect = true
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    hideKeyboard(view)
                }
            ),
        ) { newValue ->
            setNameTextFieldValue(newValue)
        }
    }
}

@Preview
@Composable
private fun PreviewCategoryModal() {
    IvyWalletPreview {
        CategoryModal(
            modal = CategoryModalData(null),
            onCreateCategory = { _, _ -> },
            onEditCategory = { _, _ -> },
            dismiss = {},
        )
    }
}
