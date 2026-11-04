package com.ivy.onboarding.steps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.AccountBalance
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingProgressSlider
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.onboarding.components.Suggestions
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.ui.theme.GradientIvy
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.IvyDark
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.GradientCutBottom
import com.ivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.ivy.wallet.ui.theme.components.OnboardingButton
import com.ivy.wallet.ui.theme.dynamicContrast
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.toComposeColor
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1Row

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.OnboardingAccounts(
    baseCurrency: String,

    suggestions: List<CreateAccountData>,
    accounts: List<AccountBalance>,
    suggestionSubmission: com.ivy.design.system.EditorSubmission = remember { com.ivy.design.system.EditorSubmission { com.ivy.wallet.ui.theme.modal.edit.accountSuggestionErrorMessage(it) } },
    editorModals: com.ivy.legacy.ui.EditorModalHost = remember { com.ivy.legacy.ui.EditorModalHost() },

    onCreateAccount: (CreateAccountData, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onAddSuggestedAccount: (CreateAccountData, (Result<Unit>) -> Unit) -> Unit = onCreateAccount,
    onEditAccount: (Account, Double, (Result<Unit>) -> Unit) -> Unit = { _, _, _ -> },

    onSkip: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    val accountModalData = editorModals.account

    val pendingBackId = remember { java.util.UUID.randomUUID() }
    com.ivy.wallet.ui.theme.modal.AddModalBackHandling(pendingBackId, suggestionSubmission.saving) {}
    val nav = navigation()
    com.ivy.design.system.JvySubsectionScaffold(
        title = stringResource(R.string.add_accounts),
        onBack = { nav.onBackPressed() },
        backEnabled = !suggestionSubmission.saving,
        actions = {
            if (accounts.isEmpty()) com.ivy.design.system.JvyTextButton(onSkip, enabled = !suggestionSubmission.saving) {
                Text(stringResource(R.string.skip))
            }
        },
        footer = {
            if (suggestionSubmission.saving) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
            suggestionSubmission.error?.let { error ->
                Text(error, Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error, style = com.ivy.design.system.JvyTypography.Supporting)
            }
            if (accounts.isNotEmpty()) com.ivy.design.system.JvyPrimaryAction(
                stringResource(R.string.next), onDoneClick, enabled = !suggestionSubmission.saving)
        },
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column {
                    Spacer(Modifier.height(8.dp))

                    if (accounts.isEmpty()) {
                        Spacer(Modifier.height(16.dp))

                        Image(
                            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth()
                                .heightIn(max = 160.dp),
                            painter = painterResource(id = R.drawable.onboarding_illustration_accounts),
                            contentDescription = "account illustration"
                        )

                        OnboardingProgressSlider(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            selectedStep = 2,
                            stepsCount = 4,
                            selectedColor = Orange
                        )

                        Spacer(Modifier.height(48.dp))
                    } else {
                        Spacer(Modifier.height(24.dp))
                    }

                    Accounts(
                        enabled = !suggestionSubmission.saving,
                        baseCurrency = baseCurrency,
                        accounts = accounts,
                        onClick = {
                            editorModals.account = AccountModalData(
                                account = it.account,
                                baseCurrency = baseCurrency,
                                balance = it.balance,
                                autoFocusKeyboard = false
                            )
                        }
                    )

                    if (accounts.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                    }

                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.suggestions),
                        style = UI.typo.b1.style(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Suggestions(
                        enabled = !suggestionSubmission.saving,
                        suggestions = suggestions.filter { suggestion ->
                            accounts.map { it.account.name.toLowerCaseLocal() }
                                .contains(suggestion.name.toLowerCaseLocal()).not()
                        },
                        onAddSuggestion = {
                            suggestionSubmission.submit({ complete -> onAddSuggestedAccount(it as CreateAccountData, complete) }, onSuccess = {})
                        },
                        onAddNew = {
                            editorModals.account = AccountModalData(
                                account = null,
                                baseCurrency = baseCurrency,
                                balance = 0.0
                            )
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = onEditAccount,
        dismiss = {
            editorModals.closeAccount(accountModalData?.id)
        }
    )
}

@Composable
private fun Accounts(
    baseCurrency: String,
    accounts: List<AccountBalance>,
    enabled: Boolean = true,
    onClick: (AccountBalance) -> Unit
) {
    for (account in accounts) {
        AccountCard(
            baseCurrency = baseCurrency,
            accountBalance = account,
            enabled = enabled
        ) {
            onClick(account)
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AccountCard(
    baseCurrency: String,
    accountBalance: AccountBalance,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val account = accountBalance.account
    val accountColor = account.color.toComposeColor()
    androidx.compose.material3.OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = com.ivy.design.system.JvyShapes.Card,
    ) {
        Column(Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                ItemIconMDefaultIcon(
                    modifier = Modifier.background(accountColor, CircleShape),
                    iconName = account.icon, defaultIcon = R.drawable.ic_custom_account_m,
                    tint = findContrastTextColor(accountColor),
                )
                Text(account.name, Modifier.weight(1f),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
            }
            val amount = if (accountBalance.balance.isFinite())
                com.ivy.ui.money.formatDecimalMoney(accountBalance.balance.toBigDecimal(), account.currency ?: baseCurrency)
                else "—"
            Text(com.ivy.design.system.financialText(amount),
                style = com.ivy.design.system.JvyTypography.AmountSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Empty() {
    IvyWalletPreview {
        val baseCurrency = "BGN"
        OnboardingAccounts(
            baseCurrency = baseCurrency,
            suggestions = listOf(
                CreateAccountData(
                    name = "Cash",
                    currency = baseCurrency,
                    color = Green,
                    icon = "cash",
                    balance = 0.0
                ),
                CreateAccountData(
                    name = "Bank",
                    currency = baseCurrency,
                    color = Ivy,
                    icon = "bank",
                    balance = 0.0
                ),
                CreateAccountData(
                    name = "Revolut",
                    currency = baseCurrency,
                    color = Color(0xFF4DCAFF),
                    icon = "revolut",
                    balance = 0.0
                ),
            ),
            accounts = listOf()
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Accounts() {
    IvyWalletPreview {
        val baseCurrency = "BGN"
        OnboardingAccounts(
            baseCurrency = baseCurrency,
            suggestions = listOf(
                CreateAccountData(
                    name = "Cash",
                    currency = baseCurrency,
                    color = Green,
                    icon = "cash",
                    balance = 0.0
                ),
                CreateAccountData(
                    name = "Bank",
                    currency = baseCurrency,
                    color = Ivy,
                    icon = "bank",
                    balance = 0.0
                ),
                CreateAccountData(
                    name = "Revolut",
                    currency = baseCurrency,
                    color = Color(0xFF4DCAFF),
                    icon = "revolut",
                    balance = 0.0
                ),
            ),
            accounts = listOf(
                AccountBalance(
                    account = Account(
                        name = "Cash",
                        color = Green.toArgb(),
                        icon = "cash"
                    ),
                    balance = 0.0
                )
            )
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Premium() {
    IvyWalletPreview {
        val baseCurrency = "BGN"
        OnboardingAccounts(
            baseCurrency = baseCurrency,
            suggestions = listOf(
                CreateAccountData(
                    name = "Cash",
                    currency = baseCurrency,
                    color = Green,
                    icon = "cash",
                    balance = 0.0
                ),
                CreateAccountData(
                    name = "Bank",
                    currency = baseCurrency,
                    color = Ivy,
                    icon = "bank",
                    balance = 0.0
                ),
                CreateAccountData(
                    name = "Revolut",
                    currency = baseCurrency,
                    color = Color(0xFF4DCAFF),
                    icon = "revolut",
                    balance = 0.0
                ),
            ),
            accounts = listOf(
                AccountBalance(
                    account = Account(
                        name = "Cash",
                        color = Green.toArgb(),
                        icon = "cash"
                    ),
                    balance = 0.0
                ),
                AccountBalance(
                    account = Account(
                        name = "Revolut",
                        color = IvyDark.toArgb(),
                        icon = "cash"
                    ),
                    balance = 0.0
                ),
                AccountBalance(
                    account = Account(
                        name = "Revolut",
                        color = Color(0xFF4DCAFF).toArgb(),
                        icon = "revolut"
                    ),
                    balance = 0.0
                ),
            )
        )
    }
}
