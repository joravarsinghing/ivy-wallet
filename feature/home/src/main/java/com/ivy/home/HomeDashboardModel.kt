package com.ivy.home

import androidx.compose.runtime.Immutable

/** Stable identifiers for the user-configurable Home cards. */
enum class HomeDashboardCardId(
    val stableId: String,
    val label: String,
    val description: String,
) {
    SAFE_TO_SPEND(
        stableId = "safe_to_spend",
        label = "Safe to spend",
        description = "What you can spend while keeping your protected buffer.",
    ),
    MONEY_COMING_UP(
        stableId = "money_coming_up",
        label = "Money coming up",
        description = "Planned income, expenses, and your projected balance.",
    ),
    MONEY_TRENDS(
        stableId = "money_trends",
        label = "Money trends",
        description = "Income, spending, and savings over time.",
    ),
    SPENDING_BREAKDOWN(
        stableId = "spending_breakdown",
        label = "Where your money went",
        description = "See which categories used the most money.",
    ),
}

@Immutable
data class HomeDashboardCardDefinition(
    val id: HomeDashboardCardId,
    val label: String = id.label,
    val description: String = id.description,
)

/** The initial Home order is intentionally the same as the current Home composition. */
val defaultHomeDashboardCards: List<HomeDashboardCardDefinition> =
    HomeDashboardCardId.entries.map(::HomeDashboardCardDefinition)

/** All supported cards are visible for a new user. */
val defaultVisibleHomeDashboardCards: Set<HomeDashboardCardId> =
    defaultHomeDashboardCards.mapTo(linkedSetOf()) { it.id }

/** Pure toggle behavior for a future Home preference holder or ViewModel. */
fun toggleHomeDashboardCard(
    visibleCards: Set<HomeDashboardCardId>,
    card: HomeDashboardCardId,
): Set<HomeDashboardCardId> = buildSet {
    addAll(visibleCards)
    if (!remove(card)) add(card)
}

