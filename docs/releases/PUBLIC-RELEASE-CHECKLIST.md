# Public-release acceptance and recovery

This document is a gate, not a claim that unrecorded tests have passed. Track
implementation in [the execution ledger](../plans/004-public-release-readiness.md).
Use [the per-surface UI acceptance matrix](UI-ACCEPTANCE-MATRIX.md) for individual
pages, child editors and Android system surfaces; a group heading is not a completed audit.

## Before requesting an APK

- Review every route and nested flow in the execution ledger. Record implemented,
  rendered, interaction-tested and accepted separately.
- Pass compilation, unit/financial/import/security tests, lint against the existing
  baseline, and signing/version script contracts. Do not grow baselines to hide defects.
- Review fresh Light/Dark/AMOLED candidates on 320dp phones, 720dp square folds,
  short landscape, DeX and 200% text. Goldens require explicit owner approval.
- Finish English copy; preserve other translations and locale-aware parsing, but
  do not advertise unreviewed language completeness.
- No Firebase diagnostics/polling, advertising, bank sync or cloud account.
  Core use must work offline. Observe actual network traffic before release;
  optional exchange-rate requests/external links must match the privacy policy.
- Run accessibility, keyboard/IME, auth/recovery/capture/recents, widgets,
  notifications, imports/exports and large-wallet performance on real devices.
  No attached device means **blocked**, not passed.

## Signed candidate and distribution

Current owner-requested candidate: **0.7.0 / 232**, privately signed and
non-debuggable, certificate/package matching 0.6.0 (and its verified earlier chain).
See the [0.7.0 evidence record](0.7.0-candidate.md) for test/lint, artifact hash,
signature/manifest/alignment and key-cleanup checks.
The owner authorized commit/push and a GitHub release on 8 November 2026.
Preparation is tracked in [the publishing handoff](PUBLISHING.md); the current
cloud network blocks the GitHub API/upload hosts, so publication is not yet
verified. No connected upgrade or whole-app acceptance has been completed;
the remaining gates below still apply. Keep this release a prerelease.

1. Build only on owner request with the numbered private-release script and
   `-Bump patch` for fixes (as in 0.5.1), or `-Bump minor` for a feature release.
   Never use the public test key or a debug APK.
2. Check actual APK signature/certificate, application ID, non-debuggable flag,
   manifest/permissions, version, hash and size. Do not log credentials.
3. Test an in-place upgrade with the same owner certificate. Reconcile account
   balances, transfer totals, budgets, reservations, loans and dated wealth values.
4. Only with publishing approval, create a GitHub prerelease with source/license,
   accurate notes, checksum and a directly downloadable numbered APK.
5. Validate with a small cohort for at least 72 hours. Promote the same tested
   binary, not a fresh untested rebuild. Do not claim a Play Store launch.

## Existing-installation paths

| Installed app | Safe path |
| --- | --- |
| Owner-signed `com.ivy.wallet`, matching certificate | Test a numbered in-place update; keep a verified backup first. |
| Old public-demo `com.ivy.wallet.debug` | The private app is a separate installation. Export full native ZIP/JSON, import into the private app, reconcile records and keep the original app until verified. CSV is not a complete backup. |
| Upstream Ivy `com.ivy.wallet`, different certificate | Android refuses an in-place update. Validate a full backup restore on another device or isolated Android profile before considering removal. Do not change keys/package silently or instruct uninstall before verification. |

Keep backups private; native ZIP/JSON files are not automatically encrypted.
Never include the owner's actual finances in screenshots or public issues.
Private signing does not guarantee removal of Android/Play Protect warnings.

## Defect response

For financial, privacy, authentication or destructive-import defects: stop
distribution, keep original files/device data intact, document affected builds,
and ship a tested higher-version fix. Do not recommend uninstall, downgrade,
clearing data or importing blindly as a rollback strategy.
