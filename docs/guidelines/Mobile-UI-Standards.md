# Jvy Wallet Mobile UI Standards

These standards apply to new and modified Android screens, sheets, dialogs, and reusable UI components. They are based on screenshot review of the Samsung Galaxy S22 Ultra test device running Android 16 / One UI 8.

Read the mandatory [Jvy design language](../design/JVY-DESIGN-LANGUAGE.md) first. It owns the shared typography, shapes, semantic colors, brand and navigation rules, and extends verification to narrow phones, square folds, DeX and 200% text. Existing device observations here remain useful, but are not evidence that a newly changed screen has been tested on hardware.

## Layout rhythm and gutters

- Use one clear horizontal gutter per surface. Do not combine a parent gutter with a component's hidden internal gutter.
- More-hub cards must share the same outer alignment. Search, reservation summaries, and open-source cards use the same page gutter. There is no second global Home dropdown; the bottom bar/rail and More hub own navigation.
- Goal and transaction sheets use a 24dp content gutter. Titles, section labels, fields, buttons, and horizontally scrolling selectors must align to that same gutter.
- Shared components that already add padding, such as legacy modal titles, must not be nested inside another padded title treatment. Prefer a local title style with no hidden inset when a sheet owns its layout.
- Keep 8dp for closely related elements, 16dp for neighboring groups, 24dp for major sections, and 32dp only for a deliberate screen-level transition.
- Do not use arbitrary spacer weights or one-off offsets to repair a row. Use a real grid, a stable gutter, or a scrollable container.
- Horizontally scrolling chip rows must remain clipped inside the sheet, include equal start/end content padding, and keep the section label aligned with the row.
- Use `JvySpacing` and the shared title/Close header. Titled legacy modals pass
  their title to `IvyModal`; don't insert a second title row and a large leading
  spacer. Search, result rows and footer actions share one 24dp inset; search
  glyphs have fixed sizes, not width-proportional weights on desktop windows.
- Form name/icon rows own one 24dp outer inset and a 16dp internal gap. A nested
  text field must not add the old asymmetric 28/36dp padding. Full-span grid
  errors inherit the populated grid's gutter rather than adding a second one.

## Safe areas and bottom sheets

- Full pages have one shared top-start Back target; dialogs/sheets have top-right Close/Cancel. Never use a bottom-left X as page navigation or add a second footer Back beside an existing header.
- Use `JvySubsectionScaffold` for subsection header/body/footer ownership. Existing specialized editor/statistic headers delegate to `JvyBackButton`; standalone Scaffold top bars use `JvyTopAppBar`. Preserve callbacks and navigation history.
- Legacy `IvyModal` keeps Close outside its scrolling body and task actions in its footer. Root navigation, welcome/unlock and non-interruptible import states remain deliberate semantic exceptions, not competing page layouts.

- Respect status-bar, navigation-bar, gesture, and keyboard insets.
- Keep bottom-sheet actions clear of the gesture area and avoid placing critical content at the sheet edge.
- Validate sheets at both the smallest supported width and the S22 Ultra width. No title, amount, or label may be clipped or begin at a different inset without a deliberate hierarchy reason.
- A sheet should have one visual content column. If a shared component adds padding, document it or replace it with an unpadded variant.

## Numeric and money formatting

- All user-facing money values use grouped digits: `7,000.00 AED`, never `7000.00 AED`.
- Display values use a consistent two-decimal money format unless the product explicitly chooses a currency-specific precision.
- Amount and currency must remain visually grouped and should not wrap independently on normal phone widths.
- Numeric inputs persist a canonical decimal using `.` but display and accept the app locale's decimal/grouping separators. Use `JvyDecimalField` and `canonicalDecimalInput`; never remove all commas blindly. For example, German `1,50` is `1.50`, while malformed English `1,50` must be rejected rather than becoming `150`.
- Limit money input to digits, one decimal separator, and the supported fractional precision. Preserve a trailing decimal while the user is editing.
- Reuse the shared money formatter for cards, summaries, details, validation messages, and confirmation text. Do not introduce local `Double.toString()` display formatting.
- Goal creation, goal editing, and account-reservation inputs must use the same grouped-number behavior and the same parsing rules.

## Typography and hierarchy

- Keep primary amounts prominent, but do not shrink supporting labels to make a crowded layout fit.
- Labels, helper text, and buttons must remain readable at normal device scale.
- Use copy that describes the accounting truth. Goal allocations are **reserved**, not **saved** or transferred, unless a real transaction occurred.

## Jvy logo and icon variants

- Use the Jvy color logo for the app launcher, onboarding, marketing renders, and other surfaces where the mark is the primary visual identity.
- Use the Jvy monochrome mark for Android adaptive-icon monochrome layers, notifications, quick settings, and small system surfaces. It must be a single-color silhouette with transparent negative space; do not tint or reuse the full color tile at runtime.
- Use the black mark on white for documentation, print, and high-contrast light surfaces. Use the white mark on transparent or dark surfaces.
- Do not use the inherited Ivy/V logo in new UI, launcher metadata, notifications, onboarding, or marketing assets. Legacy resource files may remain only when they are not active references.
- Keep one canonical color master and derive variants from it so silhouette, cutout placement, and safe-area padding stay consistent across densities.
- App UI uses the existing transparent mark through `JvyLogo` on plain white, not a baked-in squircle, tile, border or shadow. Preserve the aspect ratio and clear space.
- Verify launcher foreground and monochrome assets on both circular and squircle masks. The mark must remain recognizable without relying on the white tile or a surrounding shadow.

## Home insight cards and trends

- A Home insight card must answer a different question from the balance and cash-flow header. Do not repeat the same current-period total as a second “insight.” Use a trend, explanation, or next action instead.
- Trend cards must work with one populated month. Never fabricate zero-value history just to fill a selected range; retain calendar periods as unobserved gaps, show the available period, and explain that more months will make the trend clearer.
- A calendar-month trend must use calendar-month boundaries, independent of the user's custom budgeting-period start day. Never label a shifted fiscal period as if it were a calendar month.
- Range controls should use compact, horizontally scrollable chips with a clear selected state. The default range is short enough to scan, while longer ranges remain one tap away.
- A chart must expose its series in a visible legend and provide an in-context editor when users may not want every series. Do not bury chart visibility settings in global Settings.
- Legend samples are line-only: solid green income, dashed red spending and solid purple balance. Never tint a rectangle behind the samples; retain the approved fills only inside the plot.
- Chart editing must preserve at least one visible series, use plain labels (`Income`, `Spent`, `Balance`), and keep the editor aligned to the same 24dp content gutter as other dialogs.
- Use shared `JvyChartColors` for vivid income/spending/balance plot outlines, not pastel Material text roles or local hardcoded colors. Theme variants retain green/red/purple meaning with at least 3:1 outline contrast on the neutral insight card. Keep text on its AA roles; chart ink is not a small-text color.
- Use one plot matching the owner's reference: an unshaded purple balance polyline over translucent green income and red spending areas, both filled from the same zero baseline. Share the date domain and money scale across all three measures. Do not create a lower cash-flow panel, mirror positive spending below zero, stack areas or normalize series independently. Draw all fills before outlines, with balance last. Flow boundaries may ease between real samples without overshooting their endpoints; keep balance linear and unknown periods disconnected. Preserve genuinely negative reversals in geometry and exact values.
- A percentage such as `43` must become `0.43` before drawing a proportional bar. Hidden balances suppress chart geometry, scales, detailed values, spending bars and percentages, not just footer amounts.
- Missing or null periods must remain gaps. Never connect two real observations across an unobserved period, and never convert missing activity into a zero-value data point.
- A single observed period supplies exact summaries and explanatory copy, without a dot or connecting line; it must not imply a trend that the data cannot support.
- Financial charts show one readable currency scale and a visible zero baseline. Extend below zero only for a real negative observed value, never merely because a value is spending. Scale bounds use all observed, visible measures; equal amounts must occupy equal heights regardless of series.
- Sparse history, loading, empty, and hidden-balance states are first-class designs. They must not collapse into a blank card or reveal values that the user has chosen to hide.
- Range changes preserve chart height and keep the previous result visible until replacement data arrives. Do not cover it with a loading pill. A slow refresh may show a thin top-edge indicator after 350ms; reserve its height so appearing/disappearing feedback never moves the graph.
- Home must not repeat detailed account balances in a dashboard card. The Accounts tab is the authoritative account-management surface; Home may show only derived reservations or spendability when that answers a different question.
- Trends support `7D` and `30D` daily buckets plus `3M`, `6M`, and `12M` monthly buckets. Use explicit date/YearMonth identities for ordering; never sort by display labels.
- Before the first observed activity, periods are gaps. After activity starts, quiet periods are real zero-activity observations. A single activity period has exact values and no trend line or marker.
- Keep chart height fixed while changing ranges. Preserve the previous plot instead of collapsing the card or obscuring it with an overlay.
- Use one explicit horizontal plot domain for every trend range. Daily and monthly anchors share the same left/right plot insets; monthly points use the first day of each month as their anchor and must not be positioned against month-end padding.
- After data arrives, fade outgoing content/dates/axes over 160ms and reveal the new exact paths from left to right over the shared 360ms chart-update duration. Initial display does not animate. Exact summaries update immediately. Loading/editor-only recompositions do not create animation targets. Respect platform duration scaling, including disabled animations; no polling, animated digits or decorative loops. Cache paths/brushes on data/size changes and read reveal progress only in drawing. Calculate buckets off the UI thread, retaining the latest-request guard. Hidden mode removes outgoing plots immediately, and changing currency clears the previous plot before relabelling values.

## Home density and activity

- The selected chart range controls the plot, spending breakdown and recent activity together. Show the same explicit date-window caption on all three. The top-bar period supplies the historical endpoint; it must not silently clip a rolling range to that month. Publish these results together after the latest-request check. On a currency change, clear old currency-dependent results together before relabeling.
- The spending chart drill-down must retain those exact dates, including after process restoration. Do not serialize a potentially unbounded list of transactions to represent a date range. Upcoming payment actions are a separate panel, not past activity; rolling history filters must not hide Pay/Get/Skip controls. View all remains the explicit all-history escape.
- Prefer one neutral activity surface with compact rows over one large card per transaction. Home rows show category color/icon, title, account, and grouped amount; descriptions and tags stay on the edit screen.
- Recent-activity category glyphs use the same `findContrastTextColor(categoryBackground)` policy as All Transactions, including fallback icons and uncategorized rows. Never tint them with the page/theme foreground: a bright category background needs a dark glyph even in dark/AMOLED mode. Keep row labels styled for their own neutral card background, not the category circle.
- Bound Home activity to seven recent records, retain date headings, ellipsize long titles/account names, and keep the final row above the bottom navigation inset.
- A spending breakdown uses ranked horizontal bars: top three categories plus `Other`, amount and percentage, existing category colors, and an honest empty state when no categorized expenses exist. Do not add a second donut or duplicate forecast card.
- Every Home card answers one question and has one primary destination/action. Card visibility preferences may hide optional cards but must never change financial calculations.

## Cross-screen density baseline

- The newer Home interface is the baseline for Accounts, Goals, Transactions, Reports, Budgets, Planned Payments, and Settings: normal-sized controls, readable hierarchy, and useful information visible without excessive scrolling.
- Use a 24dp page gutter, 16dp standard card inset, 12–16dp section spacing, and 8dp spacing for related values. Reserve 24dp+ spacing for real section transitions.
- Prefer compact rows, inline summaries, and expandable details over large colored cards that repeat the same information. A card should earn its surface by communicating hierarchy or an action.
- Keep primary amounts prominent but avoid display-sized numbers for every row. Use tabular/grouped money formatting and right-align comparable numeric values.
- Accounts should show balance, reserved, and available money in a compact hierarchy; the Accounts tab remains authoritative and Home should not duplicate account cards.
- Goals should lead with progress, remaining amount, deadline guidance, and account allocations without oversized hero panels. Use a smaller summary surface and denser goal rows.
- Planned Payments places its title/Back before forecasts. Empty state uses the shared planning empty-state pattern and one Add action; do not lead with a redundant zero forecast. Populated forecasts retain their calculations, currency context and readable stacked metrics at narrow/large-text sizes.
- Goals creation/editing uses the shared bounded `JvyFormDialogFrame`, wrapping type chips, a real optional calendar deadline and the wallet currency. Keep Close and submit outside the scrollable body. Allow correcting a goal without deleting its reservations. Retain drafts and show save errors rather than optimistically closing before storage succeeds.
- Theme defaults must work in production's no-Surface host. Provide semantic foregrounds at `IvyMaterial3Theme` and shared heading boundaries; do not mask missing defaults with preview-only surfaces. Check light, dark and AMOLED headings, empty states, cards and fields.
- Date fields must select actual calendar dates, preserve the chosen day across time zones and storage reload, and never silently clear malformed nonempty input. Date display is localized; persisted contracts are unchanged.
- Reuse `JvyDatePickerDialog` / `JvyTimePickerDialog` through the shared picker facade or thin field adapter. Legacy activity callbacks delegate to the same implementation. A field inside another dialog hosts the shared picker locally to preserve correct window layering; that is not permission to duplicate picker content/state setup. Keep Done, cancellation, typography, shapes and insets consistent across callers.
- Date-only values stay `LocalDate`; transaction timestamps enter via the facade's `Instant` adapter. Respect inclusive minimum/maximum dates. Closing a chooser must never modify a date or time, and a stale chooser must not complete or cancel a newer request.
- Reject invalid or oversized reservations with a useful error instead of silently reducing amounts. Do not infer currency conversion: new reservations use matching-currency accounts and preserve existing unsupported allocations with a warning.
- Full transaction history may be denser than Home activity, but it must retain comfortable touch targets, compact date grouping, and enough bottom inset for navigation controls.
- Transfer headers bound both account labels. Stack them on narrow/large-text
  windows and use balanced columns at wider widths; never squeeze the receiving
  account into an unweighted remainder or stretch a shared capsule vertically.
  Preserve each stored identifier colour with a readable foreground. Receiving
  equivalents follow hidden-balance policy and retain significant crypto digits.
- Redesigns must not change navigation destinations, ledger calculations, import behavior, backup format, hidden-balance rules, or goal reservation semantics.
- For each redesigned screen, verify empty, loading, error, long-name, large-number, hidden-balance, and bottom-navigation states before release.

## Reports filter behavior

- Reports must open with a useful default: the current app period, all transaction types, all accounts, and all categories.
- A filter sheet must support narrowing one dimension independently. Selecting only a month, type, account, category, amount, keyword, or tag must produce a report without requiring unrelated sections.
- Empty selections in a dimension mean “all” rather than “invalid.” This keeps Clear and Select All predictable and prevents silent no-op filter applications.
- Apply in-progress filter-sheet state consistently across every control. Amounts, keywords, tags, period, accounts, and categories must read from the draft filter until Apply is pressed.
- A valid filter with no matching transactions must still render the report shell with zero totals and an explicit no-results explanation. Never make the screen appear unchanged or blank.
- The current month/period is the default time range; the period picker remains available for historical month and custom-range analysis.
- Restored/saved reports resolve selection IDs against live records. A missing or
  malformed explicit selection must trigger review, not be dropped and normalized
  to “all.” An intentionally empty selection still means all. A null saved period
  means default; an explicitly present empty range means all-time.
- Save query primitives and draft identity, never result/account object graphs.
  Clear and a temporarily invalid amount range are genuine editable draft states;
  preserve them across recreation without applying them. Explain invalid bounds
  beside the amount controls and disable Apply until corrected. Returning from
  editing refreshes the applied query without resetting the draft.

## Trend metric semantics

- The primary purple trend is `Balance`: the wallet balance at the end of each calendar period, calculated from the ledger through that period boundary.
- `Income` and `Spent` remain flow metrics for the selected period. They explain why the balance moved; they are not balance values themselves.
- Never label income minus spending as `Saved`, `Net worth`, or `Balance`. It is only a cash-flow surplus/deficit.
- True net-worth history lives in the Net worth feature and combines dated asset values and liabilities. Home's ledger trend still shows `Balance`; it must not be relabeled as net worth. Accounts must not duplicate the wealth summary/calculator.
- Pre-activity periods remain blank. Periods after the first observed transaction can show a real zero-flow point and a carried balance.

## Screenshot review checklist

Before merging a UI change, inspect a current-device screenshot for:

1. Shared left and right gutters across neighboring surfaces.
2. Title, label, field, and button alignment inside sheets.
3. No clipped text, chips, amounts, or buttons.
4. Consistent spacing between search, section headers, grids, cards, and actions.
5. Grouped money formatting in every state, including zero, large values, and decimal input.
6. Safe-area and bottom-navigation clearance.
7. A clear visual hierarchy without nested-card or spacer hacks.
8. Trend cards still make sense with one month, no history, and a selected metric hidden.
9. Chart editors have a visible close action, preserve one visible series, and do not clip on the S22 Ultra.

