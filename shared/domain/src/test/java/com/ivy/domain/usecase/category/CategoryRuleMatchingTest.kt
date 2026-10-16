package com.ivy.domain.usecase.category

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.junit.Test

class CategoryRuleMatchingTest {

    @Test
    fun `matches future entries case insensitively without changing existing categories`() {
        val rule = CategoryRule(
            id = "fuel",
            categoryId = "transport-fuel",
            pattern = "ENOC",
        )
        val result = matchFutureCategoryRule(
            entry = FutureCategoryEntry(
                id = "planned-fuel",
                date = LocalDate.of(2026, 10, 15),
                title = "enoc station",
            ),
            rules = listOf(rule),
            effectiveFrom = LocalDate.of(2026, 10, 14),
        )
        val alreadyCategorized = matchFutureCategoryRule(
            entry = FutureCategoryEntry(
                id = "categorized",
                date = LocalDate.of(2026, 10, 15),
                title = "ENOC station",
                existingCategoryId = "manual-category",
            ),
            rules = listOf(rule),
            effectiveFrom = LocalDate.of(2026, 10, 14),
        )

        result shouldBe CategoryRuleMatch("planned-fuel", "fuel", "transport-fuel")
        alreadyCategorized.shouldBeNull()
    }

    @Test
    fun `resolves matching rules by priority specificity and stable id`() {
        val result = matchFutureCategoryRule(
            entry = FutureCategoryEntry(
                id = "food",
                date = LocalDate.of(2026, 10, 15),
                title = "Talabat delivery",
            ),
            rules = listOf(
                CategoryRule("broad", "food", "tal", priority = 1),
                CategoryRule("specific", "delivery", "talabat", priority = 1),
                CategoryRule("higher", "preferred", "tal", priority = 2),
            ),
            effectiveFrom = LocalDate.of(2026, 10, 14),
        )

        result?.ruleId shouldBe "higher"
        result?.categoryId shouldBe "preferred"
    }

    @Test
    fun `does not match historical entries and supports exact description matching`() {
        val result = matchFutureCategoryRule(
            entry = FutureCategoryEntry(
                id = "salary",
                date = LocalDate.of(2026, 10, 14),
                title = "Salary payment",
                description = "October Salary",
            ),
            rules = listOf(
                CategoryRule(
                    id = "salary-rule",
                    categoryId = "salary",
                    pattern = "october salary",
                    field = CategoryRuleField.DESCRIPTION,
                    matchType = CategoryRuleMatchType.EXACT,
                ),
            ),
            effectiveFrom = LocalDate.of(2026, 10, 15),
        )

        result shouldBe null
    }
}
