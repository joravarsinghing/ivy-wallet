package com.ivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ivy.data.db.entity.CategoryRuleEntity

@Dao
interface WriteCategoryRuleDao {
    @Upsert
    suspend fun save(value: CategoryRuleEntity)

    @Query("UPDATE category_rules SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean)

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM category_rules")
    suspend fun deleteAll()
}
