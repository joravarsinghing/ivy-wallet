# V2 report/search recovery continuation

Owner requested finishing v2 while stopping before the session limit. This is
a bounded continuation of the verified restoration batch, not public-release
acceptance. No APK, version/signing/schema change, commit/push/release, baseline
expansion or screenshot-golden promotion is authorized here.

## Implemented

- Search restores its bounded primitive query independently of result records.
  Requests cancel superseded work and publish only the current generation;
  failure retains the query and offers a politely announced native retry action.
  An empty query includes untitled records too. Query changes reset scroll, not
  every result arrival. Returning from editing refreshes the same query while
  preserving matching rows/scroll during the read; an already running request
  is not restarted by recreation. A fresh route visit initializes separately.
- Reports save applied query primitives and page choices, not account/category/
  transaction graphs. Initialization is tied to the navigation visit; returning
  refreshes the retained query instead of replacing it with default filters.
  Startup failure offers Back/retry. Missing/stale saved context shows a review
  notice, and credentials/pending writes are never restored.
- Filter drafts restore separately from applied filters. The wrapper explicitly
  distinguishes Clear from failed restoration. Temporarily invalid amount ranges
  remain editable after recreation; the error is announced next to the amount
  controls and Apply is disabled until valid. Amount chooser IDs and the period
  chooser's primitive descriptor survive recreation. Closing still discards the
  unapplied draft; restoration never applies it automatically.
- Saved reports reject unknown/missing selection IDs, invalid transaction types,
  invalid bounds and malformed periods rather than dropping those selections
  and normalizing the result to all records. The existing explicitly present
  empty period object means all-time; a null period still means default. No
  serialized field or database/backup schema was added. Unbounded periods do not
  invent an equal-length preceding comparison period.
- Report calculation requests are serialized and generation-checked before
  publication on Main. The income/expense pair remains local until publication.
  Filtering to one account no longer removes other available account choices.

Android adaptive/testing and UI/UX Pro Max informed nonsensitive saved state,
explicit error recovery, readable error wrapping and consistent native controls.
The current Compose/Hilt/JUnit/MockK/Paparazzi/Kover stack is retained.

## Verification

`v2-report-search-focused.log` passed the initial focused tests/app compilation.
`v2-report-search-reviewed.log` passed tests but failed an indentation lint check
in the legacy filter body. The body indentation was corrected, without a lint
suppression or baseline change.

`v2-report-search-integrated.log` passed the full compile/unit/render/Android-test-
compile/lint gate in 2m 21s. A final follow-up refreshes retained report/search
queries on return from transaction editing. Its verification log is
`artifacts/verification/v2-report-search-final.log`: **passed in 2m 43s**, 2,123
tasks. Aggregate **1,960 unit/render tests / 187 suites**, zero failures/errors/
skips. App compilation, Android/Room-test compilation and app lint passed against
the unchanged inherited baseline. `git diff --check` passed too. The Android
device list was empty: Room tests remain compile-only. Version 0.4.4 / 228,
database schemas, lint baseline, screenshot references and old APK timestamps
remain unchanged. No Gradle job was left running at this checkpoint.

Stopped at this verified boundary to honor the owner's request to retain session
headroom. Full v2 is not certified complete; the remaining gates below stay open.

Actual VM tests cover process-recreated query/filter identity, retained page
choices, available account choices, missing-selection notices, queued report
requests, cancellation-insensitive old search work and failure/retry. Codec
regressions cover all-time/default distinction, malformed selections/periods,
Clear, invalid draft ranges and bounded payloads. Search loading/retry candidates
cover all eight windows; selected candidates were inspected and saved in the
[review gallery](../../artifacts/ui-review/report-search-2026-11-01/README.md).
These are not device interaction or SaveableStateRegistry end-to-end tests.

## Remaining v2 gates

- Inspect every remaining child control and ancillary settings/rate/sort/name
  dialog for restoration, accessibility, localization and short/large-text
  behavior. Do not label the whole app reviewed based on this scoped batch.
- Run actual Android interruption/Back/IME/TalkBack, fold/DeX, auth/capture,
  widgets/notifications and large-wallet performance journeys. No device was
  attached during this continuation; Room tests are compile-only.
- Genuine source-export import, transfer/timezone/currency/rollback acceptance,
  owner visual approval and future owner-signed upgrade remain open. Synthetic
  CSV fixtures and mocked transactions do not close those gates.

The execution ledger and per-surface matrix remain authoritative. Preserve the
large existing worktree and all earlier work when continuing.
