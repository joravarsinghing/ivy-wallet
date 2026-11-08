# V2 account-safety checkpoint — 30 October 2026

Later storage-boundary continuation:
[31 October checkpoint](004-v2-account-transaction-checkpoint-2026-10-31.md).
The two-write limitation below records the state at this earlier checkpoint.

Latest continuation of [the category checkpoint](004-v2-checkpoint-2026-10-30.md).
V2 remains **in progress**, not public-release ready. This is a deliberately
bounded financial-validation foundation, **not** completion of account form
acknowledgement or partial-write recovery. Stop after verification; do not start
loans/repayments or tags in this session.

## Changes

- Account creation/editing rejects nonfinite target balances before saving
  account details. Invalid account-domain conversion throws instead of returning
  from the IO block and then invoking the success refresh.
- Balance adjustment rejects nonfinite current/target balances and overflowing
  differences before constructing a transaction. Failed transaction mapping is
  now an explicit failure, not a silently omitted adjustment.
- Valid adjustment direction, amounts, account, currency, timestamp, zero/no-op
  behavior and existing fiat tolerance are unchanged. Negative finite balances
  remain supported. Storage failures and cancellation propagate as before.
- Thirteen new regressions exercise those cases using the real transaction
  mapper plus repository test doubles. No UI, callback signature, schema,
  operation identity, currency conversion or ledger formula changed here.

## Verification

Integrated verification **passed**: `:app:compileDebugKotlin testDebugUnitTest
:app:lintDebug`, **1,508 tests / 148 suites**, zero failures, errors or skips.
Log: `artifacts/verification/account-safety-integrated-2026-10-30.log`
(10m 22s). App lint passed against its unchanged baseline; this does not mean
all inherited baselined findings have been remediated. The initial focused test run
caught test-fixture constructor omissions and an exception-identity assertion
that did not allow coroutine stack recovery to copy an exception across IO;
those fixtures/assertions were corrected, not production behavior bypassed.

The previous verified baseline is 1,495 tests / 146 suites, app compilation and
app lint; see the category checkpoint. No new UI renders are required for this
nonvisual patch; existing candidates are not device or owner acceptance.

`git diff --check` passed. Version remains 0.4.4 / 228. Old APK timestamps remain
October 26 (release) and October 21 (debug); no new APK was produced. No baseline,
schema, golden, signing identity or stored format changed. Physical-device and
end-to-end account recovery acceptance remain pending.

## Important remaining boundary

Account save still writes account metadata and then a separate balance-adjustment
transaction. A failure in the second step can leave the first write committed.
The regression explicitly demonstrates that boundary rather than pretending it
is atomic. Do not delete the account to "undo" it: it may already own financial
records or have other consumers. Existing account UI still has optimistic legacy
callbacks, so failure propagation alone does not provide user-facing recovery.
No public candidate should be certified on the strength of these tests.

## Next safe batch

1. Read AGENTS.md, this note, the category checkpoint, design language and relevant
   Android/UI skills. Preserve the existing dirty worktree and root navigation.
2. Design the complete account-write boundary before adding retries: metadata,
   opening/adjustment transaction, repository cache/event publication, stable
   intent IDs, cancellation, and ambiguous completion. Check actual repository
   behavior; do not assume the category-only upsert pattern makes two writes
   atomic. Do not change financial history, currency units or database schema.
3. Wire acknowledged results end to end through Main/Accounts, transaction and
   planned editors, all-transactions editing, onboarding (including suggestions),
   and loan/repayment nested account modals. Check wealth-review return paths.
   Disable actual inputs/dismissal during pending saves, retain errors/drafts,
   and scope completions to the originating entry/modal.
4. Add actual write-boundary/failure/retry tests and pending/error adaptive
   candidates; compile, run regressions and lint. Full modal-host/process-death,
   localization, accessibility, privacy/import and physical-device acceptance
   still remain open in the execution ledger and surface matrix.

No APK, version increment, private signing, commit, push, tag, release or golden
promotion was requested. Use the verification environment/commands in the
category checkpoint. Build only after a later explicit APK request, using the
official numbered owner-signed workflow.
