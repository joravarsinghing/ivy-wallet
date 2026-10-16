# Render fixtures

`john-doe-render-fixture.json` is the deterministic John Doe dataset for screenshots, Paparazzi renders, demos, and device QA.

It mirrors the current persisted concepts:

- accounts, categories, budgets, transactions, planned payments, loans, loan records, settings, tags, and category rules;
- Jvy goal entities and multi-account goal allocations;
- AED/USD exchange-rate boundaries;
- explicit derived values for balance, reservations, safe-to-spend, goal guidance, reports, and chart anchors.

The `transactions` collection is the ledger source of truth. Account balances, flow totals, and trend points must be recalculated from it. Transfers move money between accounts and are not income or expense. Goal allocations reserve existing account money; they do not create fake transactions.

This is a render fixture, not a raw backup ZIP. Dates are ISO-8601 for readability. If an import test needs this data, adapt the dates to the backup serializer's epoch-millisecond format and keep the legacy backup envelope separate from Jvy-only tables.

The fixture intentionally covers:

- two months of daily activity with salary spikes and quiet gaps;
- multiple AED accounts, a USD account, and an excluded investment account;
- active, completed, and deadline-driven goals;
- multi-account reservations and account-level available balances;
- recurring and one-time planned payments;
- categorized and uncategorized-style ledger cases, transfers, loans, tags, and category-memory rules.
