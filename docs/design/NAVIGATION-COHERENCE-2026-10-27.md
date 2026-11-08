# App-wide navigation coherence — 27 October 2026

## Cause and chosen standard

Planned Payments was missed by the earlier planning-page migration. It still rendered a separate bottom-close bar and forecast-before-title layout, while Budgets and Loans used the shared top Back header. The written standard was not enforced across all destinations, and Planned Payments only had calculation tests, not adaptive page renders.

This sweep chooses one navigation hierarchy: root destinations retain the existing bottom bar/rail; full-page subsections use top-start Back; dialogs/sheets use top-right Close/Cancel. It does not replace the navigation engine, change financial calculations, migrate stored data or build another APK.

## Destination inventory

All 27 destinations declared in `Screens.kt` are covered by the source contract test.

| Destination | Navigation ownership |
| --- | --- |
| MainScreen | Existing five root tabs and adaptive bar/rail; no fake Back. |
| OnboardingScreen | Welcome remains root; choose-path, currency, accounts and categories use shared headers and retain step handlers. |
| CSVScreen | Added shared Back/header to manual CSV configuration; result and processing use the import adapters. |
| EditTransactionScreen | Shared editor toolbar's canonical Back; amount/type/delete/duplicate behavior retained, actions wrap. |
| TransactionsScreen | Shared statistic toolbar's Back, with category/account contrast tint retained. |
| PieChartStatisticScreen | Existing sticky analytic header now uses the canonical Back; obsolete bottom-exit helper removed. |
| EditPlannedScreen | Shared editor toolbar, including adaptive actions. |
| BalanceScreen | Shared top header replaces the bottom centered X; calculations unchanged. |
| PlannedPaymentsScreen | Shared subsection shell, planning empty state and in-flow populated Add footer. |
| CategoriesScreen | Shared subsection shell; sort/reorder retained as header actions and Add retained in footer. |
| SettingsScreen | Shared title/Back header; existing settings Back handler and version shortcut retained. |
| ImportScreen | Shared chooser/instruction/AI/result headers; Back disabled while processing, existing step history retained. |
| ReportScreen | Shared header; filter remains direct, Save/Export remain in an accessible action menu. Filter overlay has modal Close, not page Back. |
| BudgetScreen | Existing shared header and in-flow action pattern retained. |
| GoalsScreen | Existing shared header/form pattern retained. |
| SecurityScreen | Shared settings header replaces text-only Back; credential state and authorization unchanged. |
| NetWorthScreen | Standalone header uses Back and existing tools; embedded root tab has no Back. |
| LoansScreen | Existing shared planning header/filters retained. |
| SearchScreen | Added visible shared Back/header; queries and search behavior retained. |
| LoanDetailsScreen | Canonical Back via statistic toolbar, preserving colored-header contrast and actions. |
| ExchangeRatesScreen | Shared title/Back and in-flow Add footer replace bottom Back bar and 480dp spacer. |
| FeaturesScreen | Shared app bar replaces independently centered/custom chrome. |
| AttributionsScreen | Shared app bar and existing return handler. |
| ContributorsScreen | Shared app bar, existing count title and GitHub action retained. |
| ReleasesScreen | Shared app bar and existing release actions. |
| DisclaimerScreen | Shared app bar; Back only when a previous destination exists. Acceptance is not implied by leaving. |
| PollScreen | Shared header; redundant voted-state footer Back removed and content respects header insets. |

## Shared implementation

- `JvyBackButton`, `JvyCloseButton`, `JvyTopAppBar` and `JvySubsectionScaffold` own chrome. The shared controls have no Navigation dependency and cannot silently save/discard or redirect to Home.
- Legacy edit/statistic/back adapters use the same arrow and target. Legacy `IvyModal` has a persistent top-right Close and a separately scrolling body; its Back-handler registration and keyboard/dismiss callbacks remain intact. Report filters retain draft/apply semantics with modal Close at the top.
- Home and trend customization dialogs now use the bounded shared form host, with Close above the scrolling toggles and Done/Restore actions below them. Net-worth dialogs use the same shared Close control. Live preference changes, restore-default behavior and dismissal callbacks are retained.
- Report renders exposed a duplicated page title and transaction counts competing with amounts for horizontal space. The duplicate title was removed; shared income/expense summaries put counts below values and stack on narrow or large-text windows, without changing the values or chart callbacks.
- Removed obsolete Planned Payments, Categories, Exchange Rates and pie-chart bottom-exit helpers plus the unused legacy BackBottomBar. These code deletions are recoverable from Git; no user data was deleted.
- Planned Payments keeps its forecast model and list expansion/navigation callbacks. The empty state hides a redundant zero forecast and has one Add action. Populated lists retain the forecast before their groups and a pinned-in-flow Add action; narrow/large-text forecast metrics stack.
- The existing Raleway/Open Sans type system, semantic colors, primary navigation ownership, PIN/recovery protections, package/signing identity, database/backup formats and version baseline remain unchanged.

## Verification

The source guard inventories all routes and checks approved shared headers/adapters, absence of bottom page exits/Close-as-Back, and lack of navigation decisions in shared chrome. Navigation unit tests cover nested return paths, import substep interception and dialog dismissal precedence. These are targeted guardrails, not a substitute for tapping through the app.

Adaptive candidates cover empty/populated/long-value Planned Payments, report headers/totals, Home/trend customization and long legacy dialogs at 320dp, phone, dark square fold, DeX, 2× phone/fold text, short landscape and AMOLED. Manual inspection confirms planning empty-state/header alignment, readable report totals and reachable dialog Close/Save/Done. Existing Categories and Exchange Rates candidates were also inspected. No approved screenshot golden has been replaced.

Local review candidates (ignored build artifacts, not approved goldens): [Planned Payments empty](../../artifacts/ui-review/navigation-2026-10-27/planned-empty-amoled.png), [populated](../../artifacts/ui-review/navigation-2026-10-27/planned-populated-amoled.png), [Home editor at 2× text](../../artifacts/ui-review/navigation-2026-10-27/home-editor-large-text.png), [Reports](../../artifacts/ui-review/navigation-2026-10-27/reports-amoled.png).

The final regression run (`/tmp/jvy-navigation-verified.log`) completed successfully in 8m 4s: **1,204 tests across 108 suites, zero failures/errors/skips**, plus `:app:compileDebugKotlin`. This includes five source-contract checks, three new navigation-history checks and 56 new adaptive render cases. The broader run covers shared financial/data logic and the existing security, import, chart, planning, account and root-navigation suites. `git diff --check` is clean.

Settings, transaction editor, onboarding, balance, pie-chart and Features currently have no module unit-test sources: they were compiled/source-audited, not interaction-tested. Candidate rendering does not imply screenshot-golden approval. No APK was assembled, no release/signing identity or version changed, and no commit/push/release was performed.

Physical-device checks remain for system Back, keyboard insets, TalkBack, resizes and actual multi-step workflows. Busy import intentionally cannot be interrupted; root/welcome/unlock and native confirmation dialogs are semantic exceptions, not incomplete page migrations.

Android adaptive and UI/UX Pro Max guidance informed the common layout ownership, wrapping actions, stable history and accessible target semantics. No Navigation3/Compose migration or new UI dependency was introduced.
