package com.ivy.domain.usecase.category

import com.ivy.data.db.dao.read.CategoryRuleDao
import com.ivy.data.db.dao.write.WriteCategoryRuleDao
import com.ivy.data.db.entity.CategoryRuleEntity
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Offline persistence and matching boundary for remembered category rules. */
@Singleton
class CategoryRuleRepository @Inject constructor(
    private val categoryRuleDao: CategoryRuleDao,
    private val writeCategoryRuleDao: WriteCategoryRuleDao,
) {
    suspend fun findAll(): List<CategoryRule> = categoryRuleDao.findAll().map { it.toDomain() }

    suspend fun findActive(): List<CategoryRule> =
        categoryRuleDao.findActive().map { it.toDomain() }

    suspend fun findById(id: String): CategoryRule? =
        categoryRuleDao.findById(id)?.toDomain()

    suspend fun save(rule: CategoryRule) {
        writeCategoryRuleDao.save(rule.toEntity())
    }

    suspend fun saveFromTransaction(
        title: String?,
        description: String?,
        categoryId: String,
        priority: Int = 0,
        id: String = UUID.randomUUID().toString(),
    ): CategoryRule? {
        val pattern = rememberedCategoryPattern(title, description) ?: return null
        val rule = CategoryRule(
            id = id,
            categoryId = categoryId,
            pattern = pattern,
            priority = priority,
        )
        save(rule)
        return rule
    }

    suspend fun matchSuggestion(
        entry: FutureCategoryEntry,
        effectiveFrom: LocalDate,
    ): CategoryRuleMatch? = matchFutureCategoryRule(
        entry = entry,
        rules = findActive(),
        effectiveFrom = effectiveFrom,
    )

    suspend fun setEnabled(id: String, enabled: Boolean) {
        writeCategoryRuleDao.setActive(id = id, isActive = enabled)
    }

    suspend fun enable(id: String) = setEnabled(id, enabled = true)

    suspend fun disable(id: String) = setEnabled(id, enabled = false)

    suspend fun delete(id: String) {
        writeCategoryRuleDao.deleteById(id)
    }

    private fun CategoryRuleEntity.toDomain(): CategoryRule = CategoryRule(
        id = id,
        categoryId = categoryId,
        pattern = pattern,
        field = CategoryRuleField.valueOf(field),
        matchType = CategoryRuleMatchType.valueOf(matchType),
        priority = priority,
        isActive = isActive,
    )

    private fun CategoryRule.toEntity(): CategoryRuleEntity = CategoryRuleEntity(
        id = id,
        categoryId = categoryId,
        pattern = pattern,
        field = field.name,
        matchType = matchType.name,
        priority = priority,
        isActive = isActive,
    )
}
