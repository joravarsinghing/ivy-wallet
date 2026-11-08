package com.ivy.domain.usecase.category

import com.ivy.data.db.dao.read.CategoryRuleDao
import com.ivy.data.db.dao.write.WriteCategoryRuleDao
import com.ivy.data.db.entity.CategoryRuleEntity
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CategoryRuleRepositoryTest {
    private val categoryRuleDao = mockk<CategoryRuleDao>()
    private val writeCategoryRuleDao = mockk<WriteCategoryRuleDao>()
    private lateinit var repository: CategoryRuleRepository

    @Before
    fun setup() {
        repository = CategoryRuleRepository(categoryRuleDao, writeCategoryRuleDao)
    }

    @Test
    fun `saves a normalized rule from a transaction title`() = runTest {
        io.mockk.coEvery { writeCategoryRuleDao.save(any()) } just runs

        val rule = repository.saveFromTransaction(
            title = "  ENOC Station  ",
            description = "Fuel",
            categoryId = "fuel",
            id = "rule-1",
        )

        rule shouldBe CategoryRule(
            id = "rule-1",
            categoryId = "fuel",
            pattern = "enoc station",
        )
        coVerify {
            writeCategoryRuleDao.save(
                CategoryRuleEntity(
                    id = "rule-1",
                    categoryId = "fuel",
                    pattern = "enoc station",
                    field = "TITLE_OR_DESCRIPTION",
                    matchType = "CONTAINS",
                    priority = 0,
                    isActive = true,
                )
            )
        }
    }

    @Test
    fun `does not persist a rule without title or description`() = runTest {
        val result = repository.saveFromTransaction(
            title = "  ",
            description = null,
            categoryId = "fuel",
        )

        result shouldBe null
        coVerify(exactly = 0) { writeCategoryRuleDao.save(any()) }
    }

    @Test
    fun `supports disabling and deleting rules`() = runTest {
        io.mockk.coEvery { writeCategoryRuleDao.setActive(any(), any()) } just runs
        io.mockk.coEvery { writeCategoryRuleDao.deleteById(any()) } just runs

        repository.setEnabled("rule-1", enabled = false)
        repository.delete("rule-1")

        coVerify { writeCategoryRuleDao.setActive("rule-1", false) }
        coVerify { writeCategoryRuleDao.deleteById("rule-1") }
    }
}
