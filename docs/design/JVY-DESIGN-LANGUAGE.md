# Jvy Wallet design language

Status: contributor standard, established 2026-10-24. Applies to every new or modified app surface. Preserve the original Ivy character, with a calmer scale and one consistent implementation. This document complements the [Mobile UI Standards](../guidelines/Mobile-UI-Standards.md); this document owns typography, navigation, shapes and brand tokens.

## Product character

Jvy is a calm, local-first money manager for people who do not think in accounting terms. Be understandable before being impressive. Use neutral surfaces, useful numbers, familiar icons, short explanations and one clear next action. Avoid decorative dashboards, giant repeated totals, glass effects and ornamental gradients. A redesign must never change ledger truth, reservations, valuation formulas, import/export compatibility or privacy defaults.

Reuse existing translated string resources when restyling an established label. Do not replace those translations with a hard-coded English heading or metric. New English explanations need localization review before claiming complete translated-screen coverage.

## Source of truth

- Shared tokens and components: [`JvyDesign.kt`](../../shared/ui/core/src/main/java/com/ivy/design/system/JvyDesign.kt).
- Planning-page headers, primary actions and empty states: [`JvyPlanningComponents.kt`](../../shared/ui/core/src/main/java/com/ivy/design/system/JvyPlanningComponents.kt).
- Theme and semantic colors: [`IvyMaterial3Theme.kt`](../../shared/ui/core/src/main/java/com/ivy/design/system/IvyMaterial3Theme.kt).
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

## Shape, spacing and controls

- Controls, PIN fields, method selectors, buttons and small inset panels: **16dp** corner radius (`JvyShapes.Control`). Do not mix a capsule button with a square PIN field in the same flow.
- Cards: **20dp** (`JvyShapes.Card`). Sheets/dialogs: **24dp** (`JvyShapes.Sheet`). A card containing controls may therefore have a different outer radius; sibling controls must agree.
- Page gutter: **24dp**. Standard card inset: **16dp**, or 20dp for a deliberate summary. Related elements: 4–8dp; grouped content: 12–16dp; security sections: 20dp; major transitions: 24–32dp. Avoid double padding.
- Interactive targets are at least **48dp**. Use the shared button wrappers, Material IconButton, or another explicitly accessible target. Visual chips can be compact but must retain Material's minimum touch target.
- Use one primary action in a decision group. Secondary actions are outlined or text. Allow height to grow with wrapped labels; never fix a text field or button to a height that clips scaled text.
- Planning pages use `JvyScreenHeader` for a predictable top-left Back action, not a mixture of bottom Back text, circular close controls and floating add buttons. Populated pages use `JvyPrimaryAction` in the layout flow; empty pages show one contextual action through `JvyEmptyState`, not a duplicate footer action. Empty states use meaningful static vector icons, not symbols that resemble loading indicators.
- Budgets and Goals use adaptive card grids at sufficient content width (360dp minimum column); large text stays single-column. Summary blocks span all columns. Loans retains its saved list position and places labeled Pending/Completed filters above the content; these filters wrap as complete chips rather than breaking words.
- PIN values, backup codes and other credentials are memory-only state, never `rememberSaveable`, logs, analytics or screenshots. Saveable state is for nonsensitive navigation, selection and drafts only.

## Navigation: one owner per destination

Primary navigation is **Home · Accounts · Plan · Net worth · More**. Use a bottom bar on narrow windows and a rail on wider ones. Do not add a second global dropdown containing the same destinations.

- **Home:** current position, activity and useful insights. Header search is a shortcut, not another menu.
- **Accounts:** everyday account management, wallet totals, balance, reserved and available money. No net-worth card or wealth calculator here. A wallet balance is explicitly not net worth.
- **Plan:** budgets, upcoming payments, goals and loans.
- **Net worth:** the bigger picture, owned items, debts, progress, valuation details and its own contextual tools. This is its only primary home; do not duplicate it in More.
- **More:** reports, search, categories, Security and Settings. Settings owns appearance, wallet currency, sharing and app information. Search in More remains discoverable even when the Home shortcut is not visible.

Contextual tools menus (for example Net worth date, rates and CSV export) are allowed; they must not become another global app menu. Keep existing deep-link/screen entrypoints valid even when a redundant shortcut disappears.

## Financial clarity and privacy

- Prefer “What I own”, “What I owe”, “Amount left to repay”, “Your share” and “Review this estimate” over unexplained accounting jargon. Put help next to an unfamiliar concept, not solely in a distant tutorial.
- Net worth = assets minus liabilities. Show the reporting date/currency, incomplete status, stale estimates and provenance. Never imply the total is spendable cash. Link existing wallet accounts/loans before encouraging duplicate manual entry.
- Reuse the shared grouped-money formatter: `12,345.67 AED`. Keep canonical decimal inputs/parsing unchanged. Do not introduce `Double.toString()` as a display formatter.
- Use `JvyDecimalField` for amount entry: localized separators on screen, canonical decimal strings in state/storage, strict grouping validation, and a visible error for ambiguous input. Never strip commas before determining their meaning.
- Unknown is not zero. Empty, loading, error, incomplete and genuinely zero states are different. Charts retain missing observations as gaps; no invented history.
- Balance trends default to a clean polyline with subtle balance-only shading (16% maximum opacity). Do not draw a marker on every observation, smooth through invented values, stack income/spending areas or connect across missing buckets. A truly isolated observation retains a small marker and an honest single-observation explanation. An unshaded-line candidate is available for visual comparison, not as a new persisted preference.
- Chart axes show their currency once; large-text ticks must not clip. Distinct line patterns, visible series labels, exact summaries and an expandable value list complement color. Hidden-balance mode suppresses the plot, scale, detailed values and spending-percentage bars. Convert percentage values to fractions before drawing proportional bars.
- Hidden balances redact **every** amount and any magnitude-revealing presentation, including allocation bars, summaries and currency equivalents. Do not reveal values through an extra card.
- Reservations are metadata, not a transfer or transaction. Preserve existing balance/reserved/available calculations.
- Security offers PIN, biometrics or either, while retaining legacy device authentication. Existing fingerprint users can verify their current method and create a backup/recovery code without changing it. There is **one active code**, not a batch of codes; replacement invalidates the old one. Show it once in a protected dialog and ask users to keep it outside Jvy.
- Generating a code requires a fresh current-method proof, an unlocked session and unlocked device. Recovery proof can reset the PIN, not independently rotate credentials. Credentials remain encrypted outside financial backups.
- A change to a net-worth item's accounting type, source or other financial definition creates a dated replacement and archives the preceding setup from that date. Existing historical values are not rewritten. Show the effective date and explain this before saving; earlier valuations remain editable without changing their setup.
- Screenshot/recording capture is explicit opt-in where applicable. Unlock, PIN and recovery surfaces remain protected. Rotation/resizing must not trigger a fresh app lock by itself.

## Logo usage

Use `JvyLogo` in app UI. The canonical color resource `shared/ui/core/src/main/res/drawable-nodpi/jvy_wallet_logo.png` is the existing transparent launcher foreground, reused unchanged (432×432px). The documentation copy is `docs/assets/jvy-wallet-logo-hd.png`; the historical filename does not imply a higher-resolution master.

Place the transparent mark on **plain white**, including in dark-mode layouts. Do not bake in a white squircle, tile, border or shadow. Preserve its clear space and aspect ratio; use Fit, never FillBounds. Use 44–56dp for a supporting brand mark and up to 88dp on unlock/onboarding. Do not invent a new mark or upscale it as a new “HD” source.

The canonical black mark and documentation black-logo copy also have transparent backgrounds. System monochrome layers remain transparent single-color silhouettes, using Android's adaptive-icon treatment. Do not replace system masking with a baked app-UI tile. The old squircle pixels have been replaced at the existing color/black-logo paths; Git history retains prior assets.

## Responsive behavior and motion

Base decisions on the **current window**, not phone/tablet model or orientation alone. In production, account for the navigation rail's occupied width. Handle rotation, square folds, split screen, keyboard insets and resizable DeX windows without losing nonsensitive state or triggering authentication.

- Primary rail starts at 600dp: 96dp compact, 200dp at 1100dp+ or when font scale exceeds 1.3. Narrow-window large-text navigation shows the selected label with all five accessible targets still present.
- Accounts uses one column, or two when its available content width is at least 840dp and text scale is at most 1.3. Content is centered/capped at 1200dp. Metrics stack at large text sizes.
- Net worth uses a secondary detail pane at 840dp+, capped overall at 1280dp. Tabs and add actions wrap at narrow/large-text sizes. Security uses a centered scrollable column capped at 640dp.
- Critical actions stay above system/navigation/keyboard occlusion or remain reachable by scrolling. Use vertical growth, not clipped labels. Support keyboard/focus and screen-reader semantics for controls.
- Net-worth dialogs use the shared `WealthDialogFrame`: a bounded neutral surface with a persistent Close control, scrollable body and optional pinned action area. Account review pins Save, uses whole-card account targets and whole-row checkbox semantics, preserves review drafts while visiting an account editor, and returns to review after editing. Detailed repayment education is disclosed on demand rather than repeated as a long page paragraph.
- Account press feedback uses a 0.985 scale over 160ms. Shared transition guidance is 240ms. Use Compose animation APIs so Android's disabled/reduced-duration setting applies. No infinite decorative loops, animated currency digits or data changes disguised as motion.
- Read animation state in the graphics/draw phase when possible. Use stable primitive keys and content types for mixed lazy lists; keep financial work off the UI thread. Do not add speculative stability annotations or dependency upgrades for a visual effect.

## Required verification for UI work

Review current renders and tests, not just a successful compile. Minimum preview matrix:

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
