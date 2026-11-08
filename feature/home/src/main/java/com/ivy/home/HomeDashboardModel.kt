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
        label = "Balance trend",
        description = "Period-end balance with income and spending over time.",
    ),
    SPENDING_BREAKDOWN(
        stableId = "spending_breakdown",
        label = "Where your money went",
        description = "See which categories used the most money.",
    ),
    SAVED_REPORTS(
        stableId = "saved_reports",
        label = "Saved reports",
        description = "Keep your favorite analytics close on Home.",
    ),
    RECENT_ACTIVITY("recent_activity", "Recent activity", "Transactions in your selected Home period."),
}

@Immutable
data class HomeDashboardCardDefinition(
    val id: HomeDashboardCardId,
    val label: String = id.label,
    val description: String = id.description,
)

/** The initial Home order is intentionally the same as the current Home composition. */
val defaultHomeDashboardCards: List<HomeDashboardCardDefinition> =
    listOf(HomeDashboardCardId.MONEY_TRENDS, HomeDashboardCardId.SPENDING_BREAKDOWN,
        HomeDashboardCardId.RECENT_ACTIVITY, HomeDashboardCardId.SAFE_TO_SPEND,
        HomeDashboardCardId.MONEY_COMING_UP, HomeDashboardCardId.SAVED_REPORTS).map(::HomeDashboardCardDefinition)

fun normalizedHomeCardOrder(stored: List<String>): List<HomeDashboardCardId> =
    (stored.mapNotNull { value -> HomeDashboardCardId.entries.firstOrNull { it.stableId == value } } +
        defaultHomeDashboardCards.map { it.id }).distinct()

fun moveHomeCard(order: List<HomeDashboardCardId>, card: HomeDashboardCardId, offset: Int): List<HomeDashboardCardId> {
    val from = order.indexOf(card)
    if (from < 0 || from + offset !in order.indices) return order
    return order.toMutableList().apply { add(from + offset, removeAt(from)) }
}

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

