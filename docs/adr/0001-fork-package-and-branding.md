# ADR 0001: Fork package and branding

- Status: Proposed; freeze at Gate 0
- Date: 2026-10-13

Joravar should use an explicit fork identity and package namespace only after a compatibility-preserving baseline is reproducible. Until then, keep upstream package/data identifiers where changing them would affect import, backup, migrations, widgets, or existing user data. Branding and package migration must be a separate, test-backed packet.
