package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.data.db.entity.SavedAnalyticsEntity
import java.util.UUID

@Dao
interface SavedAnalyticsDao {
    @Query("SELECT * FROM saved_analytics WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun findAll(): List<SavedAnalyticsEntity>

    @Query("SELECT * FROM saved_analytics WHERE id = :id AND isDeleted = 0")
    suspend fun findById(id: UUID): SavedAnalyticsEntity?

    @Query("SELECT * FROM saved_analytics WHERE isSynced = :synced AND isDeleted = :deleted")
    suspend fun findByIsSyncedAndIsDeleted(
        synced: Boolean,
        deleted: Boolean = false,
    ): List<SavedAnalyticsEntity>
}
