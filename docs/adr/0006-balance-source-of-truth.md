# ADR 0006: Balance source of truth

- Status: Proposed; freeze at Gate 0
- Date: 2026-10-13

Room transaction/account data is the persisted source of truth. Repository caches are projections and must be invalidated or rebuilt from canonical records. Balance policies must explicitly exclude unsettled/planned records, honor account inclusion, distinguish transfers from income/expense, and surface integrity failures rather than silently dropping invalid records.
