package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.data.db.entity.CategoryRuleEntity

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules ORDER BY priority DESC, id ASC")
    suspend fun findAll(): List<CategoryRuleEntity>

    @Query("SELECT * FROM category_rules WHERE isActive = 1 ORDER BY priority DESC, id ASC")
    suspend fun findActive(): List<CategoryRuleEntity>

    @Query("SELECT * FROM category_rules WHERE id = :id")
    suspend fun findById(id: String): CategoryRuleEntity?
}
