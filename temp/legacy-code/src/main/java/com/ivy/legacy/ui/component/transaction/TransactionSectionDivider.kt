package com.ivy.legacy.ui.component.transaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.springBounce
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.components.IvyIcon

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun SectionDivider(
    expanded: Boolean,
    title: String,
    titleColor: Color,
    baseCurrency: String,
    income: Double,
    expenses: Double,

    showIncomeExpenseRow: Boolean = true,

    setExpanded: (Boolean) -> Unit
) {
    val expansionState = stringResource(if (expanded) R.string.jvy_section_expanded else R.string.jvy_section_collapsed)
    Spacer(Modifier.height(24.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) { setExpanded(!expanded) }
            .semantics { stateDescription = expansionState },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val expandIconRotation by animateFloatAsState(
            targetValue = if (expanded) -180f else 0f,
            animationSpec = springBounce()
        )

        Spacer(Modifier.width(24.dp))

        Column(Modifier.weight(1f)) {
            Text(
                modifier = Modifier.testTag("upcoming_title"),
                text = title,
                style = UI.typo.b1.style(
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor
                )
            )

            if (showIncomeExpenseRow) {
                Spacer(Modifier.height(4.dp))

                SectionDividerIncomeExpenseRow(
                    income = income,
                    expenses = expenses,
                    baseCurrency = baseCurrency
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }

        IvyIcon(
            modifier = Modifier.rotate(expandIconRotation),
            icon = R.drawable.ic_expandarrow
        )

        Spacer(Modifier.width(32.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionDividerIncomeExpenseRow(
    income: Double,
    expenses: Double,
    baseCurrency: String
) {
    val hidden = com.ivy.design.system.LocalFinancialVisibility.current.hidden
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (expenses > 0 || hidden) {
            Text(
                modifier = Modifier.testTag("upcoming_expense"),
                text = "${com.ivy.design.system.financialText("${expenses.format(baseCurrency)} $baseCurrency")} ${stringResource(R.string.expenses_lowercase)}",
                style = com.ivy.design.system.JvyTypography.Supporting.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = UI.colors.pureInverse
                )
            )
        }

        if (income > 0 || hidden) {
            Text(
                modifier = Modifier.testTag("upcoming_income"),
                text = "${com.ivy.design.system.financialText("${income.format(baseCurrency)} $baseCurrency")} ${stringResource(R.string.income_lowercase)}",
                style = com.ivy.design.system.JvyTypography.Supporting.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = UI.colors.green
                )
            )
        }
    }
}

@Preview
@Composable
private fun Preview_Income_Expenses() {
    IvyWalletComponentPreview {
        SectionDivider(
            expanded = true,
            title = "Upcoming",
            titleColor = Orange,
            baseCurrency = "BGN",
            income = 8043.23,
            expenses = 923.87
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_Expenses() {
    IvyWalletComponentPreview {
        SectionDivider(
            expanded = true,
            title = "Overdue",
            titleColor = Red,
            baseCurrency = "BGN",
            income = 0.0,
            expenses = 923.87
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_Income() {
    IvyWalletComponentPreview {
        SectionDivider(
            expanded = true,
            title = "Upcoming",
            titleColor = Orange,
            baseCurrency = "BGN",
            income = 8043.23,
            expenses = 0.0
        ) {
        }
    }
}
