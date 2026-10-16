# ADR 0002: Money representation

- Status: Proposed; freeze at Gate 0
- Date: 2026-10-13

The current typed model and Room persistence are `Double`-based, while legacy import paths have higher-precision decimal semantics. New financial calculations require a canonical exact signed monetary representation with explicit currency/asset identity. Introduce it behind adapters first; do not rewrite existing Ivy storage or silently round imported values during Wave 1.
