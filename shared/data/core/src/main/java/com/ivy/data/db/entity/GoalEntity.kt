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

@Keep
@Serializable
enum class GoalKind {
    EMERGENCY_FUND,
    PURCHASE,
    SINKING_FUND,
    CUSTOM,
}

@Keep
@Serializable
@Entity(
    tableName = "goals",
    indices = [Index(value = ["isActive"]), Index(value = ["deadline"])]
)
data class GoalEntity(
    @SerialName("name")
    val name: String,
    @SerialName("kind")
    val kind: GoalKind,
    @SerialName("targetAmount")
    val targetAmount: Double,
    @SerialName("deadline")
    @Serializable(with = KSerializerInstant::class)
    val deadline: Instant? = null,
    @SerialName("isActive")
    val isActive: Boolean = true,
    @SerialName("createdAt")
    @Serializable(with = KSerializerInstant::class)
    val createdAt: Instant = Instant.now(),
    @SerialName("updatedAt")
    @Serializable(with = KSerializerInstant::class)
    val updatedAt: Instant = createdAt,
    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID = UUID.randomUUID(),
)
