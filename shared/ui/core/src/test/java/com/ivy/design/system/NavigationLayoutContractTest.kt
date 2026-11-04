package com.ivy.design.system

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Source guardrails complement renders; they do not claim to test device navigation. */
class NavigationLayoutContractTest {
    private val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile && File(it, "feature").isDirectory }
    private fun source(path: String) = File(root, path).readText()
    private val routes = mapOf(
        "MainScreen" to "feature/main/src/main/java/com/ivy/main/MainScreen.kt",
        "OnboardingScreen" to "feature/onboarding/src/main/java/com/ivy/onboarding/OnboardingScreen.kt",
        "CSVScreen" to "feature/import-data/src/main/java/com/ivy/importdata/csv/CSVScreen.kt",
        "EditTransactionScreen" to "feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionScreen.kt",
        "TransactionsScreen" to "feature/transactions/src/main/java/com/ivy/transactions/TransactionsScreen.kt",
        "PieChartStatisticScreen" to "feature/piechart/src/main/java/com/ivy/piechart/PieChartStatisticScreen.kt",
        "EditPlannedScreen" to "feature/planned-payments/src/main/java/com/ivy/planned/edit/EditPlannedScreen.kt",
        "BalanceScreen" to "feature/balance/src/main/java/com/ivy/balance/BalanceScreen.kt",
        "PlannedPaymentsScreen" to "feature/planned-payments/src/main/java/com/ivy/planned/list/PlannedPaymentsScreen.kt",
        "CategoriesScreen" to "feature/categories/src/main/java/com/ivy/categories/CategoriesScreen.kt",
        "SettingsScreen" to "feature/settings/src/main/java/com/ivy/settings/SettingsScreen.kt",
        "ImportScreen" to "feature/import-data/src/main/java/com/ivy/importdata/csvimport/ImportScreen.kt",
        "ReportScreen" to "feature/reports/src/main/java/com/ivy/reports/ReportScreen.kt",
        "BudgetScreen" to "feature/budgets/src/main/java/com/ivy/budgets/BudgetScreen.kt",
        "GoalsScreen" to "feature/goals/src/main/java/com/ivy/goals/GoalsScreen.kt",
        "SecurityScreen" to "feature/security/src/main/java/com/ivy/security/ui/SecurityScreens.kt",
        "NetWorthScreen" to "feature/networth/src/main/java/com/ivy/networth/NetWorthScreen.kt",
        "LoansScreen" to "feature/loans/src/main/java/com/ivy/loans/loan/LoansScreen.kt",
        "SearchScreen" to "feature/search/src/main/java/com/ivy/search/SearchScreen.kt",
        "LoanDetailsScreen" to "feature/loans/src/main/java/com/ivy/loans/loandetails/LoanDetailsScreen.kt",
        "ExchangeRatesScreen" to "feature/exchange-rates/src/main/java/com/ivy/exchangerates/ExchangeRatesScreen.kt",
        "FeaturesScreen" to "feature/features/src/main/java/com/ivy/features/FeaturesScreen.kt",
        "AttributionsScreen" to "feature/attributions/src/main/java/com/ivy/attributions/AttributionsScreen.kt",
        "ContributorsScreen" to "feature/contributors/src/main/java/com/ivy/contributors/ContributorsScreen.kt",
        "ReleasesScreen" to "feature/releases/src/main/java/com/ivy/releases/ReleasesScreen.kt",
        "DisclaimerScreen" to "feature/disclaimer/src/main/java/com/ivy/disclaimer/DisclaimerScreen.kt",
        "PollScreen" to "feature/poll/impl/src/main/java/com/ivy/poll/impl/ui/PollScreen.kt",
    )

    @Test fun `every registered destination has an explicit navigation audit entry`() {
        val declared = Regex("data (?:class|object) (\\w+Screen)").findAll(source("shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt"))
            .map { it.groupValues[1] }.toSet()
        assertEquals("Audit new destinations before adding another navigation style", declared, routes.keys)
    }

    @Test fun `all subsection destinations delegate to approved top navigation`() {
        val approved = listOf("JvyScreenHeader(", "JvySubsectionScaffold(", "JvyTopAppBar(", "JvyBackButton(",
            "ItemStatisticToolbar(", "Toolbar(", "AiImportScreenContent(", "DisclaimerTopAppBar(", "ImportFrom(")
        routes.filterKeys { it !in setOf("MainScreen", "OnboardingScreen") }.forEach { (route, path) ->
            assertTrue("$route must use the shared header or an audited adapter", approved.any { source(path).contains(it) })
        }
    }

    @Test fun `full pages cannot reintroduce bottom exit bars or Close as Back`() {
        val forbidden = Regex("(?:BackBottomBar|PlannedPaymentsBottomBar|CategoriesBottomBar|ExchangeRatesBottomBar|PieChartStatisticBottomBar)\\(")
        routes.forEach { (route, path) ->
            val text = source(path)
            assertFalse("$route has an obsolete bottom exit bar", forbidden.containsMatchIn(text))
            assertFalse("$route uses Close as page navigation", Regex("(?<!Jvy)CloseButton\\(").containsMatchIn(text))
            assertFalse("$route has a competing text-only Back", text.contains("Text(\"Back\")"))
        }
    }

    @Test fun `shared chrome never decides how to save discard pop or navigate`() {
        val chrome = source("shared/ui/core/src/main/java/com/ivy/design/system/JvySubsectionScaffold.kt")
        assertFalse(chrome.contains("com.ivy.navigation"))
        assertFalse(chrome.contains("navigateTo("))
        assertTrue(chrome.contains("onBack: () -> Unit"))
    }

    @Test fun `customization dialogs share the bounded form host`() {
        listOf("HomeDashboardEditorDialog.kt", "HomeTrendEditorDialog.kt").forEach { file ->
            assertTrue(source("feature/home/src/main/java/com/ivy/home/$file").contains("JvyFormDialogFrame("))
        }
        assertTrue(source("feature/networth/src/main/java/com/ivy/networth/WealthEditors.kt").contains("JvyCloseButton(close)"))
    }

    @Test fun `transaction editors retain shared chrome and contextual destructive actions`() {
        val toolbar = source("temp/legacy-code/src/main/java/com/ivy/legacy/ui/component/edit/core/Toolbar.kt")
        assertTrue(toolbar.contains("JvyScreenHeader("))
        assertTrue(toolbar.contains("nav.onBackPressed()"))
        assertTrue(toolbar.contains("DropdownMenu("))
        assertFalse(toolbar.contains("FlowRow("))
        assertTrue(toolbar.contains("backEnabled = enabled"))
        assertTrue(source(routes.getValue("EditTransactionScreen")).contains("enabled = !saving"))
        assertTrue(source(routes.getValue("EditPlannedScreen")).contains("enabled = !state.saving"))
    }

    @Test fun `active onboarding cannot restore absolute screen coordinates or floating task footers`() {
        val base = "feature/onboarding/src/main/java/com/ivy/onboarding/steps/"
        listOf("OnboardingType.kt", "OnboardingSetCurrency.kt", "OnboardingAccounts.kt", "OnboardingCategories.kt")
            .forEach { file ->
                val text = source(base + file)
                assertTrue("$file must use shared in-flow chrome", text.contains("JvySubsectionScaffold("))
                assertFalse(text.contains("GradientCutBottom("))
                assertFalse(text.contains("Alignment.BottomCenter"))
            }
        val welcome = source(base + "OnboardingSplashLogin.kt")
        assertFalse(welcome.contains("screenHeight"))
        assertFalse(welcome.contains("screenWidth"))
        assertTrue(welcome.contains("verticalScroll("))
    }

    @Test fun `editor restoration is scoped to visits not just equal route arguments`() {
        val rootActivity = source("app/src/main/java/com/ivy/wallet/RootActivity.kt")
        assertTrue(rootActivity.contains("SaveableStateProvider(\"route:\$entryId\")"))
        listOf("feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionViewModel.kt",
            "feature/planned-payments/src/main/java/com/ivy/planned/edit/EditPlannedViewModel.kt").forEach { path ->
            val text = source(path)
            assertTrue(text.contains("startedEntryId == entryId"))
            assertTrue(text.contains("nav.registerScreenBackHandler(screen) { explicitSavePending }"))
            if (path.contains("/edit-transaction/")) {
                assertTrue(text.contains("isCurrentTransactionWrite(ownerEntryId, revision, nav.currentEntryId, writeRevision)"))
                val policy = source("feature/edit-transaction/src/main/java/com/ivy/transaction/TransactionWriteSnapshot.kt")
                assertTrue(policy.contains("owner != null && owner == current && revision == newestRevision"))
            } else {
                assertTrue(text.contains("nav.currentEntryId == ownerEntryId"))
            }
        }
    }

    @Test fun `legacy parent recomposition cannot re-register Back above its existing child`() {
        val host = source("temp/legacy-code/src/main/java/com/ivy/legacy/legacy/ui/theme/modal/IvyModal.kt")
        assertTrue(host.contains("val backId = remember { UUID.randomUUID() }"))
        assertFalse(host.contains("val backId = remember(id)"))
        assertTrue(host.contains("nav.registerModalBackHandler(modalId)"))
    }

    @Test fun `titled legacy adapters cannot add a second scrolled title`() {
        val legacy = "temp/legacy-code/src/main/java/com/ivy/legacy/"
        val modal = legacy + "legacy/ui/theme/modal/"
        val paths = listOf("LoanModal.kt", "LoanRecordModal.kt", "MonthPickerModal.kt",
            "RecurringRuleModal.kt", "CurrencyModal.kt", "ChooseIconModal.kt",
            "ChooseStartDateOfMonthModal.kt", "edit/AccountModal.kt", "edit/CategoryModal.kt",
            "edit/ChooseCategoryModal.kt", "edit/CalculatorModal.kt").map { modal + it } + listOf(
            legacy + "ui/component/tags/ShowTagModal.kt", legacy + "ui/component/tags/AddOrEditTagModal.kt",
            legacy + "legacy/ui/theme/components/ChangeTransactionTypeModal.kt",
            "feature/categories/src/main/java/com/ivy/categories/CategoriesScreen.kt",
            "feature/budgets/src/main/java/com/ivy/budgets/BudgetModal.kt",
            "feature/exchange-rates/src/main/java/com/ivy/exchangerates/modal/AddRateModal.kt")
        paths.forEach { path ->
            val text = source(path)
            assertTrue("$path must give the modal its title", text.contains("title ="))
            assertFalse("$path duplicates the modal header", Regex("\\bModalTitle\\(").containsMatchIn(text))
        }
    }

    @Test fun `modern and titled legacy hosts share the non-scrolling header`() {
        val modern = source("shared/ui/core/src/main/java/com/ivy/design/system/JvyFormDialog.kt")
        val legacy = source("temp/legacy-code/src/main/java/com/ivy/legacy/legacy/ui/theme/modal/IvyModal.kt")
        assertTrue(modern.contains("JvyDialogHeader(title, onClose"))
        assertTrue(legacy.contains("JvyDialogHeader(title, requestClose"))
        assertTrue(modern.indexOf("JvyDialogHeader(title, onClose") < modern.indexOf(".verticalScroll("))
        assertTrue(legacy.indexOf("JvyDialogHeader(title, requestClose") < legacy.indexOf("verticalScroll(scrollState!!"))
    }
}
