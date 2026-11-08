# V2 account feedback checkpoint — 31 October 2026

Continuation of [the transactional storage checkpoint](004-v2-account-transaction-checkpoint-2026-10-31.md).
V2 remains **in progress**, not public-release accepted. Preserve all existing
worktree changes. No APK, signing/version allocation, commit/push/tag/release or
golden promotion is authorized by this continuation.

## Implemented

- Real account write results reach Main/Accounts creation, transaction/planned
  editors, all-transactions account editing, onboarding (forms and suggestions),
  and nested loan/repayment account forms. These are acknowledgements of account
  writes, **not** completion of loan/repayment persistence migration.
- Account forms close only after success. Pending Save, name/icon/colour,
  currency, amount, inclusion checkbox and Close are disabled using native
  enabled state. Input callbacks also check live state before mutation. Progress
  and politely announced generic errors use shared Jvy controls and typography.
- A conflict offers Done/Close and review guidance instead of another write.
  Missing/deleted accounts cannot be recreated by a stale edit or creation retry.
  The terminal state lives with the submission controller, not an exception or
  financial payload in saved state. Storage exception messages are not displayed.
- Main's account modal now belongs to its ViewModel. Main and planned-payment
  dismissal checks the originating modal ID. Other nested/local modal hosts still
  need the restoration work listed below. Refreshes are entry-scoped in the
  transaction/planned/loan paths; idling counters use finally where applicable.
- Creation uses the modal's stable transient UUID and a deterministic opening
  transaction UUID. Within the Room transaction, a replay must match the original
  account definition and opening entry; it acknowledges that creation without
  resetting a balance changed by later activity. Changed drafts require review,
  not silent overwrite. A required opening entry is verified before commit.
- The opening entry still uses the existing income/expense adjustment calculation
  and title; only its UUID is supplied explicitly. Native records remain the same
  schema. Zero openings do not invent a transaction. Creation-intent data is not
  added to database/backup models or credential state.
- One-click account **and category** suggestions keep a live VM-owned intent for
  retries, then start fresh after acknowledgement. Reusing a suggestion after
  renaming an added item must not overwrite that item. Late completion cannot
  clear a newer intent. Onboarding account suggestions also retain progress/error
  state and disable Back/Skip/Next/other selections during the write.
- Existing-account edits retain the established absolute-target balance semantics
  and are serialized by the coordinator. This is not a new persisted per-edit
  exactly-once token or a general metadata compare-and-swap protocol.

## Skills and verification strategy

UI/UX Pro Max's focused form-submit/error-recovery guidance informed native
disabled controls, visible save state, retained drafts and actionable conflict
copy. Android testing guidance informed separate writer/controller regressions,
adaptive production-form renders and actual Room tests. Existing Hilt/JUnit4,
MockK/Paparazzi/Kover and Compose 1.6.8 / Material 3 1.2.1 remain unchanged; no
framework installation, navigation migration or dependency upgrade was needed.

- New regressions cover creation replay, changed draft/target, later activity,
  deleted/missing accounts, rollback/retry, simultaneous submissions, required
  opening verification, supplied opening UUID, acknowledged errors/refresh
  failure, retained modal state and stale suggestion completion.
- Account pending, recoverable error and terminal-review candidates cover all
  eight existing windows (including AMOLED, 2× text, square fold, short landscape
  and DeX). Renders are not interaction tests or owner visual approval.
- Real Room coverage now has seven instrumented cases, including replay with
  later activity and stale deletion. Compilation passed in the final gate;
  connected execution remains pending, not counted among local unit tests.
- `account-feedback-focused-2026-10-31.log` is an intermediate compile/test pass.
  The first full attempt was stopped to fix the completed-suggestion/rename edge
  case; it is not counted as final verification. Latest source is being checked
  by `artifacts/verification/account-feedback-complete-2026-10-31.log`; all
  1,569 unit/render tests passed there, but lint rejected the modified checkbox's
  composable parameter order. That helper and its positional call were corrected
  without changing the baseline. The final rerun is
  `artifacts/verification/account-feedback-verified-2026-10-31.log`.
- **Final gate passed:** `:app:compileDebugKotlin testDebugUnitTest
  :shared:data:core:compileDebugAndroidTestKotlin :app:lintDebug` (9m 43s),
  **1,569 tests / 153 suites**, zero failures, errors or skips. Seven Room cases
  compiled but were not device-run or included in that count. App lint passed
  against its unchanged baseline; inherited baselined findings remain separate.
- Inspected pending/AMOLED, failed-save/200% text and terminal-review/short-
  landscape production-form candidates. Error/recovery actions are reachable
  while fields scroll. Copies and limits are indexed at
  `artifacts/ui-review/account-feedback-2026-10-31/README.md`; no goldens changed.
- `git diff --check` passed. Version remains 0.4.4 / 228, old APK timestamps
  remain October 26 release / October 21 debug, and no APK was produced. No
  database/backup schema, signing identity or dependency changed in this batch.

## Remaining work / next continuation

1. Finish local modal-host and process-death restoration: transaction and
   all-transactions account forms, onboarding forms and nested loan/repayment
   forms still have local hosts. Save primitive nonsensitive drafts/IDs, never
   credentials, imported payloads or entire database object graphs. Do not
   restore a busy flag without an actual owning operation.
   **Confirmed next-priority lifecycle gap:** `OnboardingViewModel.start` calls
   `initiateSettings` and `router.splashNext` every time the screen starts, and
   `initiateSettings` resets `_currency` to the default. Guard initialization by
   navigation-entry identity, preserve explicit restart/Back behavior separately,
   and test recreation at Currency/Accounts/Categories including pending saves.
   Retained submission/pool objects alone are not proof this flow restores.
2. Run connected Room tests and actual phone/fold/DeX journeys for pending
   rotation, IME, keyboard/TalkBack, Back, terminal conflict, failed edit/retry,
   creation acknowledgement lost around commit, unrelated activity/reorder/import
   and large-wallet performance. Unit/render success is not device acceptance.
3. Review stale metadata/concurrent absolute-balance edit semantics before any
   stronger exactly-once/concurrent-edit claim. The creation replay contract is
   deliberately not an account-merge or currency-conversion mechanism.
4. Complete loan/repayment and tag persistence acknowledgements, full privacy,
   accessibility, localization and import acceptance from the execution ledger.
   New English recovery/helper copy still needs localization review.
5. No release certification or APK is implied by this checkpoint. If an APK is
   later requested, only the official numbered owner-signed workflow is valid.

Use the Java 17/offline verification environment from previous checkpoints. The
final gate is app compilation, root unit/render tests, data-core Android-test
compilation and app lint. Do not grow baselines or record goldens to hide failures.
