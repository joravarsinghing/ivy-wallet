# V2 shared Home date-window continuation

The owner approved one chart-selected date scope for Balance trend, Where your
money went and Recent activity, then requested a privately signed numbered APK.
This is not approval to publish a release or certify every remaining v2 gate.

## Implementation

- One inclusive local-calendar window defines all three cards. A 30-day view in
  early November includes October. Three/six/twelve-month ranges use calendar
  months, not the budgeting-period start day. The top-bar period anchors the end;
  an explicit custom period supplies its end date.
- Conversion uses local start-of-day through the last nanosecond before the next
  local midnight. This respects daylight saving and avoids including a following
  midnight transaction in two buckets.
- Query and category calculation run off the UI thread. History, breakdown and
  plot are published together only for the latest generation/range/currency.
  The previous result remains visible while a replacement loads.
- All three cards identify their actual date scope. An empty November no longer
  hides October entries belonging to the selected rolling window. Empty-state
  copy distinguishes an empty range from a wallet with no history.
- Spending's View chart passes bounded dates, not a large serialized transaction
  list. The route codec reads legacy six-field chart entries and new dated
  entries; incomplete or invalid dates fail closed. Fixed-scope drill-downs do
  not expose a second period picker that could silently change the selection.
- Upcoming payment actions stay in a separate panel, outside past activity's
  date scope. View all still opens full transaction history.
- Candidate inspection exposed squeezed headings and payment labels at 200%
  text. A shared adaptive Home heading places its action below the full-width
  text at enlarged scales. Planned/large-text/long-amount activity rows wrap
  amount/action groups and retain readable account labels; Pay/Get and View all
  use shared native text buttons. Existing fonts, colors and card tokens remain.
- No database or backup schema, package identity, signing identity, dependency,
  screenshot golden or lint-baseline migration is authorized by this fix.

## Verification and artifact

The initial focused gate passed in 6m 38s (744 tasks): Home unit/render tests,
navigation tests and app compilation. A second focused gate added actual chart
ViewModel tests plus sixteen composed-content adaptive cases. Inspection caught
the large-text issues above; the corrected focused gate passed in 1m 36s
(243 tasks). These checks are not device interactions or approved goldens.
Selected inspected candidates are in the
[review gallery](../../artifacts/ui-review/home-window-2026-11-04/README.md).

The numbered-build contract suite passed using its isolated fake Gradle fixture;
it did not create an installable APK or allocate a real version. PowerShell's
default user cache was read-only; workspace-local XDG cache/config/data paths
resolved that environment problem without changing the official build script.

The initial integrated run completed all 2,103 tests but failed one new lint
error: parameter ordering in the shared heading helper. The helper was corrected
without a suppression or baseline update. The final integrated gate passed in
**3m 14s**, 2,127 tasks:
`artifacts/verification/v2-home-window-integrated-fixed-2026-11-04.log`.
It includes app compilation, all unit/render tests, Android/Room-test compilation
and app lint against the unchanged inherited baseline. Aggregate **2,103 tests /
200 suites**, zero failures/errors/skips. The 27 added cases comprise five date
boundaries, two shared-query cases, two route-codec cases, two actual chart
ViewModel cases and sixteen adaptive composed-content cases. Room execution
remains blocked without a device. `git diff --check` passed.

The official private numbered build passed in **11m 51s**, 1,546 tasks:
`artifacts/verification/v2-private-release-2026-11-04.log`. It allocated
**v0.5.0 / versionCode 229**, using `-Variant release -Bump minor`, not a debug
or public-test-key build. The artifact is
`artifacts/apk/Jvy_wallet_v0.5.0.apk`, **12,709,058 bytes / 12.12 MiB**.

SHA-256:
`763bd9508b0cf35c53cd1c7ebf5ef94411b40edeb166f22a62aedc29358cd07f`.

Post-build checks of the actual APK passed: v2 signature, one signer matching the
preceding owner-signed 0.4.4 certificate, unchanged `com.ivy.wallet` identity,
version/code, API 28 minimum / API 34 target, non-debuggable flag, ZIP alignment
and checksum agreement with the official manifest. No keystore files or test
host/instrumentation manifest entries were found. Firebase manifest components
are absent; cleartext traffic and Android automatic backup are disabled. These
checks are not network-traffic observation or hardware acceptance.

The Base64 keystore transport was decoded only in a unique private temporary
directory outside the checkout (0700 directory / 0600 file). The child build
received the file path, not the encoded transport. `finally` removed that copy;
zero temporary release-key directories remained afterward. Configured owner
credentials were not logged, written into source or changed in the parent
environment. No signing identity, package, database/backup schema, baseline or
screenshot reference was changed. The official script's checksum manifest and
the additional verification record remain in ignored `artifacts/apk/`.

The tested accumulated v2 source and allocated version are recorded locally
under the required personal Git identity. Generated Python caches and the
pre-existing separate security-review artifact are excluded and left untouched.
No push, tag, GitHub release, app-store upload or other publication is authorized
or performed. Full v2 still has the ledger's remaining child-control/privacy,
genuine-export, device and owner-acceptance gates; this APK is a signed candidate,
not whole-app public-release certification.

## Required owner/device smoke checks

- Keep a private, verified full native backup before updating. Do not uninstall
  or clear data if Android refuses an update; check package/certificate first.
- In an empty new month, select 30d and confirm October activity/spending remains
  visible when its dates belong to that range. Check 7d and the monthly ranges,
  historical anchors, custom endpoints and the shared date captions.
- Open View chart and check its dates and total; return, switch ranges rapidly
  and confirm no older result replaces the latest selection. Check date/currency
  changes and hidden balances without flashes of stale or private values.
- Confirm upcoming payment actions remain usable outside past activity's range.
  Inspect enlarged text, narrow phones, square fold, landscape and DeX windows.
- Reconcile account balances, transaction counts, transfers, reservations,
  budgets, loans and dated wealth after a same-key in-place upgrade. Source/unit
  checks are not evidence that this actual device upgrade succeeded.
