# W2 Goals Screenshot Review

Date: 2026-10-14  
Product: Jvy Wallet  
Evidence: user-provided screenshots `Screenshot_20261014_122039_Jvy Wallet.jpg`, `Screenshot_20261014_122043_Jvy Wallet.jpg`, and `Screenshot_20261014_122047_Jvy Wallet.jpg`.

Primary QA device: Samsung Galaxy S22 Ultra, Snapdragon, Android 16, One UI 8.

## What the screenshots prove

- The Goals destination is reachable from More > Quick access.
- Two active goals can be displayed together.
- A goal can have a deadline (`June 2027` in the evidence), a target, progress, and account allocation status.
- A goal can show money reserved from a named account (`Emergency Fund`, `7,000.00 AED`).
- The summary correctly aggregates the visible reservations (`7,000.00 AED`, `2 active goals`).
- A goal can exist with no reservation yet (`Microsoft Surface Ultra`, `0.00 AED`, `Not reserved yet`).
- The goal detail flow exposes remaining target, progress, reserved account, and a `Reserve money` action.

## Logic observations

### P0: retire the two meanings of “Savings goal”

The More menu still renders the inherited `Savings goal` row and routes its click to the legacy `BufferModal`. The Home ViewModel now calculates the displayed buffer from active Goal allocations, while the modal still writes `Settings.bufferAmount`. This creates a split-brain state:

1. The displayed amount can represent real account reservations.
2. The editor still edits a global target that is not used by Home safe-to-spend after reload.
3. The screenshot labels the result `Left to spend`, which implies spendable cash rather than reserved cash.

Required follow-up:

- Replace the legacy More-menu row with a compact Goals summary and a `View goals` action.
- Remove or clearly label the legacy global buffer editor as a migration-only setting.
- Use one vocabulary everywhere: `Target`, `Reserved`, `Remaining`, and `Available to spend`.
- Preserve imported `bufferAmount` data, but convert it to an explicit draft/protected-reserve goal instead of silently presenting it as progress.

### P1: “saved” should not overstate what an allocation means

The current model reserves money inside existing accounts; it does not move money, create a transaction, or create a separate account. The screenshots say `7,000.00 AED saved`, which may teach users that money was transferred when it was only earmarked.

Preferred wording until contribution transfers exist:

- `7,000 AED reserved`
- `8,000 AED remaining`
- `Reserved from Emergency Fund`

Use `saved` only when the product has a traceable contribution event or a real goal account balance.

### P1: per-account availability must remain explicit

The Goals flow correctly shows the account used for a reservation, but the More-menu aggregate does not explain which accounts are blocked or how a multi-account total was formed. Future summary copy should expose a drill-down such as `AED 7,000 reserved across 1 account` and show account-level available-to-spend values before a user makes a large purchase.

### P1: category-scoped goals are not implemented yet

The current screenshots demonstrate account reservations and custom named goals, not category-linked savings. A category goal needs a separate definition of progress: category spending is not automatically savings. Before shipping category goals, decide whether they are:

- a contribution rule that creates planned transfers;
- a category-linked target with manual contributions; or
- a sinking-fund forecast based on recurring category costs.

Do not label category spend as saved money without an explicit contribution event.

### P2: deadline intelligence is still missing

The deadline is displayed, but the screen does not show the required monthly contribution, whether the user is on track, or what happens if the deadline is missed. Add:

`Needed monthly: AED X` → `On track / Behind by AED Y / Complete`.

The calculation must use remaining target, remaining months, current reservations, and the selected currency.

### P2: goal identity can contradict goal type

The evidence shows a goal named `Emergency Fund` whose type line says `Purchase`. This may be user input, but the UX should make the distinction obvious. Consider templates that prefill both name and type, plus an edit surface that lets the user correct either value.

## UI and UX observations

### P0: goal detail sheet content is horizontally clipped

In the detail screenshot, the type/deadline line and the large saved amount begin at or beyond the left edge. The sheet appears to be rendering content wider than its safe horizontal padding. Verify `IvyModal` content constraints and add a consistent horizontal inset at the modal content boundary. This is a readability/accessibility defect, not a taste preference.

### P1: Quick access grid is structurally unbalanced

`Goals` appears alone in a third row under the second column, leaving a large empty area. Use a real four-column grid with a stable slot order, or move Goals into a dedicated Planning section near Budgets and Planned Payments. Do not add arbitrary spacer weights for each new item.

### P1: Goals icon duplicates Budgets

The More menu currently reuses the Budgets icon for Goals. Give Goals a distinct target/flag/lock icon so users can scan the grid without reading every label. Keep the icon semantics consistent with goal cards.

### P1: remaining Ivy branding is visible

The More menu still says `Share Ivy`, and the open-source card still links to and names Ivy Wallet upstream. For the fork, use `Share Jvy` or `Share Jvy Wallet` and the fork URL while retaining a small `Forked from Ivy Wallet` attribution where appropriate.

### P2: theme action label is ambiguous

The dark-mode shortcut is shown as `Dark mode` while dark mode is already selected. Prefer an action label (`Switch to light mode`) or a clearly selected state with supporting semantics so the user knows what tapping it will do.

### P2: summary card should explain the number

`Money with a job` is a strong headline, but the summary should include one short explanation such as `Reserved in goals and excluded from Safe to Spend.` This reduces the need to infer the meaning from the detail sheet.

### P2: card information hierarchy can be tighter

The list is readable, but the saved amount, target amount, deadline, and account status are spread across several rows. A compact `reserved / target` line plus one progress line would make comparison faster, especially with more than three goals. Keep the spacious rhythm, but reduce repeated labels.

### P2: missing lifecycle actions

The detail sheet only offers `Reserve money` and `Done`. Future iterations need `Edit`, `Pause`, `Complete`, and `Delete`, with a confirmation for destructive actions. A paused goal must release its reservations from Safe to Spend without deleting its history.

## Follow-up implementation order

1. Replace the legacy More-menu Savings goal row with Goals-aware copy and navigation.
2. Fix modal safe horizontal padding and verify on small and large Android widths.
3. Add a dedicated Goals icon and rebalance Quick access layout.
4. Correct fork branding and attribution links in the More menu.
5. Add goal monthly contribution and on-track calculations.
6. Add category-linked goal semantics only after contribution rules are specified.
7. Add lifecycle actions and historical progress snapshots.

## Source references inspected

- `feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt`
- `feature/home/src/main/java/com/ivy/home/HomeViewModel.kt`
- `feature/home/src/main/java/com/ivy/home/HomeTab.kt`
- `feature/goals/src/main/java/com/ivy/goals/GoalsScreen.kt`
- `feature/goals/src/main/java/com/ivy/goals/GoalModals.kt`
- `feature/goals/src/main/java/com/ivy/goals/GoalsViewModel.kt`
- `docs/plans/001-goals-and-saving-pools.md`
- `docs/adr/0013-goals-and-account-reservations.md`

