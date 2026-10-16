package com.ivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ivy.data.db.entity.GoalEntity
import java.time.Instant
import java.util.UUID

@Dao
interface WriteGoalDao {
    @Upsert
    suspend fun save(value: GoalEntity)

    @Upsert
    suspend fun saveMany(values: List<GoalEntity>)

    @Query("UPDATE goals SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: UUID, isActive: Boolean, updatedAt: Instant)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
