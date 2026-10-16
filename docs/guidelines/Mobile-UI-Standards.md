# Jvy Wallet Mobile UI Standards

These standards apply to new and modified Android screens, sheets, dialogs, and reusable UI components. They are based on screenshot review of the Samsung Galaxy S22 Ultra test device running Android 16 / One UI 8.

## Layout rhythm and gutters

- Use one clear horizontal gutter per surface. Do not combine a parent gutter with a component's hidden internal gutter.
- More-menu cards must share the same outer alignment. Search, reservation summaries, and open-source cards use the same page gutter.
- Goal and transaction sheets use a 24dp content gutter. Titles, section labels, fields, buttons, and horizontally scrolling selectors must align to that same gutter.
- Shared components that already add padding, such as legacy modal titles, must not be nested inside another padded title treatment. Prefer a local title style with no hidden inset when a sheet owns its layout.
- Keep 8dp for closely related elements, 16dp for neighboring groups, 24dp for major sections, and 32dp only for a deliberate screen-level transition.
- Do not use arbitrary spacer weights or one-off offsets to repair a row. Use a real grid, a stable gutter, or a scrollable container.
- Horizontally scrolling chip rows must remain clipped inside the sheet, include equal start/end content padding, and keep the section label aligned with the row.

## Safe areas and bottom sheets

- Respect status-bar, navigation-bar, gesture, and keyboard insets.
- Keep bottom-sheet actions clear of the gesture area and avoid placing critical content at the sheet edge.
- Validate sheets at both the smallest supported width and the S22 Ultra width. No title, amount, or label may be clipped or begin at a different inset without a deliberate hierarchy reason.
- A sheet should have one visual content column. If a shared component adds padding, document it or replace it with an unpadded variant.

## Numeric and money formatting

- All user-facing money values use grouped digits: `7,000.00 AED`, never `7000.00 AED`.
- Display values use a consistent two-decimal money format unless the product explicitly chooses a currency-specific precision.
- Amount and currency must remain visually grouped and should not wrap independently on normal phone widths.
- Numeric inputs accept an unformatted canonical value but display grouping while the user types. Commas are presentation; they must be removed before parsing or persistence.
- Limit money input to digits, one decimal separator, and the supported fractional precision. Preserve a trailing decimal while the user is editing.
- Reuse the shared money formatter for cards, summaries, details, validation messages, and confirmation text. Do not introduce local `Double.toString()` display formatting.
- Goal creation, goal editing, and account-reservation inputs must use the same grouped-number behavior and the same parsing rules.

## Typography and hierarchy

- Keep primary amounts prominent, but do not shrink supporting labels to make a crowded layout fit.
- Labels, helper text, and buttons must remain readable at normal device scale.
- Use copy that describes the accounting truth. Goal allocations are **reserved**, not **saved** or transferred, unless a real transaction occurred.

## Home insight cards and trends

- A Home insight card must answer a different question from the balance and cash-flow header. Do not repeat the same current-period total as a second “insight.” Use a trend, explanation, or next action instead.
- Trend cards must work with one populated month. Never fabricate zero-value history just to fill a selected range; retain calendar periods as unobserved gaps, show the available period, and explain that more months will make the trend clearer.
- A calendar-month trend must use calendar-month boundaries, independent of the user's custom budgeting-period start day. Never label a shifted fiscal period as if it were a calendar month.
- Range controls should use compact, horizontally scrollable chips with a clear selected state. The default range is short enough to scan, while longer ranges remain one tap away.
- A chart must expose its series in a visible legend and provide an in-context editor when users may not want every series. Do not bury chart visibility settings in global Settings.
- Chart editing must preserve at least one visible series, use plain labels (`Income`, `Spent`, `Balance`), and keep the editor aligned to the same 24dp content gutter as other dialogs.
- Use the existing UI palette for chart series: green for income, red for spending, and the primary accent for balance. Keep the card surface neutral so the lines carry the emphasis.
- Missing or null periods must remain gaps. Never connect two real observations across an unobserved period, and never convert missing activity into a zero-value data point.
- A single observed period renders as points without a connecting line and uses explanatory copy; it must not imply a trend that the data cannot support.
- Financial charts must show a readable value scale. When all observed values are non-negative, use a visible zero baseline; only extend below zero when a real observed value is negative. Scale bounds are calculated from observed values only.
- Sparse history, loading, empty, and hidden-balance states are first-class designs. They must not collapse into a blank card or reveal values that the user has chosen to hide.
- Range changes must preserve the chart's measured height and keep the previous result visible under a lightweight updating state until the replacement data arrives.
- Home must not repeat detailed account balances in a dashboard card. The Accounts tab is the authoritative account-management surface; Home may show only derived reservations or spendability when that answers a different question.
- Trends support `7D` and `30D` daily buckets plus `3M`, `6M`, and `12M` monthly buckets. Use explicit date/YearMonth identities for ordering; never sort by display labels.
- Before the first observed activity, periods are gaps. After activity starts, quiet periods are real zero-activity points. A single activity period is rendered as a point without a connecting line.
- Keep chart height fixed while changing ranges. Preserve the previous plot under a small updating overlay instead of collapsing the card.
- Use one explicit horizontal plot domain for every trend range. Daily and monthly anchors share the same left/right plot insets; monthly points use the first day of each month as their anchor and must not be positioned against month-end padding.

## Home density and activity

- Prefer one neutral activity surface with compact rows over one large card per transaction. Home rows show category color/icon, title, account, and grouped amount; descriptions and tags stay on the edit screen.
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
- Full transaction history may be denser than Home activity, but it must retain comfortable touch targets, compact date grouping, and enough bottom inset for navigation controls.
- Redesigns must not change navigation destinations, ledger calculations, import behavior, backup format, hidden-balance rules, or goal reservation semantics.
- For each redesigned screen, verify empty, loading, error, long-name, large-number, hidden-balance, and bottom-navigation states before release.

## Reports filter behavior

- Reports must open with a useful default: the current app period, all transaction types, all accounts, and all categories.
- A filter sheet must support narrowing one dimension independently. Selecting only a month, type, account, category, amount, keyword, or tag must produce a report without requiring unrelated sections.
- Empty selections in a dimension mean “all” rather than “invalid.” This keeps Clear and Select All predictable and prevents silent no-op filter applications.
- Apply in-progress filter-sheet state consistently across every control. Amounts, keywords, tags, period, accounts, and categories must read from the draft filter until Apply is pressed.
- A valid filter with no matching transactions must still render the report shell with zero totals and an explicit no-results explanation. Never make the screen appear unchanged or blank.
- The current month/period is the default time range; the period picker remains available for historical month and custom-range analysis.

## Trend metric semantics

- The primary purple trend is `Balance`: the wallet balance at the end of each calendar period, calculated from the ledger through that period boundary.
- `Income` and `Spent` remain flow metrics for the selected period. They explain why the balance moved; they are not balance values themselves.
- Never label income minus spending as `Saved`, `Net worth`, or `Balance`. It is only a cash-flow surplus/deficit.
- A true net-worth trend is a separate future capability that must combine historical asset balances and liabilities. Until that exists, show `Balance` and do not imply that it represents net worth.
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

