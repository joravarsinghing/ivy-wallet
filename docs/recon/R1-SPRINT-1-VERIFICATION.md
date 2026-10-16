# R1 Sprint 1 — Savings Insight Verification

Status: QA/documentation report only; no production, importer, test, or Gradle files changed.

Date: 2026-10-13

Workspace: `E:\AI\joravar-ivy-wallet`

## Scope

Sprint 1 introduces a savings insight over imported and newly recorded transactions. The insight must be a read-only consumer of the existing financial data. It must not reinterpret, repair, deduplicate, or rewrite Ivy import data as a side effect.

This report defines the compatibility invariants and the verification required before accepting the next Jvy Wallet APK.

## Code inspected

- `feature/import-data/src/main/java/com/ivy/importdata/csv/domain/CSVImporterV2.kt`
- `temp/legacy-code/src/main/java/com/ivy/legacy/domain/deprecated/logic/csv/CSVImporter.kt`
- `shared/domain/src/main/java/com/ivy/domain/usecase/csv/IvyCsvRow.kt`
- `shared/domain/src/main/java/com/ivy/domain/usecase/csv/ReadCsvUseCase.kt`
- `shared/domain/src/test/java/com/ivy/domain/usecase/csv/ExportCsvUseCasePropertyTest.kt`
- `docs/recon/R0-IMPORT-COMPATIBILITY.md`
- `docs/recon/R0-VERIFICATION.md`
- `ROADMAP_STATUS.md`

## Ivy CSV compatibility invariants

These are release-gate invariants. A savings insight change fails verification if any of them changes, even when the insight itself appears correct.

### File shape and parsing

1. The Ivy export header remains exactly 15 columns, in this order:

   `Date, Title, Category, Account, Amount, Currency, Type, Transfer Amount, Transfer Currency, To Account, Receive Amount, Receive Currency, Description, Due Date, ID`

2. Ivy CSV exports are UTF-16. The import flow must continue to accept the files produced by Ivy without requiring the user to resave them.

3. CSV quoting, commas inside titles/descriptions, empty optional fields, and the header row must retain their existing behavior. The newer importer drops the header before mapping rows; the legacy importer skips it in the CSV reader.

### Amount and transaction direction

4. Expense and income amounts are interpreted as non-negative magnitudes. Transaction type carries direction; a savings calculation must not infer direction from a changed sign convention.

5. Transfer rows use transfer-specific fields and must remain transfers, not income or expense:

   - source account: `Account`
   - destination account: `To Account`
   - source amount/currency: transfer fields appropriate to the selected import path
   - destination amount/currency: receive/to-amount fields appropriate to the selected import path
   - `Amount` may be zero for Ivy transfer exports

6. Source and destination accounts must remain distinct when the CSV identifies distinct accounts. Transfer amounts must not be counted as savings income or spending.

7. The current R0 finding must remain visible in verification: `CSVImporterV2` currently reads the transfer source amount from the mapped `toAmount` field and then reads that field again for `toAmount`. This is a known compatibility defect and must not be “fixed” implicitly by the savings insight. It needs a dedicated importer regression change in a separately owned packet.

8. Zero-amount transactions are not persisted by the observed importers. A savings insight must handle an empty imported transaction set without manufacturing a zero-value transaction or a positive savings result.

### Accounts, categories, and currencies

9. Existing accounts and categories are reconciled by case-insensitive exact name. The savings insight must use the persisted transaction relationships and must not create, rename, merge, or delete these records.

10. Missing accounts/categories may be created by the importer according to the existing import path. The insight must not treat those reconciliation operations as savings activity.

11. Account currency is authoritative. Missing or invalid currency falls back to the configured base currency; transfer destination currency falls back to the source account currency where the importer specifies that behavior.

12. The insight must not add values across currencies without an explicit supported conversion policy. Until FX is implemented for this packet, it must either scope the result to the base currency or present a clear incomplete/multi-currency state.

### Dates, IDs, and persistence

13. Ivy export dates are local ISO date-times without an offset. Existing import behavior converts local time to UTC storage. The insight must group transactions using the product’s documented local reporting period and must not regroup them by raw UTC date in a way that moves transactions across a user’s local day or month.

14. Date parsing remains path-specific. The legacy path supports multiple date formats and has a fallback for unparseable dates; the manual/new path can reject invalid mapped dates. The insight must consume only persisted valid transactions and must not reparse CSV text.

15. Imported transaction IDs and relationship IDs must retain the existing behavior for the selected import path. The insight must not use a newly generated insight ID or calculation run ID to mutate transaction identity.

16. Import result counts (`rowsFound`, `transactionsImported`, `accountsImported`, `categoriesImported`, and failed-row contents/indexes) must be unchanged when the insight is enabled or disabled.

## Savings insight non-interference contract

The implementation should satisfy all of the following:

- Read-only: no writes to transactions, accounts, categories, currencies, settings, or import metadata.
- Deterministic: identical persisted inputs and reporting period produce the same result.
- Explainable: show the period, included transaction types, exclusions, and whether the result is complete across currencies.
- Direction-safe: savings is derived from persisted income and expense semantics; transfers are excluded from income/spending totals.
- Import-safe: enabling the insight before import, after import, or on an empty database does not alter import output.
- Null/empty-safe: no crash and no misleading positive insight when there are no eligible transactions.
- Precision-safe: do not introduce a second rounding or floating-point conversion that changes persisted amounts.
- Period-safe: boundary timestamps are tested in the user’s local timezone, including month-end and daylight-saving transitions where applicable.

## Proposed verification commands

Run from `E:\AI\joravar-ivy-wallet` with the isolated toolchain recorded in `ROADMAP_STATUS.md`:

```powershell
$env:JAVA_HOME = 'E:\AI\toolchains\jdk17'
$env:ANDROID_HOME = 'E:\Android'
$env:GRADLE_USER_HOME = 'E:\AI\gradle-cache'
```

Baseline and focused checks:

```powershell
.\gradlew.bat tasks --offline --no-daemon --console=plain
.\gradlew.bat :ci-actions:base:test --offline --no-daemon --console=plain
.\gradlew.bat :feature:import-data:testDebugUnitTest --offline --no-daemon --console=plain --max-workers=2
.\gradlew.bat testDebugUnitTest --offline --no-daemon --console=plain --max-workers=2
.\gradlew.bat assembleDemo --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain -x :app:uploadCrashlyticsMappingFileDemo
```

The two unit-test commands are currently expected to reproduce the documented Hilt aggregation stall at `hiltAggregateDepsDebugUnitTest`. A timeout or stall is a test-gate blocker, not a passing result. The APK command must exclude the external Crashlytics symbol upload for local verification; an unfiltered build can fail on DNS even after local packaging succeeds.

If a device/emulator is available, add a manual APK smoke pass after the build. Do not classify an APK as accepted solely because `assembleDemo` succeeds.

## Acceptance checks for the next APK

### A. Build and identity

- [ ] `assembleDemo` completes with the documented local command.
- [ ] The APK installs and launches as Jvy Wallet.
- [ ] The savings insight is reachable from the intended user flow and does not block existing transaction/account screens.
- [ ] No import, backup, database, package, or Gradle file was changed as part of the savings insight packet unless separately approved and recorded.

### B. Ivy CSV import regression smoke test

Use a real Ivy export plus a small fixture containing at least one expense, one income, one transfer, an existing account/category with case variation, an unknown account/category, a non-base currency, quoted text containing commas, and an empty optional field.

- [ ] Import succeeds without manual UTF conversion or file rewriting.
- [ ] The exact 15-column header is accepted.
- [ ] Imported row count and failed-row indexes match the importer result.
- [ ] Expense and income magnitudes and directions are unchanged.
- [ ] Transfer source/destination accounts remain linked and the transfer is excluded from savings income and spending.
- [ ] Existing account/category matching remains case-insensitive exact-name matching.
- [ ] Unknown account/category creation remains importer-owned and is not reported as savings.
- [ ] Currency fallback behavior matches the selected importer and no unsupported cross-currency total is presented as exact.
- [ ] Dates remain in the expected local reporting period.
- [ ] Reopening the savings insight does not duplicate or modify imported transactions.

### C. Savings insight correctness

Seed a controlled dataset with known values, for example: income 2,000, expense 1,250, transfer 500, and an excluded/unsupported-currency transaction.

- [ ] The displayed savings result is 750 for the supported single-currency case.
- [ ] The 500 transfer does not inflate income or expense and does not alter the 750 result.
- [ ] Boundary transactions are assigned to the correct local period.
- [ ] Repeated calculation produces the same value and explanation.
- [ ] Empty data, only transfers, only expenses, and unsupported multi-currency data produce safe, understandable states.
- [ ] The insight’s refresh/recompute path causes no database writes outside its own explicitly approved presentation/cache boundary.

### D. Before-release evidence

- [ ] Attach the exact APK path, SHA-256, build command, and environment.
- [ ] Record import fixture name and before/after transaction/account/category counts.
- [ ] Record expected versus observed savings values for each fixture.
- [ ] Record any Hilt stall, Crashlytics DNS failure, emulator issue, or other blocker as a blocker; do not convert it to a pass.
- [ ] QA signs off only after the focused importer test gate is resolved or explicitly waived by the orchestrator with an ADR.

## Current blocker and ownership boundary

The focused import-data unit-test task currently stalls at `:feature:import-data:hiltAggregateDepsDebugUnitTest`; the full unit-test task reproduces the same Hilt aggregation stall. This report does not alter that harness. The next engineering packet should diagnose the harness separately from the savings insight so that a test failure cannot be confused with a product regression.

The savings insight owner may consume existing repositories/use cases and add isolated calculation/UI code, but must not modify the CSV importer or compatibility adapters in the same packet. Any importer correction requires a separate contract-first change, regression fixtures, and an updated compatibility report.
