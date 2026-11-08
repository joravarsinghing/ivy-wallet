# ADR 0009: Net-worth valuation semantics

- Status: Proposed; freeze at Gate 0
- Date: 2026-10-13

Net worth is assets minus liabilities, but its completeness and valuation policy must be explicit. Define treatment for excluded accounts, loans/loan records, transfers, unsettled payments, multi-currency FX snapshots, missing rates, and manually valued assets before exposing a precise-looking number.
