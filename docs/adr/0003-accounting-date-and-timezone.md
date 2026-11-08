# ADR 0003: Accounting date and timezone

- Status: Proposed; freeze at Gate 0
- Date: 2026-10-13

Preserve current Ivy behavior at the compatibility boundary: CSV date/time values are interpreted as device-local input and stored as UTC instants. New code must use date-specific timezone rules, retain source-timezone assumptions where needed, and never change historical import meaning without a migration/test decision. Date-only semantics and pending/due dates require explicit policies before implementation.
