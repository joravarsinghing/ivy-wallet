package com.ivy.design.system

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Coverage guard, not visual/device acceptance. A new destination requires an explicit chart decision. */
class ChartDistributionContractTest {
    private val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }
    private val contextual = setOf("TransactionsScreen", "BalanceScreen", "PlannedPaymentsScreen", "ReportScreen", "BudgetScreen", "GoalsScreen", "NetWorthScreen", "LoansScreen", "LoanDetailsScreen", "SearchScreen")
    private val existing = setOf("MainScreen", "PieChartStatisticScreen")
    private val taskOnly = mapOf(
        "OnboardingScreen" to "Setup, no personal history yet", "CSVScreen" to "Import mapping and validation",
        "EditTransactionScreen" to "Financial editor, not an analysis destination", "EditPlannedScreen" to "Schedule editor",
        "CategoriesScreen" to "Category management; category activity opens Transactions",
        "SettingsScreen" to "Preferences", "ImportScreen" to "Import preview and confirmation",
        "SecurityScreen" to "Protected credentials, no financial visualization",
        "ExchangeRatesScreen" to "Quote management; no stored comparable rate history",
        "FeaturesScreen" to "Feature information", "AttributionsScreen" to "Licensing", "ContributorsScreen" to "Credits",
        "ReleasesScreen" to "Release notes", "DisclaimerScreen" to "Legal information", "PollScreen" to "Legacy compatibility route"
    )
    @Test fun `every registered destination has a reviewed contextual chart decision`() {
        val text = File(root, "shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt").readText()
        val routes = Regex("data (?:class|object) (\\w+Screen)\\b").findAll(text).map { it.groupValues[1] }.toSet()
        assertEquals(routes, contextual + existing + taskOnly.keys)
        assertTrue(taskOnly.values.all { it.isNotBlank() })
    }
    @Test fun `financial sections really call shared charts without depending on Reports`() {
        val hooks = mapOf(
            "accounts/AccountsContent.kt" to "account-balance-chart", "budgets/BudgetScreen.kt" to "budget-usage-chart",
            "goals/GoalsScreen.kt" to "goal-funding-chart", "transactions/TransactionsScreen.kt" to "ContextualActivityChart",
            "search/SearchScreen.kt" to "ContextualActivityChart", "networth/NetWorthScreen.kt" to "JvyAnalyticsCard")
        hooks.forEach { (key, marker) ->
            val module = key.substringBefore('/')
            assertTrue(File(root, "feature/$module/src/main/java/com/ivy/$key").readText().contains(marker))
            assertFalse(File(root, "feature/$module/build.gradle.kts").readText().contains("projects.feature.reports"))
        }
        assertTrue(File(root, "feature/loans/src/main/java/com/ivy/loans/loandetails/LoanDetailsScreen.kt").readText().contains("LoanDetailsCharts(state)"))
        assertTrue(File(root, "feature/planned-payments/src/main/java/com/ivy/planned/list/PlannedPaymentsScreen.kt").readText().contains("JvyAnalyticsCard(chart)"))
        assertTrue(File(root, "feature/balance/src/main/java/com/ivy/balance/BalanceScreen.kt").readText().contains("JvyAnalyticsCard(chart)"))
    }
    @Test fun `management charts follow operational cards rather than replacing the first useful actions`() {
        listOf(Triple("accounts/AccountsContent.kt", "items(state.accountsData.chunked(columns)", "account-balance-chart"),
            Triple("budgets/BudgetScreen.kt", "items(state.budgets,", "budget-usage-chart"),
            Triple("goals/GoalsScreen.kt", "items(goals.size,", "goal-funding-chart"),
            Triple("loans/loan/LoansScreen.kt", "items(loans,", "loan-overview-chart")).forEach { (path, list, chart) ->
            val module = path.substringBefore('/')
            val text = File(root, "feature/$module/src/main/java/com/ivy/$path").readText()
            assertTrue(path, text.indexOf(list) in 0 until text.indexOf(chart))
        }
    }
}
