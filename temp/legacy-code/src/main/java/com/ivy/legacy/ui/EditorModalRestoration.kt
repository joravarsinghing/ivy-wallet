package com.ivy.legacy.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.ivy.data.model.Category
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.design.system.*
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.component.tags.TagEditorDraft
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import java.security.MessageDigest
import java.util.UUID

private fun fingerprint(vararg values: Any?): String {
    val digest = MessageDigest.getInstance("SHA-256")
    values.forEach { value ->
        val v = value?.toString()
        digest.update("${v?.length ?: -1}:${v.orEmpty()}".toByteArray(Charsets.UTF_8))
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
private fun Account.signature() = fingerprint(id, name, currency, color, icon, includeInBalance, isDeleted)
private fun Category.signature() = fingerprint(id, name, color, icon)
private fun Tag.signature() = fingerprint(id, name, description, color, icon, creationTimestamp)

/** Descriptors reconnect existing rememberSaveable fields; original records are always reread. */
fun EditorModalHost.saveState(): List<String>? = if (account == null && category == null && tags.draft == null) null else ProcessDraftState.encode(buildMap {
    account?.let { value ->
        put("accountSession", value.id.toString()); put("accountRecord", value.account?.id?.toString())
        put("accountBaseline", value.account?.signature()); put("currency", value.baseCurrency)
        put("balance", value.balance.toString()); put("adjust", value.adjustBalanceMode.toString())
        put("nonzero", value.forceNonZeroBalance.toString()); put("accountFocus", value.autoFocusKeyboard.toString())
    }
    category?.let { value ->
        put("categorySession", value.id.toString()); put("categoryRecord", value.category?.id?.value?.toString())
        put("categoryBaseline", value.category?.signature()); put("categoryFocus", value.autoFocusKeyboard.toString())
    }
    tags.draft?.let { value ->
        put("tagSession", value.id.value.toString()); put("tagRecord", value.original?.id?.value?.toString())
        put("tagBaseline", value.original?.signature())
    }
})

/** False means a stale/missing descriptor was refused; no writes or pending operations resume. */
fun EditorModalHost.restoreState(payload: List<String>, baseCurrency: String, accounts: List<Account>,
    categories: List<Category>, allTags: List<Tag> = emptyList(), balances: Map<UUID, Double> = emptyMap()): Boolean {
    val fields = ProcessDraftState.decode(payload) ?: return false
    var complete = true
    fun recover(block: () -> Unit) { if (runCatching(block).isFailure) complete = false }
    if (account == null && fields["accountSession"] != null) recover {
        val id = requireNotNull(fields.draftUuid("accountSession"))
        val originalId = fields.draftUuid("accountRecord")
        val original = originalId?.let { key -> requireNotNull(accounts.firstOrNull { it.id == key }) }
        require(fields["currency"] == baseCurrency)
        val balance = requireNotNull(fields.draftDouble("balance"))
        if (original != null) {
            require(original.signature() == fields["accountBaseline"])
            // Never relabel an old amount as today's balance or another currency.
            require(balances[original.id] == balance)
        }
        account = AccountModalData(original, baseCurrency, balance, fields.draftBoolean("adjust"),
            fields.draftBoolean("nonzero"), fields.draftBoolean("accountFocus"), id)
    }
    if (category == null && fields["categorySession"] != null) recover {
        val originalId = fields.draftUuid("categoryRecord")
        val original = originalId?.let { id -> requireNotNull(categories.firstOrNull { it.id.value == id }) }
        require(original == null || original.signature() == fields["categoryBaseline"])
        category = CategoryModalData(original, requireNotNull(fields.draftUuid("categorySession")), fields.draftBoolean("categoryFocus"))
    }
    if (tags.draft == null && fields["tagSession"] != null) recover {
        val originalId = fields.draftUuid("tagRecord")
        val original = originalId?.let { id -> requireNotNull(allTags.firstOrNull { it.id.value == id }) }
        require(original == null || original.signature() == fields["tagBaseline"])
        tags.draft = TagEditorDraft(original, TagId(requireNotNull(fields.draftUuid("tagSession"))))
    }
    return complete
}

@Composable
fun rememberEditorModalRestoration(host: EditorModalHost, ready: Boolean, baseCurrency: String,
    accounts: List<Account>, categories: List<Category>, tags: List<Tag> = emptyList(), balances: Map<UUID, Double> = emptyMap()): Boolean {
    val restoration = rememberProcessRestoration("nested-editor-host", host::saveState)
    var resolved by remember { mutableStateOf(restoration.payload == null) }
    var notice by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ready) {
        if (ready && !restoration.consumed) {
            restoration.consumed = true
            restoration.payload?.let { notice = !host.restoreState(it, baseCurrency, accounts, categories, tags, balances) }
            resolved = true
        }
    }
    if (notice) EditorRestorationNotice { notice = false }
    return resolved
}

@Composable
fun EditorRestorationNotice(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.jvy_restore_review_title)) },
        text = { Text(stringResource(R.string.jvy_restore_review_message)) },
        confirmButton = { JvyTextButton(onDismiss) { Text(stringResource(R.string.done)) } })
}

@Composable
fun restoreEditorDescriptor(key: String, ready: Boolean, snapshot: () -> List<String>?, restore: (List<String>) -> Boolean): Boolean {
    val saved = rememberProcessRestoration(key, snapshot)
    var resolved by remember { mutableStateOf(saved.payload == null) }
    var notice by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ready) {
        if (ready && !saved.consumed) {
            saved.consumed = true
            saved.payload?.let { notice = !restore(it) }
            resolved = true
        }
    }
    if (notice) EditorRestorationNotice { notice = false }
    return resolved
}
