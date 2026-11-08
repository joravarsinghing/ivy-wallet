package com.ivy.data.repository

import com.ivy.data.db.dao.read.SavedAnalyticsDao
import com.ivy.data.db.dao.write.WriteSavedAnalyticsDao
import com.ivy.data.db.entity.SavedAnalyticsEntity
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** Persistence boundary for saved filters and reports. */
class SavedAnalyticsRepository @Inject constructor(
    private val reader: SavedAnalyticsDao,
    private val writer: WriteSavedAnalyticsDao,
) {
    suspend fun findAll(): List<SavedAnalyticsEntity> = reader.findAll()

    suspend fun findById(id: UUID): SavedAnalyticsEntity? = reader.findById(id)

    suspend fun save(value: SavedAnalyticsEntity) {
        writer.save(value.copy(updatedAt = Instant.now(), isSynced = false))
    }

    suspend fun saveMany(values: List<SavedAnalyticsEntity>) {
        writer.saveMany(values.map { it.copy(isSynced = false) })
    }

    suspend fun delete(id: UUID) {
        writer.markDeleted(id = id, updatedAt = Instant.now())
    }

    suspend fun findPendingSync(): List<SavedAnalyticsEntity> =
        reader.findByIsSyncedAndIsDeleted(synced = false)
}
