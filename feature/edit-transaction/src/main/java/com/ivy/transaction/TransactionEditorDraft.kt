package com.ivy.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.design.system.ProcessDraftState
import com.ivy.design.system.draftUuid
import java.security.MessageDigest
import java.time.Instant

internal object TransactionEditorDraft {
    fun fields(value: Transaction): Map<String, String?> = mapOf(
        "id" to value.id.toString(), "account" to value.accountId.toString(), "toAccount" to value.toAccountId?.toString(),
        "category" to value.categoryId?.toString(), "type" to value.type.name,
        "amount" to value.amount.stripTrailingZeros().toPlainString(), "toAmount" to value.toAmount.stripTrailingZeros().toPlainString(),
        "title" to value.title, "description" to value.description,
        "date" to value.dateTime?.toEpochMilli()?.toString(), "due" to value.dueDate?.toEpochMilli()?.toString(),
        "paidFor" to value.paidFor?.toEpochMilli()?.toString(),
    )
    fun fingerprint(value: Transaction): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val fields = fields(value) + mapOf("loan" to value.loanId?.toString(), "record" to value.loanRecordId?.toString(),
            "rule" to value.recurringRuleId?.toString(), "attachment" to value.attachmentUrl, "deleted" to value.isDeleted.toString())
        fields.toSortedMap().forEach { (key, v) ->
            digest.update("${key.length}:$key${v?.length ?: -1}:${v.orEmpty()}".toByteArray(Charsets.UTF_8))
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    fun decode(payload: List<String>?): Pair<Transaction, Map<String, String?>>? = runCatching {
        val f = requireNotNull(ProcessDraftState.decode(payload))
        fun instant(key: String) = f[key]?.toLong()?.let(Instant::ofEpochMilli)
        fun amount(key: String) = requireNotNull(f[key]).toBigDecimal().also {
            require(it >= java.math.BigDecimal.ZERO && it.toDouble().isFinite())
        }
        Transaction(id = requireNotNull(f.draftUuid("id")), accountId = requireNotNull(f.draftUuid("account")),
            toAccountId = f.draftUuid("toAccount"), categoryId = f.draftUuid("category"),
            type = TransactionType.valueOf(requireNotNull(f["type"])), amount = amount("amount"), toAmount = amount("toAmount"),
            title = f["title"], description = f["description"], dateTime = instant("date"), dueDate = instant("due"),
            paidFor = instant("paidFor")) to f
    }.getOrNull()
}
