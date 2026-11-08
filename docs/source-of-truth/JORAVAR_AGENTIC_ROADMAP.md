# Joravar Money — Multi-Agent Implementation Roadmap

> **Purpose:** Turn the Joravar Money / Ivy Wallet fork PRD into an execution system where multiple coding agents can work in parallel without contradicting each other, duplicating work, or silently breaking Ivy compatibility.
>
> **Source PRD:** `JORAVAR_IVY_WALLET_PRD.md`
>
> **Primary constraint:** Ivy Wallet data compatibility, balance correctness, and deterministic financial calculations take precedence over feature velocity.

---

## 1. Operating Model

Do **not** give several agents broad instructions such as “improve Ivy Wallet.” That guarantees overlapping edits and architectural drift.

Use this structure instead:

```text
Product PRD
   ↓
Orchestrator / Integration Agent
   ↓
Frozen contracts + ownership map
   ↓
Parallel specialist agents in isolated branches/worktrees
   ↓
Automated checks + specialist review
   ↓
Integration branch
   ↓
Wave gate
   ↓
Next wave
```

The unit of parallel work is a **bounded work packet**, not an epic.

Every work packet must define:

1. Objective.
2. Owned files/modules.
3. Files/modules the agent must not edit.
4. Inputs/contracts it consumes.
5. APIs/contracts it is allowed to expose.
6. Acceptance tests.
7. Required documentation.
8. Merge prerequisites.

---

# PART I — RULES THAT PREVENT AGENTS FROM CONTRADICTING EACH OTHER

## 2. Single-Writer Ownership

At any moment, each architectural surface has one owner.

Suggested permanent ownership:

| Surface | Owner |
|---|---|
| Root Gradle / version catalog / CI | Platform Agent |
| Ivy import/export compatibility | Compatibility Agent |
| Money, currency, time primitives | Financial Core Agent |
| DB schema + migrations | Data Architecture Agent |
| Transactions / merchant / rules | Ledger Agent |
| Bills / safe-to-spend | Cash-Flow Agent |
| Goals / emergency fund / debt | Planning Agent |
| Assets / liabilities / net worth | Wealth Agent |
| Home / navigation composition | Experience Agent |
| Education content framework | Education Agent |
| Cross-feature regression / release gates | Verification Agent |
| Product contracts / ADR index / merge decisions | Orchestrator |

Agents may consume another owner's API. They may not silently rewrite it.

---

## 3. Protected Shared Files

The following should be treated as **integration-owned files** because they are likely merge-conflict hotspots:

- root `settings.gradle*`;
- root `build.gradle*`;
- version catalogs;
- application manifest where globally shared;
- top-level navigation graph;
- dependency-injection composition root;
- database class/version declaration;
- central backup registry;
- central serialization registry;
- release workflows;
- package/application ID configuration.

A specialist agent that needs a change in a protected file should not edit it directly. It should create a small integration request such as:

```md
INTEGRATION_REQUEST.md

Need: register :feature:net-worth in settings.gradle.kts
Reason: Wealth vertical slice
Expected dependency direction: :screen:wealth -> :feature:net-worth -> :shared:domain
No other root changes required.
```

The Platform/Integration owner applies these changes after the specialist branch is ready.

---

## 4. Contract-First Rule

Before two agents depend on each other, freeze the interface between them.

Examples:

```kotlin
interface MoneyCalculator { ... }
interface IvyImporter { ... }
interface AccountBalanceUseCase { ... }
interface SafeToSpendUseCase { ... }
interface NetWorthUseCase { ... }
```

The contract includes more than the Kotlin signature. It also defines:

- units;
- currencies;
- rounding;
- nullability;
- timezone meaning;
- error semantics;
- whether a result is cached or canonical;
- whether negative values are valid;
- invariants.

Once a wave starts, an agent may not change a shared contract without an ADR/change request accepted by the Orchestrator.

---

## 5. Architecture Decision Records

Use `/docs/adr/`.

Minimum ADRs before feature work:

```text
0001-fork-package-and-branding.md
0002-money-representation.md
0003-accounting-date-and-timezone.md
0004-ivy-compatibility-boundary.md
0005-database-migration-policy.md
0006-balance-source-of-truth.md
0007-feature-module-dependency-rules.md
0008-backup-format-versioning.md
0009-net-worth-valuation-semantics.md
0010-financial-calculation-explainability.md
```

An ADR is frozen after acceptance. Reversal requires a superseding ADR rather than silently editing history.

---

## 6. Branch and Worktree Strategy

Use one worktree per agent.

Example:

```text
main
integration/wave-1
agent/w1/platform-baseline
agent/w1/ivy-compat
agent/w1/financial-core
agent/w1/data-architecture
agent/w1/verification
```

Rules:

1. All agents in a wave branch from the same integration commit.
2. Agents do not merge one another's branches.
3. Agents do not rebase another agent's branch.
4. Agents do not perform repo-wide formatting or dependency upgrades unless explicitly assigned.
5. Agent PRs target the wave integration branch, not `main`.
6. Only the Orchestrator/Integration Agent advances `main` after the wave gate passes.
7. If a packet depends on another agent's unfinished feature, use the frozen interface plus fake/test implementation instead of editing that agent's code.

---

## 7. Feature Flags

Incomplete product features must remain behind flags.

Suggested flags:

```text
beginner_home_v1
smart_ledger_v1
safe_to_spend_v1
planning_v1
net_worth_v1
education_v1
wealth_v1
ai_coach_v1
```

A feature flag can be enabled by default only after the Verification Agent confirms its wave acceptance criteria.

---

## 8. Definition of Done for Every Agent

Every coding packet is incomplete until it has:

- code;
- tests;
- no unrelated edits;
- public API/KDoc where needed;
- an entry in `AGENT_HANDOFF.md`;
- known limitations;
- exact commands used to verify the work;
- confirmation that Ivy import fixtures still pass if the packet touches data, transactions, accounts, money, dates, or backup.

Required handoff format:

```md
# Agent Handoff

## Scope completed
...

## Files/modules changed
...

## Public contracts added/changed
...

## Tests added
...

## Verification commands
...

## Assumptions
...

## Known limitations
...

## Integration requests
...

## Risks for next agents
...
```

---

# PART II — AGENT ROLES

## 9. A0 — Orchestrator / Integration Agent

This agent owns **coordination, not feature implementation**.

Responsibilities:

- convert PRD items into bounded packets;
- maintain ownership map;
- approve ADRs;
- freeze shared contracts;
- detect overlapping file ownership before work begins;
- review handoffs;
- merge agent branches into wave integration branch;
- resolve cross-agent conflicts centrally;
- run the full gate suite;
- update roadmap state.

The Orchestrator must not “quickly fix” a specialist feature during integration unless the fix is trivial. Non-trivial defects go back to the owning agent.

---

## 10. A1 — Platform / Fork Agent

Owns:

- reproducible build;
- fork identifiers/rebranding mechanics;
- CI;
- dependency/build tooling;
- feature-flag infrastructure;
- release configuration;
- upstream baseline metadata;
- licensing/notice plumbing.

Does not own financial business logic.

---

## 11. A2 — Ivy Compatibility Agent

Owns:

- Ivy CSV parser/mapping;
- import preview;
- row diagnostics;
- import provenance;
- fingerprints/idempotency;
- Ivy-compatible export;
- golden fixtures;
- transfer import semantics;
- compatibility documentation.

This agent should preserve Ivy quirks inside a compatibility boundary rather than leaking them into new domain models.

---

## 12. A3 — Financial Core Agent

Owns:

- exact Money type;
- currency semantics;
- rounding;
- accounting dates;
- transaction instant/timezone conversion rules;
- canonical balance calculation;
- calculation invariants/property tests.

This is a high-authority agent. Other financial features consume these primitives and may not invent their own money/date rules.

---

## 13. A4 — Data Architecture Agent

Owns:

- repository interfaces/implementations;
- new DB entities;
- migrations;
- snapshot persistence primitives;
- data-change observation;
- transaction boundaries;
- fake repositories for parallel feature development.

It does **not** decide product formulas.

---

## 14. A5 — Experience / Home Agent

Owns:

- beginner information hierarchy;
- home dashboard;
- global “one next action” presentation;
- why/drilldown UI patterns;
- composition of outputs from other features.

It consumes calculations; it does not recreate them in view models.

---

## 15. A6 — Ledger Agent

Owns:

- fast transaction flow;
- merchant/payee;
- category rules;
- tags;
- split/refund/reimbursement behavior;
- recurring-candidate detection inputs.

It must preserve transfer distinction and canonical transaction semantics.

---

## 16. A7 — Cash-Flow Agent

Owns:

- recurring bills;
- obligations timeline;
- beginner budget buckets;
- safe-to-spend engine;
- committed/flexible classifications.

It consumes Money, dates, balances, and ledger APIs.

---

## 17. A8 — Planning Agent

Owns:

- emergency fund;
- essential-expense classification;
- goals;
- debt payoff calculations;
- avalanche/snowball;
- protection plan;
- monthly check-in domain model.

---

## 18. A9 — Wealth Agent

Owns:

- asset/liability read model;
- base reporting currency;
- FX valuation snapshots;
- net worth;
- completeness/confidence;
- net-worth snapshots;
- change decomposition;
- wealth UI contracts.

---

## 19. A10 — Education Agent

Owns:

- lesson schema;
- contextual education trigger rules;
- progress tracking;
- plain-language explanations;
- calculator explanation components.

No financial recommendation may contradict deterministic outputs from core features.

---

## 20. A11 — Verification / Release Agent

Owns test strategy across agent boundaries.

Responsibilities:

- golden import regression suite;
- DB migration tests;
- timezone boundary suite;
- balance reconciliation tests;
- transfer-conservation property tests;
- screenshot/accessibility tests;
- performance baselines;
- cross-feature end-to-end scenarios;
- release gate report.

The Verification Agent should preferably not author the production code it verifies.

---

# PART III — STEP-BY-STEP EXECUTION WAVES

## 21. Wave 0 — Reconnaissance and Contract Freeze

### Goal

Understand the fork before changing behavior.

### Parallel agents

#### W0-A — Repository Cartographer

Deliver:

- module dependency map;
- current data flow for account/category/transaction/loan/import/backup;
- list of deprecated/legacy boundaries;
- list of protected shared files;
- build/run/test commands;
- hotspot files likely to cause conflicts.

**Production edits:** none.

#### W0-B — Ivy Data Archaeologist

Deliver:

- exact CSV field mapping;
- transfer cases;
- account/category creation behavior;
- date parsing behavior;
- currency behavior;
- import error behavior;
- starter golden fixtures generated from/compatible with stock Ivy behavior.

**Production edits:** none except isolated test fixtures if safe.

#### W0-C — Financial Correctness Analyst

Deliver:

- current money representations;
- current balance computation path;
- cache/snapshot behavior;
- timezone conversion path;
- identified precision and stale-balance risks;
- proposed invariants.

**Production edits:** none.

#### W0-D — Product/UX Decomposer

Deliver:

- current screens vs PRD target screens;
- which Ivy flows should be retained unchanged;
- beginner Home wireflow/spec;
- feature-flag map;
- cross-feature UI dependencies.

**Production edits:** none.

### Orchestrator Gate 0

Do not start Wave 1 until these decisions are frozen:

- package/rebrand strategy;
- Money representation;
- accounting date/timezone semantics;
- Ivy compatibility boundary;
- DB migration strategy;
- balance source of truth;
- module dependency rules;
- protected-file ownership.

### Gate 0 output

Tag/commit:

```text
baseline/contracts-v1
```

---

## 22. Wave 1 — Safe Fork Foundation

### Goal

Create a fork that builds reliably and can evolve without compromising Ivy data.

### Run these agents simultaneously

#### W1-A — Platform Baseline Agent

Owns:

- fork metadata;
- package/application naming strategy;
- `UPSTREAM.md`;
- CI;
- test workflows;
- feature flag infrastructure;
- baseline smoke build.

Must not touch importer/domain calculations.

#### W1-B — Ivy Compatibility Agent

Owns a dedicated compatibility surface, for example:

```text
compatibility/ivy/
  IvyCsvParser
  IvyFieldMapper
  IvyImportValidator
  IvyImportPreview
  IvyImportExecutor
  IvyCompatibleExporter
```

Deliver:

- golden fixture suite;
- import preview model;
- row-level error model;
- provenance/fingerprint model;
- same-file duplicate protection;
- reconciliation report;
- same-currency and cross-currency transfer tests.

#### W1-C — Financial Core Agent

Deliver:

- canonical Money type;
- exact arithmetic adapters;
- currency rounding policy;
- accounting date primitives;
- timezone conversion helpers;
- canonical balance interface;
- property-based tests.

Important: old Ivy entities may continue to use legacy representations internally. New domain APIs should expose the canonical type.

#### W1-D — Data Architecture Agent

Deliver:

- migration test harness;
- repository boundaries needed by new work;
- fake/in-memory repositories;
- transaction-safe write API;
- new-entity registration pattern;
- data-change event abstraction if required.

Do not add feature-specific schema unless defined in an accepted contract.

#### W1-E — Verification Agent

Independently builds:

- stock-Ivy import regression suite;
- transaction/account balance reconciliation suite;
- midnight/timezone regression suite;
- DB migration smoke suite;
- transfer conservation tests;
- baseline performance measurement.

### Gate 1 — “Trustworthy Foundation”

All must pass:

- clean checkout builds in CI;
- Ivy golden files import without silent loss;
- invalid rows are surfaced;
- unchanged file re-import does not duplicate by default;
- same/cross-currency transfers reconcile;
- new financial calculations do not use raw floating-point arithmetic as authoritative money;
- canonical and full-history balances agree;
- timezone boundary tests pass;
- DB migration tests pass;
- backup/export smoke test passes.

Tag:

```text
foundation/v1
```

---

## 23. Wave 2 — Understand & Control

### Goal

A beginner can answer: “What came in, what went out, what is coming, and what can I safely spend?”

### Contract freeze before parallel work

Freeze:

```text
TransactionReadModel
Merchant/Payee
RecurringCandidate
Obligation
BudgetBucket
SafeToSpendResult
NextAction
```

### Parallel agents

#### W2-A — Ledger Vertical Slice

Deliver:

- merchant/payee model;
- category rule engine;
- transaction search/polish;
- tags foundation;
- refund/reimbursement semantics;
- recurring-candidate output.

Do not implement safe-to-spend.

#### W2-B — Bills & Recurrence Agent

Consumes ledger recurrence candidates.

Deliver:

- recurring bill model;
- confirmation/edit UX;
- upcoming obligations query;
- missed/changed recurring bill detection;
- obligations timeline API.

#### W2-C — Beginner Budget Agent

Deliver:

- beginner bucket/classification model;
- committed vs flexible spending;
- simple monthly targets;
- overspend explanation outputs.

Avoid advanced budgeting modes for now.

#### W2-D — Safe-to-Spend Agent

Consumes:

- canonical balance;
- obligations;
- selected reserve/goal commitments;
- accounting date.

Deliver:

- deterministic safe-to-spend use case;
- explanation breakdown;
- edge-case tests for negative/zero balances and incomplete bill data.

It must expose the breakdown, not only a number.

#### W2-E — Beginner Home Agent

Works against fake implementations of the frozen APIs if needed.

Deliver:

- month income/spend;
- upcoming bills;
- safe-to-spend card;
- “one next action” slot;
- “Why?” drill-down pattern;
- progressive disclosure for advanced data.

#### W2-F — Education Framework Agent

Deliver framework only:

- lesson entity/schema;
- contextual trigger interface;
- dismiss/complete/progress state;
- reusable explanation UI;
- no AI.

### Gate 2 — “Understand & Control”

Scenario tests:

1. New Ivy user imports history.
2. App shows current account totals correctly.
3. Bills can be confirmed.
4. Home explains income vs spend.
5. Safe-to-spend is computed from deterministic inputs.
6. Transfers do not inflate spending.
7. User can inspect why safe-to-spend has its value.
8. Core flows work fully offline.

Enable by default only:

```text
beginner_home_v1
smart_ledger_v1
safe_to_spend_v1
education_v1   # framework/basic lessons only
```

---

## 24. Wave 3 — Protect & Plan

### Goal

Turn tracking into a concrete plan for resilience.

### Freeze first

```text
EssentialExpenseSummary
EmergencyFundStatus
Goal
GoalContributionPlan
Liability
DebtPayoffScenario
MonthlyCheckIn
ProtectionNextAction
```

### Parallel agents

#### W3-A — Essential Spending + Emergency Fund

Deliver:

- essential-expense tagging/classification;
- monthly essential baseline;
- emergency-fund target;
- months-of-coverage calculation;
- progress state;
- educational explanation.

#### W3-B — Goals Agent

Deliver:

- savings goal entity;
- target amount/date;
- contribution history;
- recommended contribution calculation;
- goal status and risk of missing target;
- no automated movement of money.

#### W3-C — Liability/Debt Agent

Deliver:

- normalize existing Ivy loan data into liability read model;
- interest/minimum-payment fields where available/user-entered;
- snowball and avalanche engines;
- payoff projections;
- extra-payment scenario comparisons;
- deterministic explanation.

#### W3-D — Monthly Check-In Agent

Consumes existing outputs.

Deliver a review containing:

- income;
- essential/flexible spend;
- savings;
- debt reduction;
- upcoming risks;
- one suggested next action;
- plain-language changes from previous period.

#### W3-E — Planning UX Agent

Deliver:

- Plan screen composition;
- emergency fund card;
- goals;
- debt plan;
- one coherent protection journey rather than unrelated calculators.

### Gate 3 — “Protection Plan”

A user must be able to answer:

- How many months of essential expenses can I survive?
- What goal am I saving toward and am I on track?
- What debt should I attack next under the chosen strategy?
- What single action improves my position this month?

No planning feature may silently alter user transactions, budgets, goals, or debt records.

---

## 25. Wave 4 — Net Worth & Wealth Foundation

### Goal

Make financial strength measurable across assets and liabilities.

### Freeze first

```text
Asset
AssetValuation
LiabilityValuation
FxSnapshot
NetWorthSnapshot
NetWorthCompleteness
NetWorthChangeDriver
```

### Parallel agents

#### W4-A — Asset Registry Agent

Deliver:

- cash/bank account mapping;
- manual asset records;
- valuation history;
- include/exclude from net worth;
- asset type taxonomy.

#### W4-B — FX Valuation Agent

Deliver:

- base reporting currency setting;
- FX snapshot persistence;
- deterministic conversion rules;
- historical reproducibility;
- stale-rate indicator.

No live-provider dependency should be required for offline viewing of existing snapshots.

#### W4-C — Net Worth Engine Agent

Consumes asset, liability, and FX contracts.

Deliver:

- canonical net-worth calculation;
- completeness/confidence model;
- snapshot generation;
- property tests;
- edge cases for missing valuations/rates.

#### W4-D — Change Decomposition Agent

Deliver:

- change caused by cash saving;
- debt reduction;
- asset contributions;
- valuation movement;
- FX movement where meaningful;
- unexplained/incomplete component when data is insufficient.

Never fabricate a driver merely to make components sum nicely.

#### W4-E — Wealth UX Agent

Deliver:

- current net worth;
- assets vs liabilities;
- historical trend;
- completeness status;
- “why it changed” view;
- prompts for missing major items.

### Gate 4 — “Trustworthy Net Worth”

Must answer accurately:

> What is my net worth, how complete is the number, and why did it change?

Release gate includes:

- exact reconciliation of assets minus liabilities;
- historical FX reproducibility;
- no double-counting account balances and manually entered assets;
- no transfer-created net-worth gain;
- liability payoff increases net worth only according to actual balance change;
- incomplete data is visibly disclosed.

---

## 26. Wave 5 — Grow

### Goal

Help a stable user understand long-term wealth building without turning the app into a broker.

### Parallel agents

#### W5-A — Investment Tracking Agent

- investment account/asset type;
- contribution history;
- contribution vs market-gain decomposition;
- manual/current valuation;
- allocation summary.

#### W5-B — Projection Agent

- compound-growth scenarios;
- long-term goal projection;
- inflation-aware option if assumptions are explicit;
- sensitivity ranges rather than false precision.

#### W5-C — Opportunity-Cost Agent

- compare spending/saving choices;
- explicit assumptions;
- no moralizing language;
- educational context.

#### W5-D — Advanced Education Agent

- investing basics;
- compounding;
- diversification concepts;
- risk/time horizon;
- difference between contribution and return.

### Gate 5

- projections clearly label assumptions;
- no buy/sell recommendations;
- no deterministic calculation comes from an LLM;
- contribution and valuation gains are not confused;
- offline core remains intact.

---

## 27. Wave 6 — Coach & Optional Connectivity

Do this only after prior calculation engines are stable.

### Parallel agents

#### W6-A — AI Explanation Agent

AI may:

- summarize deterministic insights;
- explain a calculation in simpler terms;
- answer questions using computed metrics;
- point to educational content.

AI may not:

- invent balances;
- compute authoritative totals from raw transactions;
- change records without explicit user confirmation;
- provide unqualified personalized investment instructions.

#### W6-B — Backup/Sync Agent

- optional encrypted backup/sync;
- conflict model;
- restore verification;
- local-first remains default.

#### W6-C — Connectivity Agent, optional

- account aggregation/bank sync only if strategically approved;
- imported/synced data must map into the same canonical ledger;
- provenance mandatory.

### Gate 6

Every AI numerical statement must be traceable to:

```text
user data → deterministic use case → structured result → AI explanation
```

Never:

```text
raw financial data → AI guess → displayed number
```

---

# PART IV — DEPENDENCY GRAPH

## 28. High-Level Dependency DAG

```text
                         ┌──────────────┐
                         │ Platform/CI  │
                         └──────┬───────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                 │
        ┌─────▼─────┐     ┌─────▼─────┐     ┌────▼─────┐
        │ Ivy Compat│     │Financial  │     │ Data Arch│
        │           │     │ Core      │     │          │
        └─────┬─────┘     └─────┬─────┘     └────┬─────┘
              │                 │                │
              └────────────┬────┴───────┬────────┘
                           │            │
                    ┌──────▼─────┐ ┌────▼────────┐
                    │ Smart      │ │ Bills/Budget│
                    │ Ledger     │ │ /Cash Flow  │
                    └──────┬─────┘ └────┬────────┘
                           │            │
                           └──────┬─────┘
                                  │
                           ┌──────▼──────┐
                           │ Safe Spend  │
                           └──────┬──────┘
                                  │
             ┌────────────────────┼────────────────────┐
             │                    │                    │
       ┌─────▼────┐        ┌──────▼─────┐       ┌─────▼─────┐
       │ Emergency│        │ Goals/Debt │       │ Home/Check│
       │ Fund     │        │ Planning   │       │ In        │
       └─────┬────┘        └──────┬─────┘       └───────────┘
             │                    │
             └─────────┬──────────┘
                       │
                ┌──────▼──────┐
                │ Net Worth   │◄──── FX/Valuation
                └──────┬──────┘
                       │
                ┌──────▼──────┐
                │ Grow/Invest │
                └──────┬──────┘
                       │
                ┌──────▼──────┐
                │ AI Coach    │
                └─────────────┘
```

Education can work alongside Waves 2–5 because it consumes structured outcomes rather than owning financial calculations.

---

# PART V — MERGE PROTOCOL

## 29. Before an Agent Starts

The Orchestrator creates an `AGENT_TASK.md` containing:

```md
# Task ID
W2-D-SAFE-SPEND-01

# Base commit
<exact SHA>

# Objective
Implement deterministic safe-to-spend calculation and explanation model.

# Owned paths
feature/safe-to-spend/**
shared/domain/safe-to-spend/**

# Read-only dependencies
shared/domain/money/**
feature/bills/api/**
feature/accounts/api/**

# Forbidden paths
settings.gradle.kts
app/** navigation composition
shared/data/backup/**
compatibility/ivy/**

# Frozen inputs
SafeToSpendInput v1
Money v1
AccountingDate v1

# Required outputs
SafeToSpendResult v1
Unit/property tests
AGENT_HANDOFF.md

# Acceptance criteria
...
```

If owned paths overlap another active task, **do not start the agent**.

---

## 30. During Agent Work

Agent rules:

- commit small logical changes;
- run local tests frequently;
- do not fix unrelated warnings;
- do not upgrade dependencies opportunistically;
- do not rename public APIs outside scope;
- record discovered cross-scope defects instead of modifying another agent's module;
- if blocked by another API, create a fake implementation matching the frozen contract;
- write a change request if the frozen contract is genuinely insufficient.

---

## 31. Contract Change Procedure

If Agent B discovers that Agent A's contract is insufficient:

1. Agent B writes `CONTRACT_CHANGE_REQUEST.md`.
2. It describes the missing use case, not a preferred implementation only.
3. Orchestrator asks the contract owner to assess it.
4. Contract owner updates contract + tests if accepted.
5. Contract version increments, e.g. `SafeToSpendResult v1 → v2`.
6. All affected agents rebase/merge the integration update.
7. No agent unilaterally forks the concept into `SafeToSpendResult2`, `NewMoney`, etc.

---

## 32. PR Review Order

For each packet:

1. **Scope check** — no unrelated files.
2. **Contract check** — frozen APIs respected.
3. **Owner review** — domain correctness.
4. **Verification review** — tests and edge cases.
5. **Integration dry run** — merge into wave integration branch.
6. **Full gate suite**.
7. Merge accepted.

---

## 33. Merge Conflict Policy

A merge conflict is treated as a coordination signal.

Rules:

- specialist agents do not resolve conflicts in files they do not own;
- integration owner resolves protected-file conflicts;
- domain owner resolves domain conflicts;
- if two agents modified the same business logic, stop and redefine ownership rather than mechanically choosing both sets of changes;
- never solve a semantic conflict with “take ours/theirs” without understanding invariants.

---

# PART VI — TESTING STRATEGY FOR PARALLEL DEVELOPMENT

## 34. Test Pyramid by Ownership

### Specialist agent tests

- unit tests;
- property tests for calculations;
- repository tests;
- component/UI tests.

### Integration tests

- import → ledger → balances;
- ledger → bills → safe-to-spend;
- loans/liabilities → net worth;
- FX snapshots → historical net worth;
- backup → restore → reconciliation.

### Release gate tests

- stock Ivy CSV corpus;
- DB migration chain;
- midnight/timezone cases;
- transfer conservation;
- balance recomputation vs cache;
- round-trip backup/restore;
- cold-start/basic navigation;
- accessibility/screenshot checks;
- performance thresholds.

---

## 35. Golden Invariants

These should run in CI from Wave 1 onward.

### Ledger conservation

For a same-currency transfer between included accounts:

```text
change in total net cash = 0
```

### Net worth

```text
net worth = total included assets - total included liabilities
```

### Transfer reporting

A transfer must not count as income or spending merely because money moved between owned accounts.

### Import idempotency

Re-importing the same unchanged source should not silently duplicate records.

### Balance integrity

```text
cached balance == full-history recomputed balance
```

within the exact canonical money rules.

### Historical reproducibility

A historical net-worth snapshot should not change simply because today's FX rate changed.

### Explanation integrity

Any displayed financial result with a “Why?” action must expose its deterministic input breakdown.

---

# PART VII — RECOMMENDED PARALLEL CAPACITY

## 36. How Many Agents at Once?

More agents are not automatically faster.

Recommended:

| Stage | Productive simultaneous agents |
|---|---:|
| Wave 0 | 4 + Orchestrator |
| Wave 1 | 5 + Orchestrator |
| Wave 2 | 5–6 + Orchestrator |
| Wave 3 | 4–5 + Orchestrator |
| Wave 4 | 4–5 + Orchestrator |
| Wave 5 | 3–4 + Orchestrator |
| Wave 6 | 2–3 + Orchestrator |

Once more than ~6 agents are changing production code simultaneously, integration overhead is likely to exceed the gained throughput unless module boundaries are already very mature.

---

# PART VIII — FIRST 20 EXECUTION PACKETS

## 37. Immediate Backlog

### Wave 0

- **W0-01:** Map Gradle/module dependency graph.
- **W0-02:** Trace transaction/account/category repository flows.
- **W0-03:** Characterize Ivy CSV import/export semantics.
- **W0-04:** Build initial Ivy fixture corpus.
- **W0-05:** Trace money types and rounding sites.
- **W0-06:** Trace balance calculation/cache paths.
- **W0-07:** Trace date/timezone conversions.
- **W0-08:** Produce current → target screen map.

### Wave 1

- **W1-01:** Fork metadata, upstream baseline, licensing notes.
- **W1-02:** CI build/test baseline.
- **W1-03:** Feature-flag infrastructure.
- **W1-04:** Ivy compatibility module skeleton.
- **W1-05:** Golden importer tests.
- **W1-06:** Import preview + row result model.
- **W1-07:** Fingerprinting/idempotency.
- **W1-08:** Canonical Money v1.
- **W1-09:** AccountingDate/timezone v1.
- **W1-10:** Balance source-of-truth use case + reconciliation tests.
- **W1-11:** Migration test harness.
- **W1-12:** Cross-feature verification suite.

Several packets can be combined if one agent is retained across closely related tasks, but ownership should remain explicit.

---

# PART IX — COPY/PASTE AGENT PROMPTS

## 38. Generic Specialist Prompt

```text
You are a specialist coding agent working on Joravar Money, a fork of Ivy Wallet.

Your task ID is: <TASK_ID>
Base commit: <SHA>

Read these first:
1. JORAVAR_IVY_WALLET_PRD.md
2. JORAVAR_AGENTIC_ROADMAP.md
3. relevant ADRs
4. AGENT_TASK.md

You own only:
<OWNED_PATHS>

Treat these as read-only dependencies:
<READ_ONLY_PATHS>

Do not edit:
<FORBIDDEN_PATHS>

Follow frozen contracts exactly. Do not introduce competing Money, date, balance, import, or net-worth abstractions.

If a frozen contract is insufficient, stop changing that contract and create CONTRACT_CHANGE_REQUEST.md explaining the missing use case and proposed minimal change.

Do not perform unrelated refactors, repo-wide formatting, dependency upgrades, or opportunistic cleanup.

Required before completion:
- implement scope;
- tests;
- run specified verification commands;
- create AGENT_HANDOFF.md;
- list every changed public contract;
- list known limitations and integration requests.

Preserve Ivy import compatibility and financial correctness above convenience.
```

---

## 39. Orchestrator Prompt

```text
You are the Orchestrator for Joravar Money.

Your job is not to implement every feature. Your job is to keep parallel agents coherent.

Before starting agents:
1. inspect active ownership;
2. split work into non-overlapping packets;
3. freeze shared contracts;
4. assign exact base commit and owned paths;
5. identify protected files;
6. define acceptance tests.

During integration:
- reject unrelated changes;
- reject duplicate domain abstractions;
- route semantic conflicts to the owning agent;
- apply protected-file integration changes centrally;
- run the complete wave gate;
- do not advance the wave if Ivy compatibility, balance integrity, migrations, or timezone tests regress.

Keep ROADMAP_STATUS.md current with task status, branch, owner, dependencies, blocking contract changes, and merge SHA.
```

---

# PART X — STATUS TRACKING

## 40. `ROADMAP_STATUS.md` Format

```md
# Current integration base
SHA: ...
Wave: 2

| Task | Agent | Branch | Owner paths | Depends on | Status | PR | Merge SHA |
|---|---|---|---|---|---|---|---|
| W2-A-01 | Ledger | agent/w2/ledger | feature/transactions/** | Money v1 | In progress | | |
| W2-B-01 | Bills | agent/w2/bills | feature/bills/** | RecurringCandidate v1 | Blocked | | |
| W2-D-01 | Safe Spend | agent/w2/safe-spend | feature/safe-to-spend/** | Obligation v1 | Ready | | |

## Active contract changes
- CCR-004: add due-date confidence to Obligation v1 — awaiting Cash-Flow owner.

## Gate failures
- none
```

This file becomes the human-readable control plane for all agents.

---

# PART XI — WHAT MUST NEVER BE PARALLELIZED CARELESSLY

## 41. Serialize These Changes

Do not let multiple agents concurrently modify:

1. DB version/migration chain.
2. Canonical Money type.
3. Accounting date semantics.
4. Balance source-of-truth logic.
5. Ivy import mapping semantics.
6. Root navigation/composition.
7. Root Gradle/version catalog.
8. Backup schema version.
9. Net-worth valuation semantics.
10. Application/package identity.

Agents can prepare dependent code against frozen APIs, but one owner must merge changes to each of these surfaces.

---

# PART XII — PRACTICAL FIRST RUN

## 42. Recommended First Session

Spin up exactly five agents plus one Orchestrator:

### Agent 1 — Repository Cartographer

Do W0-01 + W0-02.

### Agent 2 — Ivy Data Archaeologist

Do W0-03 + W0-04.

### Agent 3 — Financial Correctness Analyst

Do W0-05 + W0-06 + W0-07.

### Agent 4 — UX/Product Decomposer

Do W0-08 and map the PRD's first three product stages onto current screens/modules.

### Agent 5 — Verification Scout

Independently inspect current tests, CI, migrations, importer tests, and high-risk regression gaps. Produce a proposed release-gate test matrix without changing production code.

### Orchestrator

Consume all five reports, create ADR 0001–0010 drafts, assign ownership surfaces, and produce the Wave 1 `AGENT_TASK.md` packets.

**Do not start feature coding before this first session completes.** A few hours saved by skipping contract mapping can cost days of contradictory agent work later.

---

# PART XIII — SUCCESS CRITERIA FOR THE AGENTIC PROCESS

## 43. Process KPIs

The agentic workflow is working if:

- most specialist PRs touch only their owned paths;
- shared API changes are rare and intentional;
- feature branches remain small enough to review;
- wave integration produces few semantic conflicts;
- failed gates identify a clear owner;
- Ivy compatibility tests never become optional;
- agents reuse canonical Money/time/balance abstractions;
- new features can be disabled independently;
- every merged calculation has tests explaining its invariants;
- humans can inspect `ROADMAP_STATUS.md` and immediately know what each agent is doing.

Warning signs:

- several agents edit the same “utils” file;
- multiple concepts named `Money`, `Amount`, `Balance`, or `DateHelper` appear;
- agents modify unrelated code “while here”;
- integration requires choosing between two implementations of the same concept;
- the UI calculates financial values itself;
- feature agents directly parse Ivy CSV;
- different features apply different rounding or timezone rules;
- tests are added only after all branches are merged.

---

# 44. Recommended Build Order in One Line

```text
Map → Freeze contracts → Protect Ivy → Fix Money/Time/Balance → Build ledger/cash-flow in parallel → Protect/Plan → Net Worth → Grow → AI/connectivity last.
```

The key to successful multi-agent development is **parallelizing implementation behind stable boundaries, not parallelizing architectural decisions**.
