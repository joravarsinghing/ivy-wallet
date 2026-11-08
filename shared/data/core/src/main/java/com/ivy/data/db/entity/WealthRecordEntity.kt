package com.ivy.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Versioned wealth documents. Policies, valuations, FX, and snapshots are append-only. */
@Entity(tableName = "wealth_records", indices = [Index(value = ["kind"]), Index(value = ["ownerId", "effectiveDate"])])
data class WealthRecordEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val ownerId: String,
    val effectiveDate: String,
    val payload: String,
)
