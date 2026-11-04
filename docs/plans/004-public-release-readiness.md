# Public release readiness — execution ledger

Approved direction: owner-signed GitHub APK; English-first; no automatic crash
diagnostics; guard implicit-currency records instead of migrating their schema.
Premium means reliable, accessible and understandable, not a paid tier.

Resumed verification and cross-session integration review: 2026-11-04.

Subsequent owner-requested packaging: [private release 0.5.1 / 230](../releases/0.5.1-candidate.md)
now includes the four corrections below. Official release build succeeded;
actual signature, matching owner identity, non-debuggable manifest, alignment
and checksum checks passed. Temporary private-key copy removed. Source/version
committed locally before handoff; no push, tag or publication. Hardware upgrade
and full public-release acceptance remain unverified.

Latest verified continuation: [four owner-reported corrections after 0.5.0](004-v2-four-fixes-checkpoint-2026-11-04.md).
Thirty real trend buckets now cover 30d and longer ranges, expanded account
actions use readable identifier ink, the native period dialog pins Apply above
root chrome and uses shared calendars, and all app-UI logo backings use the
shared proportional rounded token. An explicit relative-option chip preserves
selection of the unchanged one-week default. Final gate passed in **12m 25s**:
**2,185 tests / 205 suites**, zero failures/errors/skips, app and Android-test
compilation plus lint against the unchanged baseline. Eighty-two added cases
include actual control actions and adaptive candidates, not hardware acceptance.
No new APK/version, commit or publication; the installed 0.5.0 candidate predates
this batch. Device/owner and remaining full-v2 release gates stay separate.

Previous verified continuation: [shared Home date window and requested signed candidate](004-v2-home-window-checkpoint-2026-11-04.md).
The owner approved chart-selected dates for all three Home cards. Their results
now share a date window and latest-request publication; spending drill-down
retains it, and upcoming payment actions stay separate. Large-text Home headings
and amount/action groups reflow without squeezing account labels. Final gate
passed: **2,103 tests / 200 suites**, zero failures/errors/skips, app and
Android-test compilation plus app lint against the unchanged baseline. Official
private release **0.5.0 / 229** built and passed actual signature/identity,
non-debuggable, alignment, manifest and checksum checks. Its certificate matches
0.4.4; the temporary private-key copy was removed. Device/owner and other open
v2 gates remain separate. No push, tag or publication.

Previous verified continuation: [read-only privacy and quiet-history coverage](004-v2-privacy-coverage-checkpoint-2026-11-03.md).
Header/day/section/category/transfer-equivalent leaks are masked through the
reactive policy; hidden net-flow hue and zero-row presence no longer disclose
values. Transfer account labels are bounded/adaptive, native section controls
announce expansion, and tiny crypto equivalents retain significant precision.
Home quiet windows use recorded coverage from settled, nondeleted live-account
entries without loading the full wallet; all dated financial calculations remain
unchanged. Empty-period activity copy no longer implies no transaction ever
existed. The Home date-scope decision was subsequently approved and implemented
in the 4 November continuation above.
Final gate passed in 17m 55s: **2,076 tests / 196 suites**, zero failures/errors/
skips, app compilation, Android-test compilation and app lint against the
unchanged baseline. Ninety new unit/render cases include actual composed text/
semantics across eight windows, quiet/zero-balance coverage and currency display.
Selected candidates were inspected, not approved as goldens. Full v2 still needs
the remaining child-control/privacy review and real-export/device/owner gates.
No APK or publication.

Previous continuation: [CSV import reliability](004-v2-import-checkpoint-2026-11-02.md).
Both CSV paths use the atomic write coordinator; strict shared amount parsing,
currency preflight, independent transfer amounts, conservative signed transfer
pairing, single historical timezone conversion and canonical retry fingerprints
are implemented. Unmappable rows are reported, planned due dates remain distinct,
and onboarding import exits once. Final gate passed in 16m 58s: **1,986 tests /
193 suites**, zero failures/errors/skips, app compilation, Android-test compilation
and app lint against the unchanged baseline. No APK or publication. Remaining
child-control/privacy review and genuine-export/device/owner acceptance stay open;
do not certify full v2 from this scoped batch.

Previous continuation: [report/search recovery](004-v2-report-search-checkpoint-2026-11-01.md).
Applied/draft report filters, Clear/invalid draft distinction, strict saved-query
decoding, all-time periods, latest-request publication and retained search/return
refresh are implemented and verified. Full gate passed in 2m 43s: **1,960 tests /
187 suites**, zero failures/errors/skips, app compilation, Android-test compilation
and app lint against the unchanged baseline. Actual VM lifecycle regressions and
eight-window search error/loading candidates supplement codec/source checks.
Selected candidates are in the linked review gallery, not approved goldens.
Stopped at this verified boundary with headroom; ancillary child-control review,
real-export/device/owner acceptance remain open. No APK or Git publication.

Previous continuation: [restoration checkpoint](004-v2-restoration-checkpoint-2026-11-01.md).
The interrupted navigation/editor/modal recovery and planned-write/export batch
now passes its full local verification gate: **1,930 tests / 182 suites**, zero
failures/errors/skips, app compilation, Android-test compilation and lint against
the unchanged baseline. Log: `artifacts/verification/v2-restoration-lint-fixed.log`
(16m 6s). A missing Modifier API was corrected without suppressing lint or changing
the baseline. Full v2 remains open; use the checkpoint's outstanding review findings
and device/owner acceptance gates. No APK or Git publication was produced.

This ledger separates implementation from evidence. A source change or passing
compile is not device acceptance. Existing navigation work is preserved.
The [per-surface acceptance matrix](../releases/UI-ACCEPTANCE-MATRIX.md) lists
individual pages, nested editors and Android surfaces; review child controls too.

## Work packages

| Package | Implementation | Evidence / acceptance |
| --- | --- | --- |
| Route and nested-flow inventory | Existing navigation inventory; extend below | Device journeys pending |
| Reactive financial visibility | Shared reactive policy, common amounts, planning/loan/report/chart presenters and private widget cache implemented | Full leak/semantics and device sweep remains |
| Read-only privacy and transfer layout | Header/day/section/category/receiving-equivalent masking, hidden hue/zero-presence guards; bounded account labels, native expansion semantics and precision-preserving crypto display | 80 actual-content adaptive/semantics cases plus three format regressions pass; reviewed candidates are not whole-page/device/owner acceptance |
| Home quiet-history coverage and shared scope | Account-filtered coverage retains dated balances through quiet windows; the approved chart window now controls plot/spending/activity together, with fixed-date drill-down and separate upcoming actions | Coverage/query and date-window regressions plus sixteen adaptive scope cases pass; actual Room execution, device interaction and owner acceptance remain pending; final gate/build status in the 4 November checkpoint |
| Reports filter draft and hit testing | Hidden/exiting content removed, Cancel discards, Clear changes draft, Apply commits once | Source checked; real hit-testing/Back/keyboard acceptance pending |
| Base-currency safety in settings/import | DAO write guard and pre-merge checks implemented; no schema migration | Policy and import refusal regressions added; final run below |
| Form restoration, validation, save acknowledgement | Acknowledged Budget/manual-rate/category/account/loan/repayment/tag writes; stable creation IDs; atomic linked-loan writes/tag deletion; retained nested hosts, keypad and recurrence drafts | Priority compilation/unit/render/lint gate passed below; full process-death, device and owner acceptance remain open |
| Account validation and form feedback | Nonfinite targets rejected; real result callbacks, pending/error/review states, stable creation/opening IDs, missing-account guards and safe creation replay implemented | Feedback/startup regressions pass; nested hosts are VM-owned; device journeys, full process-death restoration and broader concurrent-edit semantics remain pending |
| Account transactional storage | Creation/edit commit metadata and balance adjustment in one Room transaction; isolated transaction read cache, publication after commit, invalidation and late-read generations; shared atomic coordinator | Coordinator/memo/replay regressions pass; seven account Room cases plus three loan/tag atomic cases compile, connected execution pending; latest full gate below |
| Shared shapes, bounded/scrollable layouts, accessibility | Legacy bounded host, outlined inputs, control shapes, native checkbox state, reorder alternatives; transaction/planned 600dp hacks removed; onboarding uses shared in-flow shells | Actual management, planned, transaction and onboarding candidates added; owner/device acceptance pending |
| Shared spacing follow-up | One title/Close row for 17 legacy adapters and modern forms; aligned search/name fields, grouped loan controls, growing filters and obsolete footer spacers removed; loan-detail label/metrics wrap | Integrated compilation, 1,842 unit/render tests and lint passed; selected final candidates reviewed; hardware/owner acceptance pending |
| Remove inherited diagnostics/cloud/poll behavior; permissions | Firebase runtime/plugin removed, survey closed, unused permissions/cleartext removed; capture focus policy unified | Actual release dependency graph and merged release manifest checked; network/device acceptance remains |
| Failed deletion and nested Back cleanup | Cache removal follows acknowledged delete; modal registrations unregister only their owned handler, including out-of-order disposal and same-ID replacement | New failure/cancellation and Back ownership regressions pass; hardware lifecycle acceptance remains |
| Account-selector alignment | Real content padding replaces spacer items; stable account keys/ID selection; actual account indices on initial load and refresh, including in previews | Index/identity/reorder/missing-account regressions pass; production-form candidates reviewed below; hardware acceptance pending |
| CSV import reliability | Both paths share atomic storage, strict amounts and currency preflight; separate transfer amounts, signed pairing, exact timezone conversion, canonical retries, failed-row reporting and single onboarding exit; no schema change | 26 added regressions pass within the latest full gate; synthetic source fixtures and reversible stores are not genuine-export or SQLite/device acceptance |
| Documentation, CI and release gates | Checklist/update paths/privacy policy/Java 17/blocking app lint updated; official script retained without obsolete Firebase task | Numbered workflow contracts pass; owner-requested 0.5.0 / 229 private candidate built and APK-verified in the 4 November checkpoint; no publication or device-upgrade certification |

Visual-review follow-up: actual onboarding candidates exposed a large-text amount
clipping defect; the account row now stacks grouped money on a neutral card.
Suggestions use shared outlined controls, currency selection uses one native
search/radio list, and welcome no longer positions content using global screen
pixels. Actual editor candidates exposed wrapped destructive header buttons;
shared headers now own a contextual Actions menu. Legacy identifier foregrounds
use measured AA contrast instead of the old 0.5-luminance heuristic. The root also
refreshes widget privacy after security settings change, not only after opening
the legacy settings toggle. Final regression results are recorded below.

Continuation review caught a regression risk in the initial retained-editor
startup guard: separate new-editor visits can have equal route arguments. Each
navigation visit now has its own identity, restored on Back and unchanged by
configuration changes. Saveable page state and editor initialization use that
identity; pending saves consume system Back, and late completions cannot pop an
unrelated page. Regression cases cover equal destinations, re-entering Add,
discarded entries, pending Back and stale handler disposal. The interrupted
intermediate verification is not counted as a final pass.

## Open implementation work — do not certify the full plan yet

- Execute the compiled Room rollback/replay cases and actual UI interruption
  journeys, including transaction-side linked-loan edits and rate reuse. Local
  coordinator/creator tests pass; mocks and saveable text are not real-database
  or device persistence acceptance.
- Complete modal-host restoration and dirty-dismiss behavior across nested and
  process-recreated flows, including recurrence and selection drafts. Never save
  credentials, imported payloads or full database object graphs into Bundles.
- Finish page-by-page accessibility, localization and adaptive review, including
  short windows, large text, keyboard focus/error announcement and source-specific
  import instructions. Existing shared-token adoption is not whole-app acceptance.
- Complete real-export import verification, including transfer-join edge cases,
  timestamp/timezone round trips, explicit currency reconciliation and interrupted
  storage writes. Sanitized parser examples are not acceptance of every source
  version or locale; do not silently convert unsupported decimal/currency formats.
- Complete actual transaction-editor and onboarding device journeys. Real
  production-form candidates now cover the eight preview windows, including
  transfers, failed saves and seven onboarding states; candidates are not
  interaction tests or owner acceptance.
- Prove all financial display branches respect privacy; current shared redaction
  is intentionally not described as a finished exhaustive security audit.

## Verification record

Spacing follow-up after the verified priority batch:
[spacing review](../design/SPACING-REVIEW-2026-10-31.md). The new shared header
retains existing dismissal/save ownership and the established fonts/theme.
Final verification log: `artifacts/verification/v2-integrated-reviewed-2026-11-01.log`
(1m 56s), following `v2-integrated-final-2026-11-01.log` (14m 15s). Both passed
app compilation, root unit/render tests, Room-test compilation and app lint
against the unchanged inherited baseline. Final aggregate **1,842 tests / 169
suites**, zero failures/errors/skips. Real Room/device execution remains pending.
The [cross-session integration audit](004-v2-integration-audit-2026-11-01.md)
also records recurrence/report-count validation and loan-detail inset follow-ups.
The [review gallery](../../artifacts/ui-review/spacing-2026-10-31/README.md) contains
selected inspected final candidates and clearly labelled before-images. No APK,
version/schema/baseline/golden change or Git publication occurred. This closes the
local priority/spacing verification batch, not the open full-v2 gates above.

Priority continuation on 2026-10-31: onboarding starts once per live navigation
entry and uses the stored currency, with a retryable settings-read failure.
Default categories are written before setup completes. Actual startup tests pass
for repeated starts, Currency/Accounts/Categories recreation, pending suggestion
ownership, failure/retry and a fresh route visit. Nested account/category hosts
now belong to the onboarding, transaction and history ViewModels. Loan/repayment
data copies retain nested account hosts and their live submission controllers.
No credential, import payload or complete database graph is added to saved state.

Loan/repayment writes now acknowledge actual storage and include their linked
wallet transaction in the same Room transaction. Creates reuse their intent IDs;
replays don't repeat a linked payment or reprice a committed repayment. Invalid
nonfinite/conversion values and missing/deleted parents are rejected. Deleting
a loan includes its records and linked transactions. Tag create/edit/delete and
selection have acknowledged error recovery; deletion includes tag associations
atomically without deleting transactions. Native disabled controls, shared
footers, explicit tag Edit, date/time wrapping and politely announced errors
follow the existing design language. Keypad and recurrence drafts use primitive
savers. These source changes are not connected-device or process-death proof.

Logs: `onboarding-lifecycle-2026-10-31.log` passed. Intermediate
`loan-tag-focused-2026-10-31.log` caught a test-only MockK inline-value argument
cast (corrected); the atomic coordinator's seven and loan creator's twelve cases
passed there. `priority-reliability-full-2026-10-31.log` was deliberately stopped
for the tag-selection acknowledgement follow-up, not counted as a complete gate.
The later full gate passed: app compilation, root unit/render tests, Room-test
compilation and app lint against the unchanged inherited baseline. **1,745 tests /
165 suites**, zero failures/errors/skips. Log:
`artifacts/verification/priority-reliability-complete-2026-10-31.log` (3m 7s).
After a test-fixture-only correction aligning loan currencies/paid balances, the
same command passed again in 1m 9s:
`artifacts/verification/priority-final-reviewed-2026-10-31.log`. Ten targeted Room
cases compiled only; `adb devices -l` was empty. No APK, version allocation,
schema/baseline change or golden promotion occurred.

This gate also covers missing-FX recovery, current linked-principal lookup inside
the atomic boundary, preserved metadata-only timestamps, frozen transaction
drafts and route/revision-scoped feedback. Selected final fictional large-text,
AMOLED and short-window candidates are in the
[review gallery](../../artifacts/ui-review/priority-reliability-2026-10-31/README.md).
These are not whole-app interaction or owner acceptance.

Latest continuation:
[priority reliability checkpoint](004-v2-priority-reliability-checkpoint-2026-10-31.md).
Priority verification is complete; the open implementation and device gates
above mean full v2 is not certified.

Previous continuation:
[account feedback checkpoint](004-v2-account-feedback-checkpoint-2026-10-31.md).
This advances account acknowledgement/replay, not full v2 or device acceptance.

- Account-feedback final gate on 2026-10-31 passed: app compilation, root
  unit/render tests, Room-test compilation and app lint; **1,569 tests / 153
  suites**, zero failures, errors or skips. Seven Room tests compiled, not
  device-executed. Log:
  `artifacts/verification/account-feedback-verified-2026-10-31.log` (9m 43s).
  The earlier attempt passed tests but failed a composable parameter-order lint
  check; the helper/call were corrected and rerun without baseline growth.
  Pending/error/review form candidates were inspected at AMOLED, 200% text and
  short landscape. Baselines, schemas, goldens, version and old APKs are unchanged.
  The onboarding startup/recreation gap recorded there is addressed by the later
  VM lifecycle tests above; full process-death restoration remains unverified.

Latest storage continuation:
[31 October account transaction checkpoint](004-v2-account-transaction-checkpoint-2026-10-31.md).
This does not close account UI acknowledgement, stable retry or device acceptance.

- Final account-transaction gate on 2026-10-31 passed: app compilation, root
  unit/render tests, data-core instrumented-test compilation and app lint;
  **1,522 tests / 150 suites**, zero failures, errors or skips. Four real Room
  tests compiled but were not device-executed or included in that test count.
  Log: `artifacts/verification/account-atomic-final-2026-10-31.log` (14m 31s).
  The earlier integrated attempt was deliberately stopped to address repeated
  metadata queries; its partial output is not counted as a final pass. The added
  1,000-iteration mapping regression verifies transaction-local cache reuse, not
  hardware performance. Baselines, schemas, goldens, version and old APKs remain
  unchanged. No APK or Git distribution action was performed.

Latest bounded continuation:
[account-safety checkpoint](004-v2-account-safety-checkpoint-2026-10-30.md).
This adds financial validation only; it does not close the open account-save
acknowledgement or partial-write recovery work package.

- Account-safety integrated gate on 2026-10-30 passed: app compilation, root
  unit/render tests and app lint; **1,508 tests / 148 suites**, zero failures,
  errors or skips. Log:
  `artifacts/verification/account-safety-integrated-2026-10-30.log` (10m 22s).
  Valid adjustment amounts, directions, currency/account/time and existing fiat
  tolerance remain covered. No UI source, schema, version, baseline or golden
  changed in this nonvisual continuation. No device or public-release acceptance
  is inferred from the gate.

Category continuation checkpoint: see
[the resume note](004-v2-checkpoint-2026-10-30.md). Category create/edit now
returns storage acknowledgement through management, transactions, planned
payments and onboarding. The modal retains generic errors, disables editing and
Close during writes, and retries creation with the same intent UUID. The writer
serializes category operations and preserves existing order on retry. Refresh
errors after commit are not reported as storage failure. Retained modal data
and onboarding suggestion state share their live submission controller; nested
local modal-host restoration and process-death behavior are still open.
Transaction/planned refreshes check the originating entry **after** the
suspending fetch and only auto-select if the selection revision is unchanged.
Final integrated category verification is recorded in the checkpoint, not
inferred from earlier passes below.

- Category-integrated gate on 2026-10-30 passed: app compilation, root unit/render
  tests and app lint, **1,495 tests / 146 suites, zero failures, errors or skips**.
  Log: `artifacts/verification/category-integrated-2026-10-30.log`.
  The added category candidates were reviewed at 200% text, short landscape and
  AMOLED; they do not replace device/owner acceptance. Existing lint baseline,
  screenshot goldens, schemas, version and old APK timestamps remain unchanged.

- On resumption, compilation, app lint and 1,454 tests across 139 suites passed
  with no failures, errors or skips. The subsequent selector change also passed
  the integrated compile/unit/lint command. Final CSV-integrated verification
  and candidate refinements are recorded after completion below, not inferred
  from these earlier passes. The existing lint baseline is unchanged.
- The focused CSV suite passed after catching previously unsupported dotted
  1Money dates and minute-resolution ISO input in the supplied fixtures. This
  covers strict parsing and actual invalid-row retention/no-write cases, not
  every real export or the complete manual CSV importer.
- Final integrated follow-up on 2026-10-30: `:app:compileDebugKotlin
  testDebugUnitTest :app:lintDebug` passed after strict RFC parsing and raw-row
  privacy were included. **1,469 tests / 143 suites, zero failures, errors or
  skips**. App lint passed against its unchanged baseline; this does not mean
  pre-existing baselined warnings/errors were all remediated.
- Numbered-build contract tests passed again. Their fake Gradle fixtures are not
  installable APKs. Version remains 0.4.4 / 228, and existing APK timestamps are
  unchanged. No screenshot golden, database schema or signing identity changed.
- Reviewed production-content candidates show the selected transfer destination
  at 200% text, grouped onboarding money without clipping, and reachable
  landscape failure/Save controls. A local review index is available at
  `artifacts/ui-review/readiness-2026-10-30/README.md`; these fictional-data
  candidates are not owner approval, device interaction tests or marketing shots.
- Offline verification initially exposed a missing annotation artifact after
  Firebase removal. Proxy-aware resolution populated the normal Gradle cache;
  subsequent verification runs offline, without restoring the diagnostics SDK.
- Actual `releaseRuntimeClasspath` and merged release manifest tasks passed:
  no Firebase runtime/initializer or removed broad permissions; cleartext false.
- The real Gradle release-signing verification task refused the repository's
  public test certificate. No owner credentials were requested or used.
- Rechecked on 2026-10-30 after the final source changes: the actual release
  runtime graph still contains no Firebase SDK, the merged release manifest has
  cleartext disabled and none of the removed broad permissions/initializer, and
  the real signing gate rejects the known public test key. That expected task
  refusal is a successful security assertion, not an owner-signed APK build.
- Official numbered-build contract suite passed in an isolated fake-Gradle
  fixture. Its synthetic files are not installable APKs.
- No Android device is attached. No physical-device acceptance is claimed.
- `version.properties` remains 0.4.4 / 228. No APK version was allocated,
  signing identity changed, or external distribution performed.

## Surface inventory

The registered-route inventory is maintained by `NavigationLayoutContractTest`
and [the navigation audit](../design/NAVIGATION-COHERENCE-2026-10-27.md).
Review every child flow, not only its page heading:

- Home: activity, insight visibility/order, trend ranges/series/value list,
  periods, search and transaction shortcuts.
- Accounts: account create/edit/delete/reorder, balances and reservations,
  currencies, icons and inclusion switches.
- Plan: budgets (category selection is inline; no account-selector launch claim),
  planned payments and recurrence, goals/edit/deadline/reservations, loans and
  repayments, empty/populated/error states and their nested confirmations.
- Net worth: overview/items/history, guide, accounts/loans review, item editor,
  dated replacement, valuation/ownership, rates, historical date and export.
- More: reports/filter/saved reports/CSV, all-transactions/statistics,
  search/query/results, category create/edit/reorder/rules.
- Settings: theme, currency, month start, language, hiding, notifications,
  security, backup/export/import, destructive reset and app information.
- Import/onboarding: source chooser, instructions, file chooser, manual CSV,
  AI guide/schema/preview/confirm, progress/results/failure details, currency,
  initial accounts/categories and completion.
- Security: unlock, existing-method proof, PIN/either/biometric setup,
  recovery generation/replacement/reset and protected capture.
- System surfaces: balance/add-transaction widgets, shortcuts, notification
  actions, scheduled reminders, Quick Settings tile, recents and external intents.
- Information: features, license/attributions, contributors, releases,
  disclaimer. Retire dormant upstream payment polling rather than reviving it.

## Release gates (not implementation claims)

1. Compile, financial/import/security unit regressions and relevant lint pass.
2. Fresh candidate renders cover Light/Dark/AMOLED, 320×640, 360×800,
   720-square, 1280×800, 640×360, 2× text, long values and failures.
   Owner approval is required before promoting goldens.
3. Real phone/fold/DeX: IME, resize/rotate, Back, keyboard, TalkBack, reduced
   motion, authentication/capture, system surfaces, offline/network observation,
   large-wallet responsiveness and owner-signed in-place upgrade.
4. Build only when explicitly requested, through the numbered private-release
   script. No debug/public-test-key distribution. No credential logging.
5. Commit/push/publish only when requested. Publish an approved prerelease,
   validate with a small cohort for at least 72 hours, then promote the identical
   verified artifact. Stop distribution for serious defects; recover with a
   higher-version fix, not an uninstall/downgrade instruction.

App ID, signing identity, database/backup schemas, ledger formulas and historical
semantics must not change. Backups contain private financial data and are not
automatically encrypted. A private signature does not prevent Play Protect checks.
