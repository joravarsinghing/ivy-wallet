package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.ivy.design.system.JvyOutlinedButton
import com.ivy.design.system.JvySpacing
import com.ivy.design.system.JvyTypography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import com.ivy.data.model.IntervalType
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.addKeyboardListener
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.closeDay
import com.ivy.legacy.utils.formatDateWeekDayLong
import com.ivy.legacy.utils.formatNicely
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.onScreenStart
import com.ivy.design.utils.thenIf
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientIvy
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.IntervalPickerRow
import com.ivy.wallet.ui.theme.components.IvyCircleButton
import com.ivy.wallet.ui.theme.components.IvyDividerLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
data class RecurringRuleModalData(
    val initialStartDate: LocalDateTime?,
    val initialIntervalN: Int?,
    val initialIntervalType: IntervalType?,
    val initialOneTime: Boolean = false,
    val id: UUID = UUID.randomUUID()
)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.RecurringRuleModal(
    modal: RecurringRuleModalData?,

    dismiss: () -> Unit,
    onRuleChanged: (LocalDateTime, oneTime: Boolean, Int?, IntervalType?) -> Unit,
) {
    val timeProvider = LocalTimeProvider.current
    val initialDate = rememberSaveable(modal?.id?.toString()) {
        (modal?.initialStartDate ?: timeProvider.localNow()).toString()
    }
    var startDate by rememberSaveable(modal?.id?.toString(), stateSaver = Saver<LocalDateTime, String>(
        save = { it.toString() }, restore = { LocalDateTime.parse(it) })) {
        mutableStateOf(LocalDateTime.parse(initialDate))
    }
    var oneTime by rememberSaveable(modal?.id?.toString()) {
        mutableStateOf(modal?.initialOneTime ?: false)
    }
    var intervalN by rememberSaveable(modal?.id?.toString()) {
        mutableStateOf(modal?.initialIntervalN ?: 1)
    }
    var intervalType by rememberSaveable(modal?.id?.toString()) {
        mutableStateOf(modal?.initialIntervalType ?: IntervalType.MONTH)
    }

    val modalScrollState = rememberScrollState()

    IvyModal(
        id = modal?.id,
        visible = modal != null,
        dismiss = dismiss,
        hasUnsavedChanges = startDate.toString() != initialDate || oneTime != (modal?.initialOneTime ?: false) ||
            intervalN != (modal?.initialIntervalN ?: 1) || intervalType != (modal?.initialIntervalType ?: IntervalType.MONTH),
        scrollState = modalScrollState,
        title = stringResource(R.string.plan_for),
        PrimaryAction = {
            ModalSet(
                modifier = Modifier.testTag("recurringModalSet"),
                enabled = validate(oneTime, intervalN, intervalType)
            ) {
                dismiss()
                onRuleChanged(
                    startDate,
                    oneTime,
                    intervalN,
                    intervalType
                )
            }
        }
    ) {
        val rootView = LocalView.current
        onScreenStart {
            hideKeyboard(rootView)
        }

        // One-time & Multiple Times
        TimesSelector(oneTime = oneTime) {
            oneTime = it
        }

        if (oneTime) {
            OneTime(
                date = startDate,
                onDatePicked = {
                    startDate = it
                }
            )
        } else {
            MultipleTimes(
                startDate = startDate,
                intervalN = intervalN,
                intervalType = intervalType,

                modalScrollState = modalScrollState,

                onSetStartDate = {
                    startDate = it
                },
                onSetIntervalN = {
                    intervalN = it
                },
                onSetIntervalType = {
                    intervalType = it
                }
            )
        }
    }
}

private fun validate(
    oneTime: Boolean,
    intervalN: Int?,
    intervalType: IntervalType?
): Boolean {
    return oneTime || intervalN != null && intervalN > 0 && intervalType != null
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TimesSelector(
    oneTime: Boolean,

    onSetOneTime: (Boolean) -> Unit
) {
    FlowRow(
        modifier = Modifier.padding(horizontal = JvySpacing.PageGutter).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JvySpacing.Related),
        verticalArrangement = Arrangement.spacedBy(JvySpacing.Related),
    ) {
        TimesSelectorButton(
            selected = oneTime,
            label = stringResource(R.string.one_time)
        ) {
            onSetOneTime(true)
        }

        TimesSelectorButton(
            selected = !oneTime,
            label = stringResource(R.string.multiple_times)
        ) {
            onSetOneTime(false)
        }

    }
}

@Composable
private fun TimesSelectorButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    JvyOutlinedButton(onClick, Modifier.semantics { this.selected = selected },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)) {
        Text(label, style = JvyTypography.Body)
    }
}

@Composable
@Suppress("ParameterNaming")
private fun OneTime(
    date: LocalDateTime,
    onDatePicked: (LocalDateTime) -> Unit
) {
    Spacer(Modifier.height(JvySpacing.Group))

    DateRow(dateTime = date) {
        onDatePicked(it)
    }

}

@Composable
private fun MultipleTimes(
    startDate: LocalDateTime,
    intervalN: Int,
    intervalType: IntervalType,

    modalScrollState: ScrollState,

    onSetStartDate: (LocalDateTime) -> Unit,
    onSetIntervalN: (Int) -> Unit,
    onSetIntervalType: (IntervalType) -> Unit
) {
    Spacer(Modifier.height(JvySpacing.Group))

    Text(
        modifier = Modifier
            .padding(start = JvySpacing.PageGutter),
        text = stringResource(R.string.starts_on),
        style = UI.typo.b2.style(
            color = UI.colors.pureInverse,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(JvySpacing.Related))

    DateRow(dateTime = startDate) {
        onSetStartDate(it)
    }

    Spacer(Modifier.height(JvySpacing.Group))

    IvyDividerLine(
        modifier = Modifier.padding(horizontal = 24.dp)
    )

    Spacer(Modifier.height(JvySpacing.Group))

    Text(
        modifier = Modifier
            .padding(start = JvySpacing.PageGutter),
        text = stringResource(R.string.repeats_every_text),
        style = UI.typo.b2.style(
            fontWeight = FontWeight.ExtraBold,
            color = UI.colors.pureInverse
        )
    )

    Spacer(Modifier.height(JvySpacing.Related))

    val rootView = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    onScreenStart {
        rootView.addKeyboardListener { keyboardShown ->
            if (keyboardShown) {
                coroutineScope.launch {
                    delay(200)
                    modalScrollState.animateScrollTo(modalScrollState.maxValue)
                }
            }
        }
    }

    IntervalPickerRow(
        intervalN = intervalN,
        intervalType = intervalType,
        onSetIntervalN = onSetIntervalN,
        onSetIntervalType = onSetIntervalType
    )

}

@Composable
@Suppress("ParameterNaming")
private fun DateRow(
    dateTime: LocalDateTime,
    onDatePicked: (LocalDateTime) -> Unit
) {
    val ivyContext = ivyWalletCtx()
    JvyOutlinedButton({ ivyContext.pickDate(dateTime.toLocalDate(), onDatePicked) },
        Modifier.fillMaxWidth().padding(horizontal = JvySpacing.PageGutter).testTag("recurring_modal_pick_date")) {
        Column(Modifier.weight(1f)) {
            val date = dateTime.toLocalDate()
            val closeDay = date.closeDay()

            Text(
                text = closeDay ?: date.formatNicely(
                    pattern = "EEEE, dd MMM"
                ),
                style = JvyTypography.Body,
            )

            if (closeDay != null) {
                Spacer(Modifier.height(4.dp))

                Text(
                    text = date.formatDateWeekDayLong(),
                    style = JvyTypography.Supporting,
                )
            }
        }

        Spacer(Modifier.width(JvySpacing.Related))
        androidx.compose.material3.Icon(Icons.Outlined.DateRange, null,
            Modifier.size(24.dp))
    }
}

private fun IvyWalletCtx.pickDate(
    initialDate: LocalDate,
    onDatePicked: (
        LocalDateTime
    ) -> Unit
) {
    datePicker(
        initialDate = initialDate
    ) {
        onDatePicked(it.atTime(12, 0))
    }
}

@Preview
@Composable
private fun Preview_oneTime() {
    IvyWalletPreview {
        BoxWithConstraints(Modifier.padding(bottom = 48.dp)) {
            RecurringRuleModal(
                modal = RecurringRuleModalData(
                    initialStartDate = null,
                    initialIntervalN = null,
                    initialIntervalType = null,
                    initialOneTime = true
                ),
                dismiss = {},
                onRuleChanged = { _, _, _, _ -> }
            )
        }
    }
}

@Preview
@Composable
private fun Preview_multipleTimes() {
    IvyWalletPreview {
        BoxWithConstraints(Modifier.padding(bottom = 48.dp)) {
            RecurringRuleModal(
                modal = RecurringRuleModalData(
                    initialStartDate = null,
                    initialIntervalN = null,
                    initialIntervalType = null,
                    initialOneTime = false
                ),
                dismiss = {},
                onRuleChanged = { _, _, _, _ -> }
            )
        }
    }
}
