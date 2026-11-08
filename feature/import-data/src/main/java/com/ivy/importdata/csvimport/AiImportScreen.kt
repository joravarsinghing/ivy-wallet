package com.ivy.importdata.csvimport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ivy.data.backup.AiImportContract
import com.ivy.data.backup.AiImportPreview
import com.ivy.design.system.*
import com.ivy.importdata.R

@Composable
internal fun AiImportEntryCard(onOpen: () -> Unit) {
    OutlinedCard(Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(), shape = JvyShapes.Card) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.ai_import_entry_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.ai_import_entry_body), style = JvyTypography.Supporting)
            JvyOutlinedButton(onOpen, Modifier.fillMaxWidth()) { Text(stringResource(R.string.ai_import_open)) }
        }
    }
}

/** No personal data is copied: the guide/schema are built from public serializer descriptors. */
@Composable
internal fun AiImportScreenContent(
    preview: AiImportPreview?, error: String?, onboarding: Boolean,
    onBack: () -> Unit, onChooseFile: () -> Unit, onConfirm: () -> Unit,
    backModifier: Modifier = Modifier,
) {
    var schemaVisible by rememberSaveable { mutableStateOf(false) }
    var copied by rememberSaveable { mutableStateOf(false) }
    var confirmed by rememberSaveable(preview) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
            JvyScreenHeader(stringResource(if (preview == null) R.string.ai_import_title else R.string.ai_import_review_title), onBack, backModifier = backModifier)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                error?.let { message -> item(key = "error") {
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.ai_import_error_title), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                        Text(message, style = JvyTypography.Supporting, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    } }
                } }
                if (preview == null) item(key = "privacy") {
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.ai_import_privacy_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.ai_import_privacy_body), style = JvyTypography.Supporting)
                    } }
                }
                if (preview == null) {
                    item(key = "copy-guide") {
                        Text(stringResource(R.string.ai_import_step_one), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.ai_import_step_one_body), style = JvyTypography.Supporting)
                        Spacer(Modifier.height(12.dp))
                        JvyButton(onClick = { clipboard.setText(AnnotatedString(AiImportContract.guide)); copied = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.ai_import_copy_guide))
                        }
                        if (copied) Text(stringResource(R.string.ai_import_copied), style = JvyTypography.Supporting,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    item(key = "convert") {
                        Text(stringResource(R.string.ai_import_step_two), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.ai_import_step_two_body), style = JvyTypography.Supporting)
                    }
                    item(key = "choose") {
                        Text(stringResource(R.string.ai_import_step_three), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.ai_import_step_three_body), style = JvyTypography.Supporting)
                    }
                    item(key = "schema-controls") {
                        JvyOutlinedButton(onClick = { schemaVisible = !schemaVisible }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (schemaVisible) R.string.ai_import_hide_schema else R.string.ai_import_show_schema))
                        }
                        JvyTextButton(onClick = { clipboard.setText(AnnotatedString(AiImportContract.schema)); copied = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.ai_import_copy_schema))
                        }
                    }
                    if (schemaVisible) item(key = "schema") {
                        Surface(shape = JvyShapes.Control, color = MaterialTheme.colorScheme.surfaceVariant) {
                            SelectionContainer { Text(AiImportContract.schema, Modifier.padding(16.dp).fillMaxWidth(), style = JvyTypography.Supporting) }
                        }
                    }
                } else {
                    item(key = "counts") {
                        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.ai_import_counts), style = MaterialTheme.typography.titleMedium)
                            preview.counts.filterValues { it > 0 }.forEach { (name, count) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(name, Modifier.weight(1f), style = JvyTypography.Body)
                                    Text(count.toString(), style = JvyTypography.AmountSmall)
                                }
                            }
                            Text(stringResource(R.string.ai_import_currencies, preview.data.accounts.mapNotNull { it.currency }.distinct().sorted().joinToString(", ")), style = JvyTypography.Supporting)
                        } }
                    }
                    item(key = "review-warning") {
                        Text(stringResource(R.string.ai_import_review_body), style = JvyTypography.Body)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(if (onboarding) R.string.ai_import_currency_onboarding else R.string.ai_import_currency_existing), style = JvyTypography.Supporting)
                    }
                    item(key = "confirmation") {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(confirmed, role = Role.Checkbox, onValueChange = { confirmed = it }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(confirmed, onCheckedChange = null)
                            Text(stringResource(R.string.ai_import_confirmation), Modifier.padding(start = 12.dp).weight(1f), style = JvyTypography.Body)
                        }
                    }
                }
            }
            HorizontalDivider()
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                if (preview == null) JvyOutlinedButton(onChooseFile, Modifier.fillMaxWidth()) { Text(stringResource(R.string.ai_import_choose_json)) }
                else JvyButton(onConfirm, Modifier.fillMaxWidth(), enabled = confirmed) { Text(stringResource(R.string.ai_import_confirm_import)) }
            }
        }
    }
}
