package com.ivy.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ivy.design.system.JvyButton
import com.ivy.design.system.JvyLogo
import com.ivy.design.system.JvyTextButton
import com.ivy.design.system.JvyTypography
import com.ivy.legacy.Constants
import com.ivy.legacy.utils.openUrl
import com.ivy.onboarding.OnboardingState
import com.ivy.ui.R

/** Welcome has no artificial Back destination; the router retains the splash/login state. */
@Composable
fun BoxWithConstraintsScope.OnboardingSplashLogin(
    onboardingState: OnboardingState,
    onSkip: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().systemBarsPadding().imePadding(),
            contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    JvyLogo(Modifier.size(88.dp))
                    Text(stringResource(R.string.jvy_wallet), style = JvyTypography.PageTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.your_personal_money_manager), style = JvyTypography.Body,
                        color = MaterialTheme.colorScheme.onBackground)
                    JvyTextButton({ openUrl(uriHandler, Constants.URL_JVY_WALLET_REPO) }) {
                        Text(stringResource(R.string.opensource))
                    }
                    Text(stringResource(R.string.enter_with_offline_account),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.your_data_will_be_saved_only_locally),
                        style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.by_signing_in, stringResource(R.string.terms_conditions),
                        stringResource(R.string.privacy_policy)),
                        style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    JvyTextButton({ openUrl(uriHandler, Constants.URL_TC) }) {
                        Text(stringResource(R.string.terms_conditions))
                    }
                    JvyTextButton({ openUrl(uriHandler, Constants.URL_PRIVACY_POLICY) }) {
                        Text(stringResource(R.string.privacy_policy))
                    }
                }
                JvyButton(onSkip, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    enabled = onboardingState != OnboardingState.SPLASH) {
                    Text(stringResource(R.string.offline_account))
                }
            }
        }
    }
}
