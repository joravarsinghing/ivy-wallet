package com.ivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ivy.data.db.entity.GoalAllocationEntity
import java.util.UUID

@Dao
interface WriteGoalAllocationDao {
    @Upsert
    suspend fun save(value: GoalAllocationEntity)

    @Upsert
    suspend fun saveMany(values: List<GoalAllocationEntity>)

    @Query("DELETE FROM goal_allocations WHERE goalId = :goalId")
    suspend fun deleteByGoalId(goalId: UUID)

    @Query("DELETE FROM goal_allocations WHERE goalId = :goalId AND accountId = :accountId")
    suspend fun delete(goalId: UUID, accountId: UUID)

    @Query("DELETE FROM goal_allocations")
    suspend fun deleteAll()
}
