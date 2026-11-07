# Home chart facelift — 7 November 2026

The owner first referenced TradingView Lightweight Charts, then explicitly chose
visual inspiration with the lightest native implementation rather than embedding
the library. No upstream code, dependency, WebView, attribution requirement,
network behavior, APK/version or financial-data change is introduced.

UI/UX Pro Max informed the restrained line/area hierarchy and readable axes;
Android Compose performance guidance retained cached paths/brushes and draw-only
animation reads. Changes are limited to the chart's presentation:

- Content-sized right-hand money scale, with font-scale-aligned tick/grid centers.
- Crisp 2dp purple balance line, unchanged saturated green income and dashed red
  spending outlines; softer overlapping zero-based fills and quieter guides.
- No dots, thick balance halo, duplicate subtitle when exact period dates exist,
  pan/zoom gesture interception or decorative loop. Existing finite reveal,
  shared Home filter, exact totals, single scale and unknown-history gaps remain.
- Compact real-date labels, including years at year boundaries; small/large
  monetary ticks no longer collapse into zero or enormous strings. This formats
  the display only, never changes BigDecimal calculations or stored values.

## Verification

Final focused gate passed in **2m 4s**, **1,731 tasks** (38 executed): **82 tests /
seven suites**, zero failures/errors. Includes 32 real-layout candidates across
eight windows/themes, axis formatting, shared-scale/gap geometry, sampling,
reduced-motion state, six animated transitions and shared-date captions. App
compilation and app lint passed against the unchanged baseline.
Log: `artifacts/verification/home-chart-facelift-final-2026-11-07.log`.

Visual review corrected the flow gradients to fade from each series' real peak
toward the common zero baseline; opacity no longer disappears when balance is
much larger than cash flow. This does not rescale any data coordinates. Final
Light/AMOLED candidates were inspected and retained in the
[review gallery](../../artifacts/ui-review/chart-facelift-2026-11-07/README.md).
Post-check indentation cleanup was verified whitespace-only; `git diff --check`
passed. No screenshot goldens/baselines are approved or promoted. LayoutLib
tests do not establish physical frame-time, TalkBack, fold or DeX acceptance.

Version remains **0.5.1 / 230**. No APK, signing-key access, commit, push, tag or
publication; the existing APK predates these changes. Earlier Home timeline and
root-swipe work remains intact.
