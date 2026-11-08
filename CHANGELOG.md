# Changelog

Notable Jvy Wallet changes are recorded here. APK numbers identify successful
builds; the milestones below were not all published as separate GitHub releases.
The current distribution channel is **prerelease**, not device-certified stable.

## 0.7.0 — 2026-11-08

Android version code: **232**. Includes the earlier unpublished milestones below.

### Added

- Saved Home card ordering, accessible Move up/down controls, visibility options
  and Restore defaults. Recent activity is configurable too.
- Exchange-rate Refresh, provider date, last successful refresh and explicit
  stale/unknown/offline states. Manual overrides survive automatic updates.
- Bundled, offline-readable Terms of Use and Privacy Policy, with inherited
  source notices and the full GPL available from Terms.

### Improved

- Restored Home's green/silver financial-card identity and compact date controls.
- Shared Home-style shaded time-series rendering across contextual charts,
  preserving saturated outlines, gaps, signs and hidden-balance protection.
- Compact visual summaries above account, budget, goal, loan and payment lists.
- Branded wallet-balance summary and left-aligned, outlined account Add actions.
- Persistent activity cards for empty and sparse periods, without invented lines.
- Large-text Customize toolbar now reflows without splitting its action label.
- Contact section names Joravar, with one LinkedIn destination. Removed redundant
  contributor/attribution pages and modules; required notices remain available.

### Fixed

- Account graphs and day totals honor Transfers as income/expense, using the
  sending and receiving account's stored native amounts. Wallet-wide cash-flow
  charts still exclude internal transfers.
- Account day totals display their actual currency instead of a misleading
  wallet-currency label.

### Compatibility and verification

- Unchanged package, owner signing key, financial database and backup schema.
- 336 focused cases across 24 suites passed; final Home/layout follow-up reran
  86 cases. Compilation, release safety lint and lint against the existing
  baseline passed. Signature, alignment and artifact checksum verified.
- Physical upgrades, accessibility/performance acceptance, legal-copy review
  and cohort testing remain pending. See [release notes](docs/releases/0.7.0.md).

## 0.6.0 — 2026-11-07

- Added Reports → Explore and contextual analytics throughout the app.
- Added shared native chart models/rendering and exact-value disclosures.
- Preserved missing-value gaps, native-currency separation and hidden balances.
- Restyled Home's chart with a native TradingView-inspired appearance; no
  TradingView library, WebView or remote chart code was added.

## 0.5.1 — 2026-11-04

- Updated long-range trend sampling to retain thirty bounded intervals.
- Fixed period-chooser confirmation/reachability, account-action contrast and
  rounded app-logo usage.

## 0.5.0 — 2026-11-04

- Unified Home's historical totals, trend, spending and activity under one
  persisted date window, independently of optional-card visibility.
- Kept current balances and upcoming forecasts explicitly separate.
- Removed the obsolete two-tab Home/Accounts root swipe shortcut.

## 0.4.4 — 2026-10-26

- Consolidated date/time picking and corrected Goals dark-mode presentation,
  real calendar deadlines and planning-flow consistency.

## 0.4.3 — 2026-10-25

- Added a public AI-conversion schema/guide and local import review workflow.
- Improved Home trends and recent-activity icon contrast.

## 0.4.2 — 2026-10-25

- Unified planning-page navigation, actions, empty states and adaptive layouts.
- Improved budget progress, wealth review dialogs and truthful chart rendering.

## 0.4.1 — 2026-10-24

- Established shared typography, shapes, branding and design-language guidance.
- Hardened financial editing, decimal inputs, backup restoration and signing.

## 0.4.0 — 2026-10-23

- Introduced beginner-focused navigation and clearer financial explanations.

## 0.3.0 — 2026-10-22

- Expanded app security, dated wealth tracking and complete financial backups.

## 0.2.15 — 2026-10-17

Previous remote release/build baseline. Inherited Ivy Wallet history remains
in Git; it is not rewritten by this changelog.
