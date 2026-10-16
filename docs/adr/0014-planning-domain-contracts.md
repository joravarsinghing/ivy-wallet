# ADR 0014: Freeze planning domain contracts

Status: Accepted
Date: 2026-10-14

## Context

The next planning features need stable domain seams before UI, persistence, and
navigation work proceeds. The existing application has legacy `Double` money
boundaries and ledger models that are not suitable as the source of truth for a
new forecast or suggestion engine.

## Decision

### Money Coming Up

- The forecast window is exactly 30 calendar days, inclusive of its opening
  date.
- The forecast accepts planned income and planned expenses only. Ledger
  transactions, transfers, and historical entries are not forecast inputs.
- Active goal/account reservations are deducted once from the opening balance.
  They are not deducted again on each forecast day.
- A forecast is a pure read-only projection. Calculating it never creates,
  edits, or deletes a ledger entry.
- New forecast amounts use `BigDecimal`. Legacy `Double` values are converted
  only at an explicit boundary.

### Goal funding guidance

- Guidance is derived from the goal target, current active reservations, due
  date, and an optional planned contribution amount.
- A target and a planned contribution are not reserved money. Only active
  account allocations count as reserved.
- Guidance calculation has no write side effects and does not create a
  contribution, transfer, or transaction.

### Category-rule matching

- Matching is deterministic: active rules are normalized case-insensitively;
  ties resolve by priority, then pattern specificity, then rule ID.
- Existing category assignments win over suggestions.
- Rules apply to future entries on or after the effective date only; they do
  not rewrite historical entries.
- The domain contract returns a suggestion. Applying a category remains an
  explicit caller action.

## Consequences

- UI and persistence layers can consume stable, pure contracts without
  coupling calculations to Room or ledger writes.
- Exact-money tests can prove boundary behavior before adapters are introduced.
- A later adapter may translate legacy `Double` planned-payment data into the
  BigDecimal contracts, but must not move floating-point arithmetic into the
  domain calculations.
