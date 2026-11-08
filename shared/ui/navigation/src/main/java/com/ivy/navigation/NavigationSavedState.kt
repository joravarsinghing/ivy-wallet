package com.ivy.navigation

import com.ivy.base.model.TransactionType
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

/** Versioned, bounded primitive route keys. Never serializes a Screen or database object graph. */
internal object NavigationSavedState {
    private const val VERSION = "jvy-navigation-1"
    private const val MAX_ENTRIES = 32
    private const val MAX_CHARACTERS = 65536
    private const val MAX_SELECTION = 500
    private const val MAX_FIELD = 32768

    fun encode(entries: List<Pair<UUID, Screen>>): ArrayList<String>? = runCatching {
        require(entries.isNotEmpty() && entries.size <= MAX_ENTRIES)
        val output = arrayListOf(VERSION, entries.size.toString())
        entries.forEach { (id, screen) ->
            val fields = route(screen)
            output += id.toString()
            output += fields.size.toString()
            fields.forEach { value ->
                require(value == null || value.length <= MAX_FIELD)
                output += if (value == null) "n" else "s$value"
            }
        }
        require(output.sumOf(String::length) <= MAX_CHARACTERS)
        output
    }.getOrNull()

    fun decode(saved: List<String>): List<Pair<UUID, Screen>>? = runCatching {
        require(saved.size in 2..1024 && saved.sumOf(String::length) <= MAX_CHARACTERS)
        var index = 0
        fun next() = saved[index++]
        require(next() == VERSION)
        val count = next().toInt().also { require(it in 1..MAX_ENTRIES) }
        val entries = List(count) {
            val id = uuid(next())
            val fields = next().toInt().also { require(it in 1..16) }
            val values = List(fields) {
                val value = next()
                require(value.length <= MAX_FIELD + 1)
                when { value == "n" -> null; value.startsWith("s") -> value.drop(1); else -> error("Invalid field") }
            }
            id to screen(values)
        }
        require(index == saved.size && entries.map { it.first }.toSet().size == count)
        entries
    }.getOrNull()

    private fun uuid(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value.lowercase()) }
    private fun ids(values: List<UUID>) = values.also { require(it.size <= MAX_SELECTION) }.joinToString(",")
    private fun readIds(value: String?) = value?.let { if (it.isEmpty()) emptyList() else it.split(",").also { parts -> require(parts.size <= MAX_SELECTION) }.map(::uuid) }
    private fun route(screen: Screen): List<String?> = when (screen) {
        MainScreen -> listOf("main"); OnboardingScreen -> listOf("onboarding")
        BalanceScreen -> listOf("balance"); PlannedPaymentsScreen -> listOf("planned")
        CategoriesScreen -> listOf("categories"); SettingsScreen -> listOf("settings")
        ReportScreen -> listOf("reports"); BudgetScreen -> listOf("budgets"); GoalsScreen -> listOf("goals")
        SecurityScreen -> listOf("security"); NetWorthScreen -> listOf("wealth"); LoansScreen -> listOf("loans")
        SearchScreen -> listOf("search"); ExchangeRatesScreen -> listOf("rates"); FeaturesScreen -> listOf("features")
        ReleasesScreen -> listOf("releases"); DisclaimerScreen -> listOf("disclaimer"); PollScreen -> listOf("retired-poll")
        is CSVScreen -> listOf("csv", screen.launchedFromOnboarding.toString())
        is ImportScreen -> listOf("import", screen.launchedFromOnboarding.toString())
        is LoanDetailsScreen -> listOf("loan", screen.loanId.toString())
        is EditTransactionScreen -> listOf("transaction", screen.initialTransactionId?.toString(), screen.type.name,
            screen.accountId?.toString(), screen.categoryId?.toString())
        is EditPlannedScreen -> listOf("planned-editor", screen.plannedPaymentRuleId?.toString(), screen.type.name,
            screen.amount?.also { require(it.isFinite()) }?.toString(), screen.accountId?.toString(), screen.categoryId?.toString(),
            screen.title, screen.description)
        is TransactionsScreen -> listOf("history", screen.accountId?.toString(), screen.categoryId?.toString(),
            screen.unspecifiedCategory?.toString(), screen.allTransactions.toString(), screen.transactionType?.name,
            ids(screen.accountIdFilterList), (screen.restoredTransactionIds ?: screen.transactions.takeIf { it.isNotEmpty() }?.map { it.id })?.let(::ids))
        is PieChartStatisticScreen -> listOf("pie", screen.type.name, screen.filterExcluded.toString(), ids(screen.accountList),
            screen.treatTransfersAsIncomeExpense.toString(), (screen.restoredTransactionIds ?: screen.transactions.takeIf { it.isNotEmpty() }?.map { it.id })?.let(::ids),
            screen.dateRange?.from?.toString(), screen.dateRange?.to?.toString(), screen.cashFlowOnly.toString())
        else -> error("Unsupported destination")
    }

    private fun screen(fields: List<String?>): Screen {
        fun field(index: Int) = fields[index]
        fun required(index: Int) = requireNotNull(field(index))
        fun id(index: Int) = field(index)?.let(::uuid)
        fun bool(index: Int) = required(index).toBooleanStrict()
        fun type(index: Int) = TransactionType.valueOf(required(index))
        val route = required(0)
        val count = when (route) {
            "csv", "import", "loan" -> 2; "transaction" -> 5; "planned-editor", "history" -> 8; "pie" -> 6
            else -> 1
        }
        require(fields.size == count || route == "pie" && fields.size in 8..9)
        return when (route) {
            "main" -> MainScreen; "onboarding" -> OnboardingScreen; "balance" -> BalanceScreen
            "planned" -> PlannedPaymentsScreen; "categories" -> CategoriesScreen; "settings" -> SettingsScreen
            "reports" -> ReportScreen; "budgets" -> BudgetScreen; "goals" -> GoalsScreen; "security" -> SecurityScreen
            "wealth" -> NetWorthScreen; "loans" -> LoansScreen; "search" -> SearchScreen; "rates" -> ExchangeRatesScreen
            "features" -> FeaturesScreen; "attributions" -> SettingsScreen; "contributors" -> SettingsScreen
            "releases" -> ReleasesScreen; "disclaimer" -> DisclaimerScreen; "retired-poll" -> PollScreen
            "csv" -> CSVScreen(bool(1)); "import" -> ImportScreen(bool(1)); "loan" -> LoanDetailsScreen(requireNotNull(id(1)))
            "transaction" -> EditTransactionScreen(id(1), type(2), id(3), id(4))
            "planned-editor" -> EditPlannedScreen(id(1), type(2), field(3)?.toDouble()?.also { require(it.isFinite()) }, id(4), id(5), field(6), field(7))
            "history" -> TransactionsScreen(id(1), id(2), field(3)?.toBooleanStrict(), bool(4), field(5)?.let(TransactionType::valueOf),
                readIds(field(6)).orEmpty(), restoredTransactionIds = readIds(field(7)))
            "pie" -> PieChartStatisticScreen(type(1), bool(2), readIds(field(3)).orEmpty().toImmutableList(),
                treatTransfersAsIncomeExpense = bool(4), restoredTransactionIds = readIds(field(5)),
                dateRange = if (fields.size == 6 || field(6) == null && field(7) == null) null else
                    TransactionDateRange(java.time.Instant.parse(required(6)), java.time.Instant.parse(required(7))),
                cashFlowOnly = if (fields.size == 9) bool(8) else false)
            else -> error("Unknown destination")
        }
    }
}
