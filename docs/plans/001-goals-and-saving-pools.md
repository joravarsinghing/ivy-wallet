# Jvy Wallet Goals & Saving Pools

Status: Proposed product and implementation plan  
Scope: Goals, account reservations, contribution plans, and goal-aware Safe to spend  
Compatibility: Ivy Wallet imports remain unchanged and continue to work

## 1. Problem

The current `Savings goal` / `bufferAmount` setting is one global number. It is being used as both:

- a target the user wants to reach;
- money that is already protected;
- a reserve applied to every account; and
- the input to emergency-fund coverage.

Those meanings must be separated. A target of AED 50,000 is not AED 50,000 saved. A goal also needs to answer where the money is held, how it will be funded, when it is needed, and whether it should reduce Safe to spend.

## 2. Product model

### 2.1 Core terms

| Term | Meaning | Example |
|---|---|---|
| Goal | Desired outcome with a target amount | New laptop |
| Target | Total amount required | AED 6,000 |
| Reserved | Current account money intentionally assigned to a goal | AED 1,500 in ADCB |
| Planned | Future contribution rule or amount | AED 750/month for six months |
| Remaining | Target minus reserved money | AED 4,500 |
| Available to spend | Account balance not reserved for active goals | AED 3,200 |
| Funding source | Where planned or reserved money comes from | ADCB, Salary category, manual |
| Allocation | A goal’s reserved amount in one account | Laptop → ADCB → AED 1,500 |

The product should use “Reserved for goals” and “Available to spend”. “Blocked” may be used in education text, but not as the primary label because the money has not been moved or frozen at the bank.

### 2.2 Goal types

Goals share one engine but have different defaults:

- **Emergency fund** — long-term safety reserve; default protected from Safe to spend.
- **Purchase** — laptop, phone, holiday, furniture; optional due date; default protected when reserved.
- **Sinking fund** — recurring future cost such as insurance, annual fees, or school expenses.
- **Custom** — any user-defined target with no special assumptions.

Goal type changes education and defaults, not the underlying accounting rules.

### 2.3 Funding sources

Funding sources are deliberately different from account allocations:

1. **Account allocation**: actual money currently held in an account is reserved for the goal. This is what affects Available to spend.
2. **Category contribution plan**: a rule describing where future contributions should come from, such as “20% of Salary income” or “AED 500 from the Travel budget each month”. A category is not treated as a cash account.
3. **Manual contribution**: the user records a planned or completed contribution and chooses the account where the money is held. It never creates a fake transaction or changes the account balance.

A goal may have multiple sources and multiple account allocations. For example:

```text
Laptop — target AED 6,000, due in six months
  ADCB allocation: AED 1,500 reserved
  Cash allocation: AED 500 reserved
  Salary rule: AED 500/month planned
  Saved: AED 2,000
  Remaining: AED 4,000
```

## 3. Financial rules

### 3.1 Goal progress

```text
saved(goal) = sum(active account allocations for goal)
remaining(goal) = max(target(goal) - saved(goal), 0)
progress(goal) = min(saved(goal) / target(goal), 1)
```

Targets must never be counted as saved automatically.

### 3.2 Account protection

```text
reserved(account) = sum(active allocations from account to active goals)
available(account) = max(current balance(account) - reserved(account), 0)
```

The allocation editor must prevent a user from reserving more than the account’s current available balance. If an account later falls below its reservations, the app must show an **Underfunded allocation** warning rather than silently claiming the goal is funded.

```text
safeToSpend = sum(available(account) for eligible accounts)
```

This replaces the current global `balance - bufferAmount` calculation. Planned contributions and planned payments may be shown separately until they are implemented as a full cash-flow forecast.

### 3.3 Goal timing

If a goal has a due date:

```text
required per month = remaining / months until due
```

If a goal has a contribution plan:

```text
projected finish = now + remaining / planned contribution rate
```

The UI must explain whether a date is based on actual reserved money, a plan, or both. It must never present a projection as a guarantee.

### 3.4 Emergency coverage

Emergency coverage uses actual reserved emergency-fund money, not the target:

```text
emergency months covered = emergency goal saved / normalized monthly essential expenses
```

The expense baseline should be selectable in a later iteration: recent average, current month, or manually set essential spending. The first implementation should label the baseline clearly.

## 4. Example user journeys

### Laptop in six months

1. Tap **Goals → Add goal**.
2. Select **Purchase** and enter `Laptop`, `AED 6,000`, and a date six months away.
3. Add ADCB and Cash as allocation accounts.
4. Reserve AED 1,500 from ADCB and AED 500 from Cash.
5. Jvy Wallet shows AED 2,000 saved, AED 4,000 remaining, and AED 667/month required.
6. Safe to spend falls only by AED 2,000, not by the unearned AED 6,000 target.

### ADCB target plus emergency fund

```text
ADCB account goal: AED 20,000
Emergency Fund goal: AED 50,000
```

These are two separate goals. The ADCB goal tracks the balance reserved in ADCB. The emergency goal can reserve money in a dedicated Emergency Fund account, or in multiple accounts. Emergency coverage is calculated from the emergency goal’s actual reserved balance.

### Category-backed saving

1. Create a Holiday goal.
2. Add a category rule: `AED 300/month from Travel savings plan`.
3. Each month, Jvy Wallet shows the expected contribution and asks the user to assign money from one or more accounts.
4. Only confirmed account allocations count as saved or protected.

This preserves the distinction between a spending category and money that actually exists.

## 5. UI/UX information architecture

### 5.1 Entry points

Phase 1 should avoid another permanent bottom-navigation item. Add Goals to:

- Quick access on the wallet screen;
- the More menu;
- a compact Home summary card when active goals exist;
- the Budget screen as “Send available budget to a goal”.

Promote Goals into bottom navigation only after usage validates it as a primary destination.

### 5.2 Goals list screen

Header:

- `Goals`;
- total reserved for goals;
- total remaining across active goals;
- `Available to spend` link with a short explanation.

Sections:

- **Needs attention** — overdue, underfunded, or off-track goals;
- **Active** — goal cards sorted by due date, then priority;
- **Completed**;
- **Paused**.

Goal card:

- icon, name, and goal type;
- saved amount / target amount;
- progress bar with accessible percentage text;
- remaining amount;
- due date and required monthly amount;
- source chips such as `ADCB`, `Cash`, `Salary rule`;
- small `Reserved` label when it affects Safe to spend;
- overflow actions: edit, pause, complete, archive.

Empty state:

> Give your money a job. Create a goal for something important, then reserve money from one or more accounts.

### 5.3 Add goal flow

Use a four-step wizard with a persistent summary at the bottom:

**Step 1 — What are you saving for?**

- type: Emergency fund, Purchase, Sinking fund, Custom;
- name, icon, color;
- optional template chips: Laptop, Phone, Holiday, Car, Home, Education.

**Step 2 — How much and by when?**

- target amount and currency;
- optional due date;
- instant calculation of required monthly amount;
- clear distinction between target and currently saved: `Saved today: AED 0`.

**Step 3 — Where should the money come from?**

- choose one or more accounts for current allocations;
- optionally add category contribution rules;
- optionally add a recurring planned contribution;
- show account balances and current Available to spend before entry.

**Step 4 — Protect and review**

- reserve current money now, or create the goal without reserving;
- toggle: `Count reserved money as unavailable to spend`;
- review target, saved, remaining, deadline, plan, and account impact;
- confirmation copy: `This does not move money at your bank. It earmarks money inside Jvy Wallet.`

### 5.4 Goal detail screen

Top area:

- large progress ring or battery;
- `AED 2,000 saved of AED 6,000`;
- `AED 4,000 remaining`;
- status: On track, Needs AED X/month, Underfunded, or Complete.

Sections:

- **Timeline** — due date, projected finish, required contribution;
- **Reserved money** — account allocation rows with current balance, reserved, and available;
- **Contribution plan** — category rules and recurring plans;
- **Activity** — allocation changes and manual adjustments, clearly separated from transactions;
- **Education** — one short explanation of how the numbers work.

Primary actions:

- `Reserve money`;
- `Add a funding source`;
- `Adjust allocation`;
- `Edit goal`;
- `Pause` or `Complete`.

### 5.5 Reserve money sheet

The sheet is account-first because reservations affect spendability:

- account name and current balance;
- current reserved amount;
- available amount;
- amount field;
- goal impact after saving;
- validation if the amount exceeds available money;
- multi-account allocation summary;
- explicit save action.

Never call this a transfer. It is an internal reservation.

### 5.6 Account detail integration

Add a `Goals` section to each account:

```text
ADCB
Balance                         AED 11,257.83
Reserved for goals              AED  1,500.00
Available to spend              AED  9,757.83

Laptop                          AED  1,500.00
Emergency fund                  AED      0.00
```

Tapping the section opens the allocation breakdown. This makes the “why did Safe to spend change?” question answerable.

### 5.7 Home integration

Replace the current global-buffer interpretation with:

- **Safe to spend** — sum of eligible account balances after active reservations;
- **Goal progress** — next deadline, amount saved, and amount needed this month;
- **Emergency cushion** — actual emergency-goal reserved money divided by the chosen expense baseline.

If no goals exist, show a beginner-friendly prompt to create one rather than a misleading reserve number.

### 5.8 Budget integration

On Budget detail and completion states, offer:

- `Reserve leftover for a goal`;
- `Create a goal from this budget`;
- `Add this category as a contribution rule`.

Budget spending remains transaction-based. Goal reservations remain balance-based. The UI must not imply that an unspent category budget is cash unless the user assigns actual money from an account.

## 6. Data and architecture

### 6.1 New persisted concepts

Use new tables/entities rather than changing Ivy transaction semantics:

```text
Goal
  id, name, type, targetAmount, currency, dueDate,
  icon, color, status, protectFromSafeToSpend,
  expenseBaseline, createdAt, updatedAt

GoalAllocation
  id, goalId, accountId, reservedAmount,
  currency, createdAt, updatedAt

GoalFundingRule
  id, goalId, sourceType, sourceId,
  amount, percentage, frequency, active

GoalActivity
  id, goalId, accountId?, amount, type,
  note, createdAt
```

`GoalActivity` is metadata, not a transaction. Existing Ivy transactions remain the source of account balances. `GoalAllocation` is the current reservation state; activity explains how it changed.

### 6.2 Currency and exactness

- Store financial values using the new exact-money boundary described in the PRD; do not add more `Double` calculations.
- A goal has one target currency.
- Account balances are converted to the goal currency using the existing exchange-rate boundary.
- If conversion is unavailable, show `Needs exchange rate` and exclude that allocation from totals rather than silently using a stale or zero value.

### 6.3 Ivy compatibility

- Existing `Settings.bufferAmount` remains readable for old Ivy imports and backups.
- On first goals migration, a non-zero legacy buffer becomes a draft `Protected reserve` goal with the same target, no saved allocation, and a setup explanation.
- The migration must never claim the legacy target is saved.
- Old imports with no goals remain valid and simply show no active goals.
- New goal data is optional in exports and ignored by older Ivy versions.
- Import tests must verify old CSV/backup data, account IDs, transfers, currencies, and settings continue to reconcile exactly.

## 7. Implementation plan

### Packet G0 — Contract and calculations

- Freeze the Goal, GoalAllocation, GoalFundingRule, and GoalActivity contracts.
- Implement exact calculators for progress, remaining, required contribution, account available amount, and emergency coverage.
- Add invariants: no duplicate account allocation row, no negative target, no over-reservation, no target counted as saved.
- Add ADR before schema work.

### Packet G1 — Persistence and migration

- Add Room entities, DAOs, repositories, and migrations.
- Add optional backup/export serialization.
- Add legacy `bufferAmount` adapter and migration preview.
- Add repository and migration tests using Ivy backup fixtures.

### Packet G2 — Goals list and creation flow

- Build `feature/goals` with list state, empty state, goal cards, wizard, and detail navigation.
- Ship Purchase and Emergency fund types first.
- Use existing Ivy visual primitives while keeping copy beginner-friendly.

### Packet G3 — Account reservations

- Add reserve-money sheet and multi-account allocation.
- Add account detail breakdown.
- Wire reservations to Available to spend.
- Add conflict and underfunded states.

### Packet G4 — Home and budget integration

- Replace global-buffer Home calculations with reservation-aware calculations.
- Add goal summary and emergency card based on actual saved amounts.
- Add budget-to-goal handoffs without creating fake transactions.

### Packet G5 — Funding rules and projections

- Add category contribution rules, recurring plans, projected finish, and notifications.
- Add manual goal activity with audit history.
- Add category surplus suggestions only after period and currency rules are proven.

## 8. Acceptance criteria

- Creating a AED 50,000 goal shows AED 0 saved until money is actually allocated.
- Reserving AED 2,000 from ADCB reduces ADCB Available to spend by AED 2,000 and no other account.
- A goal can have allocations from ADCB and Cash simultaneously.
- Two goals cannot reserve the same available amount twice.
- A target amount never changes an account balance or creates a transaction.
- Emergency coverage uses actual emergency-goal allocations, not the emergency goal target.
- Category rules affect plans and projections, not balances, until money is explicitly allocated.
- A user can see why Safe to spend changed and which goals consumed the reservation.
- Old Ivy imports and backups still load with no goals and no data loss.
- Older Ivy builds can ignore the optional goal section in new exports.
- All calculations have exact-money tests for zero, fractional, boundary, multi-account, underfunded, and currency-conversion cases.

## 9. Recommended first release

Ship the smallest trustworthy slice first:

1. Purchase and Emergency fund goals.
2. Target, due date, and actual account allocations.
3. Multi-account reservations.
4. Goal-aware Safe to spend.
5. Goal list, detail, and account breakdown.
6. Legacy buffer migration as a draft target, never as saved money.

Defer category automation, notifications, and automatic transfers until the reservation model is proven. The product’s first promise should be simple and reliable:

> You decide what money has a job. Jvy Wallet shows what is reserved, what is still needed, and what is genuinely available to spend.
