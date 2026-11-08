# V2 restoration continuation — stopped at owner's request

Stopped on 1 November 2026 when the owner reported 5% usage remaining. This is
a resume checkpoint, **not full-v2 or public-release acceptance**. Preserve the
existing large dirty worktree. Do not generate an APK, commit/push/release, change
signing/version/schema, expand lint baselines or promote screenshot goldens.

## Changes made in this continuation

- Added bounded, versioned primitive navigation keys for all registered routes,
  preserving distinct visit IDs and Back history. Selected transactions save IDs,
  not financial record graphs; history/pie reload the current rows, preserving an
  explicit empty selection rather than widening it to all records.
- Root startup validates live route identifiers and setup/legal boundaries before
  publishing restored navigation. It does not restore authentication or replay a
  consumed widget Add intent. Storage startup failure has retry/exit; refused
  saved contexts have a review notice. Reporting period and root tab are saved.
- Added a bounded primitive draft codec and one-time restoration holder. A second
  interruption during recovery retains its original payload until consumed.
- Transaction/planned editors restore stable draft IDs and editable primitives,
  check their committed baseline, reject missing records and acknowledge an
  already committed creation instead of adding it twice. Existing-edit lookup
  no longer silently becomes a new record. Transaction transfer display avoids
  zero division/nonterminating decimal division. Late events are ignored while
  an explicit save is pending.
- Planned payment saving freezes the submitted rule, accepts valid one-time rules
  without recurrence fields, and commits rule/generated entries atomically through
  `PlannedPaymentWriter`. Identical retries do not regenerate instances; missing
  accounts/categories or stale edits require review. Deletion is acknowledged and
  atomic too. Strict generation rejects an unmappable entry instead of reporting
  a partial success; existing background callers retain their compatibility mode.
- Added descriptor recovery for shared account/category/tag hosts, budgets, loans
  and repayments. Records are reread; original records, pending controllers and
  callbacks are not serialized. Missing/stale/currency-changed contexts are refused.
  Parent recovery must resolve before child fields compose, otherwise a temporary
  null modal can consume and reset their saved values. Nested loan account hosts
  follow that same ordering. Loan amount currency remains explicit on recovery.
- Setup restores its last step without preloading defaults or completing setup.
  Net-worth date/currency/range restore; goal save acknowledgements are scoped to
  the live VM session instead of waiting for an obsolete revision after death.
- Net-worth export now uses the shared private-data warning and an immutable
  dated summary/history request. The CSV remains in live VM memory through
  rotation, never in saved state. A result without its request after process death
  reports interruption instead of exporting another view or a blank file.

## Resumed verification: passed

The subsequent continuation completed the full verification command. The first
resumed run passed compilation/tests but failed `ComposeModifierMissing` on
`JvyEditorLoading`. Its public API now exposes and forwards a default Modifier;
no suppression, baseline expansion or intentional layout change was made.

`artifacts/verification/v2-restoration-lint-fixed.log` passed in 16m 6s (2,123
tasks): app compilation, all unit/render tests, Android/Room-test compilation and
app lint against the unchanged inherited baseline. Aggregate: **1,930 tests /
182 suites**, zero failures, errors or skips. The existing Hilt/JUnit/MockK/
Paparazzi/Kover stack was retained, following the Android testing skill.
This closes the interrupted local verification gate, not the remaining work
below, device acceptance or owner approval of new candidate images.

### Historical interrupted runs

The baseline before this continuation remains the 1,842-test integrated pass in
`004-v2-integration-audit-2026-11-01.md`. Do not apply that result to the new code.

`v2-restoration-foundation-2026-11-01.log` passed the initial foundation checks.
Intermediate bootstrap/focused/integrated runs failed after source changes crossed
their dependency-compilation boundaries; they are not final passes.

`artifacts/verification/v2-restoration-final-2026-11-01.log` compiled the app and
passed eight actual `RootRestorationTest` cases, then failed compiling a loan test
because of a cross-module nullable-property smart cast. That assertion was fixed
using an explicit non-null access. Later export/validator/event-guard edits were
also made; those are not verified by that earlier run.

The latest full run is
`artifacts/verification/v2-restoration-verified-2026-11-01.log`. It was interrupted
at the owner's stop request before completion (its final tail reached transaction
unit-test preparation). The Gradle client and daemon have stopped. **It has not
passed.** Resume by inspecting its tail and rerunning the full command below.
Resolve compilation/tests/lint without growing baselines. New tests cover codecs,
root startup, live-route validation, planned writer/lifecycle, nested descriptors,
loan/budget restoration and export identity; loading/retry candidates were added
to the eight-window planned-editor render suite. Inspect the actual resulting
candidate images; no new golden has been approved.

```bash
env -u SIGNING_STORE_FILE -u SIGNING_STORE_PASSWORD -u SIGNING_KEY_ALIAS \
  -u SIGNING_KEY_PASSWORD -u JVY_SIGNING_STORE_BASE64 \
  JAVA_HOME=/workspace/jdk-17.0.20.1 ANDROID_HOME=/workspace/android-sdk \
  ANDROID_SDK_ROOT=/workspace/android-sdk ANDROID_USER_HOME=/workspace/.android \
  GRADLE_USER_HOME=/workspace/.gradle \
  ./gradlew :app:compileDebugKotlin testDebugUnitTest \
  :shared:data:core:compileDebugAndroidTestKotlin :app:lintDebug \
  --offline --no-daemon --no-configuration-cache --max-workers=2 \
  -Dorg.gradle.jvmargs='-Xmx4g -Dfile.encoding=UTF-8 -Djavax.net.ssl.trustStore=/workspace/codex-java-cacerts' \
  -Pkotlin.compiler.execution.strategy=in-process
```

Run one Gradle process at a time. Android tests are compile-only: no connected
device is available. `git diff --check` passed at the stop. Database schemas,
version 0.4.4 / 228, inherited lint baseline and old APK timestamps are unchanged.
No installable APK, Git publication or credential operation occurred.

## Remaining work / review findings

- Complete the verification above and inspect new recovery/error renders. Add
  real interaction/SaveableStateRegistry restoration tests where needed; pure
  codec and mocked atomic tests are not proof of Android lifecycle/SQLite behavior.
- Continue the full ledger, acceptance matrix and exhaustive accessibility/privacy
  review. Report applied/draft filters, search/query UI and ancillary settings/rate
  dialogs still need process-restoration review; do not call all modals complete.
  Search currently uses unsaved text and unguarded concurrent results. Reports
  initialize their default filter on screen composition; the existing saved-report
  decoder can drop missing selected IDs and normalize an empty result to all.
  Its saved period representation also needs review for explicit all-time ranges.
  These observations were not changed in this continuation.
- Imports: real source exports, transfer joins/timezones/currency reconciliation
  and interrupted storage acceptance remain open; sanitized fixtures are not
  source-version-wide verification. No genuine vendor files were invented.
- Device-only gates remain: real Room rollback/replay, IME/Back/TalkBack,
  resize/fold/DeX, authentication/recovery/capture, widgets/notifications,
  performance, private signed upgrade and owner visual approval.

Read `AGENTS.md`, relevant local Android/UIUX skills and the mandatory design
language before resuming. The current stack is retained; no Navigation3/Compose
upgrade or testing-framework migration is authorized by a skill recommendation.
