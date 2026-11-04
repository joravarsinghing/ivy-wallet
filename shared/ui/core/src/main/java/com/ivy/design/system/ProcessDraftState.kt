package com.ivy.design.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.UUID

/** Bounded primitive form state, deliberately not an object/credential serializer. */
object ProcessDraftState {
    private const val VERSION = "jvy-draft-1"
    fun encode(fields: Map<String, String?>): List<String>? = runCatching {
        require(fields.size <= 96 && fields.keys.all { it.length in 1..64 })
        val result = listOf(VERSION) + fields.toSortedMap().flatMap { (key, value) -> listOf(key, if (value == null) "n" else "s$value") }
        require(result.sumOf(String::length) <= 65536)
        result
    }.getOrNull()

    fun decode(saved: List<String>?): Map<String, String?>? = runCatching {
        requireNotNull(saved)
        require(saved.size in 1..193 && saved.size % 2 == 1 && saved.first() == VERSION)
        require(saved.sumOf(String::length) <= 65536)
        val result = linkedMapOf<String, String?>()
        saved.drop(1).chunked(2).forEach { (key, value) ->
            require(key.length in 1..64 && !result.containsKey(key))
            result[key] = when { value == "n" -> null; value.startsWith("s") -> value.drop(1); else -> error("Invalid primitive") }
        }
        result
    }.getOrNull()
}

/** Payload is restored once; callbacks and in-flight work are never saved. */
class ProcessRestoration internal constructor(val payload: List<String>?) {
    var consumed: Boolean = false
    internal fun snapshotForSave(current: () -> List<String>?): List<String>? =
        (if (!consumed && payload != null) payload else current())?.takeIf { ProcessDraftState.decode(it) != null }
}

@Composable
fun rememberProcessRestoration(key: String, snapshot: () -> List<String>?): ProcessRestoration {
    val currentSnapshot by rememberUpdatedState(snapshot)
    return rememberSaveable(key, saver = Saver(
        save = { state: ProcessRestoration -> state.snapshotForSave(currentSnapshot) },
        restore = { ProcessRestoration(it) },
    )) { ProcessRestoration(null) }
}

fun Map<String, String?>.draftUuid(key: String): UUID? = get(key)?.let { value ->
    UUID.fromString(value).also { require(it.toString() == value.lowercase()) }
}
fun Map<String, String?>.draftDouble(key: String): Double? = get(key)?.toDouble()?.also { require(it.isFinite()) }
fun Map<String, String?>.draftBoolean(key: String): Boolean = getValue(key)?.toBooleanStrict() ?: false
