# Owner-led coherence corrections

Scope: the ten issues reported after the 0.6.0 candidate. No APK, version bump,
push, tag or release is part of this change.

Subsequent owner APK request: the implementation is included in the numbered
[0.7.0 / 232 signed candidate](../releases/0.7.0-candidate.md). The implementation
gate below remains distinct from build/signature checks and device acceptance.

## Implemented contracts

1. Home keeps its single timeline, but the date chooser is compact and quick
   ranges use one scrollable row. Income/expense tiles restore the green/silver
   identity through tested shared foreground/background tokens.
2. Customize supports persisted order, visibility and Restore defaults. Move
   up/down controls work without dragging and include accessible card labels.
   Recent activity is configurable too. Date controls remain outside the list.
   Current/future cards keep their own time context when moved above history.
3. Account charts honor Transfers as income/expense. Sending amounts and
   receiving amounts use their stored native-currency legs, not inferred FX.
   Account day dividers use the same preference and label their actual currency.
   Wallet-wide cash-flow charts continue excluding internal transfers. Existing
   account-header adjustment treatment is preserved; cash-flow charts explicitly
   exclude balance adjustments.
4. Activity trend remains present in empty and sparse periods. Unknown history
   is not zero; quiet dates after the first observation can be real zero flows.
   Isolated points have latest exact amounts and an explanation, not a fake line.
   The complete dated result stays in place during replacement calculations.
5. Accounts' wallet total uses the branded primary container and matching text.
6. Compact contextual summaries precede management lists. Ranked previews show
   three entries, with all exact values available on demand. Planned Payments
   leads with its compact projection and discloses the detailed forecast.
7. Home and other time-series charts use the same native `JvyTrendPlot` drawing
   implementation: shaded flow areas, saturated outlines, dashed spending,
   right-side scale, no dots, no bridging gaps. Bar/donut/waterfall geometry is
   retained where it answers a different question. Existing Home motion remains.
8. Account Add income/expense controls are left-aligned outlined buttons with
   contrast-derived foreground/border colors on user-selected backgrounds.
9. Exchange rates retain the existing Fawaz Ahmed Currency API lookup/fallbacks.
   Provider date and successful refresh time are persisted only after rates are
   validated and stored. Unknown/stale/offline states are explicit. Refresh has
   a pending state; manual overrides remain protected. These are daily reference
   rates, not guaranteed live transaction prices. A read-only HTTPS probe of the
   existing jsDelivr endpoint returned a dated EUR table with 340 entries.
10. Contact names Joravar and has one LinkedIn destination. Contributor and
    decorative attribution modules/routes are removed; restored legacy visits
    safely return to Settings. Terms/privacy open bundled, scrollable documents
    in Settings and onboarding, without requiring a browser or network. Original
    GPL text and inherited asset/source attribution links remain under Terms.

## Verification

Passed **336 cases / 24 suites**, zero failures, errors or skips, including
account transfer/native-currency day totals, empty/sparse activity, quote
freshness/failure/manual-override protection, restored navigation, dashboard
ordering, shared chart geometry/contrast, legal-source contracts and adaptive
renders. App compilation and lint passed against the unchanged baseline.
Broad gate: **3m 24s**, `artifacts/verification/ui-coherence-tests.log`.

Render review caught a 200% text defect in the inherited Customize toolbar:
its action broke mid-word. The toolbar now uses shared button/typography roles
and wraps complete controls. Final compilation, lint and **86 Home/layout
cases** passed again (**2m 7s**):
`artifacts/verification/ui-coherence-handoff.log`.
The Home preview fixture now reconciles its summary/plot totals and labels the
actual rolling period instead of combining a current-month label with sample
rolling dates. This fixture correction does not change production calculations.

Inspected fresh phone, AMOLED, large-text and hidden-account previews, including
shared percent comparisons and quote freshness. Home's checked large-text
toolbar no longer splits Customize. Candidate images (local, ignored):

- `artifacts/ui-review/coherence-Hugyca/home-amoled.png`
- `artifacts/ui-review/coherence-Hugyca/home-large-text.png`
- `artifacts/ui-review/coherence-Hugyca/safe-to-spend-first.png`
- `artifacts/ui-review/coherence-Hugyca/accounts-summary.png`
- `artifacts/ui-review/coherence-Hugyca/exchange-rates-amoled.png`

No screenshot goldens were promoted. Source guards and LayoutLib are not
physical-device acceptance. Device upgrade, TalkBack, keyboard/DeX gestures,
rate-provider availability on the owner's network, legal-copy review and final
visual approval remain separate. Removed feature source is recoverable from
Git history; original licence text and inherited attribution links are retained.

No financial database or backup format is changed. New local preferences are
presentation order/visibility and quote freshness metadata, not financial data.
