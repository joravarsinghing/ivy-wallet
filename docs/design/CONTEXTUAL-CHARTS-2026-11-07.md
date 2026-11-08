# Contextual charts throughout Jvy

Owner-requested distribution, 7 November 2026. A financial view should have a
useful visual in its own context, not require a trip to Reports. This is not a
requirement to put decorative plots inside every editor or settings page.

## Placement audit

| Surface and subview | Visual / decision |
| --- | --- |
| Home | Keep the approved balance/income/spending trend and category breakdown. One shared Home timeline; no additional date filter. |
| Accounts | New signed current-balance comparison. Native currency selector, explicit excluded-account labels; never a net-worth summary. |
| All transactions | New income/spending activity line, scoped to the displayed settled records. |
| Account activity | Same scoped activity chart; **cash flow, not account-balance history**. Transfers and opening adjustments are excluded and disclosed. |
| Category activity | Same chart, based only on the category-filtered transaction list. |
| Search results | Chart only for the completed, matching result snapshot. Stale/loading/failed search results do not gain a misleading new chart caption. |
| Budgets | New percentage-used comparison across independent limits, with existing per-budget progress tracks retained. Overlapping budgets are never summed. Refunds and >100% usage retain their actual signs/magnitudes. |
| Goals | New reserved-versus-target percentage comparison, plus existing individually labelled progress tracks. Invalid targets/unsupported reservations remain unavailable. |
| Goal detail | Keep its existing contextual progress track and exact funding guidance. No invented contribution timeline or redundant second progress plot. |
| Planned payments | New current-to-projected waterfall using exactly the existing forecast's net change and closing balance. Clearly a projection, not history or a guarantee. |
| Balance detail | New waterfall for the existing selected-period planned-payment calculation. No new forecasting assumptions. |
| Loans, Pending / Completed | New remaining-principal comparison for the displayed tab, separating native currencies and labelling borrowed versus lent money. |
| Loan detail | New principal-history line and principal/interest repayment split. Unknown dates or conversions do not create fictional history. |
| Net worth, Overview | Asset-allocation donut replaces the complete-picture allocation bars; incomplete-picture bars remain an explicitly known subtotal. Value-change waterfall replaces the old plain change rows. |
| Net worth, I own / I owe | New largest-position comparison using included top-level positions in the reporting currency. Unknown values remain gaps. |
| Net worth, Progress | Shared native history renderer replaces the old dotted Canvas. Maximum thirty interval-end observations; incomplete intervals stay disconnected. Full existing history rows remain available. |
| Wealth item detail | New individual history using that item's dated values from the existing workspace; absent/excluded observations remain gaps. |
| Reports / Explore | Keep deeper comparative/statistical exploration and its saved-query behavior. It is no longer the only consumer of the chart infrastructure. |
| Category statistics / pie chart | Keep the existing purpose-built pie/statistic visualization; no redundant second pie. |

### Explicit exclusions

These pages have been reviewed; a chart would not add trustworthy information
to the task being performed:

- Plan and More are navigation hubs. Do not duplicate the graphs from every
  destination above their shortcuts.
- Transaction, planned-payment, account, budget, goal, loan and repayment
  **editors** prioritize accurate input, validation and acknowledged saves.
- Categories manages names/icons/rules. Its category-activity destination now
  has a scoped chart; a chart of category counts would be decoration.
- Exchange rates manages quotes. The database does not supply a comparable rate
  history; a line would invent observations, and bars across differing quote
  units would imply a false comparison.
- Settings and Security manage preferences and protected credentials. Unlock,
  PIN and recovery flows must not gain financial charts or unprotected data.
- Onboarding, CSV mapping, import validation/preview and backup/export tasks
  have functional progress/count feedback where appropriate, not invented
  personal-money trends before data exists or before import confirmation.
- Features, Releases and Disclaimer are informational; the later owner correction
  removes Contributors/Attributions routes and retains notices in bundled Terms.
  or legal pages. Poll remains a compatibility route, not financial analysis.
- Empty financial views retain their useful empty state. No fake zeros or
  decorative axes are added merely to satisfy a chart count.

`ChartDistributionContractTest` classifies every registered destination and
guards real financial-screen hooks. New registered routes require a new explicit
decision. Source coverage is not visual or physical-device acceptance.

## Shared implementation

- Models and waterfall geometry: `shared/base/.../analytics/ChartModels.kt`.
- Existing analytics reducers promoted to `shared/domain/.../analytics/AnalyticsEngine.kt`.
  Reports retains a compatibility facade; no calculation is silently replaced.
- Context-specific pure builders: `ContextCharts` and `WealthCharts` in shared
  domain. They consume existing read snapshots and perform no queries or writes.
- Native renderer: `JvyAnalyticsCard` and `JvyContextCharts` in shared UI. Reports
  uses the same renderer through its adapter; other features never depend on
  the Reports feature module.
- Legacy transaction adapter: `ContextualActivityChart` uses existing filtered
  history, off-main-thread grouping, current-request composition keys and no
  extra ledger query, FX request or background subscription.
- Loan detail uses already loaded records and accounts; no per-point database
  queries. Interest is not subtracted from principal. Foreign amounts require
  their stored loan-currency conversion or a matching native currency.

No financial database/backup schema, ledger balance, valuation formula, import
contract, root navigation destination, version or signing identity is changed.

## Visual and data rules

One contextual question per chart. Use native legends, meaningful headings,
short explanatory copy, exact-value disclosure, shared Jvy fonts, vivid plot
tokens and neutral surfaces. The host owns its 24dp gutter once; grids pass
zero extra horizontal padding to the shared card. Different currencies are
selectable, never stacked into a combined monetary total. Hidden mode removes
geometry, scales, ratios and exact values, not only footer money strings.

Owner correction: compact management comparisons now precede the account,
budget, goal and loan lists; the planned-payment waterfall precedes the schedule.
Page navigation and filters remain first. Contextual ranked bars preview three
entries with full values available on demand, rather than a long analytics hero.
This order is guarded in the source contract. Activity and wealth visuals stay
near their summary. A single observation or isolated observations show their exact
amounts and a no-trend explanation, not an empty axis frame, invented line or dot.

Historical activity, current positions, reservation progress and projected
balances remain explicitly different concepts. A forecast's last bar is a total,
not another increment. A zero or invalid target produces an unavailable ratio,
not a guessed 0%. Missing accounts, rates, dates and individual values are never
coerced to zero. No contributions, merchant identities, liquidity or statistical
probabilities are inferred from data that does not record them.

## Verification and acceptance

Pure tests cover native-currency separation, signed refunds/overfunding,
zero/unknown targets, total-bar geometry, interest/principal/increase semantics,
missing conversions, future records, incomplete history, asset/holding
double-counting and scoped transaction eligibility. Source guards cover all
registered routes and forbid feature-to-Reports coupling. Adaptive native
candidates exercise phone, fold, DeX, landscape, dark/AMOLED and 200% text.

The execution ledger records final command results. Renders, source/interaction
guards and unit tests do not certify physical phone/fold/DeX, TalkBack, keyboard,
midnight rollover, very-large-wallet performance or owner visual acceptance.
No screenshot goldens are promoted. New copy is English-first and still needs
translation review. No APK, version bump, commit, push or release is authorized
by this implementation.

Final local gate: **512 tests / 36 suites**, zero failures/errors/skips; app
compilation and lint against the unchanged baseline passed in **11m 2s**.
Evidence: `artifacts/verification/contextual-charts-handoff-2026-11-07.log`.
Render review includes actual phone account/management ordering, planned-payment
ordering, shared dark large-text principal history, 200% target comparisons and
AMOLED projections. These are candidate renders, not owner/device approval.
