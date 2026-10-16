package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY deadline ASC, createdAt ASC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY isActive DESC, deadline ASC, createdAt ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun findById(id: UUID): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeById(id: UUID): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY deadline ASC, createdAt ASC")
    suspend fun findActive(): List<GoalEntity>
}
