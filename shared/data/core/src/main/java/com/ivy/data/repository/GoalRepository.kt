package com.ivy.data.repository

import androidx.room.withTransaction
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.db.dao.read.GoalAllocationDao
import com.ivy.data.db.dao.read.GoalDao
import com.ivy.data.db.dao.write.WriteGoalAllocationDao
import com.ivy.data.db.dao.write.WriteGoalDao
import com.ivy.data.db.entity.GoalAllocationEntity
import com.ivy.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence boundary for goals and account reservations.
 *
 * Allocation replacement is intentionally transactional so a goal never exposes
 * a partially updated set of account reservations to observers.
 */
@Singleton
class GoalRepository @Inject constructor(
    private val database: IvyRoomDatabase,
    private val goalDao: GoalDao,
    private val goalAllocationDao: GoalAllocationDao,
    private val writeGoalDao: WriteGoalDao,
    private val writeGoalAllocationDao: WriteGoalAllocationDao,
) {
    fun observeActiveGoals(): Flow<List<GoalEntity>> = goalDao.observeActive()

    fun observeAllGoals(): Flow<List<GoalEntity>> = goalDao.observeAll()

    fun observeGoal(id: UUID): Flow<GoalEntity?> = goalDao.observeById(id)

    fun observeAllocations(goalId: UUID): Flow<List<GoalAllocationEntity>> =
        goalAllocationDao.observeByGoalId(goalId)

    fun observeActiveAllocations(accountId: UUID): Flow<List<GoalAllocationEntity>> =
        goalAllocationDao.observeActiveByAccountId(accountId)

    suspend fun findGoal(id: UUID): GoalEntity? = goalDao.findById(id)

    suspend fun findActiveGoals(): List<GoalEntity> = goalDao.findActive()

    suspend fun findAllocations(goalId: UUID): List<GoalAllocationEntity> =
        goalAllocationDao.findByGoalId(goalId)

    suspend fun findActiveAllocations(accountId: UUID): List<GoalAllocationEntity> =
        goalAllocationDao.findActiveByAccountId(accountId)

    suspend fun saveGoalWithAllocations(
        goal: GoalEntity,
        allocations: List<GoalAllocationEntity>,
    ) {
        require(allocations.all { it.goalId == goal.id }) {
            "Every allocation must belong to the goal being saved"
        }
        require(allocations.all { it.reservedAmount >= 0.0 }) {
            "Reserved amounts cannot be negative"
        }

        database.withTransaction {
            writeGoalDao.save(goal)
            writeGoalAllocationDao.deleteByGoalId(goal.id)
            if (allocations.isNotEmpty()) {
                writeGoalAllocationDao.saveMany(allocations)
            }
        }
    }

    suspend fun replaceAllocations(
        goalId: UUID,
        allocations: List<GoalAllocationEntity>,
    ) {
        require(allocations.all { it.goalId == goalId }) {
            "Every allocation must belong to the goal being updated"
        }
        require(allocations.all { it.reservedAmount >= 0.0 }) {
            "Reserved amounts cannot be negative"
        }

        database.withTransaction {
            writeGoalAllocationDao.deleteByGoalId(goalId)
            if (allocations.isNotEmpty()) {
                writeGoalAllocationDao.saveMany(allocations)
            }
        }
    }

    suspend fun setGoalActive(goalId: UUID, isActive: Boolean, now: Instant = Instant.now()) {
        database.withTransaction {
            writeGoalDao.setActive(goalId, isActive, now)
        }
    }

    suspend fun deleteGoal(goalId: UUID) {
        database.withTransaction {
            writeGoalAllocationDao.deleteByGoalId(goalId)
            writeGoalDao.deleteById(goalId)
        }
    }
}
