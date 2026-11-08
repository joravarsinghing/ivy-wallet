# Cross-session integration audit — 1 November 2026

This consolidates the interrupted priority/reliability and spacing work. It is
not public-release acceptance. Preserve the existing worktree, app identity,
0.4.4 / 228 version, data/backup formats and owner-signing workflow. No APK,
commit, push, tag, release or golden promotion is part of this continuation.

## Integration findings corrected

- Modern and 17 titled legacy adapters share a persistent title/Close row and
  24dp content alignment. Native pending/disabled controls and their original
  save/dismiss callbacks are retained; no replacement navigation framework.
- Recurrence controls stack at narrow/large-text widths; month choices use equal
  columns; search/name/colour labels align. Month/icon selections retain primitive
  live drafts. The [spacing review](../design/SPACING-REVIEW-2026-10-31.md) records
  the concrete screenshot findings rather than inferring quality from compilation.
- Clearing/mistyping a recurrence count previously retained the preceding valid
  number. Invalid input now propagates an invalid count, preserves its editable
  raw text, explains how to correct it and prevents recurrence confirmation.
  Report `last N` periods also reject zero/negative counts. Existing explicit
  calendar and open-ended/all-time periods remain valid. No stored range or
  transaction is rewritten.
- Loan details retained two 96dp footer-compensation spacers. The list now owns
  navigation-bar clearance and a 24dp final inset; actual populated/private/error
  candidates cover all eight adaptive windows. Review exposed a squeezed
  borrowed/lent label producing a tall, nearly vertical word and a large title
  gap. Name/type/date now stack within a bounded column; repayment/interest
  metrics wrap and use their measured contrast foreground. The legacy styled
  detail header is not falsely described as a full neutral-card redesign.
- Source-contract tests still expected an inline transaction route guard after
  that guard moved to the stricter route/revision policy. They now check the
  actual call and policy. Audited external Kotlin files are explicit test inputs,
  preventing stale up-to-date source-contract results after another module changes.
- The icon selector's composable parameter order was corrected without a lint
  suppression or baseline expansion. Its runtime callers use named arguments;
  the positional preview was updated too.

## Read-only cross-checks

- Reviewed the execution ledger and per-surface matrix together, keeping old
  checkpoints historical rather than treating each session's partial result as
  a complete release pass.
- Checked root route-scoped saved state, capture protection and security/widget
  refresh wiring. Credential fields are not added to saved state or financial
  backups. Rotation/configuration state is not the same as process-death proof.
- Checked actual loan detail/list/nested-account acknowledgement wiring, current
  linked-write/conversion boundaries and queued transaction feedback policy.
- Checked backup read snapshot and preference allowlist, manifest backup/cleartext
  restrictions and signing separation. No new dependency, runtime diagnostics,
  application ID, key or version change was introduced.
- Searched scoped production paths for unfinished implementations and financial/
  authentication logging. Remaining fake-DAO TODOs are referenced by tests, not
  treated as evidence that a user-facing production route is implemented.
- Ran the full local compilation/unit/render/lint gate, not just the newly added
  tests. Its final result and candidate gallery are recorded below.

## Verification status

The prior spacing gate passed 1,804 unit/render tests but failed the icon-selector
parameter-order lint. `spacing-reviewed-2026-11-01.log` was deliberately stopped
after the integration follow-ups above; it is not a final pass. The next stable
full run included recurrence validation and loan-detail candidates too.

That stable integrated gate passed:
`artifacts/verification/v2-integrated-final-2026-11-01.log` (14m 15s). The
loan-detail preview then exposed the name/type wrap issue above; the correction
and modifier-owned common root were reverified with the same full command:
`artifacts/verification/v2-integrated-reviewed-2026-11-01.log` (1m 56s, 2,119
tasks; 34 executed, 2,085 up-to-date).

Final aggregate: **1,842 unit/render tests / 169 suites**, zero failures, errors
or skips. App Kotlin compilation, Android/Room-test Kotlin compilation and app
lint passed against the unchanged inherited baseline. This is not a claim that
the repository has no inherited lint debt. New validation tests cover invalid
count propagation and report-period acceptance; loan-detail candidates cover
populated, private and retryable missing states across all eight windows.

Selected final candidates were inspected, including 200% text, AMOLED and short
landscape, and saved in the [review gallery](../../artifacts/ui-review/spacing-2026-10-31/README.md).
Its before-images explain the defects; they are not approved layouts. The design
skills informed the shared 8/16/24dp rhythm, bounded/growing controls, readable
errors and adaptive window review without changing fonts or upgrading the stack.

`git diff --check` passed. Database schemas, screenshot references, lint baseline,
version and old APK outputs remain unchanged. No connected device was available
(`adb devices -l` empty); the ten targeted Room cases are compiled, not executed.
No APK, commit/push/tag/release or signing-key operation occurred.

## Remaining public-release gates

Full navigation/modal restoration after process death, exhaustive page/child
control accessibility/localization/privacy review, and source-specific real-export
import acceptance remain open implementation/acceptance work. No screenshot or
mocked transaction test closes them.

Ten targeted Room rollback/replay cases are compiled-only until device execution.
IME/Back/TalkBack, physical fold/DeX resizing, auth/recovery/capture, widgets,
notifications, performance, signed upgrade and owner visual acceptance need their
actual environment and authority. No connected device means unverified, not passed.

Use the [execution ledger](004-public-release-readiness.md),
[acceptance matrix](../releases/UI-ACCEPTANCE-MATRIX.md) and
[release checklist](../releases/PUBLIC-RELEASE-CHECKLIST.md) as the canonical open
worklist. Do not mark full v2 or the public release complete from this report.
