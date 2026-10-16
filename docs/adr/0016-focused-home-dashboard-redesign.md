# ADR 0016: Focused Home dashboard hierarchy

- Status: Accepted
- Date: 2026-10-15
- Owners: Jvy Wallet orchestrator

## Decision

Home answers four different questions in order: what can I safely spend, what is coming up, where did money go, and what activity needs attention. The detailed Accounts tab is not duplicated on Home. Recent transactions become a compact bounded activity feed, and spending structure is represented by ranked horizontal bars rather than another donut or total card.

Trend periods use explicit local calendar identities. `7D` and `30D` use daily buckets; `3M`, `6M`, and `12M` use `YearMonth` buckets. Periods before first activity remain gaps, while quiet periods after activity starts are real zeroes. A single observed period is shown as a point without a connecting line.

## Invariants

- Ivy import data and the ledger remain unchanged.
- Goal reservations remain metadata and are not fake transactions.
- Dashboard visibility changes presentation only.
- All Home amounts use grouped formatting and hidden-balance behavior.
- The full Transactions screen retains its existing detailed cards.
