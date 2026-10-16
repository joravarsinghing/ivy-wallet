# BudgetBakers Wallet UI reference and Jvy Wallet mash-up

Status: reconnaissance complete; first blend in implementation  
Date: 2026-10-15  
Scope: supplied BudgetBakers Wallet 9.3.12 split bundle, supplied BudgetBakers Wallet screenshots, public `morluto/rea` reference repository, and the current Jvy Wallet Home implementation.

## Executive decision

Jvy Wallet should adopt BudgetBakers Wallet's **dashboard composition model**, not reproduce its branding or every visual treatment.

The useful model is:

1. Accounts are visible near the top and remain the user's anchor.
2. Dashboard cards answer one question each.
3. Charts, records, planned payments, and summaries are independent modules.
4. The user can add, hide, and eventually reorder modules.
5. Empty and loading states reserve the same space as the final card, so the page does not jump.

Jvy should keep its own strengths:

- beginner-friendly explanations;
- Safe to Spend and goal reservations as explicit, deterministic calculations;
- Ivy Wallet import compatibility;
- the existing Jvy purple/teal/red visual language;
- privacy-first, offline-first behavior;
- no premium upsell card or unexplained financial score.

The current implementation is a **Jvy Home dashboard editor, adaptive trends, compact activity feed, and spending breakdown**. The Accounts tab remains the authoritative account-management surface; Home intentionally does not repeat an account snapshot card. This keeps the useful modularity of Wallet without recreating its duplicate information wall.

## Evidence from the supplied APKs

### Artifact facts

The supplied artifacts were inspected from copies. No files in either APK bundle or the Jvy source tree were modified during analysis.

### 9.3.12 split-bundle update

The newer artifact is an APKM split bundle rather than a single installable APK. Its `info.json` identifies BudgetBakers Wallet release `9.3.12`, version code `90179`, package `com.droid4you.application.wallet`, and minimum API 27. The base APK declares target SDK 36 and compile SDK 37. The bundle includes `base.apk`, the `arm64-v8a` split needed by the Samsung S22 Ultra Snapdragon, an `xxxhdpi` split, and language/resource splits.

The 9.3.12 resource table reinforces the dashboard findings with resources for dashboard chart height and text, goals, records, planned payments, balance trends, account types, widgets, and dashboard menu entries. These names support the same modular information architecture seen in the supplied screenshot, while still not proving the original implementation's calculation semantics.

- Package: `com.droid4you.application.wallet`
- Application label: `Wallet`
- Version: `4.12.1-samsung`
- Version code: `1000004232`
- Minimum SDK: 16
- Target SDK: 25
- APK size: approximately 33.9 MB
- Primary implementation family: classic Android resources and activities, with Kotlin metadata and support libraries
- Resource table includes 2,650 `res/` entries in the inspected archive

The APK/APKM files were copied into temporary audit folders and inspected with Android SDK `aapt2` and ZIP inventory tooling. The `rea` repository was cloned as a read-only reference into a separate temporary folder. REA documents an Android workflow based on a pinned JADX headless engine and an Apktool resource decoder; the current machine does not have Node/JADX/Apktool configured, so this pass uses the Android SDK resource tooling directly and records that limitation rather than pretending that decompiled Java or runtime observations were obtained. The user's stated permission to reuse BudgetBakers material is recorded as an input, but this first blend intentionally uses Jvy-owned UI and semantics rather than copying proprietary code or assets.

### Dashboard and domain evidence

The resource table contains names that confirm the screenshot is backed by a broad dashboard/domain model rather than a single hard-coded screen:

- `layout_dashboard_content`
- `layout_dashboard_onboarding`
- `dashboard_chart_height`
- `dashboard_chart_margin`
- `dashboard_chart_text`
- `dashboard_item_text`
- `dashboard_list_divider`
- `layout_planned_payments`
- `layout_report_content_expenses`
- `activity_goal_detail`
- `activity_goal_distribute_savings`
- `goal_progress_bar`
- `progress_goal`
- `drawable/cashflow`
- `drawable/cumulative_expenses`
- `drawable/income_expense`
- `widget_balance_preview`

The resource IDs also expose concepts for:

- account balances and balance editing;
- planned payment lists and progress;
- budgets, goals, debts, credit, and expenses;
- records and “no records” states;
- premium/promotion surfaces;
- dashboard content and onboarding.

This is useful evidence for information architecture. It is not evidence that every resource is visible in the supplied screenshot or that the original app's internal calculations should be copied.

### Screenshot anatomy

The supplied screenshot shows this vertical flow:

1. Global navigation: menu, Home, notification affordance.
2. Section switcher: Accounts / Budgets & Goals.
3. Account strip: Cash, ADCB, Emergency Fund, Add account.
4. Small mode chips: Spending, Balance, Planned payments.
5. Promotional cards: Google Pay and Premium.
6. Dashboard summary card: balance and cash-flow gauges plus Outlook, Spending, Credit, Debt gauges.
7. Expense structure: donut chart.
8. Last records overview: compact record list/loading skeleton.
9. Balance trend: time-series chart.
10. Upcoming planned payments: compact list/loading skeleton.
11. Actual balance: single-value card.
12. Add more cards affordance and floating add action.

The important product pattern is the separation of the dashboard into cards. The important warning is that the reference screen is very dense and includes several overlapping summaries.

## What Jvy should borrow

### 1. Accounts-first orientation

Keep account details in the Accounts tab. Home may show a derived reservation or spendability summary only when it answers a different question. The Accounts tab should show:

- total visible balance;
- a horizontal list of account chips/cards;
- each account's spendable amount after its goal reservations;
- a clear “reserved for goals” sublabel where applicable;
- hidden-balance behavior consistent with the existing Jvy privacy toggle.

An account is not a goal. A goal allocation is metadata that explains why part of an account balance is protected; it must never create a fake ledger transaction.

### 2. Modular cards

Create a small stable registry of Home modules. Each module has:

- a stable ID;
- a title and short explanation;
- a default visibility;
- a minimum supported data state;
- an owner/navigation destination;
- a test tag and accessibility label.

Initial registry candidates:

| Module | Default | Primary question |
| --- | --- | --- |
| Balance and cash flow header | Always on | What do I have and how did this period move? |
| Safe to Spend | On | What can I safely spend? |
| Accounts | Accounts tab | Where is the money? |
| Goals | On when goals exist | What money already has a job? |
| Money trends | On | Is my cash flow improving? |
| Where your money went | On | Which categories used the most? |
| Upcoming payments | On when planned payments exist | What will hit me next? |
| Recent records | On | What did I just record? |

“Always on” means the module cannot be removed, not that it must be visually dominant.

### 3. Add-more-cards as a customization entry point

The reference app's “Add more cards” idea is a good answer to different user needs. Jvy's version should open a full-screen or bottom-sheet editor with:

- visible modules;
- available modules;
- one-line descriptions;
- a preview or sample state;
- hide/show controls;
- drag handles only when reorder is implemented safely;
- a “restore defaults” action.

Do not introduce a dashboard editor that silently changes the meaning of Safe to Spend or removes required warnings. The editor changes presentation, never financial semantics.

### 4. Stable skeletons and explicit empty states

Borrow the reference app's use of reserved card space, but make the copy more honest:

- Loading: show a compact skeleton inside the final card bounds.
- No records: explain what data is missing and how to create it.
- One data point: show the point and a “more history will make this useful” message.
- No category data: do not show a meaningless donut; use a short setup prompt.
- No planned payments: do not repeat Safe to Spend; show “Nothing scheduled” and a single action to add a planned payment.

## What Jvy should reject

### Premium and partner promotion in the money flow

The screenshot includes Google Pay and Premium cards. These are not appropriate for Jvy's current promise. They compete with the user's financial questions and weaken trust. Jvy can later have a separate, dismissible integrations area, but it should not appear between accounts and core financial information.

### Too many opaque gauges

The large Balance/Cash-flow gauges plus four smaller Outlook/Spending/Credit/Debt gauges create visual authority without enough explanation. A beginner can read “good” or “bad” without understanding what changed.

Jvy should use:

- a clear number;
- a short sentence explaining the drivers;
- a small bar, delta, or trend only when it adds meaning;
- a tap target for the calculation explanation.

If a health score is ever added, it must have a deterministic formula, visible inputs, and an explanation screen.

### Dense cards and duplicated truth

The reference page can place many cards one after another because it is a mature dashboard. Jvy currently has a smaller user journey and already shows Safe to Spend, Money Coming Up, and Money Trends. Adding an account summary or expense card must not duplicate the same balance, period cash flow, or reserved total in three different forms.

Every card should have one job and one primary action.

## Jvy-specific target layout

### Home order for the current implementation

1. Jvy header and period selector.
2. Balance and income/expense header.
3. Safe to Spend, with a “why” affordance.
4. Money Coming Up.
5. Money Trends, with `7D`, `30D`, `3M`, `6M`, and `12M` plus metric editor.
6. Where your money went, using ranked bars.
7. Recent Activity, using a compact seven-row feed.

This order follows the average user's questions:

> What do I have? Where is it? What can I use? What needs attention? What pattern should I learn from?

### Home card sizing

Use the existing mobile standards as a hard baseline:

- 24dp content gutter for card internals and major sections;
- 16dp vertical gap between cards;
- 16dp minimum touch target spacing around controls;
- compact card header and one-line supporting copy;
- no horizontal clipping for long account or goal names;
- consistent grouped currency formatting;
- bottom sheets keep their action row above the navigation/gesture inset.

Compact activity rows may use a smaller internal gutter, but the surface must align visually with the 24dp outer content grid.

### Visual direction

Keep Jvy recognizable:

- near-black page background;
- charcoal cards with clear elevation through tonal contrast, not excessive shadows;
- purple for interactive controls and goal progress;
- teal/green for positive income or savings;
- red/pink for negative cash flow or attention states;
- off-white primary text and muted gray secondary text.

The reference app's colored account tiles are useful as a scan pattern, but Jvy should use restrained color and preserve accessible text contrast. Account color is a label, not a risk score.

## Technical integration plan

### Phase A — dashboard contracts

Add a presentation-layer `HomeCardId` and an immutable `HomeDashboardLayout` containing ordered visible IDs. Persist only the layout preference, not financial values. Unknown IDs must be ignored so future releases can add cards without breaking older preferences.

The default order should be defined in code and versioned. A layout migration should be a pure mapping from the previous layout to the new one.

### Phase B — account snapshot

Build the account module from the existing account and reservation sources. It must:

- calculate displayed values at the presentation boundary;
- leave ledger balances unchanged;
- show reserved money once per account;
- respect hidden-balance state;
- link to the existing account and goal screens;
- handle zero accounts, one account, many accounts, and long names.

### Phase C — expense structure and planned payments

Reuse existing transaction and planned-payment queries. Use a horizontal category bar list as the default visual; add a donut only if labels remain readable with small datasets. The planned-payment card should show the next few dated events, not another totalized forecast.

### Phase D — customization

Ship visibility toggles first. Add drag-and-drop reorder only after state restoration and accessibility tests are stable. The editor must not require a database migration; DataStore is sufficient for presentation preferences.

### Phase E — verification

Required checks:

- imported Ivy data opens unchanged;
- no card preference can change account balances, reservations, goals, or transactions;
- Paparazzi coverage for default, customized, empty, loading, hidden-balance, and long-name states;
- Samsung S22 Ultra / Android 16 / One UI 8 manual verification;
- no horizontal clipping and no bottom-sheet action-row overlap;
- numbered APK build only after the implementation is accepted.

## Reverse-engineering limits and provenance

This pass inspected the user-supplied APK archive, manifest facts, resources, IDs, and the supplied screenshot. It did not:

- run the BudgetBakers APK;
- attach to a BudgetBakers account;
- inspect private services, APIs, or server data;
- claim that resource names alone describe runtime behavior;
- decompile Java/Kotlin classes, because the REA JADX provider is not configured on this machine;
- copy any BudgetBakers asset into Jvy.

The public REA repository is a workflow/reference dependency for future authorized static or runtime analysis. Its Android documentation emphasizes artifact-bound evidence, local analysis, explicit limitations, and no writes to the original APK. Those principles are adopted here.

## Acceptance criteria for the mash-up

The redesign is ready for implementation review when:

1. Home has one clear account snapshot and no duplicate balance summary.
2. The user can hide/show optional cards without changing financial calculations.
3. Each card has a single question, a single primary action, and explicit empty/loading/error states.
4. Jvy's existing trend gaps, single-point behavior, and updating overlay remain intact.
5. Goals and reserved money remain clearly distinct from actual account balances.
6. No promotional card interrupts the core financial flow.
7. Ivy imports and existing transaction semantics remain unchanged.

