# ADR 0012: Jvy Wallet branding without namespace migration

- **Status:** Accepted for Wave 1 branding packet
- **Date:** 2026-10-13
- **Owner:** Orchestrator

## Context

The fork must visibly become **Jvy Wallet** while preserving Ivy Wallet import compatibility and avoiding an unsafe package/database migration during the first product change. The launcher mark should retain the existing Ivy silhouette, use red as the brand color, and be rotated to read as a stylized `J`.

## Decision

1. Change the visible application label to `Jvy Wallet` for release, demo, debug, onboarding, and transaction-reminder surfaces.
2. Keep the existing Android application ID, Kotlin/Java package names, database schema identifiers, and Ivy import/source identifiers unchanged in this packet.
3. Replace launcher resources with density-specific red rotated artwork while preserving adaptive-icon, legacy-icon, and monochrome resource paths.
4. Defer application-ID/package migration to a separate ADR with an explicit upgrade, backup, and import-compatibility test plan.

## Consequences

- A rebuilt APK is visibly distinguishable as Jvy Wallet and can continue using existing Ivy data paths.
- The APK may still update/replace an installed Ivy package because the application ID is intentionally unchanged.
- Internal source identifiers that refer to Ivy remain as compatibility vocabulary; they are not branding regressions.
- The inherited Wave 0 Hilt test-harness stall remains a gate for deeper Wave 1 implementation and must be resolved or explicitly accepted before financial-domain changes.
