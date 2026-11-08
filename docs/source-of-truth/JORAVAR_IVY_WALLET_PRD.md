# Joravar Money — Product Requirements Document & Ivy Wallet Fork Improvement Plan

> **Working title:** Joravar Money (TBD)  
> **Source:** `Ivy-Apps/ivy-wallet`  
> **Repository reviewed:** `main`, 13 October 2026  
> **Document status:** Draft v0.1 — fork baseline  
> **Primary audience:** Product, Android engineering, QA, design  
> **Core constraint:** Existing Ivy Wallet user data must remain importable without silent loss.

---

## 1. Executive Summary

The fork should not become “Ivy Wallet plus more charts.” It should evolve Ivy's fast, local-first money tracker into a **beginner-friendly personal financial operating system**.

The target user is an ordinary person who does not understand personal finance terminology, does not want to maintain spreadsheets, and may only know whether there is money in the bank today. The product must progressively help that person:

1. **Understand** where money is going.
2. **Control** spending and upcoming obligations.
3. **Protect** themselves with buffers, emergency savings, and a debt plan.
4. **Build** savings toward concrete goals.
5. **Grow** assets and net worth over time.
6. **Optimize** financial decisions with clear, explainable scenarios.

The fork should preserve the best parts of Ivy Wallet — fast manual entry, accounts, categories, budgets, transactions, loans, reports, exchange rates, local ownership of data, and import/export — while adding a clearer financial journey centered on **net worth, cash flow, safe-to-spend, goals, debt reduction, and education**.

The most important engineering principle is:

> **Ivy compatibility is a permanent boundary, not a one-time migration script.**

Stock Ivy CSV exports must remain importable. The fork should also provide an **Ivy-compatible export mode** for all fields Ivy can represent, while storing new fork-only capabilities in a richer versioned format.

---

## 2. Product Vision

### Vision statement

**Make good money decisions obvious to people who were never taught personal finance.**

### Product promise

At any time, the app should answer five questions in plain language:

1. **How much money do I actually have?**
2. **Where is it going?**
3. **How much can I safely spend?**
4. **What should I do next?**
5. **Am I getting financially stronger?**

### North-star concept

The app moves the user through a financial progression:

`Track → Control → Protect → Build → Grow`

The interface should reveal more advanced tools only when they become useful. A new user should never be presented with a finance terminal full of ratios, investment jargon, or setup work.

---

## 3. Target User

### Primary persona: “Average Joe”

The target user:

- earns income but does not closely understand cash flow;
- may track spending inconsistently or not at all;
- sees budgeting as restrictive or complicated;
- may have loans or credit-card balances without understanding total interest cost;
- may have savings but no emergency-fund target;
- may invest eventually but does not understand asset allocation or compounding;
- wants simple answers, not accounting terminology;
- is likely to abandon an app if setup takes too long;
- needs help developing financial habits, not merely recording transactions.

### Jobs to be done

When I receive or spend money, I want to record it quickly so that my numbers stay trustworthy.

When I open the app, I want to know what I can safely spend without accidentally using rent, bill, or savings money.

When my month goes badly, I want the app to explain why in normal language.

When I have debt, I want to know the fastest sensible way out and the effect of paying more.

When I save, I want to know whether I am building a useful safety buffer and reaching my goals.

When my finances improve, I want to see my **net worth rise** and understand what caused the change.

When I am ready to invest, I want education and tracking without being pushed into speculative decisions.

---

## 4. Goals and Non-Goals

### Product goals

- Preserve Ivy Wallet import compatibility.
- Make transaction tracking simpler and more trustworthy.
- Make budgeting understandable to a beginner.
- Introduce **net worth** as a first-class metric.
- Convert raw data into one clear next financial action.
- Teach concepts contextually rather than through a separate textbook.
- Support debt, emergency funds, savings goals, and investments as parts of one financial picture.
- Remain useful without requiring bank connections or cloud accounts.
- Remain local-first and privacy-conscious by default.
- Build calculations that are deterministic, testable, and explainable.

### Non-goals for initial releases

- Brokerage execution or stock trading.
- Personalized “buy/sell this investment” recommendations.
- Tax filing.
- Full double-entry business accounting.
- Business invoicing/payroll.
- Credit underwriting.
- Social feed or financial-status comparison with other users.
- AI as the source of financial calculations.

AI may explain calculations later, but it must never be the authoritative calculation engine.

---

# PART I — IVY WALLET REPOSITORY ANALYSIS

## 5. Repository Baseline

The reviewed repository is currently archived, so the fork should assume it is taking ownership of maintenance rather than depending on continued upstream development.

The current Gradle structure is highly modular and already separates screens, features, shared data, shared domain, calculations, repositories, backup, import, reports, loans, exchange rates, budgets, transactions, accounts, and settings.

Notable modules include:

- `screen:home`
- `screen:accounts`
- `screen:budgets`
- `screen:transactions`
- `screen:planned-payments`
- `screen:backup`
- `screen:loans`
- `screen:balance`
- `screen:reports`
- `feature:accounts`
- `feature:transactions`
- `feature:budgets`
- `feature:backup`
- `feature:exchange-rates`
- `feature:import-data`
- `feature:loans`
- `feature:reports`
- `shared:data:model`
- `shared:data:db`
- `shared:data:backup`
- `shared:data:repository`
- `shared:domain:calculation`
- `shared:domain:legacy`
- `shared:domain:deprecated`

The literal presence of `legacy` and `deprecated` domain modules, combined with repository migration issues such as #2961 and #2963, shows that the codebase was already in an architectural transition. The fork should **continue that migration incrementally**, not restart the entire app.

### Repository strengths to retain

1. **Good feature coverage at the tracker layer** — accounts, transactions, budgets, loans, reports, planned payments, exchange rates, backup/import.
2. **Local-first product DNA** — suitable for users who do not want financial-account aggregation.
3. **Fast manual tracking** — a key advantage over finance apps that require heavy onboarding.
4. **Modular project structure** — new financial capability can be isolated in modules rather than growing a monolith.
5. **Existing import infrastructure** — far better than rebuilding migration from scratch.
6. **Existing multi-currency concepts** — necessary for net worth and users who live/work across currencies.
7. **Open-source base** — suitable for a transparent calculation model and auditable privacy behavior.

### Repository weaknesses / fork risks

1. **Legacy/new data-layer split** increases regression risk.
2. **Money representation is inconsistent.** The newer core `Value` model wraps a `Double`, while the legacy import path persists amounts using `BigDecimal` semantics.
3. **Balance correctness has historically been sensitive.** Issue history includes stale/incorrect account balance behavior and explicit work on balance snapshots/use cases.
4. **Timezone/date correctness needs hardening.** Historical issue #3118 describes transactions moving to the following date after time changes.
5. **Import is powerful but should gain preview, idempotency, provenance, and explicit reconciliation reporting.**
6. **The existing UX is primarily a money tracker.** It lacks a beginner financial progression centered around safety, goals, debt, and wealth.
7. **Backup safety is important.** Historical issues #2709 and #2647 reinforce the need for automatic backup options and protection before destructive actions.

---

## 6. What to Reuse, Refactor, and Add

| Area | Decision | Rationale |
|---|---|---|
| Transaction entry | **Reuse + simplify** | Core strength; protect speed. |
| Accounts | **Reuse + extend** | Add financial role: spendable, reserve, investment, debt-linked, excluded. |
| Categories | **Reuse + extend** | Add essential/flexible classification and smarter rules. |
| Budgets | **Refactor UX** | Current concept remains useful, but beginner mode should be simpler. |
| Loans | **Reuse data where possible + unify as liabilities** | Loans must feed net worth and debt strategy. |
| Reports | **Reuse foundation + redesign insights** | Move from descriptive charts to explanations/actions. |
| Exchange rates | **Reuse boundary + harden snapshots** | Net worth needs historical FX reproducibility. |
| CSV import | **Protect as compatibility contract** | Critical for existing Ivy users. |
| Backup | **Version and expand** | New entities need richer export/restore. |
| Core monetary type | **Refactor gradually** | `Double` should not remain the authoritative type for financial calculations. |
| Net worth | **Add** | Central financial outcome. |
| Goals | **Add/expand** | Needed to turn saving into plans. |
| Emergency fund | **Add** | Core beginner protection layer. |
| Safe-to-spend | **Add** | Bridges budgeting and daily decisions. |
| Debt payoff engine | **Add** | Makes liabilities actionable. |
| Education | **Add** | Contextual learning is a core differentiator. |
| AI coach | **Later** | Useful only after deterministic metrics are reliable. |

---

# PART II — IVY DATA COMPATIBILITY

## 7. Compatibility Requirement

### P0 requirement

A user with a valid Ivy Wallet CSV export must be able to import it into the fork without silent data loss.

The stock Ivy import implementation currently maps fields such as:

- Account
- Amount
- Category
- Currency
- Date
- Description
- Title
- Type

The importer also contains Ivy-specific semantics that must be preserved, not “cleaned up” accidentally.

### Important Ivy importer behaviors observed

1. Accounts and categories are reconciled by name and may be created during import.
2. Account currency is preserved.
3. Transaction type can be influenced by the sign of the imported amount depending on mapping rules.
4. Imported transaction amounts are normalized to absolute values after determining transaction type.
5. Transfers can contain a secondary amount for cross-currency movement.
6. For Ivy-format transfer rows, the destination account is represented using the field mapped as **Title** in the importer flow.
7. Blank descriptions are converted to null.
8. Date parsing supports configured formats and UNIX epoch seconds.
9. Import results already distinguish imported, skipped, and failed rows.

These behaviors are compatibility requirements even where the representation is awkward.

---

## 8. Compatibility Architecture

Create a permanent boundary rather than allowing Ivy quirks to spread into new domain code.

Suggested package/module concept:

```text
compatibility/
  ivy/
    IvyCsvParser
    IvyFieldMapper
    IvyLegacyModelAdapter
    IvyImportValidator
    IvyImportPreview
    IvyImportExecutor
    IvyCompatibleExporter
```

### Import pipeline

```text
Select file
   ↓
Fingerprint source
   ↓
Parse without writing
   ↓
Validate rows
   ↓
Map Ivy semantics
   ↓
Show import preview
   ↓
User confirms
   ↓
Transactional import
   ↓
Reconcile accounts/categories/transfers
   ↓
Post-import verification
   ↓
Import report
```

### New import entities

```text
ImportBatch
- id
- sourceType
- sourceFileHash
- importedAt
- sourceAppVersion? 
- rowCount
- importedCount
- skippedCount
- failedCount

ImportedRecordRef
- importBatchId
- sourceRowHash
- entityType
- entityId
- originalRow? (optional/audit-safe)
```

### Idempotency

Importing an unchanged Ivy file twice must not silently duplicate the ledger.

Default behavior:

- calculate file fingerprint;
- calculate semantic row fingerprint;
- detect previous import batches;
- show `Already imported`, `New`, `Changed`, and `Conflict` counts;
- require explicit user choice before duplicating known records.

---

## 9. Compatibility Test Matrix

Create permanent golden fixtures checked into test resources.

### Mandatory fixtures

- standard income and expense rows;
- multiple accounts;
- multiple categories;
- account/category missing locally;
- same-currency transfer;
- cross-currency transfer with second amount;
- transfer destination encoded using Ivy's title-field behavior;
- negative/signed amount handling;
- blank title/description;
- locale decimal separators;
- supported human-readable date formats;
- UNIX timestamps;
- timezone and daylight-saving boundaries;
- duplicate rows;
- file imported twice;
- invalid row among valid rows;
- large history;
- deleted/excluded-account edge cases where representable.

### Acceptance standard

- **Zero silent loss.**
- Every rejected row appears in an import report with reason.
- Re-import is idempotent by default.
- Transfer pairing remains correct.
- Account and category mapping is deterministic.
- Imported totals reconcile with source totals within exact supported currency precision.

### Round-trip test

```text
Stock Ivy CSV
   → Joravar Money import
   → optional new enrichment
   → Export as Ivy-compatible CSV
   → Stock Ivy import
```

All fields representable by Ivy must survive the round trip.

Fork-only metadata can be omitted from Ivy-compatible export, but the user must be told what will not be represented.

### Native backup compatibility

Do **not** promise native Ivy backup compatibility until exact backup fixtures and schema versions are captured and tested. Treat this as an explicit investigation/release gate.

CSV compatibility is the guaranteed P0 interoperability path.

---

# PART III — PRODUCT EXPERIENCE

## 10. Beginner Financial Journey

The app should not expose every feature immediately.

### Stage 1 — Understand

Goal: build a trustworthy money picture.

Features:

- accounts;
- transaction tracking;
- category suggestions;
- income vs spending;
- recurring transaction detection;
- current cash position;
- simple explanations.

Success message:

> “You now know where your money is going.”

### Stage 2 — Control

Goal: avoid accidental overspending.

Features:

- simple budgets;
- bills/commitments;
- safe-to-spend;
- upcoming cash-flow timeline;
- spend pace;
- category/merchant rules.

Success message:

> “Your important expenses are covered. Here is what is safe to spend.”

### Stage 3 — Protect

Goal: reduce financial fragility.

Features:

- emergency-fund target;
- cash buffer;
- debt overview;
- due-date alerts;
- avalanche/snowball scenarios.

Success message:

> “You can cover 2.8 months of essential expenses.”

### Stage 4 — Build

Goal: direct surplus toward goals.

Features:

- savings goals;
- goal deadlines;
- required monthly contribution;
- allocation suggestions;
- progress streaks without guilt mechanics.

Success message:

> “At your current contribution, your car fund reaches its target in June.”

### Stage 5 — Grow

Goal: build lasting wealth.

Features:

- net worth;
- investment assets;
- contributions vs gains;
- compound-growth scenarios;
- diversification education;
- opportunity-cost calculator.

Success message:

> “Your net worth increased because you saved AED 1,200 and reduced debt by AED 450.”

---

## 11. Information Architecture

Limit primary navigation to five destinations:

### 1. Home

Answer: **What do I need to know today?**

Default beginner cards:

- Safe to spend
- Current cash
- Upcoming bills
- Month status
- One next action
- Net worth trend once configured

### 2. Transactions

Answer: **What happened to my money?**

- timeline;
- fast add;
- search;
- filters;
- split transaction;
- tags;
- merchant/category rules;
- transfer handling;
- attachments later.

### 3. Plan

Answer: **What should my money do?**

- budgets;
- bills;
- emergency fund;
- goals;
- debt payoff.

### 4. Wealth

Answer: **Am I getting richer or poorer?**

- net worth;
- assets;
- liabilities;
- investment tracking;
- net-worth change drivers;
- long-term scenarios.

### 5. Insights / Learn

Answer: **What does this mean and what should I improve?**

- month-end report;
- trends;
- contextual lessons;
- calculators;
- later: grounded AI coach.

---

# PART IV — FUNCTIONAL REQUIREMENTS

## 12. Home Dashboard

### P0/P1 requirements

Show:

- available/spendable cash;
- income this month;
- expenses this month;
- safe-to-spend amount;
- bills due before next income date;
- budget status;
- savings/investing this month;
- net worth when setup is sufficiently complete;
- one recommended next action.

### Design rule

Do not show more than one high-priority warning/action at once.

Bad:

> “Budget exceeded. Emergency fund low. Net worth down. Subscription high. Goal late.”

Good:

> **Best next step:** “Keep AED 420 aside for bills due before salary.”

Then offer “Why?” and secondary details.

---

## 13. Transactions

### Must-have improvements

- retain fast entry;
- income / expense / transfer clearly separated;
- optional merchant/payee field;
- split transactions;
- recurring flag/detection;
- tags;
- notes and attachments later;
- transfer fees represented explicitly;
- refund/reimbursement handling;
- category suggestions;
- user-defined automation rules.

### Rule examples

```text
If title/merchant contains “ENOC”
→ Category = Transport > Fuel

If title/merchant contains “Talabat”
→ Category = Food > Delivery

If account = Salary Account and title contains “Salary”
→ Type = Income > Salary
```

Corrections should improve future suggestions locally.

---

## 14. Beginner Budgeting

Do not require the user to understand zero-based budgeting on day one.

### Default beginner structure

Every expense category can optionally map to:

- **Committed** — rent, bills, minimum loan payments;
- **Future** — savings, emergency fund, goals, investments;
- **Flexible** — food out, shopping, entertainment, variable lifestyle spending.

Avoid judgmental labels such as “waste.”

### Advanced modes later

- category caps;
- zero-based budgeting;
- envelope budgeting;
- percentage plans;
- rollover budgets;
- custom formulas.

### Budget intelligence

After enough history, the app may say:

> “You usually spend AED 430–520 on groceries. Use AED 500 as next month's starting budget?”

Never change a budget silently.

---

## 15. Safe-to-Spend

Safe-to-spend should be one of the product's signature metrics.

Conceptual calculation:

```text
Safe to Spend =
  Spendable Cash
+ Expected Available Income
- Scheduled Bills
- Reserved Goal Contributions
- Required Debt Payments
- Chosen Cash Buffer
```

Time horizon defaults to the next expected income date or end of budget period.

### Requirements

- show amount;
- show date horizon;
- show calculation breakdown;
- let the user exclude uncertain income;
- never count transfers as income;
- allow a user-defined minimum cash buffer;
- warn when data completeness is low.

Example:

> **AED 58/day safe to spend until 31 Oct**  
> After keeping aside rent, utilities, minimum debt payments, and your AED 500 buffer.

---

## 16. Bills and Recurring Commitments

Detect likely recurring transactions using deterministic heuristics first:

- similar payee/title;
- similar amount range;
- periodic interval;
- repeated account/category.

User confirms recurring status.

Show:

- next due date;
- expected amount/range;
- monthly total;
- annualized total;
- amount due before next income;
- subscriptions separately if useful.

Example:

> “Recurring commitments use 41% of your average monthly income.”

---

## 17. Emergency Fund

Emergency fund is a core Plan feature, not just a generic savings goal.

### Calculation

```text
Emergency Coverage (months) =
Emergency Savings / Average Monthly Essential Expenses
```

### Requirements

- user chooses accounts/assets counted as emergency money;
- user chooses or accepts suggested essential categories;
- target is configurable;
- app may explain common target ranges but must not present one range as universally correct;
- show current months of coverage;
- show amount needed for next milestone;
- provide one contribution suggestion based on actual surplus.

Example:

> **2.4 months covered**  
> Add AED 760 to reach 3 months.

---

## 18. Goals

Each goal includes:

- name;
- target amount;
- current allocated amount;
- deadline optional;
- priority;
- funding account(s);
- recurrence/contribution plan;
- category/type;
- paused/completed state.

Calculated outputs:

- remaining amount;
- months remaining;
- required contribution;
- projected completion date;
- effect of changing contribution.

Example:

> “AED 575/month reaches your target by March 2028.”

---

## 19. Debt and Liabilities

Existing Ivy loan concepts should feed a unified liability model rather than creating parallel debt records unnecessarily.

### Debt fields

- lender / label;
- current balance;
- original principal optional;
- APR/interest rate optional;
- minimum payment;
- due date;
- term optional;
- linked account/transactions;
- secured/unsecured optional;
- included in net worth toggle.

### Payoff strategies

- **Avalanche** — highest effective interest first.
- **Snowball** — smallest balance first.
- **Custom** — user priority.

### Scenario outputs

- debt-free date;
- total projected interest;
- effect of additional monthly payment;
- monthly minimum burden;
- principal reduction trend.

If APR is missing, clearly say the interest projection is incomplete.

---

# PART V — NET WORTH & WEALTH

## 20. Net Worth as a First-Class Product Object

### Core formula

```text
Net Worth = Total Assets - Total Liabilities
```

### Asset groups

- cash/current accounts;
- savings accounts;
- investment accounts;
- stocks/funds entered manually or through later integrations;
- property;
- vehicles if the user chooses;
- gold/valuable assets if material;
- other manually valued assets.

### Liability groups

- credit cards;
- personal loans;
- auto loans;
- mortgages;
- money borrowed;
- other debt.

### Critical UX requirement: completeness

A precise-looking net worth number is misleading if major debt/assets are missing.

Show a setup signal such as:

> **Net worth setup: 80% complete**  
> “Add your outstanding car loan for a more accurate picture.”

Do not calculate the completeness score using mysterious AI. Base it on explicit setup checkpoints and user confirmations.

---

## 21. Multi-Currency Net Worth

Each asset/liability keeps its native currency.

The user selects a base reporting currency.

For every conversion used in a historical snapshot, retain/refer to the FX rate and valuation timestamp so the same historical number can be reproduced.

### Requirements

- native amount is never destroyed;
- base-currency amount is derived;
- FX source/date is visible on demand;
- missing/stale FX rate is flagged;
- transfer across currencies must not artificially increase income or net worth;
- FX movement is separated from saving/investment performance where feasible.

---

## 22. Net Worth Snapshots

Suggested model:

```text
NetWorthSnapshot
- timestamp
- baseCurrency
- totalAssets
- totalLiabilities
- netWorth
- completenessScore
- fxRateSetId
```

Daily snapshots are acceptable internally; charts may default to monthly points for readability.

### Change explanation

Do not only show:

> “Net worth +AED 2,400.”

Prefer:

> Net worth increased AED 2,400 this month:
> - +AED 1,500 from saving/investing
> - +AED 600 from debt reduction
> - +AED 220 asset value change
> - +AED 80 FX movement

Where attribution cannot be calculated reliably, say so.

---

## 23. Investment Tracking — Later Stage

Initial investment scope is **tracking and education**, not execution/advice.

Track:

- asset/account value;
- cost/contributions;
- withdrawals;
- realized cash flows if entered;
- gain/loss where sufficient data exists;
- asset allocation;
- fees if known.

Important distinction:

```text
Portfolio change ≠ investment performance
```

Separate:

- money contributed;
- money withdrawn;
- market/valuation change;
- income/dividends if tracked.

Any future projection must show assumptions and uncertainty. Never frame assumed returns as promises.

---

# PART VI — EDUCATION & INSIGHTS

## 24. Contextual Financial Education

Education should happen when a concept becomes relevant.

Examples:

When emergency savings are set up:

> **What is “months of coverage”?**  
> It estimates how long your emergency savings could cover your essential expenses if income stopped.

When APR is entered:

> **Why APR matters**  
> A higher rate generally makes the same debt more expensive to carry over time.

When net worth is first shown:

> **Why net worth matters**  
> Your bank balance tells you what you can access now. Net worth also includes what you own and what you owe.

### Learning design rules

- 20–60 second explanations;
- plain language first;
- jargon defined inline;
- use the user's own numbers only when calculations are deterministic;
- no guilt language;
- allow “Tell me more” for depth;
- remember completed lessons so the UI does not nag.

---

## 25. Monthly Money Check-In

Generate a deterministic monthly summary:

- income;
- expenses;
- cash saved;
- amount invested;
- principal debt reduction;
- savings/wealth-building rate;
- net-worth change;
- biggest category changes;
- recurring-cost changes;
- goal progress;
- one or two next actions.

Example:

```text
October
Income                    AED 8,000
Spent                     AED 5,640
Saved / invested          AED 1,510
Debt principal reduced    AED   450
Net worth change          +AED 2,080

What changed?
Dining was AED 310 above your 3-month average.

Best next step
Move AED 400 to your emergency fund to reach 3 months of coverage.
```

---

## 26. Financial Health Score — Optional, Later

If added, the score must be transparent and decomposable.

Potential components:

- cash-flow stability;
- savings/wealth-building rate;
- emergency coverage;
- debt burden;
- bill reliability/cash buffer;
- goal progress;
- net-worth momentum.

Do not hide the formula behind a proprietary “AI score.”

The user must be able to tap the score and see exactly why it changed.

---

## 27. Opportunity-Cost Calculator

For considered purchases, allow scenario comparison without guilt.

Example inputs:

- purchase price;
- buy now vs delay;
- alternative use of money;
- optional assumed investment return;
- horizon.

Output language:

> “Buying this uses AED 4,000 today and delays your emergency-fund target by about 2 months.”

If showing compound-growth alternatives, assumptions must be editable and clearly labelled.

---

# PART VII — AI COACH (POST-MVP)

## 28. AI Scope

AI should answer questions grounded in calculated ledger data, such as:

- “Why did I overspend this month?”
- “What changed compared with last month?”
- “Can I afford AED 3,000 for a laptop?”
- “What happens if I pay AED 300 extra toward this loan?”
- “What is hurting my savings rate?”

### AI safety/product rules

- Never invent transactions or balances.
- Every numerical answer must originate from deterministic app calculations.
- Show the calculation/source facts behind important claims.
- Do not make unsolicited speculative investment recommendations.
- Separate education from personalized financial advice.
- Permit the user to disable AI entirely without losing core features.
- Prefer on-device/private processing where practical; if cloud AI is used, require clear consent and strict data minimization.

---

# PART VIII — DATA & ENGINEERING ARCHITECTURE

## 29. Money Type Migration

Current core model uses a `Value` wrapper around `Double`, while legacy/import code uses higher-precision decimal handling.

Do not perform a big-bang rewrite.

### Target principle

All new authoritative financial calculations should use an exact representation:

- integer minor units where currency scale is fixed; **or**
- `BigDecimal`/equivalent domain decimal with explicit rounding rules.

Suggested domain model:

```text
Money
- amount: Decimal / exact minor units
- currency: CurrencyCode
```

### Migration strategy

1. Keep Ivy import adapters compatible with source representations.
2. Convert at the compatibility boundary into canonical `Money`.
3. Introduce exact-money use cases for new features first.
4. Migrate old calculations incrementally behind tests.
5. Keep serialization/migrations versioned.
6. Never silently round imported amounts beyond the currency's supported precision.

---

## 30. Deterministic Calculation Engine

Create pure use cases/services for:

- account balance;
- category totals;
- income/expense totals;
- safe-to-spend;
- recurring commitments;
- emergency coverage;
- required goal contribution;
- debt amortization;
- savings rate;
- wealth-building rate;
- net worth;
- net-worth change decomposition;
- FX conversion;
- compound-growth scenarios.

These should be testable without UI, database, network, or AI.

### Property-based tests are recommended for

- transfers conserving total value absent fee/FX revaluation;
- balance aggregation;
- debt amortization monotonicity;
- import/re-export invariants;
- currency conversion invariants;
- snapshot reproducibility.

---

## 31. Suggested New Domain Entities

Use existing Ivy entities where possible. Do not duplicate data simply to make a new feature easier.

Potential additions:

```text
Asset
AssetValuation
LiabilityView / LiabilityDetails
NetWorthSnapshot
FxRateSnapshot
RecurringCommitment
FinancialGoal
GoalContribution
CategoryBehavior
MerchantRule
ImportBatch
ImportedRecordRef
FinancialMetricSnapshot
LearningProgress
```

### Category behavior

Add behavior metadata rather than replacing Ivy categories:

```text
CategoryBehavior
- categoryId
- spendingClass: COMMITTED | FUTURE | FLEXIBLE | UNCLASSIFIED
- essential: Boolean?
```

This preserves old category data while enabling emergency-fund and safe-to-spend calculations.

---

## 32. Strangler Migration Strategy

Do not rewrite Ivy in one release.

```text
Existing screen / legacy use case
          ↓
Compatibility / adapter boundary
          ↓
New repository interfaces
          ↓
Canonical domain models
          ↓
New calculation engine
```

New features should depend on the clean side of the boundary.

Legacy modules shrink over time as each old flow gains:

- repository abstraction;
- tests;
- canonical money/time handling;
- migrated UI/read model.

---

## 33. Time and Timezone Strategy

Given historical date-shift bugs, define time semantics explicitly.

Requirements:

- transaction event stores an unambiguous instant plus required local-zone context where user-facing calendar date must be preserved;
- import records source timezone assumptions;
- daily/monthly reports use the user's selected reporting zone;
- changing device timezone must not silently move a transaction to a different intended accounting date;
- boundary tests include UTC offsets, DST regions, midnight, month-end, and year-end.

For a finance app, “which day did this expense belong to?” is business data, not merely formatting.

---

## 34. Balance Integrity

Balance and net-worth accuracy are trust-critical.

### Requirements

- one canonical account-balance use case;
- clearly defined treatment of transfers, refunds, adjustments, and excluded accounts;
- balance cache/snapshot invalidation on transaction mutation;
- reconciliation test between full-history calculation and cached calculation;
- background integrity check after import/restore;
- user-visible reconciliation tools for opening balance mismatches;
- no hidden “fix balance” transaction unless explicitly shown and confirmed.

---

# PART IX — BACKUP, PRIVACY, SECURITY

## 35. Backup Strategy

Support two export families:

### A. Ivy-compatible CSV

For interoperability.

Contains only data/semantics stock Ivy can represent.

### B. Joravar full backup

Versioned format containing all fork data, including:

- Ivy-compatible ledger data;
- goals;
- asset valuations;
- liability details;
- net-worth history;
- rules;
- recurring commitments;
- education progress;
- settings;
- provenance metadata.

### Safety requirements

- validate backup before reporting success;
- preview restore impact;
- transactional restore;
- create a safety backup before destructive restore/delete operations;
- checksum/integrity metadata;
- explicit schema version;
- future migration chain tests.

Optional cloud backup can be added later, but core functionality must not require cloud login.

---

## 36. Privacy & Security Principles

- Local-first by default.
- No ads based on financial data.
- No selling of financial data.
- No required account for core offline use.
- Encrypt sensitive local data where platform architecture permits without harming reliability.
- Never log raw financial records in production diagnostics.
- Export only after explicit user action.
- Cloud features are opt-in.
- Explain what data leaves the device before enabling integrations or AI.
- Add app/device lock support.
- Support secure deletion behavior consistent with platform limitations.

---

# PART X — NON-FUNCTIONAL REQUIREMENTS

## 37. Reliability

- Core transaction entry must work offline.
- App restart must not lose confirmed transactions.
- Import/restore is transactional.
- Calculation results are reproducible.
- Crash recovery must not leave partial transfers/import batches.

## 38. Performance

Targets should be benchmarked on representative mid-range Android hardware.

Key scenarios:

- cold start;
- Home dashboard load;
- account list load;
- transaction list with 50k+ records;
- search/filter;
- report calculation;
- large Ivy import;
- net-worth snapshot recomputation.

Prefer incremental calculations/caches with correctness reconciliation rather than repeatedly scanning the entire ledger.

## 39. Accessibility

- scalable text;
- no critical meaning expressed by color alone;
- readable currency formatting;
- TalkBack labels;
- 48dp-class touch targets where appropriate;
- avoid tiny superscript decimal styles;
- support reduced motion.

## 40. Localization

- externalize all copy;
- localize date, number, and currency display;
- keep stored monetary amount separate from locale formatting;
- allow app-language selection independent of system language if feasible;
- test RTL early if intended for broad international use.

---

# PART XI — UX PRINCIPLES

## 41. Product Design Rules

1. **Plain language before finance terminology.**
2. **One primary action per screen.**
3. **Progressive disclosure.** Advanced metrics appear as the user grows.
4. **Explain every important number.** “Why?” should be one tap away.
5. **No shame.** Avoid red/guilt-heavy experiences for discretionary spending.
6. **Data completeness is visible.** Do not pretend a partial financial picture is exact.
7. **Transfers are not spending.** The UI and reports must make this obvious.
8. **The app recommends; the user decides.** Budgets and categories never change silently.
9. **Celebrate resilience, not consumption restriction.** Emergency coverage and debt reduction are positive milestones.
10. **Never trade correctness for animation or AI novelty.**

---

# PART XII — ROADMAP

## 42. Phase 0 — Fork & Compatibility Foundation

### Objective

Create a safe baseline before adding product features.

### Scope

- establish fork/rebrand/package strategy;
- build current app in clean CI;
- capture baseline screenshots and smoke flows;
- create Ivy CSV golden fixtures;
- build permanent Ivy compatibility layer;
- add import preview/report/provenance;
- add idempotent re-import detection;
- document source and rounding semantics;
- create canonical exact `Money` type for new domain code;
- define canonical time/timezone semantics;
- test balance correctness;
- set up database migration policy;
- verify backup implementation and licensing/notice obligations before distribution.

### Exit criteria

- all golden Ivy files import with zero silent loss;
- invalid rows are explained;
- unchanged file re-import does not duplicate by default;
- same/cross-currency transfers reconcile;
- full-history and cached balances agree;
- timezone boundary test suite passes;
- CI runs unit, import, migration, and calculation tests.

---

## 43. Phase 1 — Understand & Control

### Scope

- beginner Home redesign;
- fast transaction flow polish;
- merchant/payee support;
- category/merchant rules;
- recurring bill detection;
- simple budget classes;
- safe-to-spend;
- upcoming obligations timeline;
- contextual beginner explanations.

### Exit criteria

A new user can import/setup data and answer within minutes:

- what came in;
- what went out;
- what bills are coming;
- what is safe to spend.

---

## 44. Phase 2 — Protect & Plan

### Scope

- emergency-fund calculator;
- essential-expense classification;
- savings goals;
- debt/liability unification;
- avalanche/snowball calculators;
- monthly money check-in;
- cash buffer warnings.

### Exit criteria

The user can see a clear protection plan and a measurable next step.

---

## 45. Phase 3 — Wealth & Net Worth

### Scope

- asset registry;
- liability registry/view;
- net-worth calculation;
- setup completeness;
- historical snapshots;
- FX snapshots;
- change-driver explanation;
- Wealth tab.

### Exit criteria

The app can answer accurately:

> “What is my net worth, how confident is the number, and why did it change?”

---

## 46. Phase 4 — Grow

### Scope

- investment assets;
- contribution vs gain separation;
- asset allocation view;
- compound-growth scenarios;
- opportunity-cost calculator;
- deeper financial education;
- long-term goal projections.

### Exit criteria

The app helps a financially stable user understand wealth-building choices without becoming a trading app.

---

## 47. Phase 5 — Coach & Optional Connectivity

### Scope

- evidence-grounded AI coach;
- natural-language insights;
- optional bank/account sync if strategically desired;
- optional private cloud backup/sync;
- configurable automation.

### Exit criteria

Every AI numerical claim is traceable to deterministic calculations and underlying user data.

---

# PART XIII — PRIORITY MATRIX

## 48. P0 — Must Not Ship Without

- Ivy CSV compatibility tests;
- no silent import loss;
- transactional import/restore;
- reliable transaction/account balances;
- transfer correctness;
- timezone correctness;
- exact money type for all new calculations;
- DB migration tests;
- data export/backup path;
- core offline operation;
- privacy baseline;
- crash/error reporting that excludes raw financial records.

## 49. P1 — Core Product Differentiators

- beginner Home;
- safe-to-spend;
- recurring bills;
- beginner budget model;
- emergency fund;
- goals;
- debt payoff planning;
- monthly check-in;
- net worth;
- assets/liabilities;
- net-worth history/change drivers;
- contextual learning.

## 50. P2 — Later Enhancements

- investment tracking depth;
- opportunity-cost calculator;
- financial health score;
- AI coach;
- cloud backup/sync;
- bank aggregation;
- advanced budgeting strategies;
- advanced reports/widgets.

---

# PART XIV — INITIAL ENGINEERING BACKLOG

## 51. Epic A — Fork Baseline

- [ ] Create fork and rename app identifiers safely.
- [ ] Record upstream baseline commit.
- [ ] Add `UPSTREAM.md` explaining ancestry and compatibility policy.
- [ ] Establish CI build/test pipeline.
- [ ] Add release signing/environment strategy.
- [ ] Verify license, notices, attribution, and distribution obligations.

## 52. Epic B — Ivy Compatibility

- [ ] Capture stock Ivy CSV fixtures.
- [ ] Characterize field mappings and transfer semantics.
- [ ] Add `IvyCompatibility` module/boundary.
- [ ] Add import dry-run/preview.
- [ ] Add row-level error report.
- [ ] Add file + row fingerprints.
- [ ] Add idempotent import behavior.
- [ ] Add post-import reconciliation totals.
- [ ] Add Ivy-compatible export.
- [ ] Add round-trip integration test.
- [ ] Investigate native Ivy backup formats separately.

## 53. Epic C — Money, Time & Balance Correctness

- [ ] Define canonical `Money` type.
- [ ] Define currency rounding policy.
- [ ] Define `AccountingDate` / transaction timezone semantics.
- [ ] Add account-balance canonical use case.
- [ ] Add transfer conservation tests.
- [ ] Add cache invalidation/reconciliation tests.
- [ ] Add midnight/DST/timezone regression suite.
- [ ] Add property-based calculation tests.

## 54. Epic D — Beginner Home

- [ ] Design beginner dashboard hierarchy.
- [ ] Add upcoming bills card.
- [ ] Add month income/spend card.
- [ ] Add safe-to-spend engine/card.
- [ ] Add one-next-action engine.
- [ ] Add “Why?” drilldown.

## 55. Epic E — Smart Ledger

- [ ] Add merchant/payee model.
- [ ] Add category rules.
- [ ] Add tags.
- [ ] Add split transactions.
- [ ] Add recurring transaction candidate detector.
- [ ] Add refund/reimbursement handling.

## 56. Epic F — Protection & Planning

- [ ] Add committed/future/flexible classification.
- [ ] Add essential-expense classification.
- [ ] Add emergency coverage engine.
- [ ] Add goals.
- [ ] Add goal contribution calculator.
- [ ] Normalize existing loans into liability read model.
- [ ] Add avalanche/snowball engine.
- [ ] Add monthly report.

## 57. Epic G — Wealth

- [ ] Add asset model.
- [ ] Add asset valuation history.
- [ ] Add liability inclusion in net worth.
- [ ] Add base reporting currency.
- [ ] Add FX rate snapshots.
- [ ] Add net-worth engine.
- [ ] Add completeness model.
- [ ] Add net-worth snapshots.
- [ ] Add change decomposition.
- [ ] Add Wealth UI.

## 58. Epic H — Education

- [ ] Define learning content schema.
- [ ] Add contextual lesson component.
- [ ] Add user learning progress.
- [ ] Write core lessons for budgets, emergency funds, debt, net worth, investing, and compounding.
- [ ] Add calculator explanations with assumptions.

---

# PART XV — SUCCESS METRICS

## 59. Activation

Track:

- successful import/setup rate;
- time to first transaction;
- time to first useful insight;
- percentage who configure upcoming commitments;
- percentage who complete a budget baseline;
- percentage who complete net-worth setup.

## 60. Data Trust

- percentage of golden fixture rows imported or explicitly reported;
- silent-loss incidents: target **0**;
- duplicate rate from unchanged re-import: target **0 by default**;
- post-import reconciliation mismatch rate;
- balance integrity failures;
- backup restore success rate.

## 61. Engagement

Prefer meaningful finance behaviors over generic daily-active-user pressure:

- monthly check-in completion;
- transaction categorization quality;
- recurring-bill confirmations;
- goal updates;
- debt-plan use;
- net-worth review frequency.

Do not design manipulative daily streaks merely to increase opens.

## 62. Financial Outcome Signals

With user consent and only as product metrics where privacy permits:

- savings/wealth-building rate trend;
- emergency coverage trend;
- debt principal trend;
- net-worth trend;
- goal completion;
- avoidable late/overdraft events if explicitly tracked.

Do **not** judge product success solely by positive investment/net-worth returns because markets, income, and life events are external to the app.

## 63. Trust & Quality

- crash-free sessions;
- import error rate;
- balance correction rate;
- percentage of important metrics with successful “Why?” explanations;
- user-reported wrong-calculation incidents;
- backup confidence/restore test success.

---

# PART XVI — ACCEPTANCE CRITERIA FOR MVP

## 64. MVP Definition of Done

The first branded fork is not considered ready until all of the following are true:

### Compatibility

- [ ] Representative Ivy CSV exports import successfully.
- [ ] Same- and cross-currency transfers reconcile.
- [ ] No failed/skipped rows disappear silently.
- [ ] Re-importing the same file is safe by default.
- [ ] Ivy-compatible export exists for representable fields.

### Ledger correctness

- [ ] Account balances reconcile with full transaction history.
- [ ] Transfers do not count as ordinary income/expense.
- [ ] Date/time behavior is stable across timezone changes.
- [ ] New financial calculations use the canonical exact money representation.

### Beginner value

- [ ] User sees income, spending, bills, and safe-to-spend.
- [ ] User can understand each major number via a plain-language explanation.
- [ ] The Home screen identifies one best next action.

### Protection and growth foundation

- [ ] Emergency-fund coverage works.
- [ ] Goals work.
- [ ] Debt balances feed liabilities.
- [ ] Net worth calculates assets minus liabilities.
- [ ] Net-worth setup completeness is shown.
- [ ] Net-worth history can be captured.

### Data safety

- [ ] Full fork backup/restore is versioned and tested.
- [ ] Destructive operations warn and offer safety backup where appropriate.
- [ ] Core use works offline.
- [ ] Privacy behavior is documented.

---

# PART XVII — KEY RISKS & MITIGATIONS

## 65. Risk: Breaking Ivy Imports During Refactor

**Mitigation:** golden files, permanent compatibility module, round-trip tests, adapters around legacy semantics.

## 66. Risk: Big-Bang Architecture Rewrite

**Mitigation:** strangler migration; new domain code beside legacy; migrate one path at a time behind repositories/tests.

## 67. Risk: Financial Numbers Lose Trust

**Mitigation:** exact money domain, deterministic calculations, reconciliation tests, “Why?” breakdowns, visible completeness.

## 68. Risk: Net Worth Is Misleading

**Mitigation:** explicit inclusion/exclusion, setup completeness, timestamps for manual valuations, FX provenance, liability prompts.

## 69. Risk: Beginner UX Becomes Overwhelming

**Mitigation:** stage-based progressive disclosure, one next action, simple default budget taxonomy, advanced modes optional.

## 70. Risk: AI Hallucinates Financial Guidance

**Mitigation:** AI never calculates ledger truth; tool/calculation outputs are authoritative; source facts shown; AI disabled by default until foundation is mature.

## 71. Risk: Balance Cache Creates Stale Numbers

**Mitigation:** canonical balance use case, explicit invalidation, full-history reconciliation tests, integrity checks after import/restore.

## 72. Risk: Timezone Bugs Change Accounting Dates

**Mitigation:** separate event instant from intended accounting date/zone semantics; regression fixtures around midnight/DST/timezone changes.

## 73. Risk: License/Brand Confusion

**Mitigation:** verify source license/notices before distribution, preserve required notices/source obligations, use new branding/package/store identity, and describe Ivy only as an import-compatibility source rather than implying official affiliation. Legal review is a release gate where needed.

---

# PART XVIII — DECISIONS TO MAKE BEFORE IMPLEMENTATION

## 74. Product Decisions

1. Final product name and package ID.
2. Whether beginner budget defaults use `Committed / Future / Flexible` or another plain-language taxonomy.
3. Whether net-worth history is snapshotted daily or event-driven with monthly rendering.
4. Whether manual assets such as vehicles are included by default or opt-in.
5. Which currencies/FX provider are supported first.
6. Whether salary/payday is user-declared, detected, or both.
7. Whether cloud backup belongs before or after net-worth MVP.
8. Whether investment tracking is manual-first or connector-first.

## 75. Engineering Decisions

1. `BigDecimal` vs exact minor-unit money representation.
2. Canonical transaction time/accounting-date model.
3. Import provenance retention limits.
4. Full backup serialization format and encryption strategy.
5. Existing Room schema migration strategy.
6. Balance snapshot/cache architecture.
7. FX snapshot persistence policy.
8. How much legacy repository code can be retired without destabilizing import/backup.

---

# PART XIX — FORMULA APPENDIX

## 76. Core Formulas

### Net worth

```text
Net Worth = Total Assets - Total Liabilities
```

### Savings rate

Use an explicitly defined denominator and keep definition consistent:

```text
Savings Rate = Cash Saved / Net Income
```

### Wealth-building rate

```text
Wealth-Building Rate =
(Cash Saved + Investment Contributions + Debt Principal Repaid)
/ Net Income
```

Avoid counting interest paid as wealth building.

### Emergency coverage

```text
Emergency Coverage =
Eligible Emergency Savings / Average Monthly Essential Expenses
```

### Goal contribution

Simplest non-interest version:

```text
Required Monthly Contribution =
(Target - Current Allocated Amount) / Months Remaining
```

### Safe-to-spend

```text
Safe to Spend =
Spendable Cash
+ Expected Available Income
- Scheduled Bills
- Required Debt Payments
- Reserved Goal Contributions
- Cash Buffer
```

### Debt-to-income style burden

```text
Debt Payment Burden = Required Monthly Debt Payments / Net Monthly Income
```

Clearly label this as an app planning metric unless using a jurisdiction-specific lender definition.

### Net-worth growth

```text
Net Worth Change = Current Net Worth - Previous Net Worth
```

Percentage change needs careful handling when previous net worth is zero or negative; do not display a misleading percentage in those cases.

---

# PART XX — REPOSITORY EVIDENCE REVIEWED

## 77. Code Areas

During this review, the following current repository areas were inspected or mapped:

- root `settings.gradle.kts` module graph;
- `feature/import-data`;
- CSV importer flow and Ivy Wallet import instructions;
- current account/transaction/value models under `shared:data:model`;
- repository issue history around data-layer migration, balances, timezone behavior, backup, and testing.

Important observed implementation details include:

- current importer depends on legacy account/category/transaction models;
- import mapping supports account, amount, category, currency, date, description, title, and type;
- transfer import supports destination-account and secondary-amount semantics;
- new core `Value` is currently a `Double` wrapper;
- the project includes explicit legacy/deprecated modules;
- historical architecture tasks explicitly sought migration from old DAOs to repository abstractions;
- issue history includes balance, timezone, backup, and UI-regression concerns relevant to fork quality gates.

### Selected upstream issues relevant to the fork

- `#2961` — migrate account data access toward `AccountRepository`.
- `#2963` — migrate transaction data access toward `TransactionRepository`.
- `#2990` — balance loading/caching optimization proposal.
- `#3143` — dedicated account-balance use case and property-based tests.
- `#3118` — transaction date/timezone shift bug report.
- `#3352` / `#3398` — balance update/correctness bug reports.
- `#2709` — Google Drive auto-backup request.
- `#2647` — backup before destructive account deletion.
- `#2923` — screenshot-regression test proposal.

Repository: https://github.com/Ivy-Apps/ivy-wallet

---

# 78. Product Thesis in One Sentence

> **Keep Ivy Wallet's fast, private ledger; surround it with a beginner financial journey that tells the user what is safe, what matters next, and whether their net worth is actually improving.**

---

## 79. Recommended First Implementation Sequence

If development starts immediately, do work in this order:

1. Fork/rebrand and lock the upstream baseline.
2. Make the project reproducibly build/test in CI.
3. Capture Ivy CSV compatibility fixtures before touching importer/data models.
4. Add import preview, provenance, reconciliation, and idempotency.
5. Define exact `Money` + time semantics and write calculation tests.
6. Consolidate the balance calculation path.
7. Redesign Home around safe-to-spend and upcoming obligations.
8. Add recurring commitments and beginner budget classification.
9. Add emergency fund, goals, and debt planning.
10. Add assets/liabilities and net worth.
11. Add net-worth history/change explanations.
12. Add investment education/tracking.
13. Add AI only after all numbers can be deterministically explained without AI.

This sequence deliberately prioritizes **trust and compatibility before features**. A finance app can survive having fewer features; it cannot survive users believing its balances, imports, or net worth are wrong.
