package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A local-only category suggestion rule. It is intentionally not syncable. */
@Keep
@Serializable
@Entity(
    tableName = "category_rules",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["isActive"]),
    ]
)
data class CategoryRuleEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val pattern: String,
    val field: String,
    val matchType: String,
    val priority: Int,
    val isActive: Boolean,
)
