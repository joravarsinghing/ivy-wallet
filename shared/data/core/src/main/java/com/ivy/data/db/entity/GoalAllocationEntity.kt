package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import com.ivy.base.kotlinxserilzation.KSerializerInstant
import com.ivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Keep
@Serializable
@Entity(
    tableName = "goal_allocations",
    primaryKeys = ["goalId", "accountId"],
    indices = [Index(value = ["accountId"]), Index(value = ["goalId"])]
)
data class GoalAllocationEntity(
    @SerialName("goalId")
    @Serializable(with = KSerializerUUID::class)
    val goalId: UUID,
    @SerialName("accountId")
    @Serializable(with = KSerializerUUID::class)
    val accountId: UUID,
    @SerialName("reservedAmount")
    val reservedAmount: Double,
    @SerialName("createdAt")
    @Serializable(with = KSerializerInstant::class)
    val createdAt: Instant = Instant.now(),
    @SerialName("updatedAt")
    @Serializable(with = KSerializerInstant::class)
    val updatedAt: Instant = createdAt,
)
