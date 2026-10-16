# ADR 0013: Goals use explicit account reservations

Status: Proposed  
Date: 2026-10-14

## Context

The inherited Ivy setting `Settings.bufferAmount` is one global number. The current Jvy Wallet prototype uses it as a savings target, a protected balance, and the basis of emergency-fund coverage. That makes a target of AED 50,000 appear to be money already saved and prevents goals from being tied to individual accounts.

Users need goals such as a laptop in six months, an AED 20,000 ADCB target, and an AED 50,000 emergency fund. One goal may use several accounts, and a category may describe a contribution rule without representing cash.

## Decision

Introduce a goal planning layer with four separate concepts:

1. Goal target — desired amount.
2. Goal allocation — current money reserved from a specific account.
3. Funding rule — future contribution plan from an account, category, or manual source.
4. Goal activity — auditable metadata describing allocation changes, not fake financial transactions.

Safe to spend is calculated from current eligible account balances minus active account allocations. Emergency coverage is calculated from actual emergency-goal allocations divided by an explicitly named expense baseline. Targets and planned contributions are never counted as saved until money is allocated.

## Consequences

Positive:

- Per-account goals and multi-account goals are possible.
- Safe to spend becomes explainable at the account and goal level.
- Existing Ivy transactions and import behavior remain the source of truth for balances.
- Targets, plans, and actual money are no longer conflated.

Tradeoffs:

- The feature requires new Room entities and migrations.
- Category-based saving needs a planning rule rather than pretending categories hold money.
- Currency conversion and underfunded allocations need explicit UI states.
- The legacy buffer must be migrated as an unresolved target/draft rather than treated as saved cash.

## Compatibility notes

The old `bufferAmount` field remains readable. Existing imports without goal data create no goals. A non-zero legacy buffer may be converted to a draft `Protected reserve` goal with no account allocation, preserving the amount without overstating progress.
