# Joravar Agentic Roadmap Status

Last updated: 2026-10-15 (Asia/Dubai)

## Current gate

**Wave 0 — Reconnaissance: COMPLETE / Wave 1 product packets: COMPLETE / Wave 2 product packets: IMPLEMENTED / RELEASE BUILD VERIFIED / DEVICE QA PENDING**

The first low-risk product packets are complete: Jvy Wallet identity, monthly savings, safe-to-spend, and emergency-fund coverage. The inherited Hilt unit-test aggregation stall remains a verification limitation; all four packets preserve import, database, package, and legacy data contracts.

Wave 2 now contains the three practical planning features from the current sprint: account-aware goals with reservations and funding guidance, a 30-day Money Coming Up forecast, and deterministic category memory rules. The features preserve Ivy import compatibility and keep reservations, projections, and category suggestions separate from ledger transactions.

## Wave 1 packet status

**W1-BRAND — Jvy Wallet identity: COMPLETE**

- Visible release, demo, debug, onboarding, and transaction-reminder branding now says `Jvy Wallet`.
- Launcher assets retain the existing resource paths and density matrix, with a red rotated mark for the adaptive, legacy, and monochrome variants.
- `settings.gradle.kts` now identifies the root project as `JvyWallet`.
- Android application ID remains `com.ivy.wallet.debug`; package names, database identifiers, and Ivy import/source identifiers were intentionally not migrated.
- ADR: `docs/adr/0012-jvy-wallet-branding.md`.
- Verification: `assembleDemo --offline --no-daemon --no-configuration-cache --max-workers=2 -x :app:uploadCrashlyticsMappingFileDemo` passed.
- APK: `E:\AI\joravar-ivy-wallet\app\build\outputs\apk\demo\app-demo.apk`.
- APK SHA-256: `861A34E6447A946CE837456D842D1A6DC6E1D8CC0A74E7350727D0043DD53670`.
- APK metadata label verified as `Jvy Wallet`.

**W1-SAVINGS — Monthly savings insight: COMPLETE**

- The Home screen now shows a `Monthly savings` card beneath the existing cash-flow summary.
- It calculates `income - expenses` with `BigDecimal`, then explains one of three beginner-friendly states: saving, break-even, or spending exceeds income.
- The domain calculator also exposes a savings-rate percentage plus zero-income and negative-income explanations for later surfaces.
- Focused coverage includes positive savings, fractional exact arithmetic, overspending, break-even, zero income, and negative income.
- The card is presentation-only and reads existing Home statistics; it adds no database, import, backup, or package migration.
- QA/compatibility record: `docs/recon/R1-SPRINT-1-VERIFICATION.md`.
- Full demo APK build passed after Home integration.

**W1-SAFETY — Safe-to-spend and emergency-fund coverage: COMPLETE**

- The Home screen now shows `Safe to spend`, calculated as current balance minus the configured protected buffer.
- The Home screen now shows `Safety cushion`, calculated as protected buffer divided by current-period expenses and displayed in months covered.
- Both cards explain the result for beginners, including zero, below-buffer, below-one-month, and three-plus-month states.
- The calculations use `BigDecimal`; non-terminating coverage ratios use a deterministic `DECIMAL128` context rather than binary floating point.
- The domain and UI calculations are presentation-only and read existing Home balance, buffer, and expense state. No database, import, backup, package, or schema changes were made.
- Multiple agents worked in parallel with disjoint ownership: safe-to-spend domain, emergency-fund domain, safe-to-spend UI, and emergency-fund UI. Home integration remained orchestrator-owned.
- Focused domain/UI test coverage was added for exact subtraction, buffer overrun, zero expenses, fractional coverage, and coverage boundaries.
- Verification: `assembleDemo --offline --no-daemon --no-configuration-cache --max-workers=2 -x :app:uploadCrashlyticsMappingFileDemo` passed on 2026-10-14.
- APK: `E:\AI\joravar-ivy-wallet\app\build\outputs\apk\demo\app-demo.apk`.
- APK metadata label verified as `Jvy Wallet`; application ID remains `com.ivy.wallet.debug` for Ivy-compatible upgrades.

**W2-GOALS — Goals, account reservations, and funding guidance: COMPLETE / TARGETED VERIFIED**

- Added persisted goals for emergency funds, purchases, sinking funds, and custom targets.
- Added multi-account allocations so a laptop, phone, or emergency fund can be funded from one or more named accounts without creating fake transactions.
- Added exact reservation, remaining-target, per-account-available, safe-to-spend, goal-progress, and emergency-month calculations in the domain layer.
- Added Room schema version 131 with an import-safe migration from version 130; existing Ivy tables and import identifiers are unchanged.
- Added a Goals destination, goal cards, create-goal flow, goal details, and reserve-money flow to the Jvy Wallet UI.
- Reserve-money UI shows the amount still available after other goals have blocked money in the same account; allocations are clamped to available account capacity.
- Home safe-to-spend now subtracts actual active goal reservations. The legacy global buffer target is no longer treated as already-saved money, preventing false results such as `155 months covered` from a target alone.
- UI implementation follows the mobile taste review: one clear primary action, generous spacing, high-contrast dark surfaces, cyan/green progress for healthy progress, amber for due-soon, and red only for blocked or invalid states.
- Verification: `:app:assembleDemo --offline --no-daemon --no-configuration-cache --max-workers=2 -x :app:uploadCrashlyticsMappingFileDemo` passed on 2026-10-14.
- Added deadline-aware funding guidance: remaining amount, required amount per whole month, and `Complete`, `Past due`, `Needs AED X/month`, or `No deadline` status.
- Number formatting is grouped consistently for goal amounts and monthly requirements; goal screens retain the 24dp mobile content gutter.
- Targeted verification passed: `:shared:domain:testDebugUnitTest` and `:feature:goals:testDebugUnitTest`, including Goals Paparazzi light/dark snapshots.
- No connected Android device/emulator was available for an APK screenshot or interaction pass.
- APK: `E:\AI\joravar-ivy-wallet\app\build\outputs\apk\demo\app-demo.apk`.
- ADR: `docs/adr/0013-goals-and-account-reservations.md`.

**W2-CASHFLOW — Money Coming Up forecast: COMPLETE / TARGETED VERIFIED**

- Added a default 30-day forecast using current balances, active goal reservations, planned income, and planned expenses.
- Forecast output includes available balance now, upcoming income, upcoming expenses, lowest projected available balance, projected shortfall, and forecast-based Safe to Spend.
- Goal reservations are deducted once; unallocated goal targets do not reduce available money. Transfers are intentionally excluded from this first version.
- Planned payments remain projections and the forecast never creates, edits, or skips transactions.
- Added an expandable Home card titled `Money coming up`, with plain-language explanations and a safe fallback to the current goal-aware available balance when no events exist.
- Targeted verification passed: `:shared:domain:testDebugUnitTest`, `:feature:home:testDebugUnitTest`, and Home debug/release Kotlin compilation.
- Domain calculations use `BigDecimal`; legacy persisted `Double` values are adapted at the boundary.

**W2-CATEGORY — Remember My Categories: COMPLETE / TARGETED VERIFIED**

- Added offline Room-backed category rules created from edited transactions.
- Rules normalize transaction titles, match case-insensitively by title substring, and choose deterministically by priority/specificity.
- Future transaction entry can suggest or preselect the remembered category; historical transactions and Ivy imports are never silently rewritten.
- Added disable/delete support and Room schema version 132 with an import-safe migration.
- Targeted verification passed: `:feature:edit-transaction:testDebugUnitTest` and `:shared:data:core:testDebugUnitTest`.
- No import mapping behavior was modified.

**W2-INTEGRATION — Release candidate verification: COMPLETE / DEVICE QA PENDING**

- Demo APK build passed on 2026-10-15 with `:app:assembleDemo --no-daemon --no-configuration-cache --max-workers=2 -x :app:uploadCrashlyticsMappingFileDemo`.
- APK: `E:\AI\joravar-ivy-wallet\app\build\outputs\apk\demo\app-demo.apk`.
- APK SHA-256: `1BE31B1455B778962751BDD95488F6A8EAEE79D79AA1C59D9FA7847DAC7CDADD`.
- The full unfiltered/Hilt aggregation and Ivy import test suites remain inherited environment blockers; targeted feature tests and packaging pass.
- No connected Samsung Galaxy S22 Ultra was available in this build environment, so Android 16 / One UI 8 interaction and screenshot QA remains pending.

**W2-UI — Home dashboard hierarchy pass: COMPLETE / RENDER VERIFIED**

- Reframed Home around one primary decision: `Safe to spend` appears immediately after the balance and is no longer duplicated inside the forecast card.
- Replaced the oversized monthly savings gradient with a compact neutral `This month` summary showing Income, Spent, and Saved using grouped numbers.
- Reduced green gradient usage to positive emphasis instead of using it for every insight surface; supporting cards now use the existing neutral surface palette.
- Reordered the dashboard to `Balance → Safe to spend → Money coming up → This month → Recent transactions`.
- Made the no-events forecast state actionable in plain language: users are told to add a planned bill or income to improve the forecast.
- Added `112dp` bottom content padding to the Home lazy list so the floating bottom bar cannot obscure the final transaction.
- Paparazzi dark/light renders were inspected after the change; Home tests and Kotlin compilation passed.
- Updated demo APK built on 2026-10-15: `E:\AI\joravar-ivy-wallet\app\build\outputs\apk\demo\app-demo.apk`.
- Updated APK SHA-256: `6C6B37F9B01EE723B9554FA4B20A724C3AB44E9258B7E93D1F24DE418215014B`.

**W2-HOME-TRENDS — Adjustable Home trend and editor: IMPLEMENTED / VERIFIED**

- Replaced the duplicate current-month savings summary with `Money trends`, a monthly Income / Spent / Saved line graph.
- Added 3m, 6m, and 12m range controls with no minimum-history requirement; unobserved months remain calendar slots and render as honest gaps rather than invented zero activity.
- Added a local Home editor so users can hide or restore individual trend series while the app preserves at least one visible measure.
- Added a single-month explanation, grouped currency values, hidden-balance masking, accessibility descriptions, and dark/light Paparazzi coverage.
- Persisted the selected range and visible series in the existing DataStore. No database, ledger, import, or backup schema changes were made.
- Focused verification passed after serializing Gradle runs: Home compile, Home unit tests, and `verifyPaparazziDebug`.
- Official numbered demo APK built after verification: `Jvy_wallet_v0.2.2.apk` / Android version code `208`.
- Artifact: `E:\AI\joravar-ivy-wallet\artifacts\apk\Jvy_wallet_v0.2.2.apk`.
- SHA-256: `01A654143DE2B7113E05216B4FAEEE6C4AC789DC1BDEE5EF6ADCB0BCE38AC9A9`.

**W2-HOME-TRENDS-SPARSE-DATA — Honest chart semantics: IMPLEMENTED / VERIFIED**

- Fixed sparse-history rendering so missing months are represented as null/unobserved points and are never joined by a line.
- Added an explicit y-axis scale with compact grouped labels. Non-negative financial series use a zero baseline; negative values are allowed to extend below zero only when observed.
- Single-period data renders as points without a fabricated line and explains that a second month is needed before direction can be inferred. Empty history keeps the existing first-activity empty state.
- Latest summary values now use the latest observed period instead of a trailing empty calendar slot.
- Added a populated Paparazzi fixture covering a missing month between the selected range and observed data.
- Focused verification passed: Home Kotlin compilation, Home unit tests, and Home Paparazzi recording.
- Official numbered demo APK: `Jvy_wallet_v0.2.2.apk` / Android version code `208`; packaging completed successfully after the sparse-data fix.

**W2-HOME-TRENDS-DATE-AND-LOADING — Calendar boundaries and stable refresh: IMPLEMENTED / VERIFIED**

- Trend periods now use true calendar-month boundaries (`1st` through the end of the month), independent of a user's custom budgeting-period start day. This prevents an October 1 transaction from appearing under a September-labeled trend period.
- Range changes retain the previous chart while the new range is calculated and show a compact `Updating` overlay instead of replacing the chart with a short loading message and collapsing the card.
- Added a Paparazzi fixture for the range-change loading state; focused compile, unit tests, and snapshot recording passed.
- Official numbered demo APK: `Jvy_wallet_v0.2.3.apk` / Android version code `209`.
- SHA-256: `0CE0BBA8B8D85F94FEB9175627D3F223CC9D3B885A9840B1362AC099242207A9`.

**W2-UI-MASHUP — BudgetBakers-inspired modular Home: SUPERSEDED BY W2-HOME-FOCUS**

- Inspected the supplied BudgetBakers Wallet 9.3.12 split bundle and screenshot from a read-only copy; the newer bundle targets API 36 and includes the arm64-v8a split needed by the Samsung S22 Ultra Snapdragon.
- Used the useful product pattern—accounts-first orientation, independent dashboard cards, and user-controlled card visibility—while keeping Jvy's own calculations, visual language, privacy behavior, and Ivy import compatibility.
- The earlier account snapshot experiment was intentionally removed from Home because the Accounts tab is the authoritative account surface.
- Added a Home dashboard editor with stable card IDs, persisted DataStore visibility preferences, and a restore-defaults action.
- Added grouped number formatting, hidden-balance masking, long-name ellipsis, 24dp internal card spacing, and a horizontal account strip to prevent the dense dashboard wall seen in the reference app.
- Targeted verification passed: `:feature:home:testDebugUnitTest` with 27 tests and Home production compilation.
- Reconnaissance record: `docs/recon/BUDGETBAKERS-UI-REFERENCE-AND-JVY-MASHUP.md`.
- ADR: `docs/adr/0015-budgetbakers-inspired-modular-dashboard.md`.
- Follow-up candidates: expense-structure card, planned-payments card, and drag-to-reorder after the visibility model proves useful on device.
- Official numbered demo APK: `Jvy_wallet_v0.2.4.apk` / Android version code `210`.
- APK SHA-256: `0910414CBC4EE3FBA6E0827447EE47250E6FC021A79B870E8A4FED550116EFD5`.

**W2-HOME-FOCUS — Focused dashboard redesign: IMPLEMENTED / TARGETED VERIFIED**

- Removed the redundant Home account snapshot and ignored the legacy `accounts` dashboard preference without a migration; Accounts remains unchanged.
- Added adaptive `7D`, `30D`, `3M`, `6M`, and `12M` trends. Daily buckets use local dates; monthly buckets use explicit `YearMonth` identities. Pre-activity periods are gaps and post-activity quiet periods are zeroes.
- Replaced large Home transaction cards with a compact, bounded recent-activity surface showing at most seven records, planned-payment actions, and a `View all` route. The full Transactions screen remains unchanged.
- Added `Where your money went`: top three expense categories plus `Other`, ranked bars, grouped amounts, percentages, category colors, reports navigation, and an honest no-spending state.
- Added pure breakdown tests and trend coverage for daily/monthly ranges, October 1 boundaries, sparse history, and single-period rendering.
- Documentation updated: Mobile UI standards, BudgetBakers mash-up notes, ADR 0016, and this status file.
- Verification: `:feature:home:compileDebugKotlin` and `:feature:home:testDebugUnitTest` pass with 34 tests completed; the Home Paparazzi report was generated; and the official demo APK build completed successfully. Samsung S22 Ultra QA remains pending because the local ADB daemon could not start.

**W2-HISTORY-AND-TRENDS — Editable history and intra-month fidelity: IMPLEMENTED / VERIFIED**

- Home `View all` now opens an explicit all-transactions mode instead of the compact Home-only projection. Existing month navigation can be used to reach September and tap any entry to edit it without changing the full Transactions screen's behavior.
- Restored category icons in compact Home activity rows and kept the rows bounded and tap-target friendly.
- Trend analytics now exclude opening-balance `Adjust balance` entries and transfers, while preserving genuine dated income and expense records in the ledger and history screen. This prevents opening balances from creating false peaks while keeping real September expenses editable.
- Monthly ranges retain calendar-month context but plot local-date samples inside observed months. Post-activity quiet days are real zeroes, pre-activity periods remain gaps, and date-based x positions prevent a two-point straight-line chart from hiding intra-month peaks and troughs.
- Trend summaries use the selected range totals rather than an empty trailing day, and the chart keeps a stable measured height while refreshing.
- Accounts now show goal-reserved and available amounts beneath each account's monthly activity summary.
- Focused verification passed: Home, Transactions, and Accounts Kotlin compilation; Home unit tests; and Home Paparazzi verification. Official numbered demo APK built as `Jvy_wallet_v0.2.7.apk` / Android version code `213`.
- Device QA remains pending because a Samsung Galaxy S22 Ultra / Android 16 / One UI 8 device was not connected to the build environment.

**W2-HISTORY-CRASH-FIX — View all startup path: VERIFIED**

- Fixed the remaining Home `Recent activity → View all` crash. The navigation flag was present, but the Transactions ViewModel startup dispatcher did not handle `allTransactions` and fell through to the legacy `no id provided` error.
- The all-history route now initializes the complete ledger before rendering, while preserving month navigation and tap-to-edit behavior.
- Focused Home/Transactions compilation and Home tests pass; the official numbered demo APK is `Jvy_wallet_v0.2.8.apk` / Android version code `214`.

**W2-HISTORY-PERIOD-ISOLATION — Home chart independence: VERIFIED**

- Fixed transaction month navigation mutating the shared legacy `selectedPeriod` used by Home. History now uses a local pure month cursor, so switching to September and returning Home cannot move or corrupt the Home trend anchor.
- Focused Home/Transactions compilation and Home tests pass; the official numbered demo APK is `Jvy_wallet_v0.2.9.apk` / Android version code `215`.

**W2-DENSITY-REDESIGN — Cross-screen normal-density UI: IMPLEMENTED / TARGETED VERIFIED**

- Applied the newer Home density baseline to Accounts, Goals, and the full Transactions shell: reduced oversized spacing and amounts, tightened summary/goal rows, reduced decorative border weight, removed the transaction-screen height-based spacer, and preserved navigation/action targets.
- Renamed the trend and monthly summary `Saved` metric to `Net change` while keeping the persisted `SAVINGS` key for DataStore compatibility. The value remains income minus spending and is no longer presented as money actually set aside.
- Updated `docs/guidelines/Mobile-UI-Standards.md` with cross-screen density and trend-semantics rules for future UI work.
- Targeted compilation, Home tests, and official APK packaging passed. Device screenshot QA remains pending.

**W2-BALANCE-TREND-DENSITY-FOLLOWUP — Balance semantics and legacy-card reduction: IMPLEMENTED / SOURCE VERIFIED**

- Replaced the misleading purple `Net change` series with `Balance`, calculated as the end-of-period wallet balance from the existing ledger-aware balance action. The old persisted `SAVINGS` preference key maps to `Balance` so existing users keep their chart selection safely.
- Monthly points no longer use daily income-minus-expense samples that created artificial salary spikes. Pre-activity periods remain gaps; later quiet periods retain real period-end balance values.
- Reduced the full Transactions income/expense header to a compact summary row with lightweight add actions.
- Reworked Accounts cards into compact neutral rows with a colored account accent, balance, reserved amount, available amount, and monthly flows without a large colored header panel.
- Reduced Balance trend chart height and internal padding to match the newer normal-density Home language.
- Added balance aggregation regression coverage and updated the mobile UI standards with balance-versus-net-worth rules.
- Targeted Home, Accounts, and Transactions compilation plus Home unit/Paparazzi tests pass. No APK was generated because APK creation requires an explicit build command; device QA remains pending.

**RELEASE-STANDARD — Numbered APK generation: COMPLETE**

- Added `version.properties` as the authoritative semantic version and Android version-code source.
- Added `scripts/build-jvy-apk.ps1` as the only official workflow for APKs intended for installation, QA, sharing, or release.
- Numbered artifacts use `Jvy_wallet_v<major>.<minor>.<patch>.apk`; the Android `versionCode` increases by one after each successful official build.
- The current release is `0.2.12` / version code `218`; the next official patch build will increment to `0.2.13` / version code `219`.
- Failed builds do not consume numbers. Test and compile-only Gradle tasks do not consume numbers.
- Documentation is in `README.md`, `AGENTS.md`, and `docs/standards/APK-GENERATION.md`.
- Validation: APK script syntax passed; Accounts, Goals, Home, and Transactions debug compilation passed; Home unit/Paparazzi verification passed; and the official demo APK packaging completed successfully on 2026-10-15.
- Current artifact: `artifacts/apk/Jvy_wallet_v0.2.12.apk`.
- Current artifact SHA-256: `436B32CF129C1B7ABBE22E8393C08DC27149BE51488D708F0F824D6F304CDC3B`.

## Workspace

- Working copy: `E:\AI\joravar-ivy-wallet`
- Upstream remote: `https://github.com/Ivy-Apps/ivy-wallet.git`
- GitHub fork: `https://github.com/joravarsinghing/ivy-wallet` (forked from `Ivy-Apps/ivy-wallet`)
- Published branch: `joravar/wave-0-control-plane` (current changes pending commit/push).
- Initial branch: `main` tracking `upstream/main`
- Fork control branch: `joravar/wave-0-control-plane`
- Integration base before control-plane docs: `710733dbe4729a437d573cdfb8429c50e617ded1`
- Control-plane documentation commit: `04ec41256cffce3db97c34f1ed24e0c1c5e62e5c`
- Upstream checkout is not modified by this workspace.
- The project mirror's `sources/` remains read-only; the user supplied the artifacts at `E:\` and they are copied into `docs/source-of-truth/` with verified matching hashes.
- Isolated baseline toolchains: Java 17 at `E:\AI\toolchains\jdk17`; Android SDK API 34/build-tools 34.0.0 at `E:\Android`; Gradle cache at `E:\AI\gradle-cache`.

## Source-of-truth status

| Artifact | Status | Action |
|---|---|---|
| `JORAVAR_IVY_WALLET_PRD.md` | Present at `docs/source-of-truth/` | SHA-256 matches `E:\JORAVAR_IVY_WALLET_PRD.md`. |
| `JORAVAR_AGENTIC_ROADMAP.md` | Present at `docs/source-of-truth/` | SHA-256 matches `E:\JORAVAR_AGENTIC_ROADMAP.md`. |
| Ivy Wallet upstream repository | Present | Pinned to commit recorded in the Wave 0 report. |

## Reconnaissance workstreams

| ID | Scope | Owner | Write scope | Status |
|---|---|---|---|---|
| R0-BUILD | Gradle, modules, dependency topology | Ramanujan | None; report only | Complete |
| R0-IMPORT | Ivy CSV/backup import and compatibility invariants | Dirac | None; report only | Complete |
| R0-DATA | Data, domain, Room, money model | Halley | None; report only | Complete |
| R0-VERIFY | Tests, CI, tooling, baseline commands | Fermat | None; report only | Complete; environment blocked |
| R0-UI | Navigation and product/UI flows | Hume | None; report only | Complete |

Recon agents must not edit production files, shared contracts, or control-plane files. The orchestrator owns consolidation.

## Non-negotiable constraints

1. Ivy Wallet import compatibility is a release gate, not a best-effort feature.
2. Never edit, rename, move, or delete the original upstream checkout.
3. No Wave 1 implementation before baseline build/tests and source-artifact recovery status are recorded.
4. Parallel work uses disjoint write scopes and frozen contracts; architectural changes require an ADR before implementation.
5. Financial calculations must preserve exactness and explainability; compatibility adapters must isolate legacy representations.

## Exit criteria for Wave 0

- [x] All five reconnaissance reports received and consolidated.
- [x] Repository status, branches, remotes, commit, module graph, and build system recorded.
- [x] Ivy import paths, schemas, fixtures, and invariants recorded with exact file references.
- [x] Baseline build and test commands executed or blocked with evidence.
- [x] Source PRD and roadmap copied into a non-synced project-controlled location with matching hashes.
- [x] Branch/worktree conventions and initial ADRs recorded.
- [x] Wave 1 branding packet completed under the documented low-risk exception.
- [x] Wave 1 monthly savings insight completed with import-safe integration.
- [x] Wave 1 safe-to-spend and emergency-fund insights completed with import-safe integration.
- [x] Wave 2 Goals/Saving Pools domain, persistence, navigation, and initial UI packet implemented.

## Gate failures / blockers

- `testDebugUnitTest` reaches `:feature:accounts:hiltAggregateDepsDebugUnitTest` and then remains silent for several minutes; a second run reproduces the stall. No source changes were made to work around it.
- The focused compatibility run reaches `:feature:import-data:hiltAggregateDepsDebugUnitTest` and reproduces the same stall, so Ivy import tests have not completed.
- The first unfiltered `assembleDemo` run completed local build/shrink/package work but failed at the external `:app:uploadCrashlyticsMappingFileDemo` task because DNS could not resolve `firebasecrashlyticssymbols.googleapis.com`.
- No emulator/device baseline was established, so connected instrumentation remains unrun.
- The Goals/Saving Pools focused unit-test run reaches `:shared:domain:hiltAggregateDepsDebugUnitTest` and stalls like the existing repository-wide test runs.
- Primary visual QA device supplied by the user: Samsung Galaxy S22 Ultra, Snapdragon, Android 16, One UI 8.

## Reconnaissance records

- `docs/recon/R0-BUILD-TOPOLOGY.md`
- `docs/recon/R0-IMPORT-COMPATIBILITY.md`
- `docs/recon/R0-DATA-FINANCIAL.md`
- `docs/recon/R0-UI-FLOWS.md`
- `docs/recon/R0-VERIFICATION.md`
- `docs/adr/0001` through `docs/adr/0011`
- `docs/adr/0012-jvy-wallet-branding.md`
- `docs/adr/0013-goals-and-account-reservations.md`
- `docs/recon/W2-GOALS-SCREENSHOT-REVIEW-2026-10-14.md`

## Next orchestrator actions

1. Install the redesigned demo APK on the Samsung Galaxy S22 Ultra test device and validate Home hierarchy, bottom-bar clearance, forecast expansion, goal sheets, and transaction entry visually across Android 16 / One UI 8.
2. Diagnose the Hilt unit-test aggregation stall, beginning with Gradle/Hilt worker diagnostics rather than application edits.
3. Add goal monthly contribution and on-track calculations.
4. Add category-scoped savings rules only after contribution semantics are specified.
5. Add goal lifecycle actions and historical progress snapshots.

## Screenshot-fix verification

The following screenshot-driven fixes are implemented on the current branch and included in the successful demo build:

- More menu uses the Goals reservation summary and opens the Goals screen.
- Goals quick access uses a balanced three-column layout and a dedicated target icon.
- Goal sheets have safe horizontal padding.
- Goal copy consistently describes account reservations rather than saved transactions.
- Visible fork sharing/open-source copy points to Jvy Wallet and `joravarsinghing/ivy-wallet`.

Build command: `:app:assembleDemo --offline --no-daemon --no-configuration-cache --max-workers=2 -x :app:uploadCrashlyticsMappingFileDemo`

Build result: `BUILD SUCCESSFUL` on 2026-10-14. Device validation is still pending because the connected Samsung test device was not available to the build environment.

## W2-REPORTS-FILTER-REVAMP — Independent filters and useful defaults

- Reports now open directly on the current app period with all transaction types, accounts, and categories included; the previous blank “choose every filter first” state is removed.
- Filter normalization treats an empty dimension as “all,” so a user can select only a month, expense type, account, category, amount, keyword, or tag and receive an updated report.
- The filter sheet now uses its in-progress draft consistently for period, amount, keyword, tag, account, and category controls. Selecting one item from the default all-selected set narrows that dimension directly.
- Invalid amount ranges remain rejected, while valid filters with no matching records keep the report summary visible and show the existing no-results transaction state.
- Added `ReportFilterTest` coverage for default period/type normalization, single-type filters, and invalid amount ranges.
- Verification: `:feature:reports:testDebugUnitTest` passed after the change. No APK was generated for this source-only fix.

## W2-TREND-GEOMETRY — Cohesive daily/monthly chart domain

- Monthly chart points now use the same first-to-last bucket anchor domain as daily charts. The previous month-end axis extension left the final monthly point visibly inset from the right edge.
- Grid lines and plotted points use the same small, consistent horizontal inset across all ranges, with clamped positions for sparse or single-point data.
- No financial values or bucket semantics changed; this is a presentation geometry correction only.
