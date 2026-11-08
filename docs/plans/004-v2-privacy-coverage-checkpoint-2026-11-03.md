# V2 read-only privacy and quiet-history continuation

This continues the v2 ledger; it does not certify full public-release acceptance.
No APK, version/signing change, database/backup migration, publication or screenshot
reference promotion is part of this batch. Preserve the existing dirty worktree.

## Read-only surfaces reviewed and corrected

| Surface | Correction | Evidence required |
| --- | --- | --- |
| Income/expense header cards | Route amounts through the reactive financial visibility policy | Actual composed text and semantics, visible/hidden candidates |
| Daily history divider | Mask net cash flow; suppress balance-dependent positive colour while hidden; stack on narrow/large-text windows | Same semantics checks and adaptive renders |
| Upcoming/overdue headers | Mask income/expense totals, retain both hidden totals regardless of zero, wrap complete label/value groups | Semantics, narrow/large-text candidates; device interaction remains open |
| Foreign-currency transfer card | Mask receiving equivalent; group fiat digits; retain up to 18 crypto decimals without unnecessary trailing zeros | Actual transfer cards, including tiny crypto receiving values |
| Compact category cards | Mask sign and balance together; bound the name/value column so long names and amounts do not overlap | Actual category cards in the eight preview windows |

Section headers now use a native minimum-48dp button target with an announced
expanded/collapsed state. Supplementary state strings are English-first; this is
not certification of every translation. The existing fonts, colours, shapes,
ledger values, transaction types and callbacks are preserved.

The regression checks walk the actual Compose semantics tree after drawing,
including BoxWithConstraints subcomposition, rather than checking only a helper
or source text. They inspect text and accessible descriptions in visible/hidden
states. These checks are not TalkBack-on-device or click/keyboard acceptance.
Read-only masking does not change explicit numeric entry or exported data.

Candidate inspection caught a separate inherited transfer-header failure: two
unweighted account names in a row squeezed the receiving name into a tall column,
stretching its capsule into a large oval. Transfer labels now have bounded text
columns, stack on narrow/large-text windows and sit side by side at larger widths.
Each identifier retains its own saved colour when that preference is enabled;
decorative icons do not produce duplicate accessibility announcements. Deleted
accounts use the existing translated label, not literal `null`. Tests also bound
the fixture card height to catch the stretched-header regression. The existing
default crypto formatter remains unchanged; an explicit precision overload lets
receiving equivalents retain significant digits up to 18 places. US/German
grouping/decimal and tiny-value regressions cover this follow-up.

Selected inspected candidates are in the
[review gallery](../../artifacts/ui-review/privacy-coverage-2026-11-03/README.md).
They are not accepted references.

## Owner's November / 30-day report

Two separate findings must not be conflated:

1. `HomeViewModel.loadTrnHistory` uses the top-bar period, while `loadTrend` builds
   its own range of calendar buckets. Consequently an empty November can clear
   Recent activity and monthly spending while October remains in the chart.
   The activity copy incorrectly implied the user had never made a transaction;
   it now explains that the selected period is empty and points to View all.
   The screenshot's September–November axis suggests a monthly range; the active
   chip is outside the crop, so the installed selection is not verified.
2. Trend observation coverage previously came only from activity inside the
   selected window. If all earlier entries fall before that window, it can mark
   every bucket unknown even though actual ledger balances are available.

Finding 2 is fixed: a bounded, account-filtered scalar query reads the earliest
settled, nondeleted transaction time. Planned and unrelated-account entries do
not establish coverage. It does not load/map the entire wallet. The repository
deduplicates account IDs and chunks large selections below the SQLite bind limit.
Home converts the instant to its local calendar date off the UI thread and passes
it as coverage metadata to the pure trend builder. All dated balance and flow
calculations are unchanged. Quiet windows retain the balances already calculated
for each bucket, including a genuine zero balance; wallets with no recorded
history remain unknown. A mid-month first record establishes that calendar month,
not the following month. No historical transactions or settings are rewritten.

For finding 1, the owner has been asked whether the chart range should control all
three cards (recommended), or Recent activity should be all-time while spending
stays monthly. Do not silently remove the month filter or call this decision
implemented before the owner answers. The top-bar period still anchors historical
chart endpoints; 30 daily buckets already cross month boundaries.

## Skills and verification

UI/UX Pro Max informed readable reflow, native controls and meaningful semantics;
the project's existing design language overrides generic style suggestions.
Its stack data targets a newer Compose release: only APIs available in the pinned
1.6.8 stack were used. Android testing guidance kept Hilt, JUnit4/MockK,
Paparazzi/LayoutLib and Kover; no dependency or test-framework migration occurred.

The initial focused attempt failed because of a wrong wrapping-layout API import;
this was corrected. The next attempt exposed a test inspection that ran before
subcomposition and an ungrouped receiving-equivalent value. Both were corrected.
The focused combined gate then passed in 3m 36s:
`artifacts/verification/v2-privacy-coverage-focused-2026-11-03.log`.
It covers the initial 48 actual-content privacy candidates/semantics cases,
Home trend regressions, coverage repository/fake-DAO regressions and Room-test
compilation. It predates the tiny-crypto follow-up and final fixture adjustment.

Final integrated verification passed:
`artifacts/verification/v2-privacy-coverage-reviewed-2026-11-03.log`, **17m 55s**,
2,123 tasks. App compilation, all unit/render tests, Android/Room-test compilation
and app lint against the unchanged inherited baseline passed. Aggregate
**2,076 tests / 196 suites**, zero failures/errors/skips. `git diff --check` passed.
Room query tests remain compile-only without an Android device. No Gradle job is
left running. Version 0.4.4 / 228, database/backup schemas, lint baseline,
screenshot references and pre-existing APK timestamps remain unchanged. No APK,
signing, commit/push or publication was performed. Full v2 remains open below.

The first full gate (`v2-privacy-coverage-final-2026-11-03.log`) was deliberately
cancelled after the visual review exposed the stretched header; it is not a pass.
The layout-focused pass then succeeded in 1m 45s. The subsequent layout/precision
pass (`v2-privacy-layout-precision-2026-11-03.log`) passed in 1m 53s, including
80 production-content candidates/semantics cases plus three crypto-format tests.
The final reviewed gate also includes the corrected Home empty-period copy.

## Still open

- The Home card date-scope choice above and its implementation/acceptance.
- Remaining child dialogs, restoration/dirty-dismiss, localization, adaptive and
  exhaustive read-only privacy review in the main ledger. This table is a scoped
  audit, not a claim that every financial display has been examined.
- Genuine source exports and real Room rollback/replay, phone/fold/DeX,
  Back/IME/TalkBack, auth/capture, widgets/notifications, realistic performance and
  owner visual acceptance. No Android device is attached.
- Future owner-requested numbered signing/upgrade and explicit publication gates.

Never promote candidate images into accepted references automatically.
