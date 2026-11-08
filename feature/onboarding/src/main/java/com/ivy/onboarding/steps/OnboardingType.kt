package com.ivy.onboarding.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.design.system.JvyOutlinedButton
import com.ivy.design.system.JvyPrimaryAction
import com.ivy.design.system.JvySubsectionScaffold
import com.ivy.design.system.JvyTypography
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingProgressSlider
import com.ivy.ui.R

@Composable
fun OnboardingType(
    onStartImport: () -> Unit,
    onStartFresh: () -> Unit,
) {
    val nav = navigation()
    JvySubsectionScaffold(
        title = stringResource(R.string.import_csv_file),
        onBack = { nav.onBackPressed() },
        footer = { JvyPrimaryAction(stringResource(R.string.start_fresh), onStartFresh) },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(stringResource(R.string.from_ivy_or_another_app),
                style = JvyTypography.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Image(painterResource(R.drawable.onboarding_illustration_import), null,
                Modifier.fillMaxWidth().heightIn(max = 160.dp))
            OnboardingProgressSlider(Modifier.align(Alignment.CenterHorizontally),
                selectedStep = 0, stepsCount = 4, selectedColor = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.importing_another_time_warning),
                style = JvyTypography.Body, color = MaterialTheme.colorScheme.onBackground)
            JvyOutlinedButton(onStartImport, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.import_backup_file))
            }
        }
    }
}
