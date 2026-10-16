# R0-DATA — Data and financial architecture

Status: complete, read-only reconnaissance

## Current shape

Room entities/DAOs flow through `shared:data:core` repositories and mappers into typed `shared:data:model` models, then into partially implemented `shared:domain` use cases and feature ViewModels. Legacy adapters remain active.

- Room database version is 130 with schemas 101–130 and migrations.
- `TransactionEntity` stores `Double amount`, optional `Double toAmount`, nullable date/due date, source/destination account IDs, and legacy sync/delete fields.
- Typed `Value`/`PositiveValue` models validate wrappers around `Double`; transaction direction is encoded by type and amounts are positive/non-zero.
- `BalanceBuilder`, account statistics, wallet statistics, exchange, and legacy calculations remain `Double`-based or incomplete.
- Repository mappers can drop invalid records with `mapNotNull`, fall back on invalid currency, and fall back from invalid destination amounts.
- Caches and direct DAO access coexist with repositories; planned/unsettled transaction treatment needs an explicit policy.

## Risks

1. Binary floating point is pervasive, including storage, exchange, budgets, loans, and calculations.
2. Zero and signed money are not first-class.
3. Legacy BigDecimal-to-Double round trips can lose exactness.
4. Transfers, excluded accounts, loans, unsettled payments, and unavailable FX rates are unresolved net-worth policy inputs.
5. `isDeleted` is retained for compatibility despite hard-delete paths and migration cleanup.

## Proposed ownership

`shared:data:model` owns canonical exact-money/asset/transaction contracts; `shared:data:core` owns persistence, migrations, adapters, and compatibility; `shared:domain` owns calculations and policies; `temp:legacy-code` owns legacy conversion only; features consume domain/repository APIs and do not calculate financial truth.

## References

`shared/data/model/**`, `shared/data/core/src/main/java/com/ivy/data/db/**`, repository mappers, `shared/domain/**`, `temp/legacy-code/**`.
