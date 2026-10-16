package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.data.db.entity.GoalAllocationEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface GoalAllocationDao {
    @Query("SELECT * FROM goal_allocations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<GoalAllocationEntity>>

    @Query("SELECT * FROM goal_allocations WHERE goalId = :goalId ORDER BY updatedAt DESC")
    fun observeByGoalId(goalId: UUID): Flow<List<GoalAllocationEntity>>

    @Query(
        "SELECT goal_allocations.* FROM goal_allocations " +
            "INNER JOIN goals ON goals.id = goal_allocations.goalId " +
            "WHERE goal_allocations.accountId = :accountId AND goals.isActive = 1 " +
            "ORDER BY goal_allocations.updatedAt DESC"
    )
    fun observeActiveByAccountId(accountId: UUID): Flow<List<GoalAllocationEntity>>

    @Query("SELECT * FROM goal_allocations WHERE goalId = :goalId ORDER BY updatedAt DESC")
    suspend fun findByGoalId(goalId: UUID): List<GoalAllocationEntity>

    @Query("SELECT * FROM goal_allocations WHERE accountId = :accountId ORDER BY updatedAt DESC")
    suspend fun findByAccountId(accountId: UUID): List<GoalAllocationEntity>

    @Query(
        "SELECT goal_allocations.* FROM goal_allocations " +
            "INNER JOIN goals ON goals.id = goal_allocations.goalId " +
            "WHERE goal_allocations.accountId = :accountId AND goals.isActive = 1 " +
            "ORDER BY goal_allocations.updatedAt DESC"
    )
    suspend fun findActiveByAccountId(accountId: UUID): List<GoalAllocationEntity>
}
