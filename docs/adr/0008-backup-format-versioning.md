# ADR 0008: Backup format versioning

- Status: Proposed; freeze at Gate 0
- Date: 2026-10-13

Keep native Ivy backup JSON compatibility separate from richer Joravar export. The existing top-level collections and UUID relationships remain readable. Any Joravar-only backup fields require an explicit version, migration path, provenance, and round-trip tests; do not promise native backup compatibility beyond tested schema fixtures.
