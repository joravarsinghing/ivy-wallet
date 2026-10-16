# ADR 0005: Database migration policy

- Status: Proposed; freeze at Gate 0
- Date: 2026-10-13

Room schema changes are additive and migration-tested. Existing schema versions 101–130 and legacy fields are compatibility evidence; do not delete or repurpose columns as cleanup. Prefer new tables/adapters for Joravar-only data until round-trip and downgrade/upgrade risks are understood.
