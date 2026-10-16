# Joravar Agentic Roadmap Status

Last updated: 2026-10-15 (Asia/Dubai)

## Current gate

**Wave 0 — Reconnaissance: COMPLETE / Wave 1 product packets: COMPLETE / Wave 2 product packets: IMPLEMENTED / VERIFICATION PASS IN PROGRESS**

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
- Added 3m, 6m, and 12m range controls with no minimum-history requirement; empty months are omitted rather than rendered as invented zero activity.
- Added a local Home editor so users can hide or restore individual trend series while the app preserves at least one visible measure.
- Added a single-month explanation, grouped currency values, hidden-balance masking, accessibility descriptions, and dark/light Paparazzi coverage.
- Persisted the selected range and visible series in the existing DataStore. No database, ledger, import, or backup schema changes were made.
- Focused verification passed after serializing Gradle runs: Home compile, Home unit tests, and `verifyPaparazziDebug`.
- Official numbered demo APK built after verification: `Jvy_wallet_v0.2.1.apk` / Android version code `207`.
- Artifact: `E:\AI\joravar-ivy-wallet\artifacts\apk\Jvy_wallet_v0.2.1.apk`.
- SHA-256: `7FDB62A48829585069059C30E21E0FCFAC7304E23035FAFFD14B9A890EE5FF32`.

**RELEASE-STANDARD — Numbered APK generation: COMPLETE**

- Added `version.properties` as the authoritative semantic version and Android version-code source.
- Added `scripts/build-jvy-apk.ps1` as the only official workflow for APKs intended for installation, QA, sharing, or release.
- Numbered artifacts use `Jvy_wallet_v<major>.<minor>.<patch>.apk`; the Android `versionCode` increases by one after each successful official build.
- The current release is `0.2.1` / version code `207`; the next official patch build will be `0.2.2` / version code `208`.
- Failed builds do not consume numbers. Test and compile-only Gradle tasks do not consume numbers.
- Documentation is in `README.md`, `AGENTS.md`, and `docs/standards/APK-GENERATION.md`.
- Validation: APK script syntax passed; Home compile/unit/Paparazzi verification passed; and the official demo APK packaging completed successfully on 2026-10-15.

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
