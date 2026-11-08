package com.ivy.reports

import com.ivy.data.model.Category
import com.ivy.data.model.Tag
import com.ivy.design.system.ProcessDraftState
import com.ivy.design.system.draftUuid
import com.ivy.legacy.datamodel.Account
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Query primitives only: IDs, periods, limits and keywords, never transaction/account graphs. */
internal class ReportFilterDraft(initial: ReportFilter?) {
    val value = androidx.compose.runtime.mutableStateOf(initial)
}

internal object ReportFilterState {
    fun isCleared(payload: List<String>?) = ProcessDraftState.decode(payload)?.get("cleared") == "true"
    fun save(filter: ReportFilter?): List<String>? = if (filter == null) ProcessDraftState.encode(mapOf("cleared" to "true"))
        else ProcessDraftState.encode(mapOf("id" to filter.id.toString(), "query" to Json.encodeToString(filter.toSavedDefinition())))
    fun restore(payload: List<String>?, accounts: List<Account>, categories: List<Category>, currency: String,
        tags: List<Tag>? = null, allowInvalidDraft: Boolean = false): ReportFilter? = runCatching {
        val f = requireNotNull(ProcessDraftState.decode(payload))
        requireNotNull(Json.decodeFromString<SavedAnalyticsDefinition>(requireNotNull(f["query"]))
            .toReportFilter(accounts, categories, currency, tags, validateBounds = !allowInvalidDraft)).copy(id = requireNotNull(f.draftUuid("id")))
    }.getOrNull()
}
