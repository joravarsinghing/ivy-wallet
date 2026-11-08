package com.ivy.onboarding.steps

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ivy.design.system.JvyPrimaryAction
import com.ivy.design.system.JvySubsectionScaffold
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.ui.theme.components.CurrencyPicker

@Composable
fun BoxWithConstraintsScope.OnboardingSetCurrency(
    preselectedCurrency: IvyCurrency,
    onSetCurrency: (IvyCurrency) -> Unit
) {
    var currencyCode by rememberSaveable(preselectedCurrency.code) { mutableStateOf(preselectedCurrency.code) }
    val currency = IvyCurrency.fromCode(currencyCode) ?: preselectedCurrency
    val nav = navigation()
    JvySubsectionScaffold(
        title = stringResource(R.string.set_currency),
        onBack = { nav.onBackPressed() },
        footer = { JvyPrimaryAction(stringResource(R.string.set), { onSetCurrency(currency) }) },
    ) {
        CurrencyPicker(
            modifier = Modifier.fillMaxSize(),
            initialSelectedCurrency = currency,
            preselectedCurrency = preselectedCurrency,
            includeKeyboardShownInsetSpacer = false,
        ) { currencyCode = it.code }
    }
}
