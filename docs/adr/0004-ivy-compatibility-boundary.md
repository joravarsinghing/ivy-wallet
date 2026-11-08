# ADR 0004: Ivy compatibility boundary

- Status: Proposed; freeze at Gate 0
- Date: 2026-10-13

Ivy CSV compatibility is a permanent release gate, not a one-time migration. Preserve the 16-column schema, UTF-16 export/import behavior, positive amount plus type-direction convention, transfer fields, name-based account/category reconciliation, currency fallback, ID behavior, backup relationships, and import error semantics. New capabilities use explicit adapters and golden fixtures.
