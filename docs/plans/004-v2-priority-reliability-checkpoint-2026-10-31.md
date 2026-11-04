# Priority reliability continuation — 31 October 2026

This is a working record, **not** release acceptance or a reason to stop the
owner's requested continuation. Preserve the dirty worktree. No APK, version
allocation, signing, golden promotion, commit/push/tag/release is authorized.

## Implemented in this continuation

- Onboarding initializes once per navigation entry, loads its persisted currency,
  preserves the live step/submission on recreation and offers settings-read retry.
  Preloaded accounts/categories finish before the next step/setup completion.
- Onboarding, transaction and history account/category hosts are VM-owned; loan
  and repayment modal data retain their nested account host on date-data copies.
  Keypad and recurrence drafts have primitive savers and recurrence dirty Close.
- `AtomicWriteRepository` coordinates one Room transaction and deferred memo
  invalidation/events, including nested operations and cancelled acknowledgement.
  Memo reads inside the transaction bypass pretransaction cached values.
- Loan/repayment creators validate finite positive values, require live parents
  for edits, use stable creation IDs, acknowledge actual write results and retain
  errors. Linked wallet entries and deletion of a loan's records are part of the
  same transaction. Committed repayment replays don't obtain a different quote.
- Transaction-side linked-loan edits now use the same atomic boundary. Draft
  acknowledgement flags and optional widget updates follow commit; failures
  cannot leave background-processing feedback stuck.
- Loan and repayment forms use shared typography/buttons/shapes, responsive
  date/time controls, native pending/terminal disabled controls, polite errors,
  dirty Close, stable selected-account IDs and one-time amount-picker opening.
  Missing loan details have Back, error and retry instead of a blank page.
- Tag create/edit/delete and selection propagate real acknowledgements. Creation
  IDs and original timestamps survive retry; concurrent changed tags require
  review. Deletion includes associations atomically, never transactions. Editors
  retain their live host; Edit is visible instead of long-press-only. Search work
  cancels the preceding query and publishes only to its originating route.
- Loan lists/details refresh on return without resetting an open form's date.
  Summaries group outstanding principal by currency instead of adding different
  units under a base-currency label. Hidden mode redacts the top summaries too.
  Summary grouping is presentation-only and doesn't change stored calculations.

## Verification history and final result

- `onboarding-lifecycle-2026-10-31.log`: passed actual VM recreation/retry tests.
- `loan-tag-focused-2026-10-31.log`: seven atomic and twelve initial loan tests
  passed; tag tests exposed a test-only MockK inline-wrapper argument cast,
  corrected by reconstructing `NotBlankTrimmedString` from the underlying String.
- `priority-reliability-full-2026-10-31.log` was stopped for the selection follow-up.
- `priority-reliability-verified-2026-10-31.log` compiled all changed production
  modules and passed preceding modules, but stopped at a loan render fixture's
  missing required `Account.color`. The fixture is corrected. This is not a
  completed verification pass. The later summary/privacy, return-refresh and
  transaction-side atomic changes are not certified by that log.
- Earlier full gate: `artifacts/verification/priority-reliability-final-2026-10-31.log`:
  `:app:compileDebugKotlin testDebugUnitTest
  :shared:data:core:compileDebugAndroidTestKotlin :app:lintDebug`.
  No lint baseline expansion or screenshot-reference replacement is allowed.
- That run finished with **1,693 unit/render tests / 161 suites, zero failures,
  errors or skips**, and ten targeted Room tests compiled, not executed. Lint
  found one missing modifier on the modified onboarding error host. Its modifier
  is now forwarded through both content/error layouts; it was not suppressed.
- The final follow-up is implemented: unavailable foreign quotes raise a safe,
  actionable error; principal edits read the current linked entry inside the
  atomic operation; metadata-only edits preserve its instant across timezone
  changes. A genuinely changed loan date updates the associated entry. Queued
  transaction writes freeze the submitted draft before suspension and scope
  postcommit flags/dismissal to their original entry and write revision.
- The environment disconnected before verification of that follow-up could
  start. After reconnection, the latest full gate was started as
  `artifacts/verification/priority-money-safety-verified-2026-10-31.log` using the
  same compilation/root-tests/Room-test-compilation/lint command. Its result is
  recorded below; earlier test counts do not verify later edits.
- That recovered run passed **1,717 unit/render tests / 164 suites**, including
  missing-rate recovery renders, all with zero failures/errors/skips. Room tests
  compiled. Lint rejected two uses of the onboarding modifier across alternate
  branches; both branches now share one root layout that consumes it once.
  No baseline entry was added. Older autosave feedback is also guarded by both
  route identity and write revision, with four policy regressions.
- Completed full gate:
  `artifacts/verification/priority-reliability-complete-2026-10-31.log`.
  Passed in 3m 7s, including newly added tag-selector renders and stale-feedback
  policy tests: **1,745 unit/render tests / 165 suites**, zero failures/errors/skips.
- The final fixture-only correction makes loan currencies, cards and summaries
  reconcile and marks Completed fixtures fully repaid. The same full command
  passed again in 1m 9s:
  `artifacts/verification/priority-final-reviewed-2026-10-31.log` (2,119 tasks;
  nine executed, 2,110 up-to-date). Aggregate remains **1,745 / 165**, all passing.
  App compilation, Room-test compilation and app lint passed. Lint uses the
  unchanged inherited baseline; this does not mean the entire repository is
  warning-free or baseline-free.
- New production-form candidates: loan/repayment pending, error and terminal
  review and missing-rate recovery (seven states × eight windows), tag
  pending/error/review (three × eight), tag selection (three × eight), plus hidden
  loan summaries at all eight windows. These are not interaction tests.
- Three additional real Room tests cover loan/entry rollback and tag/association
  rollback/deletion. Compile only; seven existing account Room tests were also
  compile-only. No connected device is present (`adb devices -l` was empty).
- Final fictional previews were inspected and copied into the
  [review gallery](../../artifacts/ui-review/priority-reliability-2026-10-31/README.md):
  200%-text errors/selection/currency summaries, AMOLED pending/private states
  and short-window review/empty selection. No goldens were promoted. Android
  testing guidance drove real callback/rollback boundaries and render coverage;
  UI/UX guidance retained native disabled controls, explicit recovery and the
  existing shared typography/shapes rather than adding a new visual system.

## Remaining work — not full-v2 acceptance

The [1 November integration audit](004-v2-integration-audit-2026-11-01.md) and
execution ledger supersede this historical checkpoint's latest-run status.

1. The priority compile/test/lint and selected-candidate review gates are passed.
   Missing-FX, current-principal lookup and frozen-draft tests pass; this does not
   certify every legacy ambiguous-rate record or broader concurrent-edit policy.
2. Execute real Room rollback/replay and UI lifecycle journeys on a device. Ten
   targeted Room cases are compiled, not executed; no device was available.
3. Full process-death navigation
   restoration, source-specific real-export import acceptance, exhaustive
   localization/privacy/accessibility and hardware journeys remain separate open
   work. Don't describe compile/renders as full-v2 completion.
4. Owner device/visual acceptance, numbered private APK, signed upgrade and
   distribution/cohort gates still require their actual devices/authority.

Use Java 17, the workspace SDK/Gradle cache/truststore, offline verification and
unset signing variables as in previous checkpoints. One Gradle process at a time.
Version remains 0.4.4 / 228 unless an explicitly authorized numbered build occurs.
