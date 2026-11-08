# Four owner-reported UI corrections after 0.5.0

Scope: denser honest trend sampling, readable account actions, usable period
selection and shared rounded logo backing. No APK, version allocation, signing,
publication, database/backup migration, dependency upgrade or golden promotion
is requested by this batch. Preserve existing generated/unrelated artifacts.

## Findings and implementation

- Longer trends previously used one point per calendar month. The date horizon
  and sampling now differ: seven daily samples for 7d; thirty real, contiguous,
  non-overlapping date buckets for every range from 30d upward. Each bucket sums
  original flows and reads the actual ledger balance at its inclusive end.
  Partially elapsed months, leap years, quiet history, prehistory gaps, refunds,
  exact totals and shared Home card dates retain their semantics. There are no
  synthetic observations, averaged balances or per-day full-year query loops.
- Expanded income-header actions forced green ink even on green account
  surfaces. Both actions now use the existing luminance/AA identifier foreground
  policy and retain their callbacks, stored background colors and 48dp targets.
- The period picker was an in-content legacy overlay below the main navigation
  sibling. Its footer could be obscured, while Close correctly cancelled the
  unapplied draft. It now uses a native dialog and the shared bounded form with
  a pinned, explicit Apply filter action. Its single body scrolls independently;
  Close/Back cancels. The shared calendar fields open above that dialog, permit
  inclusive same-day ranges and use complete local-day boundaries. Inactive
  relative controls default to a valid neutral draft rather than showing an
  unrelated zero-count error. An explicit selected chip activates the relative
  option, including the unchanged default of one week; merely displaying that
  default does not silently override a chosen month. Invalid active counts remain invalid. Home saves
  chooser/draft primitives, and nested report filters no longer update Home's
  global period while still editing their draft.
- The shared logo rendered an unmasked white rectangle. `JvyLogo` now clips
  white backing and unchanged transparent artwork with the shared proportional
  22% corner token. This updates Security, unlock, onboarding and the rail
  together, without baking a new tile into image assets or changing launcher
  identity. The mandatory design language and token tests document the policy.

## Skills and verification

Android adaptive/testing guidance informs reachable footer/Close controls,
saveable nonsensitive state, multiple window sizes and actual control-action
checks. Newer Nav3/Grid APIs and framework migrations are out of scope; retain
Compose 1.6.8, Material3 1.2.1, Hilt/JUnit4/MockK/Paparazzi/Kover. UI/UX Pro Max's
scrolling guidance supports one scrolling body without nested vertical scroll;
its platform-specific accessibility results are principles, not a claim of web
WCAG certification for Android. The existing Jvy design language owns tokens.

The initial focused run exposed a DeX video-thumbnail encoding issue (1000x625,
an invalid YUV frame size). All period actions passed on the other seven windows;
the actual 1280x800 test viewport was not changed. A test-only final-frame PNG
handler runs every interaction frame without video encoding. Its first direct
desktop-image callback did not compile on the Android test classpath; a JVM-only
callback bridge fixed that without adding a production or framework dependency.

The corrected focused gate passed in **6m 16s**, 767 tasks, including app
compilation and Home/categories/legacy/shared-UI tests. A follow-up added actual
body scrolling/all-time selection with pinned-footer checks, clarified chart
copy and passed in **4m 3s**, 1,741 tasks, including **app lint against the
unchanged baseline**. All 32 period action cases and eight action-color cases
passed; twenty-four sampled-trend candidates span the three longer ranges and
eight windows. Pure tests cover sampling/date coverage, exact aggregate totals,
sample endpoint geometry, quiet/unknown history, draft save/restore, same-day
and DST boundaries, logo shape and source/layout contracts. Selected candidates
were inspected in the [review gallery](../../artifacts/ui-review/four-fixes-2026-11-04/README.md).

The first whole-project gate passed in **7m 24s**. Final workflow review then
caught that a valid prefilled relative count still needed an explicit way to
activate it without editing the number. The selected relative-option chip and
eight additional real-control cases address that edge case.

Final whole-project verification passed in **12m 25s**, **2,127 tasks**:
`artifacts/verification/v2-four-fixes-final-2026-11-04.log`.
Aggregate **2,185 tests / 205 suites**, zero failures/errors/skips: **82 added
cases** over the 0.5.0 source checkpoint. These include forty period-control
cases, eight actual account-action ink cases, twenty-four longer-range trend
candidates and ten pure model/token/source regressions. App compilation,
Android/Room-test compilation and app lint passed against the unchanged
inherited baseline. `git diff --check` passed. No Gradle job remains running.

No Android device is attached. Physical touch/Back, native dialog layering,
keyboard/IME/DeX focus, TalkBack, live account data and owner visual acceptance
remain unverified; LayoutLib control semantics are not hardware evidence.
No screenshot reference was promoted. Storage/backup schemas, dependencies,
manifest/signing identity and financial ledger values remain unchanged.
Version remains **0.5.0 / 229**; no new APK, private-key materialization, commit,
push, tag or publication occurred in this batch. The existing 0.5.0 APK predates
these fixes. Remaining full-v2 public-release gates are still tracked separately
in the execution ledger.

## Subsequent owner-requested APK

After the implementation batch, the owner requested packaging. The official
private release workflow produced **0.5.1 / 230** in **10m 54s**, including all
four fixes above. Actual APK signature, matching owner certificate, package,
non-debuggable manifest, alignment and SHA-256 checks passed; the temporary key
copy was removed. See the [candidate record](../releases/0.5.1-candidate.md).
Source/version are committed locally together before handoff; no push, tag or
publication. This does not close hardware or owner-acceptance gates.
