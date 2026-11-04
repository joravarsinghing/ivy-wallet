package com.ivy.wallet.ui.theme.modal

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ivy.design.l0_system.UI
import com.ivy.design.utils.rememberInteractionSource
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.addKeyboardListener
import com.ivy.legacy.utils.consumeClicks
import com.ivy.legacy.utils.densityScope
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.keyboardOnlyWindowInsets
import com.ivy.legacy.utils.navigationBarInsets
import com.ivy.legacy.utils.onScreenStart
import com.ivy.design.utils.thenIf
import com.ivy.navigation.Navigation
import com.ivy.navigation.navigation
import com.ivy.wallet.ui.theme.components.ActionsRow
import com.ivy.wallet.ui.theme.components.CloseButton
import com.ivy.wallet.ui.theme.gradientCutBackgroundTop
import com.ivy.wallet.ui.theme.mediumBlur
import java.util.UUID
import kotlin.math.roundToInt

@Deprecated("Old design system. Use `:ivy-design` and Material3")
private const val DURATION_BACKGROUND_BLUR_ANIM = 400

@Deprecated("Old design system. Use `:ivy-design` and Material3")
const val DURATION_MODAL_ANIM = 200

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
@OptIn(ExperimentalLayoutApi::class)
// Preserve this established positional adapter; migrate callers before reordering its API.
@Suppress("ComposeParameterOrder")
fun BoxScope.IvyModal(
    id: UUID?,
    visible: Boolean,
    dismiss: () -> Unit,
    SecondaryActions: (@Composable () -> Unit)? = null,
    PrimaryAction: @Composable () -> Unit,
    scrollState: ScrollState? = rememberScrollState(),
    shiftIfKeyboardShown: Boolean = true,
    includeActionsRowPadding: Boolean = true,
    dismissEnabled: Boolean = true,
    hasUnsavedChanges: Boolean = false,
    title: String? = null,
    Content: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return

    val rootView = LocalView.current
    var discardRequested by remember(id) { mutableStateOf(false) }
    val requestClose = {
        if (dismissEnabled) {
            hideKeyboard(rootView)
            if (hasUnsavedChanges) discardRequested = true else dismiss()
        }
    }
    // Some legacy callers allocate their data ID on recomposition. Back ownership
    // belongs to this visible host, not that changing data ID, or a parent can
    // re-register above its still-open child and steal the child's Back action.
    val backId = remember { UUID.randomUUID() }
    ModalBackHandling(modalId = backId, visible = true, dismiss = requestClose)

    if (discardRequested) androidx.compose.material3.AlertDialog(
        onDismissRequest = { discardRequested = false },
        title = { Text(androidx.compose.ui.res.stringResource(com.ivy.ui.R.string.jvy_discard_title)) },
        text = { Text(androidx.compose.ui.res.stringResource(com.ivy.ui.R.string.jvy_discard_message)) },
        confirmButton = { com.ivy.design.system.JvyTextButton({
            if (dismissEnabled) { discardRequested = false; dismiss() }
        }, enabled = dismissEnabled) { Text(androidx.compose.ui.res.stringResource(com.ivy.ui.R.string.jvy_discard)) } },
        dismissButton = { com.ivy.design.system.JvyTextButton({ discardRequested = false }) {
            Text(androidx.compose.ui.res.stringResource(com.ivy.ui.R.string.jvy_keep_editing))
        } })

    Box(Modifier.fillMaxSize().background(mediumBlur())
        .testTag("modal_outside_blur")
        .clickable(enabled = dismissEnabled, onClick = requestClose).zIndex(1000f))
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(com.ivy.design.system.JvySpacing.ModalMargin)
        .zIndex(1001f), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Surface(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            shape = com.ivy.design.system.JvyShapes.Sheet,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.consumeClicks(rememberInteractionSource())) {
                if (title != null) {
                    com.ivy.design.system.JvyDialogHeader(title, requestClose,
                        closeEnabled = dismissEnabled)
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End) {
                        com.ivy.design.system.JvyCloseButton(requestClose,
                            Modifier.testTag("modal_close_button"), enabled = dismissEnabled)
                    }
                }
                Column(Modifier.weight(1f, fill = false).thenIf(scrollState != null) {
                    verticalScroll(scrollState!!)
                }.thenIf(title != null) {
                    padding(top = com.ivy.design.system.JvySpacing.Related,
                        bottom = com.ivy.design.system.JvySpacing.Group)
                }) { Content() }
                androidx.compose.material3.HorizontalDivider()
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = com.ivy.design.system.JvySpacing.PageGutter,
                    vertical = com.ivy.design.system.JvySpacing.ActionInset),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryActions?.invoke()
                    PrimaryAction()
                }
            }
        }
    }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
private fun ModalBackHandling(
    modalId: UUID?,
    visible: Boolean,
    dismiss: () -> Unit
) {
    AddModalBackHandling(
        modalId = modalId,
        visible = visible,
        action = {
            dismiss()
        }
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun AddModalBackHandling(
    modalId: UUID?,
    visible: Boolean,
    action: () -> Unit
) {
    val latestAction by rememberUpdatedState(action)
    val nav = navigation()
    DisposableEffect(nav, modalId, visible) {
        val unregister = if (visible && modalId != null) {
            nav.registerModalBackHandler(modalId) {
                latestAction()
                true
            }
        } else null
        onDispose { unregister?.invoke() }
    }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@Composable
fun ModalActionsRow(
    visible: Boolean,
    modalPercentVisible: Float,
    keyboardShownInsetDp: Dp,
    navBarPadding: Dp,

    onHeightChanged: (Int) -> Unit,

    SecondaryActions: (@Composable () -> Unit)? = null,
    PrimaryAction: @Composable () -> Unit
) {
    if (visible || modalPercentVisible > 0.01f) {
        val ivyContext = ivyWalletCtx()
        ActionsRow(
            modifier = Modifier
                .onSizeChanged {
                    onHeightChanged(it.height)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

                    val systemOffsetBottom = keyboardShownInsetDp.toPx()
                    val visibleHeight = placeable.height * modalPercentVisible
                    val y = ivyContext.screenHeight - visibleHeight - systemOffsetBottom

                    layout(placeable.width, placeable.height) {
                        placeable.place(
                            0,
                            y.roundToInt()
                        )
                    }
                }
                .gradientCutBackgroundTop(
                    pure = UI.colors.pure,
                    density = LocalDensity.current,
                    endY = 16.dp
                )
                .padding(top = 8.dp, bottom = 12.dp)
                .padding(bottom = navBarPadding)
                .zIndex(1100f)
        ) {
            Spacer(Modifier.width(24.dp))

            SecondaryActions?.invoke()

            Spacer(Modifier.weight(1f))

            PrimaryAction()

            Spacer(Modifier.width(24.dp))
        }
    }
}

@Preview
@Composable
private fun PreviewIvyModal_minimal() {
    IvyWalletPreview {
        IvyModal(
            id = UUID.randomUUID(),
            visible = true,
            PrimaryAction = {
                ModalSave {
                }
            },
            dismiss = {}
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = "My first Ivy Modal"
            )

            ModalPreviewActionRowSpacer()
        }
    }
}

@Composable
fun ModalPreviewActionRowSpacer() {
    Spacer(Modifier.height(modalPreviewActionRowHeight()))
}

@Composable
fun modalPreviewActionRowHeight() = 80.dp
