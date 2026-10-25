# Home trend: saturated ink and finite updates

## Reference correction after owner review

The owner rejected the previous two-panel/mirrored-expense interpretation. The reference is **one plot**: a purple balance line in front of overlapping green income and red spending fills. All three share the same dates, currency scale and zero baseline. Ordinary positive spending rises from zero just like income. Do not add a second chart/axis, stack the flows or inflate their magnitudes with independent normalization. Genuinely negative values stay negative.

The purple line remains the ledger's period-end wallet balance, not net worth or income minus spending. Exact summaries and the expandable value list keep their original accounting signs. The default balance line has no fill or dots. Income/spending fills have bounded eased boundaries through their actual samples, with control points confined to each interval: no overshooting or invented peaks. Their outlines and legend complement color. Unknown periods remain gaps; quiet observed periods remain real zeros. An isolated observation supplies exact values and explanatory copy without manufacturing a line. Optional balance shading remains a legacy test comparison, not a saved user preference.

`JvyChartColors` supplies saturated plot ink. Light/dark/AMOLED outlines pass 3:1 against the neutral insight card. Text retains the existing AA Material roles rather than reusing plot ink as small text. Shared typography, controls and card shapes are unchanged.

The overlay plot is 160dp tall across range changes. Green/red fills retain strong 70–80% opacity instead of washing out against the card. A thin neutral underlay keeps the purple line distinguishable where a filled area crosses it. All fills are drawn before outlines; balance is drawn last.

## Animation and compatibility

- A 240ms Compose crossfade updates plot, dates and axes together after replacement data arrives. Exact totals update immediately; initial display is static.
- Loading/editor-only changes do not create animation targets. The previous chart remains steady during loading. Rapid range requests cannot publish an older result over a newer selection.
- Hiding balances removes outgoing animation layers, scales and values immediately. Currency changes clear the previous dataset before relabelling the chart.
- Geometry and decimal normalization are prepared on data changes. Paths, strokes and brushes are cached on data/size changes; animation updates opacity, not ledger calculations.
- Compose handles Android's motion-duration scale. No polling, timer, perpetual animation, new network work, dependency upgrade or Compose migration is introduced.
- Financial formulas, persisted preferences, backup/import schemas, app identity and signing are unchanged.

## Verification

Commands (using the existing environment wrapper):

```text
:feature:home:compileDebugKotlin
:feature:home:testDebugUnitTest
:shared:ui:core:testDebugUnitTest
:shared:domain:testDebugUnitTest
```

The earlier two-panel candidate passed 292 checks but did not match the reference; passing tests did not constitute visual approval. The corrected candidate adds shared-scale/equal-height and draw-order regressions plus eight reference-composition renders using clearly fictional, ledger-consistent values. Existing sparse-history fixtures are retained: production never substitutes the varied fixture or inflates small flows for appearance.

After correction, 310 checks passed (155 Home, 61 shared UI, 94 domain), with no failures, errors or skips. This includes 88 static chart renders, 16 projection checks and three animation renders. The added crossing-area fixture checks the balance line's separation from either fill. Final light/dark reference candidates, realistic sparse data, large text and the privacy-cutoff recording were inspected; owner visual approval is still required before promoting any screenshot golden.

The superseded candidates are under ignored `artifacts/ui-review/home-trend-motion-2026-10-25/`; corrected candidates go under `artifacts/ui-review/home-trend-overlay-2026-10-25/`. These are review candidates, not approved screenshot goldens or device-performance measurements.

Before distribution, check date/range/transaction refreshes and gesture scrolling on a real phone/fold/DeX window, Android animation scales (including disabled), and TalkBack/exact-value access. Hardware frame-time/battery performance and disabled-motion behavior have not been measured in this environment. Profile a release build if measurement is needed, never a debug build.

No installable APK was generated, no APK version consumed, and no screenshot golden replaced.
