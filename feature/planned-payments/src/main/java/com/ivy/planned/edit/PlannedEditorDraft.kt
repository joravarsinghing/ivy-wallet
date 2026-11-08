package com.ivy.planned.edit

import com.ivy.base.model.TransactionType
import com.ivy.data.model.IntervalType
import com.ivy.design.system.ProcessDraftState
import com.ivy.design.system.draftBoolean
import com.ivy.design.system.draftDouble
import com.ivy.design.system.draftUuid
import com.ivy.legacy.datamodel.PlannedPaymentRule
import java.security.MessageDigest
import java.time.Instant

/** Only editable primitives and a baseline digest; never jobs, callbacks or database graphs. */
internal object PlannedEditorDraft {
    fun fields(rule: PlannedPaymentRule): Map<String, String?> = mapOf(
        "id" to rule.id.toString(), "account" to rule.accountId.toString(), "category" to rule.categoryId?.toString(),
        "type" to rule.type.name, "amount" to rule.amount.toString(), "title" to rule.title, "description" to rule.description,
        "start" to rule.startDate?.toEpochMilli()?.toString(), "oneTime" to rule.oneTime.toString(),
        "interval" to rule.intervalN?.toString(), "intervalType" to rule.intervalType?.name,
    )
    fun fingerprint(rule: PlannedPaymentRule): String {
        val digest = MessageDigest.getInstance("SHA-256")
        // Explicit lengths distinguish separators, null, and empty text without an object serializer.
        fields(rule).toSortedMap().forEach { (key, value) ->
            digest.update("${key.length}:$key${value?.length ?: -1}:${value.orEmpty()}".toByteArray(Charsets.UTF_8))
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    fun decode(payload: List<String>?): Pair<PlannedPaymentRule, Map<String, String?>>? = runCatching {
        val f = requireNotNull(ProcessDraftState.decode(payload))
        val rule = PlannedPaymentRule(
            id = requireNotNull(f.draftUuid("id")), accountId = requireNotNull(f.draftUuid("account")),
            categoryId = f.draftUuid("category"), type = TransactionType.valueOf(requireNotNull(f["type"])),
            amount = requireNotNull(f.draftDouble("amount")).also { require(it >= 0) },
            title = f["title"], description = f["description"],
            startDate = f["start"]?.toLong()?.let(Instant::ofEpochMilli), oneTime = f.draftBoolean("oneTime"),
            intervalN = f["interval"]?.toInt(), intervalType = f["intervalType"]?.let(IntervalType::valueOf),
        )
        rule to f
    }.getOrNull()
}
