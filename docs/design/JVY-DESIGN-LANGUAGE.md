# Jvy Wallet design language

Status: contributor standard, established 2026-10-24. Applies to every new or modified app surface. Preserve the original Ivy character, with a calmer scale and one consistent implementation. This document complements the [Mobile UI Standards](../guidelines/Mobile-UI-Standards.md); this document owns typography, navigation, shapes and brand tokens.

## Product character

Jvy is a calm, local-first money manager for people who do not think in accounting terms. Be understandable before being impressive. Use neutral surfaces, useful numbers, familiar icons, short explanations and one clear next action. Avoid decorative dashboards, giant repeated totals, glass effects and ornamental gradients. A redesign must never change ledger truth, reservations, valuation formulas, import/export compatibility or privacy defaults.

Reuse existing translated string resources when restyling an established label. Do not replace those translations with a hard-coded English heading or metric. New English explanations need localization review before claiming complete translated-screen coverage.

## Source of truth

- Shared tokens and components: [`JvyDesign.kt`](../../shared/ui/core/src/main/java/com/ivy/design/system/JvyDesign.kt).
- Planning-page headers, primary actions and empty states: [`JvyPlanningComponents.kt`](../../shared/ui/core/src/main/java/com/ivy/design/system/JvyPlanningComponents.kt).
- Subsection shell, canonical Back/Close controls and standalone app bars: [`JvySubsectionScaffold.kt`](../../shared/ui/core/src/main/java/com/ivy/design/system/JvySubsectionScaffold.kt).
- Theme and semantic colors: [`IvyMaterial3Theme.kt`](../../shared/ui/core/src/main/java/com/ivy/design/system/IvyMaterial3Theme.kt).
- Date/time selection: [`JvyPickerDialogs.kt`](../../shared/ui/core/src/main/java/com/ivy/ui/time/JvyPickerDialogs.kt), the existing injected `DateTimePicker` facade and the thin `JvyDateField` adapter. Do not introduce another platform dialog or feature-owned calendar.
- Legacy bridge: [`IvyWalletDesign.kt`](../../temp/old-design/src/main/java/com/ivy/design/api/systems/IvyWalletDesign.kt). It uses the same bundled families and size scale; it retains legacy baseline offsets and weights for compatibility.
- Use `IvyMaterial3Theme`, `MaterialTheme.typography`, `JvyTypography`, `JvyShapes` and the `JvyButton` / `JvyOutlinedButton` / `JvyTextButton` components. Do not create a feature-specific font family or hard-code another palette.
- Existing screens can migrate incrementally. Do not rewrite the navigation framework or upgrade Compose just to copy a reference implementation. This project currently uses Compose 1.6.8 and Material 3 1.2.1.

## Typography: Ivy's families, a bounded scale

Use the bundled **Raleway** family for interface text and **Open Sans** for dedicated monetary/numeric values. No default Roboto on a new page, no downloaded fonts, and no new brand face. Text sizes use `sp` and follow Android's font scale; never force density to shrink text.

| Role | Shared token / Material role | Size / line height | Weight |
| --- | --- | --- | --- |
| Page heading | `PageTitle` / `headlineMedium` | 28 / 36sp | ExtraBold |
| Subpage heading | `headlineSmall` | 24 / 32sp | Bold |
| Section heading | `SectionTitle` / `titleLarge` | 20 / 28sp | Bold |
| Card heading | `titleMedium` | 18 / 24sp | Bold |
| Strong row label | `titleSmall` | 16 / 24sp | SemiBold |
| Body | `Body` / `bodyLarge`, `bodyMedium` | 16 / 24sp | Medium |
| Supporting explanation | `Supporting` / `bodySmall` | 14 / 20sp | Medium |
| Button label | `labelLarge` | 14 / 20sp | Bold |
| Short metadata | `Label` / `labelMedium`, `labelSmall` | 12 / 16sp | SemiBold |
| Main amount | `Amount` | 24 / 32sp | Bold, Open Sans |
| Secondary amount / numeric input | `AmountSmall` | 16 / 24sp | SemiBold, Open Sans |
| Exceptional single hero amount | `AmountLarge` | 32 / 40sp | Bold, Open Sans |

All Material display roles are deliberately capped at the page-heading size. Do not use display-sized text for routine rows. Use the 32sp amount only when a single value genuinely needs it and long amounts have been checked. Legacy `h1/h2/b1/b2/c` now map to 28/24/20/16/12sp, retaining Ivy's original heavier heading weights.

Money uses zero letter spacing and tabular figures (`tnum`). Keep an amount and its currency visually together where space allows. At large text sizes, wrap or stack supporting content rather than ellipsizing a financial value or shrinking the font. Mixed explanatory sentences use the interface family; standalone amounts use Open Sans. Never reduce helper copy below 14sp to fit another card.

## Color and surfaces

Use theme roles, not raw hex values in features. Purple is the brand/action accent, not the background of every panel. Cards are neutral, with subtle borders/dividers and no default shadow stack. Support light, dark and AMOLED.

| Meaning | Light | Dark | Use |
| --- | --- | --- | --- |
| Brand action | `primary` #5C3DF5 | #B8ABF5 | Navigation, general actions, focus |
| What I own / money coming in | `secondary` #0C7A56 | #8ADBBC | Ownership and inflow accents |
| Ownership container | `secondaryContainer` #E7F4ED | #19372F | Small supporting surfaces |
| What I owe / money going out | `tertiary` #8F4724 | #F0BC92 | Debt/outflow accents, not a warning by itself |
| Debt container | `tertiaryContainer` #FBEEE4 | #3E2B23 | Small supporting surfaces |
| Background | `background` #FAFAFC | #141418 | Page canvas; AMOLED is black |
| Card | `surface` #FAFAFC | #1C1C21 | Card canvas; AMOLED is black |
| Inset panel | `surfaceVariant` #F0F0F4 | #28282F | Wallet summaries, grouped values |

Always pair a container with its matching `on…` role. Text contrast must be at least 4.5:1 and meaningful control outlines at least 3:1. Theme tests enforce shared palette pairs. Preserve user-assigned account/category colors; they are identifiers, not replacements for readable text. Account glyphs adjust only their rendered tint toward white/black when needed for 3:1 visibility; the saved color is unchanged. Labels and icons must explain meaning without relying on color alone. Home charts retain their documented income/expense/balance series meanings.

Legacy account/category identifiers share `identifierForeground` through both
`findContrastTextColor` adapters. Compare actual luminance ratios, not an arbitrary
“dark colour” threshold: bright green also needs a dark foreground. Retain Ivy's
off-white/off-black endpoints where they reach 4.5:1, using pure white/black near
the crossover only when required. This policy is for opaque identifier surfaces;
page text still uses semantic theme roles. Never change a saved category colour to
repair its displayed contrast.

## Shape, spacing and controls

- Controls, PIN fields, method selectors, buttons and small inset panels: **16dp** corner radius (`JvyShapes.Control`). Do not mix a capsule button with a square PIN field in the same flow.
- Cards: **20dp** (`JvyShapes.Card`). Sheets/dialogs: **24dp** (`JvyShapes.Sheet`). A card containing controls may therefore have a different outer radius; sibling controls must agree.
- Page gutter: **24dp**. Standard card inset: **16dp**, or 20dp for a deliberate summary. Related elements: 4–8dp; grouped content: 12–16dp; security sections: 20dp; major transitions: 24–32dp. Avoid double padding.
- Shared rhythm lives in `JvySpacing`: Related 8dp, Group 16dp, Section/PageGutter
  24dp, ModalMargin 16dp, ActionInset 12dp and TouchTarget 48dp. A host owns its
  horizontal gutter once; child rows do not silently add another. Use 8dp from a
  label to its choices and at least 8dp between wrapped rows of independent
  controls; a search-to-action/results transition uses 16dp. Preserve full text
  height rather than squeezing chips back to a fixed compact height.
- `JvyDialogHeader` owns one title/Close row for modern and titled legacy forms.
  Titles align to the 24dp body/footer column; the 48dp Close target stays outside
  scrolling content. Pass `title` to `IvyModal` instead of putting `ModalTitle`
  plus 24–32dp blank spacers below a separate Close row. Titled legacy bodies get
  8dp top and 16dp bottom breathing room; callers still own their horizontal
  fields/selectors. Modern form bodies retain their own documented padding. Do
  not restore old 56–160dp footer-compensation spacers: footers are in layout flow.
- Interactive targets are at least **48dp**. Use the shared button wrappers, Material IconButton, or another explicitly accessible target. Visual chips can be compact but must retain Material's minimum touch target.
- Use one primary action in a decision group. Secondary actions are outlined or text. Allow height to grow with wrapped labels; never fix a text field or button to a height that clips scaled text.
- Planning pages use `JvyScreenHeader` for a predictable top-left Back action, not a mixture of bottom Back text, circular close controls and floating add buttons. Populated pages use `JvyPrimaryAction` in the layout flow; empty pages show one contextual action through `JvyEmptyState`, not a duplicate footer action. Empty states use meaningful static vector icons, not symbols that resemble loading indicators.
- Budgets and Goals use adaptive card grids at sufficient content width (360dp minimum column); large text stays single-column. Summary blocks span all columns. Loans retains its saved list position and places labeled Pending/Completed filters above the content; these filters wrap as complete chips rather than breaking words.
- PIN values, backup codes and other credentials are memory-only state, never `rememberSaveable`, logs, analytics or screenshots. Saveable state is for nonsensitive navigation, selection and drafts only.

## Navigation: one owner per destination

Primary navigation is **Home · Accounts · Plan · Net worth · More**. Use a bottom bar on narrow windows and a rail on wider ones. Do not add a second global dropdown containing the same destinations.

Root sections change through the visible bar/rail, not page-wide horizontal
swipes. Do not restore the legacy Home ↔ Accounts swipe shortcut or add an
invisible five-page pager. Keep vertical page scrolling and control-local
horizontal gestures (such as the month button and chip rows) independent of
root selection; leave Android's system Back gesture intact.

- **Home:** current position, activity and useful insights. Header search is a shortcut, not another menu.
- **Accounts:** everyday account management, wallet totals, balance, reserved and available money. No net-worth card or wealth calculator here. A wallet balance is explicitly not net worth.
- **Plan:** budgets, upcoming payments, goals and loans.
- **Net worth:** the bigger picture, owned items, debts, progress, valuation details and its own contextual tools. This is its only primary home; do not duplicate it in More.
- **More:** reports, search, categories, Security and Settings. Settings owns appearance, wallet currency, sharing and app information. Search in More remains discoverable even when the Home shortcut is not visible.

Contextual tools menus (for example Net worth date, rates and CSV export) are allowed; they must not become another global app menu. Keep existing deep-link/screen entrypoints valid even when a redundant shortcut disappears.

### One subsection navigation contract

- Full-page subsections use **top-left Back** in LTR (top-start with a mirrored arrow in RTL), never a bottom-left arrow/X or text-only Back. Use `JvySubsectionScaffold` for new pages, or `JvyScreenHeader` / `JvyTopAppBar` within an existing shell. Specialized colored statistic/editor headers may use `JvyBackButton` with the existing contrast tint, not a different navigation glyph.
- Back remains a 48dp target with a localized accessible label. The shared control only invokes the supplied callback; it does not decide whether to discard, save, dismiss a modal, pop a substep or return through the stack. Retain the existing destination's handler.
- Root Home/Accounts/Plan/Net worth/More retain the bottom bar/rail and have no artificial Back. The initial welcome/unlock/root legal surfaces do not invent a previous page. Onboarding/import substeps use the same subsection header; an active import shows disabled Back and retains its non-interruptible write guard.
- Real dialogs/sheets use Close/Cancel, not page Back. Shared and legacy modal hosts put Close at the top-right, outside the scrolling body; keep their current dismissal, keyboard and save semantics. Report filters follow this modal rule. Native confirmation dialogs may retain their standard Cancel action.
- A bottom area owns task actions only. Keep it in layout flow above system/keyboard insets; no second exit control, fading bottom navigation line or magic spacer to conceal overlap. An empty page has one contextual Add action, while populated management lists may have one footer Add action.
- Transaction/planned editors use the shared page heading and labelled Actions
  menu for type changes, Duplicate and Delete. Do not wrap a row of destructive
  buttons into a second header row on an ordinary phone. Preserve the real
  transaction type; a loan-record restriction is a separate capability flag, not
  a fake Transfer label. Pending explicit saves disable page Back/context actions.
- Onboarding substeps use the same in-flow subsection shell; welcome has no
  artificial previous page. Keep welcome copy/links scrollable and bound logo/text
  to the current content column, never absolute screen coordinates. Initial
  account cards stack their amount below the name and use a neutral surface with
  a coloured identifier. Suggestions use the same outlined controls, not another
  capsule/shadow system.
- Titles, Back targets and actions use the shared gutters, typography and wrap behavior. Context actions must wrap or move to a labeled menu on compact/large-text windows instead of squeezing the Back target or clipping labels.
- At large text sizes, the stacked page title has 8dp separation from its
  navigation/action row. Local Pending/Completed filters grow to at least 48dp
  instead of clipping a scaled label inside a compact chip.
- Loan-detail names, type labels and dates stack in a bounded text column rather
  than squeezing a type badge into the last few pixels of a name row. Repayment
  metrics wrap as groups and keep readable contrast on legacy identifier-coloured
  headers. Use actual navigation insets plus a final 24dp list inset, not footer
  compensation spacers.
- Audit every registered route and its nested steps when changing navigation. `NavigationLayoutContractTest` requires an explicit entry for every destination and rejects legacy bottom-exit bars or Close-as-Back. This source guard complements, not replaces, actual renders and device navigation testing.
- A route's arguments are not its visit identity. `Navigation.currentEntryId`
  stays unchanged across rotation/theme/capture recomposition, gets a fresh value
  for every forward visit (even equal arguments), and restores the earlier value
  on Back. Saveable page state is keyed to that entry; discard its state after the
  entry leaves history. Editor initialization guards compare entry identity as
  well as arguments. Pending writes consume system Back, and a late save result
  may close only the entry that initiated it. Do not reuse the preceding new
  transaction merely because both visits have a null transaction ID.

## Financial clarity and privacy

- Every financial view should expose a useful contextual visual, reusing the
  shared chart renderer and pure reducers. Reports → Explore remains the deep
  analytics workspace, not the sole place with charts. Follow the
  [contextual placement audit](CONTEXTUAL-CHARTS-2026-11-07.md) for each page and
  subview; justify omissions by task relevance or insufficient trustworthy data.
  Editors, security, import confirmation and navigation hubs do not get
  decorative charts. Existing relevant progress/pie/history visuals count;
  do not add duplicates merely to increase chart count.

- Deep visual analysis lives in **Reports → Explore**, not additional Home date
  controls or root navigation. Use the [Explore contract](REPORTS-EXPLORE-2026-11-07.md)
  for scope, supported charts, advanced disclosure, exact-value fallbacks and
  read-only workspace access. Clearly distinguish report-filtered historical
  flows from current wealth, loan and reservation workspaces. Never invent
  merchant identity, contribution events, liquidity or forecast probabilities.

- Home owns one explicit date window outside optional cards. Current-cycle, selected budgeting month, rolling, custom and all-time modes drive income/expense summaries, plot, spending breakdown and recent activity together. Default to the current budgeting cycle through today; past cycles use their full bounds, and rolling choices end today independently of any previously selected month. Show actual dates and preserve custom cycle starts. Chart Edit changes series, not dates. Publish historical results together, retain exact drill-down dates and clear old-currency results together. While loading or retrying, keep the previous complete result and its previous date caption. Current balance and the separately labelled Today & upcoming group are not historical values. Saved reports retain their saved queries. See the [single-timeline checkpoint](../plans/004-v2-single-home-timeline-2026-11-04.md).

- Import assistance uses a copyable public schema/guide and a read-only validation/count preview before explicit confirmation. Never automatically send financial data to an AI, imply validation proves accounting accuracy, silently discard unsupported fields, save imported payloads into UI saved state, or let an AI conversion alter authentication/preferences. See the [AI conversion contract](../import-compatibility/AI-CONVERSION.md). Keep advanced schema text behind an explicit disclosure; retain a reachable Back control and confirmation action across window sizes.

- Prefer “What I own”, “What I owe”, “Amount left to repay”, “Your share” and “Review this estimate” over unexplained accounting jargon. Put help next to an unfamiliar concept, not solely in a distant tutorial.
- Net worth = assets minus liabilities. Show the reporting date/currency, incomplete status, stale estimates and provenance. Never imply the total is spendable cash. Link existing wallet accounts/loans before encouraging duplicate manual entry.
- Reuse the shared grouped-money formatter: `12,345.67 AED`. Keep canonical decimal inputs/parsing unchanged. Do not introduce `Double.toString()` as a display formatter.
- Use `JvyDecimalField` for amount entry: localized separators on screen, canonical decimal strings in state/storage, strict grouping validation, and a visible error for ambiguous input. Never strip commas before determining their meaning.
- Goals use the wallet currency, not a hardcoded currency label. New reservations require matching-currency accounts until explicit conversion is supported; preserve and flag existing foreign/unavailable-account allocations, never silently convert or discard them. Reject amounts above the available balance instead of clipping them. The existing goal storage has no currency field, so changing the wallet currency still needs a separate data-model/conversion design.
- Dates are typed calendar values, not arbitrary text. Use `JvyDateField` for new optional deadlines and wealth date inputs, localize only their display, and keep existing storage contracts. Convert Material picker milliseconds using UTC calendar dates so western time zones do not select the previous day. Invalid nonempty legacy deadlines must produce an error, never silently become “No deadline.”
- Calendar day grids must show all seven columns at 320dp and at 2× text scale. Use proportional columns, locale-specific week starts, full-date accessible button labels and selected/disabled semantics. Keep month/year navigation and confirmation reachable in short windows; never shrink the user's chosen text scale to fit a fixed-width calendar.
- Income/expense, loans, date-only fields and legacy date/time entry points share the picker dialogs and bounded form frame. `pickDate(Instant?)` converts an existing transaction timestamp to its device-local day; `pickCalendarDate(LocalDate?, minDate, maxDate)` must not perform that conversion. Consumers retain responsibility for recombining transaction dates/times and storing UTC timestamps. Calendar-only deadlines keep their existing date storage contract. Enforce inclusive date limits, require explicit confirmation, and never invoke a value callback on Close, Back or outside dismissal.
- Time selection respects the system's 12/24-hour preference. Retain Material clock editing on suitable windows and native numeric entry for narrow/short/large-text layouts; the same confirmed hour/minute survives presentation changes. Date and time dialogs use the existing translated Done action and shared localized cancellation semantics. New supplemental copy still needs translation review.
- Unknown is not zero. Empty, loading, error, incomplete and genuinely zero states are different. Charts retain missing observations as gaps; no invented history.
- Balance trends follow the owner's single-plot reference: a vivid, unshaded purple balance polyline in front of overlapping green income and red spending fills. All three share one date domain, one money scale and one zero baseline. Ordinary positive income and spending both rise from zero; do not mirror spending below it, split out a cash-flow panel, stack areas or normalize each measure independently. Genuinely negative values retain their signs and extend the shared scale below zero. Flow boundaries may use bounded curves through real samples, without overshoot; the balance remains a polyline. No dots, invented peaks or bridging missing buckets. An isolated observation supplies exact values and an honest explanation, not an invented line. Optional balance shading is a legacy test comparison, not the default or a persisted preference.
- Use `JvyChartColors` for saturated purple/green/red plot ink, with theme-specific variants tested at 3:1 on neutral card surfaces. These are chart-outline tokens, not small-text roles: labels and exact totals retain AA text roles. Do not replace global Material colors to brighten a plot.
- Trend legends are line-only: solid green income, dashed red spending and solid purple balance. Do not place tinted rectangles, fills or glows behind these samples; fills belong inside the plot.
- Home's native chart takes visual inspiration from financial terminals, not their trading controls: one compact right-hand value scale, quiet 0.5dp horizontal guides, a readable zero line and a crisp 2dp balance stroke without a thick halo. Saturated flow outlines sit over restrained translucent fills (48% to 8%), never pastel replacement ink. Dates align to the plot rather than extending under the value scale; include years when the domain crosses a year. Keep tiny nonzero axis values distinguishable from zero and very large labels bounded. Exact amounts remain in the native readout. Reuse cached paths/brushes; no WebView, chart package, pan/zoom layer, remote code or additional animation loop for this styling.
- Chart updates use a 160ms outgoing/axis crossfade and a 360ms eased, left-to-right reveal of the replacement paths (`JvyMotion.ChartUpdateMillis`). Exact totals change immediately; never count through invented currency values or morph unrelated daily/monthly observations. Initial display is static. Keep the old plot in place while loading; no floating loading pill or chart-obscuring overlay. Only slow refreshes (350ms+) show a thin, height-reserved top-edge progress indicator. Compose's platform motion-duration scale applies; disabled animations settle immediately. Cache geometry/brushes and read animation progress only while drawing, not during financial calculations. Calculate trend buckets off the UI thread and publish only current-generation results on it. No polling, decorative loops or extra network work. Hiding balances removes all outgoing layers immediately, and currency changes discard the previous plot.
- Chart axes show their currency once; large-text ticks must not clip. Distinct line patterns, visible series labels, exact summaries and an expandable value list complement color. Hidden-balance mode suppresses the plot, scale, detailed values and spending-percentage bars. Convert percentage values to fractions before drawing proportional bars.
- Trend sampling is separate from the chosen Home window: up to thirty days use daily samples; longer exact windows use thirty non-overlapping calendar-date buckets. Sum income/spending inside each bucket and calculate balance at its inclusive end, never average balances or interpolate invented values. Date intervals differ by at most one day. Show exact bucket dates in View values and identify the aggregation in supporting copy. Preserve unknown prehistory, real quiet zeroes, full-window totals and latest-request/motion/privacy guards. Keep query work bounded to thirty buckets, not one query per day over an entire year. Summary and plot use the same existing cash-flow reducer and eligibility (wallet-included accounts, no transfers or system balance adjustments); recent activity may still display those records.
- Period selection uses a real dialog window above root navigation and FABs, the shared bounded `JvyFormDialogFrame`, one scrolling body and a pinned, labelled Apply filter action. Close/Back cancels the draft; selecting a month is not confirmation. Use native selected chips and the shared confirmed calendar fields, including inclusive single-day ranges. Validate only the active choice, not unrelated inactive controls. Save nonsensitive draft primitives across recreation; report-filter period drafts must not commit the global Home period.
- Hidden balances redact **every** amount and any magnitude-revealing presentation, including allocation bars, summaries and currency equivalents. Do not reveal values through an extra card.
- This includes date-divider cash flow, income/expense header and section totals,
  complete category sign/value strings and receiving transfer equivalents. Mask
  before the text reaches accessibility semantics. Hidden summaries must not
  reveal a zero through conditional amount-row presence, or a net-positive value
  through its colour. Keep dates, transaction types and nonmonetary counts readable;
  hiding balances is not general anonymization of user-entered names or notes.
- Read-only presenters use `LocalFinancialVisibility` / `financialText`; the root observes the stored preference reactively. Replace hidden plots rather than merely fading or masking their labels. Explicit editing may expose the field being edited. Exports retain full values and require a private-data warning; never silently redact a financial backup.
- Legacy forms adapt to the same bounded, IME-safe host and labelled 16dp outlined controls. Account/amount controls belong in the scrolling transaction form; Save stays in layout flow, not at a screen-height coordinate. No scroll-padding hacks. Nonsensitive drafts use stable primitive IDs and savers; credentials and imported payloads never enter saved state.
- Horizontal account selectors use real content padding, stable string account
  keys and ID-based selection. Initial selection, refresh and reordering must
  keep the chosen account visible; decorative spacer items must not offset scroll
  indices. Use the same behaviour in previews and production, with labelled,
  48dp-or-larger controls and a readable selected boundary.
- Currency selection is one shared, lazy search/radio list in onboarding and
  dialogs. Save only currency codes/search text, allow long names to wrap, show a
  no-results explanation, and let the containing shell own IME insets. No custom
  keyboard listeners, 120dp footer compensation or random session ID on every
  recomposition. Selection does not write wallet settings until its host confirms.
- Saves require acknowledgement. `EditorSubmission` guards duplicate attempts and retains generic errors; asynchronous feature owners close only after storage success. Do not assume a `Unit` event dispatch means persistence succeeded. New editors must not inherit optimistic legacy callbacks; finish migration before claiming that a legacy flow is release-ready.
- Category create/edit uses that acknowledgement end to end, including nested
  transaction/planned editors and onboarding suggestions. Disable the actual
  name/icon/colour controls and Close while pending; show progress and a polite,
  generic retry error without exposing storage exceptions. Creation retries use
  the original intent UUID, not a new record ID. A refresh failure after a
  committed write is not a failed write. Keep live submission state with the
  ViewModel-owned modal/session across configuration changes, never serialize
  pending operations into a Bundle. Scope late dismissal to its originating
  modal and refresh application to its route entry; an old completion must not
  overwrite a newer category selection. This is not a claim that all legacy
  modal hosts restore after process death.
- Reservations are metadata, not a transfer or transaction. Preserve existing balance/reserved/available calculations.
- Loan/repayment and tag forms follow the same acknowledgement contract. Keep
  their live submission controller and nested account/tag host with the owning
  ViewModel/session, and save only primitive drafts. Pending controls and Close
  are natively disabled; recoverable errors retain the form, and a changed or
  missing record offers Close and review. Stable creation IDs are transient
  intent IDs, not new backup fields. A loan and its linked wallet entry commit
  together; retrying creation must not duplicate the payment or reprice a
  committed repayment. Tag deletion removes associations atomically, never the
  transactions themselves. Tag editing is a visible action, not long-press-only.
- A missing foreign-currency quote is a recoverable error, not an implicit
  same-currency amount. Offer a matching-currency account or an exchange-rate
  correction; keep the draft. Principal edits read their current linked wallet
  entry within the transaction so a stale editor cannot create a second one.
  Preserve its stored instant on metadata-only changes; change its date only
  when the user actually changes the loan date. Queued transaction writes use
  an immutable submitted draft, and only the latest originating route/revision
  may acknowledge, clear or display failure for it.
- Loan date/time controls reuse `DateTimeRow` and the shared confirmed pickers.
  Controls wrap at narrow/large-text widths and keep the current chosen value
  when reopening a picker; a late chooser cannot update a replacement form.
  Recurrence and keypad drafts retain their primitive values during recreation.
  This does not certify full route restoration after process death.
- Recurrence and report `last N` counts must be positive whole numbers. Clearing
  or mistyping the count must invalidate the parent selection, never retain the
  preceding valid value invisibly. Keep the raw nonsensitive draft, show a useful
  error and disable confirmation; existing calendar/all-time ranges stay valid.
- Account forms use the same acknowledged submission pattern, with native
  disabled inputs/Close and a politely announced recoverable error. Creation
  retries retain their original account/opening UUIDs; a confirmed creation must
  not reset later activity. Changed committed drafts or deleted accounts require
  explicit close/review, never silent overwrite or resurrection. Suggestion
  shortcuts retain failed intents but allocate a fresh intent after success,
  including when a renamed item makes the same suggestion visible again. These
  live controllers/intents are not saved credentials or backup-schema additions;
  remaining local modal-host/process restoration still needs separate acceptance.
- Security offers PIN, biometrics or either, while retaining legacy device authentication. Existing fingerprint users can verify their current method and create a backup/recovery code without changing it. There is **one active code**, not a batch of codes; replacement invalidates the old one. Show it once in a protected dialog and ask users to keep it outside Jvy.
- Generating a code requires a fresh current-method proof, an unlocked session and unlocked device. Recovery proof can reset the PIN, not independently rotate credentials. Credentials remain encrypted outside financial backups.
- A change to a net-worth item's accounting type, source or other financial definition creates a dated replacement and archives the preceding setup from that date. Existing historical values are not rewritten. Show the effective date and explain this before saving; earlier valuations remain editable without changing their setup.
- Screenshot/recording capture is explicit opt-in where applicable. Unlock, PIN and recovery surfaces remain protected. Rotation/resizing must not trigger a fresh app lock by itself.

## Logo usage

Use `JvyLogo` in app UI. The canonical color resource `shared/ui/core/src/main/res/drawable-nodpi/jvy_wallet_logo.png` is the existing transparent launcher foreground, reused unchanged (432×432px). The documentation copy is `docs/assets/jvy-wallet-logo-hd.png`; the historical filename does not imply a higher-resolution master.

Place the transparent mark on **plain white**, including in dark-mode layouts, and always clip its app-UI backing with `JvyShapes.Logo` through `JvyLogo`. This shared runtime mask has a **22% proportional corner radius**, keeping 44–88dp marks rounded without turning small marks into circles. No app-UI instance may use a sharp-corner white backing or bypass the shared component. Do not bake a squircle, tile, border or shadow into the artwork. Preserve its clear space and aspect ratio; use Fit, never FillBounds. Use 44–56dp for a supporting brand mark and up to 88dp on unlock/onboarding. Do not invent a new mark or upscale it as a new “HD” source. Android launcher/system masks still belong to the platform; do not alter the transparent master or monochrome silhouette to fake them.

The canonical black mark and documentation black-logo copy also have transparent backgrounds. System monochrome layers remain transparent single-color silhouettes, using Android's adaptive-icon treatment. Do not replace system masking with a baked app-UI tile. The old squircle pixels have been replaced at the existing color/black-logo paths; Git history retains prior assets.

## Responsive behavior and motion

Base decisions on the **current window**, not phone/tablet model or orientation alone. In production, account for the navigation rail's occupied width. Handle rotation, square folds, split screen, keyboard insets and resizable DeX windows without losing nonsensitive state or triggering authentication.

- Primary rail starts at 600dp: 96dp compact, 200dp at 1100dp+ or when font scale exceeds 1.3. Narrow-window large-text navigation shows the selected label with all five accessible targets still present.
- Accounts uses one column, or two when its available content width is at least 840dp and text scale is at most 1.3. Content is centered/capped at 1200dp. Metrics stack at large text sizes.
- Net worth uses a secondary detail pane at 840dp+, capped overall at 1280dp. Tabs and add actions wrap at narrow/large-text sizes. Security uses a centered scrollable column capped at 640dp.
- Critical actions stay above system/navigation/keyboard occlusion or remain reachable by scrolling. Use vertical growth, not clipped labels. Support keyboard/focus and screen-reader semantics for controls.
- Goal forms use `JvyFormDialogFrame`: a centered, bounded neutral sheet (640dp maximum width), persistent Close, scrollable fields and a pinned submit/error area. Use shared control shapes and wrapping choice chips, not weighted text pseudo-buttons or large spacer hacks. Keep nonsensitive drafts and their stable IDs across recreation; do not close or show a new goal until persistence succeeds. Save failures retain the draft with a visible, politely announced error. Goal details offer Edit as well as Reserve money.
- Net-worth dialogs use the shared `WealthDialogFrame`: a bounded neutral surface with a persistent Close control, scrollable body and optional pinned action area. Account review pins Save, uses whole-card account targets and whole-row checkbox semantics, preserves review drafts while visiting an account editor, and returns to review after editing. Detailed repayment education is disclosed on demand rather than repeated as a long page paragraph.
- Account press feedback uses a 0.985 scale over 160ms. Shared transition guidance is 240ms. Use Compose animation APIs so Android's disabled/reduced-duration setting applies. No infinite decorative loops, animated currency digits or data changes disguised as motion.
- Read animation state in the graphics/draw phase when possible. Use stable primitive keys and content types for mixed lazy lists; keep financial work off the UI thread. Do not add speculative stability annotations or dependency upgrades for a visual effect.

## Required verification for UI work

Review current renders and tests, not just a successful compile. Minimum preview matrix:

`IvyMaterial3Theme` supplies `LocalContentColor = onBackground` even without a Material Surface. Nested surfaces/buttons supply their own corresponding foreground. Regression previews must reproduce production's non-legacy no-Surface host; adding an extra preview-only Surface can hide black-on-dark text defects. Shared page and empty-state headings also use explicit semantic foregrounds.

| Window | dp | Font scale |
| --- | --- | --- |
| Small phone | 320×640 | 1× |
| Phone | 360×800 | 1× |
| Square fold | 720×720 | 1×, dark |
| DeX/resizable desktop | 1280×800 | 1× |
| Large-text phone | 360×800 | 2× |
| Large-text square fold | 720×720 | 2×, dark |
| Short landscape window | 640×360 | 1× |

Include empty, populated, long-name, large-number, hidden-balance and error/recovery states relevant to the change. Verify navigation ownership, text wrapping, touch targets, matching control shapes and insets. Shared font/contrast tests, account presentation tests, security authorization tests and Paparazzi previews provide guardrails; they do not replace real-device biometric, screenshot-protection, TalkBack or keyboard checks.

Run unit tests/render checks without building an APK unless requested. **Do not automatically record or replace screenshot goldens.** Provide fresh candidate previews for owner review, then promote references only after explicit approval. Keep financial and import/backup regression suites green. No UI redesign may silently change package identity, signing, version numbers or stored data formats.

When adding or changing a token, update this document and its unit tests in the same change. A feature-specific exception needs a stated reason, accessibility checks and review; it is not a new default.

See the [2026-10-25 screenshot-led consistency review](UI-REVIEW-2026-10-25.md) for the planning, chart and review-dialog rationale and verification record.

The [Home trend motion follow-up](HOME-TREND-MOTION-2026-10-25.md) records the owner's single-plot reference correction, saturated plot tokens, finite update behavior and verification limits.

The [Goals and shared-theme audit](UI-AUDIT-2026-10-26.md) records the dark-mode root cause, calendar and save-flow fixes, regression scope and remaining validation limits.

The [picker coherence follow-up](PICKER-CONSOLIDATION-2026-10-26.md) records entry-point consolidation, timestamp boundaries, legacy limit/cancel fixes and verification scope.

See the [app-wide navigation sweep](NAVIGATION-COHERENCE-2026-10-27.md) for the destination inventory, Planned Payments correction and verification limits.
