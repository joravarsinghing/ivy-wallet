package com.ivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ivy.data.db.entity.SavedAnalyticsEntity
import java.time.Instant
import java.util.UUID

@Dao
interface WriteSavedAnalyticsDao {
    @Upsert
    suspend fun save(value: SavedAnalyticsEntity)

    @Upsert
    suspend fun saveMany(values: List<SavedAnalyticsEntity>)

    @Query("UPDATE saved_analytics SET isDeleted = 1, isSynced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: UUID, updatedAt: Instant)

    @Query("DELETE FROM saved_analytics")
    suspend fun deleteAll()
}
