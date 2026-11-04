package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.JvyShapes
import com.ivy.design.system.JvySpacing
import com.ivy.design.system.JvyTypography
import com.ivy.data.model.IntervalType
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.forDisplay
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientIvy
import com.ivy.wallet.ui.theme.White

private const val RepeatIntervalCharLimit = 5

/** Invalid draft input must invalidate its parent, not reuse the preceding count. */
internal fun intervalCountValue(text: String): Int = text.toIntOrNull()?.takeIf { it > 0 } ?: 0

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IntervalPickerRow(
    intervalN: Int,
    intervalType: IntervalType,

    onSetIntervalN: (Int) -> Unit,
    onSetIntervalType: (IntervalType) -> Unit
) {
    var interNTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(selectEndTextFieldValue(intervalN.toString()))
    }
    LaunchedEffect(intervalN) {
        if (intervalCountValue(interNTextFieldValue.text) != intervalN)
            interNTextFieldValue = selectEndTextFieldValue(intervalN.toString())
    }
    val validInput = intervalN > 0 && intervalCountValue(interNTextFieldValue.text) == intervalN
    val number: @Composable (Modifier) -> Unit = { fieldModifier ->
      Column(fieldModifier, verticalArrangement = Arrangement.spacedBy(JvySpacing.Related)) {
        IvyNumberTextField(
            modifier = Modifier.fillMaxWidth().heightIn(min = JvySpacing.TouchTarget)
                .background(MaterialTheme.colorScheme.surface, JvyShapes.Control)
                .border(1.dp, if (validInput) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error, JvyShapes.Control)
                .padding(horizontal = JvySpacing.Group, vertical = JvySpacing.ActionInset),
            value = interNTextFieldValue,
            textColor = MaterialTheme.colorScheme.onSurface,
            hint = "0"
        ) {
            val filteredText = it.text.take(RepeatIntervalCharLimit)
            val changed = it.text != interNTextFieldValue.text
            interNTextFieldValue = it.copy(text = filteredText)
            if (changed) onSetIntervalN(intervalCountValue(filteredText))
        }
        if (!validInput) Text(stringResource(R.string.jvy_interval_count_error),
            Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = JvyTypography.Supporting,
            color = MaterialTheme.colorScheme.error)
      }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = JvySpacing.PageGutter)) {
        if (maxWidth < 360.dp || LocalDensity.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(JvySpacing.Related)) {
                number(Modifier.fillMaxWidth())
                IntervalTypeSelector(intervalN, intervalType, Modifier.fillMaxWidth(), onSetIntervalType)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(JvySpacing.Related), verticalAlignment = Alignment.CenterVertically) {
                number(Modifier.weight(1f))
                IntervalTypeSelector(intervalN, intervalType, Modifier.weight(3f), onSetIntervalType)
            }
        }
    }
}

@Composable
private fun IntervalTypeSelector(
    intervalN: Int,
    intervalType: IntervalType,
    modifier: Modifier = Modifier,
    onSetIntervalType: (IntervalType) -> Unit
) {
    Row(
        modifier = modifier.heightIn(min = JvySpacing.TouchTarget)
            .border(1.dp, MaterialTheme.colorScheme.outline, JvyShapes.Control),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(JvySpacing.Related))

        IvyIcon(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable {
                    onSetIntervalType(
                        when (intervalType) {
                            IntervalType.DAY -> IntervalType.YEAR
                            IntervalType.WEEK -> IntervalType.DAY
                            IntervalType.MONTH -> IntervalType.WEEK
                            IntervalType.YEAR -> IntervalType.MONTH
                        }
                    )
                }
                .padding(all = 8.dp)
                .rotate(-180f),
            icon = R.drawable.ic_arrow_right,
            contentDescription = "interval_type_arrow_left"
        )

        Text(
            modifier = Modifier.weight(1f),
            text = intervalType.forDisplay(intervalN).capitalizeLocal(),
            style = UI.typo.b2.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        )

        IvyIcon(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable {
                    onSetIntervalType(
                        when (intervalType) {
                            IntervalType.DAY -> IntervalType.WEEK
                            IntervalType.WEEK -> IntervalType.MONTH
                            IntervalType.MONTH -> IntervalType.YEAR
                            IntervalType.YEAR -> IntervalType.DAY
                        }
                    )
                }
                .padding(all = 8.dp),
            icon = R.drawable.ic_arrow_right,
            contentDescription = "interval_type_arrow_right"
        )

        Spacer(Modifier.width(JvySpacing.Related))
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IntervalPickerRow(
            intervalN = 1,
            intervalType = IntervalType.WEEK,
            onSetIntervalN = {}
        ) {
        }
    }
}
