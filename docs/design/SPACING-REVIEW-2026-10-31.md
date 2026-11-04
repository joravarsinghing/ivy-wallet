# Spacing follow-up — 31 October 2026

This follows the verified priority-reliability batch. It is a targeted
shared-layout correction, not a new palette/type system or whole-app acceptance.
No financial calculations, storage formats, navigation callbacks, signing or
version numbers are changed. No APK or screenshot-golden promotion is authorized.

## Findings and implementation

| Finding | Correction |
| --- | --- |
| Legacy forms reserved a Close-only row, then 24–32dp before another title and another large gap before fields | A shared, persistent `JvyDialogHeader` puts the title and Close in one row, matching the modern form frame. |
| Tags search used 16dp insets while results/actions used 24dp; Add touched search and the last row touched the divider | Search adopts the same 24dp gutter; 16dp search-to-results separation and body-bottom breathing room. |
| Search icons expanded proportionally on wide windows | Fixed 24dp glyphs, a 48dp clear target, and flexible input width. |
| Icon/name fields carried obsolete asymmetric 28/36dp offsets; other modal name fields used 32/36dp | One outer 24dp gutter and a 16dp icon-to-name gap; no hidden second field gutter. |
| Shared colour-picker labels began 8dp farther in than adjacent fields | Align the label to 24dp and use an 8dp label-to-swatches gap; saved colours and selection behavior are unchanged. |
| Loan type choices lacked label/row separation, and Pending/Completed chips were cramped at large text | 8dp label/wrapped-row gaps, 16dp group gaps and filters with at least 48dp height. |
| Large-text page titles touched the navigation/action row | 8dp separation in the shared stacked header. |
| Icon/category/month pickers retained 56–160dp footer-compensation spacers after footers became in-flow | Remove obsolete compensation; bounded scroll content and pinned actions remain. |
| Recurrence mixed 32dp offsets, 40–64dp gaps, weighted mode labels and a number field that squeezed the unit off-screen at 200% text | Use wrapping shared mode buttons, an in-flow date button and 8/16dp grouping; stack the number/unit row on narrow or large-text windows. Preserve existing interval and date callbacks. |
| Month choices used oversized pill padding and a separate wrapping implementation; the first candidate still had ragged rows | Use shared outlined controls in equal-width 1–3-column rows, adapting to available width and font scale, with a 24dp gutter and explicit selected semantics. |
| Month/icon selectors used non-saveable selections and generated default identities during recomposition | Retain only primitive month/icon/session values with saveable state while visible; closing/reopening reinitializes from the caller. This is not whole-route process-death proof. |
| Populated-goal error rows added a second gutter inside the grid | Error content now inherits the populated grid inset; empty errors still own one gutter. |

The title migration covers 17 adapters: account, category, category selector,
calculator, recurrence, month, currency, icon, month-start day, loan, repayment,
tag selection, tag editing, transaction type, sort, budget and manual rate.
Goals, shared date/time pickers and Home customization dialogs inherit the same
modern form header. Untitled legacy modal adapters retain their compatibility
path; specialized net-worth/security frames are not falsely marked migrated.

## Design decisions

UI/UX Pro Max's native-app 4/8dp rhythm, touch spacing, hierarchy and inset
guidance informed the fixes. The focused search returned Touch Spacing; its web
text-reflow result is not a native layout specification. The Compose query
reinforced modifier-owned padding and avoiding extra/nested scrolling containers.
Android adaptive guidance informs the window matrix and retained state; its
newer Navigation 3/Compose Grid prerequisites are outside this compatible spacing
pass. No framework migration or experimental API is introduced.

`JvySpacing` records the existing rhythm (8/16/24dp, 12dp action inset, 16dp modal
margin, 48dp targets), rather than making every screen more spacious. Closely
related content stays grouped; titles and footers stay reachable. Preserve
Raleway/Open Sans, semantic neutral surfaces, current primary navigation, native
disabled/pending behavior and financial privacy.

## Verification

First gate `artifacts/verification/spacing-integrated-2026-10-31.log` caught a
missing `Arrangement` import in the budget change; corrected, not suppressed.
The header/selector gate
`artifacts/verification/spacing-review-2026-10-31.log` exposed an outdated source
assertion after the earlier transaction guard moved into the stronger
route/revision helper. The assertion now verifies the actual helper and policy,
not a removed literal. Its new recurrence render also confirmed the squeezed
unit selector and drove the follow-up above.

The audited cross-module Kotlin sources are now explicit unit-test inputs, so a
navigation/form source edit cannot leave this source guard incorrectly
up-to-date. The first registration used parent file trees and Gradle rejected
their overlap with generated task outputs
(`artifacts/verification/spacing-final-2026-10-31.log`); inputs were narrowed to
the actual source-file set, excluding build outputs. No task-order workaround or
assertion suppression was added.

The later `spacing-verified-2026-10-31.log` passed 1,804 unit/render tests and
failed one icon-selector composable parameter-order lint. The parameters/callsite
were reordered, not suppressed. The following `spacing-reviewed-2026-11-01.log`
was deliberately stopped for the cross-session validation/inset follow-up.

Final integrated gates passed:
`artifacts/verification/v2-integrated-final-2026-11-01.log` (14m 15s), followed by
`artifacts/verification/v2-integrated-reviewed-2026-11-01.log` (1m 56s) including
the loan-detail name/type/metric correction found during visual review. App
compilation, **1,842 unit/render tests / 169 suites** (zero failures/errors/skips),
Room-test compilation and app lint passed against the unchanged inherited
baseline. See the [integration audit](../plans/004-v2-integration-audit-2026-11-01.md)
and [reviewed candidates](../../artifacts/ui-review/spacing-2026-10-31/README.md).

New coverage includes compact and long-title modern/legacy frames at all eight
preview windows, actual currency/month/icon/recurring/one-time pickers at all eight windows, a spacing-token
regression and source guards against a second scrolling title. Existing production previews
cover accounts, categories, loans, tags, goals, budgets, planned/transaction forms,
onboarding and shared pickers. Existing candidates are rerendered, not promoted.

Device IME, TalkBack, physical fold/DeX resizing and owner visual approval remain
pending. The wider [v2 execution ledger](../plans/004-public-release-readiness.md)
still has process-death restoration, source-specific real-export import and
exhaustive accessibility/privacy work; spacing renders do not close those gates.
