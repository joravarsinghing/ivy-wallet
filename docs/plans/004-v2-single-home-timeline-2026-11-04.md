# One timeline for Home — implementation checkpoint

Owner approved one exact Home window, defaulting to the current budgeting cycle
through today, preserving the configured cycle start. No APK, version increment,
push or publication is authorized by this implementation request.

## Contract

- Current balance is labelled current, not a historical endpoint value.
- Shared Home controls exist independently of every optional dashboard card.
  Current cycle, selected budget month, rolling, custom and all-time are explicit
  modes. Rolling month choices are trailing calendar durations ending today.
- Summary, plot, spending and recent activity share resolved dates and publish
  as one complete historical result. Previous results retain their dates while
  replacing them; storage/query failure leaves a visible retry path.
- Summary/plot cash flow uses the existing reducer and conversion logic, excluding
  transfers/system balance adjustments and wallet-excluded accounts. Recent
  activity still shows those records. Account balances and valuation math are
  unchanged. Spending uses the same account scope.
- Up to 30 days use daily buckets; longer windows use 30 nonoverlapping date
  intervals. Empty all-time history invents no start date or plotted point.
- The shared period dialog keeps its existing defaults for other callers. Home
  adds future-only validation and discloses capping at today without committing
  global report periods. Exact dates enter existing breakdown route arguments.
  A default-false `cashFlowOnly` route primitive retains Home eligibility in its
  breakdowns without altering ordinary reports. Saved navigation accepts the
  existing six/eight-field pie routes and the new nine-field form; no transaction
  lists are serialized to represent a range.
- Current/future Safe to spend, Money coming up and payment actions follow the
  historical group. Saved reports keep their own queries. Visibility preferences,
  metric preferences and the pending root-swipe correction are preserved.
- Nonsensitive Home mode primitives persist separately from the obsolete chart
  range preference. Relative/current modes re-resolve on resume/day rollover;
  a single screen-level midnight wake-up replaces polling. No DB/backup schema,
  dependency, financial record, app identity or signing change.

## Verification

First compilation caught cross-module nullable-property smart casts; fixed with
local values. The next focused run compiled the changed production modules and
ran 291 Home tests: 17 failures comprised sixteen obsolete empty-state-copy
assertions and a pure adapter reaching Android resources. Updated the assertions
for the approved period language and removed the adapter's resource dependency;
did not replace screenshot references or weaken date/value assertions. The
remaining Home cases, including new controls/actions and reducer tests, passed.

Final verification now targets changed behavior only: period resolution/sampling,
real cash-flow reducer/loader, explicit-date cards, chart-hidden control actions,
route restoration, breakdown dispatch and navigation ownership. App compilation
and lint run once with that corrected source. This narrowed gate replaces another
whole-project test rerun, at the owner's request to reduce unnecessary work.

Physical touch, native dialog/keyboard layering, date/time broadcast delivery,
TalkBack and actual fold/DeX acceptance require a device. Candidate renders are
not owner approval; no screenshot golden promotion. New English copy still needs
translation review.

## Final outcome

Focused gate passed in **10m 54s**, **1,761 tasks**: **74 targeted tests / seven
suites**, zero failures/errors, app compilation and app lint against the unchanged
baseline. Coverage includes twelve period/sampling cases, seven real reducer/
loader cases, sixteen explicit-date card cases, eight actual chart-hidden control
actions across the adaptive window matrix, three breakdown dispatch cases,
thirteen saved-navigation cases and fifteen navigation/source contracts.
Log: `artifacts/verification/home-single-timeline-focused-final-2026-11-04.log`.

The final preference-write ownership guard prevents an older save failure (or
retry of the same selection) from replacing a newer request's status. Its source
contract, app compilation and lint passed incrementally in **2m 38s**, **1,730
tasks**, with only 46 tasks executed:
`artifacts/verification/home-single-timeline-ownership-2026-11-04.log`.

Fresh Light/AMOLED chart-hidden Home candidates were inspected; the balance
amount now shares the 24dp gutter. Adaptive actions/renders also cover narrow
phones, square folds, 200% text, short landscape and DeX. Prior candidate renders
cover loading/error/hidden summaries. These are LayoutLib evidence, not physical
device or owner acceptance. No goldens/baselines were changed.

The root-swipe fix remains present. Version remains **0.5.1 / 230**; no APK,
private-key materialization, commit, push, tag or publication occurred. The
existing APK predates this implementation. Large-wallet performance and the
hardware/translation/public-release gates remain separate.
