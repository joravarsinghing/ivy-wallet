package com.ivy.wallet.ui.theme.modal

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.ivy.design.system.JvyOutlinedButton
import com.ivy.design.system.JvySpacing
import com.ivy.design.system.JvyTypography
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.Month.Companion.monthsList
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.onScreenStart
import com.ivy.ui.R
import java.time.LocalDate
import java.util.UUID

@SuppressLint("ComposeModifierMissing")
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.MonthPickerModal(
    initialDate: LocalDate,
    visible: Boolean,
    dismiss: () -> Unit,
    onMonthSelected: (Int) -> Unit,
    id: UUID = UUID.fromString(rememberSaveable { UUID.randomUUID().toString() }),
) {
    if (!visible) return
    var selectedMonth by rememberSaveable(id.toString(), initialDate.year, initialDate.monthValue) {
        mutableStateOf(initialDate.monthValue)
    }

    IvyModal(
        id = id,
        visible = visible,
        title = stringResource(R.string.choose_month),
        dismiss = dismiss,
        PrimaryAction = {
            ModalSave {
                onMonthSelected(selectedMonth)
                dismiss()
            }
        }
    ) {
        val view = LocalView.current
        onScreenStart {
            hideKeyboard(view)
        }

        MonthPicker(
            selectedMonth = selectedMonth,
            onMonthSelected = {
                selectedMonth = it
            }
        )

    }
}

@Composable
@Suppress("ParameterNaming")
private fun MonthPicker(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit
) {
    val months = monthsList()

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = JvySpacing.PageGutter)) {
        val columns = ((maxWidth + JvySpacing.Related) /
            (136.dp * LocalDensity.current.fontScale + JvySpacing.Related)).toInt().coerceIn(1, 3)
        Column(verticalArrangement = Arrangement.spacedBy(JvySpacing.Related)) {
            months.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JvySpacing.Related)) {
                    row.forEach { month ->
                        MonthButton(month, month.monthValue == selectedMonth, Modifier.weight(1f)) { onMonthSelected(month.monthValue) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthButton(
    month: Month,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    JvyOutlinedButton(onClick, modifier.semantics { this.selected = selected },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)) {
        Text(month.name, style = JvyTypography.Body)
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        MonthPickerModal(
            initialDate = dateNowUTC(),
            visible = true,
            dismiss = {},
            onMonthSelected = {}
        )
    }
}
