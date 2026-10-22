package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ivy.base.kotlinxserilzation.KSerializerInstant
import com.ivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * A user-authored analytics definition. It stores the query definition, never calculated
 * financial values, so reports always reflect the current ledger when opened.
 */
@Keep
@Serializable
@Entity(
    tableName = "saved_analytics",
    indices = [
        Index(value = ["kind"]),
        Index(value = ["isDeleted"]),
        Index(value = ["updatedAt"]),
    ]
)
data class SavedAnalyticsEntity(
    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID = UUID.randomUUID(),

    @SerialName("name")
    val name: String,

    /** FILTER or REPORT. Kept as a string for forward-compatible backup decoding. */
    @SerialName("kind")
    val kind: String,

    /** Versioned JSON owned by the reports feature. */
    @SerialName("definition")
    val definition: String,

    @SerialName("createdAt")
    @Serializable(with = KSerializerInstant::class)
    val createdAt: Instant = Instant.now(),

    @SerialName("updatedAt")
    @Serializable(with = KSerializerInstant::class)
    val updatedAt: Instant = createdAt,

    @Deprecated("Compatibility field used by the legacy sync shape")
    @SerialName("isSynced")
    val isSynced: Boolean = false,

    @Deprecated("Compatibility field used by the legacy sync shape")
    @SerialName("isDeleted")
    val isDeleted: Boolean = false,
) {
    companion object {
        const val KIND_FILTER = "FILTER"
        const val KIND_REPORT = "REPORT"
    }
}
