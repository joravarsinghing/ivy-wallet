# R0-UI — Product and UI flow reconnaissance

Status: complete, read-only reconnaissance

## Existing shell

`RootActivity`/`RootViewModel` gate onboarding, app lock, intents, and the main screen. Typed navigation is owned by `shared/ui/navigation/Screens.kt` and `Navigation.kt`; `app/IvyNavGraph.kt` renders screen contracts. Main currently exposes Home and Accounts, with feature entry points for transactions, budgets, planned payments, loans, reports, settings, import, and search.

## Product extension points

- Understand: Home state, reports, pie-chart drilldown, search, and customer-journey cards.
- Control: transaction, budget, planned-payment, account/category, and settings event/ViewModel seams.
- Protect: app lock, hidden values, backup/import/export, legal/disclaimer, and sensitive settings flows.
- Build: onboarding → accounts/categories → planned payments → budgets/loans.
- Grow: no dedicated domain yet; initially compose over Home/customer-journey, reports, feature flags, and setup data.

## Ownership rules

`app` owns lifecycle/root gating and the central screen switch. Navigation owns typed routes. Each feature owns its screen/state/events/ViewModel. `shared:data:core` owns persistence/import/backup, and `shared:domain` owns calculations. New modernized screens should cross the legacy boundary deliberately rather than mixing design systems implicitly.

## References

`app/src/main/java/com/ivy/wallet/RootActivity.kt`, `RootViewModel.kt`, `app/src/main/java/com/ivy/IvyNavGraph.kt`, `shared/ui/navigation/**`, `feature/main`, `feature/home`, `feature/import-data`, and `docs/guidelines/Screen-Architecture.md`.
