# Reports → Explore

Follow-up: the [contextual distribution](CONTEXTUAL-CHARTS-2026-11-07.md) promotes
the renderer and reducers into shared modules and uses them in financial pages
throughout the app. This original implementation record remains historical;
Reports is no longer the sole chart host.

Implementation follow-up, 7 November 2026. No additional root tab, chart library,
WebView, remote service, database/backup field or financial write is introduced.

## Navigation and scope

Reports retains its shared top-start Back and existing transaction-report mode.
Explore is an in-page mode with five wrapping groups: Spending, Cash flow,
Wealth, Debt and Goals. Advanced analysis is an explicit, initially-off disclosure.
Do not move these deeper charts onto Home or give Home a second date timeline.

Spending and Cash flow consume the **applied** report transaction snapshot, not
the filter sheet's pending draft. Their effective date caption caps actual
activity at now. Calendar-month summaries intersect the exact report bounds;
histories over thirty months use thirty exact intervals without discarding
older records. All-time starts at the first matching settled observation.

Wealth reads today's wealth workspace; Debt reads recorded loans through today;
Goals reads current active reservations. These are explicitly independent of
transaction-report filters. Their workspace shortcut opens the existing feature.
Saving/exporting a transaction query must not masquerade as exporting wealth or
goal chart data.

## Implemented views

| Group | Standard | Advanced |
| --- | --- | --- |
| Spending | Ranked category bars, up-to-five-slice donut, weekday totals, cumulative spending line | Positive purchase-size histogram, top recorded-title ranking, three-category trend lines, category min/quartile/median/max plots with ≥20 positive expenses |
| Cash flow | Income/spending grouped bars, income-to-surplus waterfall, daily spending heatmap (last ≤91 days of the selected range) | Cash-flow savings-rate line; income-versus-spending scatter with a short-history warning |
| Wealth | Assets versus debts, twelve observations of net-worth history, positive asset allocation | Calculator-backed month-over-month value-change waterfall when both observations are complete |
| Debt | Per-loan remaining-principal history in the loan's own currency, with borrowed/lent labelling | Recorded principal-versus-interest repayment bars |
| Goals | Current reserved versus target bars, deadline and review guidance | No invented contribution history |

Empty, incomplete, failed and genuinely zero states are different. These views
are conditional on sufficient real records: an empty workspace is not a zero
wealth chart, an undated loan is not a fabricated history, and a single point
is not a trend. Statistical plots explain their actual calculation rather than
claiming standard outlier-adjusted whiskers they do not compute.

## Money and privacy contracts

- Exact reducers use `BigDecimal`; only normalized drawing coordinates use Float.
- Flow charts exclude transfers, system opening/balance adjustments, deleted,
  planned and future entries. Signed refunds remain signed. This is explained
  next to the scope; it does not rewrite the existing report/ledger reducers.
- Amount conversion uses available wallet quotes once per distinct currency.
  These are not historical quotes. Missing accounts/rates block the flow board,
  not silently replace unconverted money with zero or publish partial ratios.
- Net worth reuses its existing dated calculator. Allocation counts only
  top-level positions, not both a portfolio group and its holdings. Incomplete
  dates remain gaps; attribution is withheld until both dates are complete.
- `WealthWorkspaceUseCase.load(..., allowLegacyMigration = false)` is mandatory
  for Explore. The existing feature keeps its default compatibility migration;
  simply opening analytics cannot merge records, capture quotes or save snapshots.
- Loan principal excludes interest; recorded increases and decreases retain
  their meaning. Missing foreign repayment conversions require review rather
  than assuming a 1:1 rate. Different loan currencies are never added together.
- Goals remain reservations, not transfers. Foreign/unavailable allocations are
  flagged rather than discarded or converted. Storage has no contribution-event
  history or goal currency, so neither is inferred.
- Hidden balances remove geometry, axes, exact values, distributions, percentages
  and value-disclosure actions. Titles, dates and explanatory copy remain.
- Financial results live only in memory, never saveable state, logs or navigation
  payloads. Saveable state contains only mode/group/disclosure controls.
- Replacing the applied snapshot, section or currency immediately replaces the
  loading slot; cancelled work cannot publish a previous scope under new labels.

## Visual and interaction contract

Use Jvy typography, 24dp page gutters, 16dp card insets, 20dp cards, neutral
surfaces and shared vivid chart ink. Group controls wrap with 8dp gaps and ≥48dp
targets. Large text is not shrunk. Chart paths/brushes are native cached drawing;
no polling or decorative animation loops. Deep exploration stays lazy and chart
geometry is bounded; exact-value disclosure reveals twenty more rows per action.

Visible legends and value lists complement color. Positive and negative steps
share a zero baseline. Ordinary income/spending are not mirrored arbitrarily.
Donut slice order is clockwise from the top and has an exact numbered key.
Heatmap dates run left to right, then down; exact signed values disambiguate
refunds, quiet dates and unknown prehistory. Text remains on semantic AA roles,
not the chart-outline colors.

Opening a category/date transaction report narrows the existing applied query
without serializing records or discarding account/tag/keyword/amount filters.
The ordinary report may separately display planned entries and its existing
report totals; Explore's documented cash-flow exclusions do not change them.

## Deliberately not fabricated

Contribution timelines need an actual event history; merchant analytics need a
reliable merchant field (titles are honestly labelled). Runway needs an explicit
liquid-funds scope; forecast scenarios need user-approved assumptions. Probability
fans, APR/amortization claims, investment returns and causal explanations are
not inferred from a transaction list. Previous-period pace comparisons, stacked
category history and treemaps remain follow-ups, not claimed completed charts.

## Verification

Focused exact-money, eligibility, conversion, calendar, loan, reservation and
read-only tests accompany the implementation. Adaptive native candidates cover
eight windows/themes, including 200% text, square fold, short landscape, DeX and
AMOLED. Candidate renders are not approved goldens or real-device acceptance.
Final test/compiler/lint outcome is recorded in the execution ledger.

Final local outcome: **154 tests / 14 suites**, zero failures/errors/skips,
including eighty native candidate renders. App compilation and lint passed
against the unchanged baseline. The last UI-only handoff reran all 121 report
cases plus app compilation/lint in **1m 39s**; the unchanged thirty-three wealth
regressions had already passed. See `artifacts/verification/explore-handoff-2026-11-07.log`
and `artifacts/verification/explore-verified-2026-11-07.log`. Renders reviewed include
light grouped bars/waterfall, AMOLED donut/heatmap, and large-text group controls.

Physical phone/fold/DeX, TalkBack, keyboard, process restoration, very large-wallet
timing and owner visual review remain pending. New explanatory copy is English-
first and needs translation review. No APK, version bump, commit, push or release
is included.
