# V2 import reliability continuation

This is a bounded continuation, not certification of the entire public release.
No APK, signing/version/schema changes, publication or screenshot-reference
promotion are part of this work. Existing worktree changes are preserved.

## Implemented

- Both legacy source adapters and manual CSV mapping use the existing atomic
  Room write coordinator. Account/category creation and transaction writes share
  one commit boundary; storage failure or cancellation escapes that boundary.
  Cancellation is not presented as a successful import or swallowed by the
  legacy import result handler. Manual import failures have a generic result
  instead of an unhandled coroutine exception. No raw financial payload is logged.
- Manual transfers debit the mapped source amount, not the destination amount.
  Destination amount, asset and account remain independent. Invalid dates,
  nonfinite amounts, missing destinations and self-transfers are rejected before
  creating metadata. Legacy transfer destination amounts must be positive too.
- Manual amount parsing does not strip arbitrary text or ambiguous decimal
  separators. Correctly grouped dot-decimal amounts and configured multipliers
  remain supported; overflow is rejected. Date parsing obeys the chosen date
  order and strict calendar validation, without a shared mutable format cache.
- Financisto joins only adjacent transfer halves with matching timestamps,
  different accounts and opposite signed amounts. Either row order is supported;
  the negative half supplies the outgoing account/amount. An unrelated expense,
  same-account row, same-sign row or different timestamp is not consumed.
  Unmappable transactions now appear in failed rows instead of disappearing.
- Legacy local wall times are converted once through the injected time converter,
  using the historical timezone offset. ISO/RFC timestamps with explicit offsets
  retain their instant. Date-only records retain the established local-noon
  convention. Existing stored transactions are not rewritten.
- Planned CSV payments with different due dates have distinct duplicate keys.
  Keys normalize CSV-only destination defaults, discarded completed deadlines
  and implicit same-amount transfers to the actual saved representation, so a
  retry does not create another income/expense. Completing onboarding's manual
  import exits once, not twice.
- Both CSV paths share strict numeric parsing and currency preflight. An existing
  account cannot be implicitly relabelled by a conflicting explicit CSV currency;
  its existing unit wins when the field is omitted. Existing custom assets retain
  their unit. Unsupported new explicit codes are rejected, not replaced with base
  currency. Both transfer sides are checked before metadata creation, and a legacy
  cross-currency transfer requires a receiving amount. Invalid currency lookup no
  longer prints the exception and its supplied input.

Android's testing skill informed the regression approach: retain Hilt, Compose,
JUnit4/MockK, LayoutLib-based Paparazzi and Kover, and distinguish reversible
mocked orchestration from actual SQLite/device evidence. Existing Room tests
remain in the Android-test source set. No testing framework migration or new
dependency was added.

## Verification

The first focused pass (`artifacts/verification/v2-import-atomic-focused.log`)
passed legacy/import unit and render tests plus app compilation in 5m 50s.
It predates the final timestamp/join/fingerprint/navigation follow-up.

The first full integrated pass (`artifacts/verification/v2-import-final.log`)
passed app compilation, all unit/render tests, Android/Room-test compilation and
app lint in 18m 19s. It predates the final canonical-key/shared-currency follow-up.
The final reviewed gate (`artifacts/verification/v2-import-reviewed.log`) passed
in **16m 58s**, 2,123 tasks: app compilation, all unit/render tests,
Android/Room-test compilation and app lint against the unchanged inherited
baseline. Aggregate **1,986 tests / 193 suites**, zero failures/errors/skips.
`git diff --check` passed. No Android device was attached; Room execution remains
blocked, not passed. Version 0.4.4 / 228, database/backup schemas, lint baseline,
screenshot references and existing APK timestamps remain unchanged. No APK,
commit/push/release or signing operation was performed.

The verified checkpoint closes this scoped import reliability batch, not all v2
implementation or public-release acceptance. The outstanding work below remains.

## Outstanding work and acceptance

- Genuine source exports remain necessary to verify source-version column drift,
  locale/encoding, transfer pairing and currency reconciliation. The ten existing
  source-shaped fixtures are synthetic, not real vendor-export acceptance.
- Execute Room rollback/replay tests and interruption journeys on Android.
  Reversible mocked stores prove coordinator use, not SQLite rollback. No Android
  device is attached; Android-test compilation is not connected execution.
- Complete the remaining child-control/accessibility/localization/adaptive and
  exhaustive financial-visibility review listed in the main execution ledger.
  This import batch does not certify untouched UI branches.
- Physical phone/fold/DeX, IME/Back/TalkBack, authentication/capture, system
  surfaces, realistic performance, owner visual acceptance and a future explicitly
  requested same-key signed upgrade remain release gates.

Keep a verified native backup before reconciling old imports. Earlier incorrect
timestamps or transfer values are not automatically migrated; do not advise a
blind reimport or promise every old bad row will match a corrected fingerprint.
