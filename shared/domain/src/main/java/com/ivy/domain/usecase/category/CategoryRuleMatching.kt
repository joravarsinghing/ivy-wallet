package com.ivy.domain.usecase.category

import java.time.LocalDate
import java.util.Locale

enum class CategoryRuleField {
    TITLE,
    DESCRIPTION,
    TITLE_OR_DESCRIPTION,
}

enum class CategoryRuleMatchType {
    CONTAINS,
    EXACT,
}

/** A local deterministic rule. It changes future suggestions, not past ledger entries. */
data class CategoryRule(
    val id: String,
    val categoryId: String,
    val pattern: String,
    val field: CategoryRuleField = CategoryRuleField.TITLE_OR_DESCRIPTION,
    val matchType: CategoryRuleMatchType = CategoryRuleMatchType.CONTAINS,
    val priority: Int = 0,
    val isActive: Boolean = true,
)

data class FutureCategoryEntry(
    val id: String,
    val date: LocalDate,
    val title: String? = null,
    val description: String? = null,
    val existingCategoryId: String? = null,
)

data class CategoryRuleMatch(
    val entryId: String,
    val ruleId: String,
    val categoryId: String,
)

/**
 * Finds one deterministic category suggestion for a future entry.
 *
 * Existing categories win over rules. Matching is case-insensitive and uses
 * locale-independent normalization. Ties are resolved by priority, then the
 * most specific (longest) pattern, then the stable rule ID.
 */
fun matchFutureCategoryRule(
    entry: FutureCategoryEntry,
    rules: Iterable<CategoryRule>,
    effectiveFrom: LocalDate,
): CategoryRuleMatch? {
    require(entry.id.isNotBlank()) { "Future category entry ID is required" }
    if (entry.date.isBefore(effectiveFrom)) return null
    if (entry.existingCategoryId != null) return null

    val matchingRule = rules
        .filter { it.isActive }
        .onEach(::requireValidRule)
        .filter { rule -> ruleMatchesEntry(rule, entry) }
        .sortedWith(
            compareByDescending<CategoryRule> { it.priority }
                .thenByDescending { normalize(it.pattern).length }
                .thenBy { it.id },
        )
        .firstOrNull()

    return matchingRule?.let {
        CategoryRuleMatch(
            entryId = entry.id,
            ruleId = it.id,
            categoryId = it.categoryId,
        )
    }
}

/** Returns only suggestions for future uncategorized entries, in input order. */
fun matchFutureCategoryRules(
    entries: Iterable<FutureCategoryEntry>,
    rules: Iterable<CategoryRule>,
    effectiveFrom: LocalDate,
): List<CategoryRuleMatch> = entries.mapNotNull {
    matchFutureCategoryRule(it, rules, effectiveFrom)
}

private fun ruleMatchesEntry(rule: CategoryRule, entry: FutureCategoryEntry): Boolean {
    val candidates = when (rule.field) {
        CategoryRuleField.TITLE -> listOfNotNull(entry.title)
        CategoryRuleField.DESCRIPTION -> listOfNotNull(entry.description)
        CategoryRuleField.TITLE_OR_DESCRIPTION -> listOfNotNull(entry.title, entry.description)
    }
    val pattern = normalize(rule.pattern)

    return candidates.any { candidate ->
        val normalizedCandidate = normalize(candidate)
        when (rule.matchType) {
            CategoryRuleMatchType.CONTAINS -> normalizedCandidate.contains(pattern)
            CategoryRuleMatchType.EXACT -> normalizedCandidate == pattern
        }
    }
}

private fun requireValidRule(rule: CategoryRule) {
    require(rule.id.isNotBlank()) { "Category rule ID is required" }
    require(rule.categoryId.isNotBlank()) { "Category rule category ID is required" }
    require(rule.pattern.isNotBlank()) { "Category rule pattern is required" }
}

fun normalizeCategoryRuleText(value: String): String = value.trim().lowercase(Locale.ROOT)

/**
 * Chooses a stable substring when the user asks to remember a category for a
 * transaction. Titles are preferred because they usually contain the
 * merchant; descriptions are the fallback when the title is empty.
 */
fun rememberedCategoryPattern(title: String?, description: String?): String? =
    listOfNotNull(title, description)
        .asSequence()
        .map(::normalizeCategoryRuleText)
        .firstOrNull(String::isNotBlank)

private fun normalize(value: String): String = normalizeCategoryRuleText(value)
