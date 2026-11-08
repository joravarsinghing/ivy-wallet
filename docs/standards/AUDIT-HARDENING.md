# Audit hardening follow-up

Implemented against the working tree on 2026-10-24. No application package, installed signing identity, version counter or database schema was changed. No installable APK was built.

## Import safety

ZIP extraction rejects traversal (including backslashes/drive paths), canonical/symlink escapes, duplicate files and directory payloads. Limits: 512 entries, 64 MiB per inflated file, 128 MiB total inflated bytes. Plain/extracted backup JSON is bounded to 64 MiB before decoding. Unsupported sizes fail explicitly rather than attempting an unbounded read. These limits apply to backups, not unrelated CSV readers.

Account name matching is allowed only for one unambiguous existing/incoming pair with the same resolved currency. Different currencies remain separate. A same-ID currency conflict is rejected before any ledger write. Legacy implicit account currencies are resolved against the originating wallet's base and made explicit before an imported base currency can change their meaning. Missing optional settings no longer erase the current settings.

## Financial correctness

Manual amounts remain unsigned estimates; their selected asset/debt type determines the sign. Old inconsistent manual setups are flagged as incomplete and excluded from the trusted subtotal until reviewed and saved, not silently reinterpreted in storage.

Structural edits create a new item plus dated archive/replacement policies using the existing backup record types. Prior items, valuations, corrections and snapshots remain. Group revisions retain their constituent values. Linked-account generations may share a source only when their active dates do not overlap. Attempts to reactivate two generations at once are rejected. Structural backdating before already-recorded later decisions/values is rejected with guidance; ordinary historical valuation corrections remain available. Older builds with the former unconditional duplicate-source validator may reject backups containing multiple non-overlapping linked-source generations; current builds continue accepting legacy backups.

Coverage reviews are selected by effective date, then recording time and ID, consistently in the reader and review dialog. Backdated reviews cannot override later effective reviews.

Amount entry in Net worth and Goals uses the same localized decimal parser, cursor-safe grouping and inline error component. Dot/comma decimal locales, valid grouping, malformed grouping, localized digits and supported precision have focused tests.

## Signing and distribution: remaining owner dependency

The publicly known development key cannot be made private by a code change. Private release builds now refuse missing credentials or the repository test key, comparing public-key bytes so a reissued certificate cannot bypass the guard. CI checks the actual Gradle refusal as well as the numbered build script's contracts. The numbered script rejects debug builds and requires explicit acknowledgement for legacy public-key demo updates. Inherited automatic APK, release/tag and hard-coded Telegram publishing workflows were removed; their previous definitions remain recoverable from Git history. Compose stability reporting compiles sources without packaging the application.

Before public distribution, the owner must supply a private keystore via secure environment settings and approve how existing `com.ivy.wallet.debug` installations move to the private identity. A different certificate or package is not a drop-in update. Do not generate a new production identity or uninstall/clear existing data as an automated workaround. See [APK generation](APK-GENERATION.md).

## UI and verification

The audited Goals summary now uses neutral shared theme roles, amount typography and card shape instead of small dark text on purple. Shared contrast tests cover those role pairs. This is a targeted migration, not a claim that every inherited screen has been converted.

Accounts and Net worth have additional previews inside the real primary navigation/content boundary, including 200% text and fold/DeX sizes. The feature preview width is asserted after subtracting the rail, so standalone 720dp renders cannot stand in for narrower production content.

Android instrumentation now covers real encrypted AndroidKeyStore records, legacy recovery-code generation, PIN preservation/code replacement and authorization across Activity recreation. CI includes these security tests in addition to data/domain instrumentation. Local compilation is not a claim of device execution: emulator/device results, real biometric prompts, capture protection and TalkBack/keyboard review remain required before release. Screenshot goldens are unchanged pending owner visual approval.

### Local verification recorded on 2026-10-24

- 659 unit/render checks across 15 affected or adjacent modules: zero failures, errors or skips. Includes 228 Paparazzi render cases; renders are not a substitute for owner approval or device testing.
- 19 isolated PowerShell build-workflow contract checks passed. Their byte-only APK fixtures are not installable Android applications.
- Direct Gradle checks rejected missing credentials, the repository test certificate and a reissued certificate using the same public key. Release signing validation accepted an isolated disposable private-key fixture and invoked the guard; no APK was packaged.
- Application Kotlin/Hilt compilation passed. Data and security Android-test Kotlin compilation passed without assembling an application APK.
- Backup instrumentation includes unsafe-archive rejection without ledger/sibling-file mutation, bounded input reads and a dated financial edit surviving a fresh Room restore. These are compiled coverage additions, not locally executed device results.
- Version remains `0.4.0` / `224`. No schema migration, production signing identity change, APK distribution or screenshot-baseline update was performed.

### Subsequent private build

The statements above describe the audit checkpoint before an APK was requested. The owner subsequently supplied a private keystore, and numbered release `0.4.1` / `225` was built and its signature verified. See the [release handoff and manual data-migration instructions](../releases/0.4.1.md). Device QA and owner confirmation of restored data remain outstanding; no screenshot baselines were updated.
