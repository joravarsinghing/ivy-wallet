# ADR 0015: BudgetBakers-inspired modular dashboard, Jvy-owned semantics

- Status: Accepted; first implementation slice in progress
- Date: 2026-10-15
- Owners: Jvy Wallet orchestrator

## Context

The supplied BudgetBakers Wallet 9.3.12 split bundle and screenshots show a mature, card-based dashboard with accounts, planned payments, reports, charts, records, and an add-more-cards customization model. Jvy already has beginner-focused cards for Safe to Spend, Money Coming Up, goals, and editable money trends.

The reference app is useful for information architecture, but copying its dense presentation would create duplicate balances, opaque scores, and promotional clutter in a product whose promise is clarity.

## Decision

Jvy will adopt a modular Home dashboard with stable card IDs and persisted presentation-only visibility preferences. The Accounts tab remains authoritative for account detail. Home uses distinct cards for Safe to Spend, Money Coming Up, adaptive trends, ranked spending breakdown, and compact recent activity.

Jvy will not copy BudgetBakers branding, premium/partner promotions, unexplained gauges, or proprietary assets in this implementation pass. Any future reuse of authorized material must be traceable and reviewed against its license/permission and third-party notices.

Financial semantics remain owned by Jvy:

- account balances come from the ledger;
- goal reservations are metadata and are deducted exactly once from spendable views;
- planned payments remain projections until posted;
- Ivy import compatibility is a release gate;
- dashboard customization cannot modify financial data.

## Consequences

Positive:

- users can make Home fit their needs;
- accounts, goals, trends, and records can grow without a big-bang rewrite;
- each new card can have independent tests and explicit states;
- the layout preference is safe to evolve without database migrations.

Trade-offs:

- the Home module registry becomes a contract that must be versioned;
- visibility and reorder preferences require persistence and migration tests;
- more flexible UI increases Paparazzi and device-QA coverage;
- a dense dashboard must be resisted until each card proves that it answers a distinct user question.

## Rejected alternatives

1. Rebuild the BudgetBakers home screen pixel-for-pixel. Rejected because it would import visual clutter and make Jvy's ownership of the product semantics unclear.
2. Add more gauges and a composite money score immediately. Rejected because beginners need explanations before scores.
3. Persist dashboard values in a new database table. Rejected because these are presentation preferences; DataStore is sufficient.
4. Replace the current Home trend chart. Rejected; its sparse-history and loading behavior are extended with explicit daily/monthly periods rather than replaced with a second chart system.

