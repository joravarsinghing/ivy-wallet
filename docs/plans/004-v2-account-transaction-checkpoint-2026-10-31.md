# V2 account transaction checkpoint — 31 October 2026

Later continuation: [account feedback checkpoint](004-v2-account-feedback-checkpoint-2026-10-31.md).
The UI/retry limitations below record this earlier storage-only batch.

Continuation of [the account validation checkpoint](004-v2-account-safety-checkpoint-2026-10-30.md).
V2 is **not complete or public-release accepted**. This batch changes the storage
boundary, not account UI acknowledgement or retry identity. Preserve the dirty
worktree and stop after the integrated verification below.

## Implemented

- `AccountWriteRepository` runs account metadata and the existing balance
  adjustment together in `IvyRoomDatabase.withTransaction`. Both account creation
  and edit/adjust paths in `AccountCreator` use it. The coordinator serializes
  these writes; no compensating account deletion is used.
- Account reads inside the boundary bypass normal memoization. A coroutine
  context marker follows dispatcher changes, so the transaction mapper sees the
  staged account/currency without leaking it into the ordinary read cache. Its
  transaction-local read cache is seeded with the staged account and discarded
  afterward; repeated ledger mapping does not repeat SQL metadata queries.
- Account cache publication/events happen after the transaction returns.
  Read caches are invalidated on every exit, including cancellation racing a
  commit acknowledgement, so they must reload durable data rather than retaining
  a possibly obsolete fully loaded list.
- Memo generations reject late single/list reads after invalidation or a newer
  successful write/delete. Short synchronized map operations protect publication;
  no suspension, database or event emission occurs under that lock. The exposed
  `items` map is now a snapshot rather than a mutable backing-map view.
- The existing adjustment mapper, calculations, finite-number validation,
  currencies, financial history and stored formats are retained. A new account's
  temporary legacy representation reuses its allocated order instead of querying
  MAX again after inserting the row. No schema/version/dependency change.

## Evidence and testing strategy

The Android `testing-setup` guidance informed the distinction between orchestration
tests and actual SQLite tests. Existing Hilt 2.52, JUnit4, MockK 1.13.12, Paparazzi
1.3.3 and Kover 0.8.3 are retained. This is a Compose app with local unit/render
tests and AndroidJUnit4 instrumented database tests; no testing framework, DI
runner, coverage plugin or screenshot stack was replaced or installed.

- Nine coordinator tests use a transaction fake and real repository/mappers to
  check rollback orchestration, cache/event timing, fresh staged reads across
  dispatchers, metadata failure, cancellation, lost acknowledgement and write
  serialization. A 1,000-iteration mapping case checks bounded metadata reads and
  isolation between successive writes. They do **not** prove SQLite rollback or
  real-device large-wallet responsiveness.
- Five memo regressions cover invalidation and late reads racing invalidation,
  update or deletion. Existing writer validation/failure tests now verify the
  creator delegates the whole operation to the coordinator.
- Four AndroidJUnit4 tests use a real in-memory Room database for opening/edit
  rollback, successful dual persistence and precommit cancellation. Compilation
  passed in the final gate; connected execution remains **pending**, not passed.
- The initial focused gate passed before the final memo-generation changes;
  `artifacts/verification/account-atomic-focused-2026-10-31.log` is intermediate
  evidence only. The first integrated run was deliberately interrupted after
  review found repeated SQL reads in the uncached path; it is not a final pass.
  Final integrated verification **passed** in
  `artifacts/verification/account-atomic-final-2026-10-31.log` (14m 31s):
  `:app:compileDebugKotlin testDebugUnitTest
  :shared:data:core:compileDebugAndroidTestKotlin :app:lintDebug`.
  **1,522 tests / 150 suites**, zero failures, errors or skips. The four connected
  tests were compiled, not executed or counted among those unit/render tests.
  App lint passed against its unchanged baseline, not a claim that all inherited
  baselined findings are fixed. Source remained frozen during this final run.
- `git diff --check` passed. Version remains 0.4.4 / 228, old APK timestamps
  remain October 26 release / October 21 debug, and no new APK was produced.
  No schema, signing identity, baseline, golden or dependency changed.

## Remaining work / next continuation

1. Run the real Room tests on an authorized connected device/emulator before
   claiming device-backed rollback acceptance. Do not build/install an APK under
   this continuation's no-APK constraint just to obtain that evidence.
2. Complete account UI result callbacks through Main/Accounts, transactions,
   planned payments, onboarding suggestions and nested loan/repayment modals.
   Preserve wealth-review return paths. Pending inputs/Close, retained errors,
   dirty dismissal, entry/modal-scoped completion and process restoration remain
   open. Current legacy forms still close optimistically. Check stale edits after
   account deletion so an old modal cannot accidentally resurrect a deleted row.
3. Add stable creation-intent IDs and define retry behavior after a lost commit
   acknowledgement. Atomicity prevents a half-write before commit; it does not
   make a fresh random-ID creation retry idempotent. Postcommit event/refresh
   failure must not be mistaken for permission to create another account.
4. Check interactions with reorder/import/other writers and actual device
   cancellation/lifecycle journeys. The coordinator serializes its own callers,
   not every unrelated writer in the app. A discarded late read can still return
   its old result to its original caller; generation guards prevent cache
   contamination, not every feature's stale UI-result application.
5. Continue loans/repayments, tags, full restoration and the surface acceptance
   matrix only after this bounded storage slice has a clean verified checkpoint.

The transaction callback is only for the related ledger write/calculation. Do not
call other memo-publishing repositories from inside it and assume their cache or
events roll back. The existing balance path writes transactions directly and
only account reads need the scoped bypass here.

No APK, signing/version allocation, commit/push/tag/release, baseline growth or
golden promotion is authorized. Use the Java 17/offline verification environment
from the preceding checkpoint; no owner credentials are required for compile,
unit tests or lint.
