# Other app / AI conversion

Status: implemented in source after APK 0.4.2. No AI service or network client is integrated. Existing source-specific CSV import and native Jvy/Ivy restore remain unchanged.

## User workflow

Settings → Import data → **Other app / AI conversion**, or the same option during onboarding:

1. Copy **AI guide + schema**. The copied text contains conversion instructions, a fictional example and a JSON Schema, never live wallet data.
2. Choose an AI provider yourself, decide whether to share an export and remove unnecessary personal identifiers. Ask for `jvy-import.json` and a separate reconciliation report. Never share authentication credentials or recovery codes.
3. Select the converted, uncompressed JSON (up to 8 MB). Validation and the count preview are read-only. No source app export is automatically uploaded by Jvy.
4. Compare the AI's report with the original export, including per-account balances, transfers, currencies and omitted features. Back up any existing wallet first. Confirm the checkbox and import only after review.

Schema validation is not proof that a model correctly interpreted source data. Jvy does not infer opening balances, invent exchange rates or guarantee arbitrary-app compatibility. AI use is optional; existing import methods remain available.

## Profile v1

Public, copyable exports: [AI guide + schema](jvy-ai-conversion-guide-v1.md) · [JSON Schema only](jvy-ai-import-v1.schema.json). Tests check that these snapshots match the production clipboard content. They contain no live wallet data. To refresh intentionally, run the data/core tests, review `shared/data/core/build/ai-import/` and update the snapshots together with the contract/version and tests.

The file is a conservative subset of the **existing native backup JSON**, not a separate ledger model or a new database schema. Its structural JSON Schema is generated from `IvyWalletCompleteData.serializer().descriptor` by [`AiImportContract`](../../shared/data/core/src/main/java/com/ivy/data/backup/AiImportContract.kt). Required IDs and profile restrictions are overlaid on the production serial names. This avoids maintaining a second set of entity field definitions.

Supported collections: accounts, categories, transactions, budgets, goals, goal allocations, planned payment rules, loans, loan records, tags, tag associations and wealth items/policies/valuations/rates. AI-produced wealth snapshots and completeness reviews are rejected. Source preferences, saved analytics definitions, category suggestions, old net-worth settings, credentials and attachment URLs are not accepted by this profile. Unsupported source features must be listed in the AI's separate report, never silently dropped.

Important semantics, included with the copied schema:

- Explicit deterministic UUIDs preserve references and make repeat conversion predictable. New IDs may duplicate existing financial records. IDs shared with different records are rejected before writes.
- Each account specifies its ISO currency. Account balances are derived from transactions, not an invented `balance` field.
- Ledger/planning timestamps are integer epoch **milliseconds**. Wealth effective dates are `YYYY-MM-DD`; wealth recording times are ISO-8601 instants. Ask about timezone and ambiguous dates.
- Income/expense amounts are positive JSON numbers. A transfer is one source/destination record with both actual currency amounts, not two flow records.
- Opening balances require source evidence and use the established `Adjust balance` title without a category. Do not double-count them as salary.
- Budget and goal targets use the selected Jvy base currency. Reserve only from accounts in that currency; mixed-currency reservations are rejected by this profile. A linked loan uses its account currency, otherwise the base currency. Loan records use their payment account currency and need the actual loan-currency `convertedAmount` for cross-currency payments. Existing users must check the base currency before import; during onboarding choose it on the next step. No FX rate is invented.
- Planned payments remain projections until posted; reservations are metadata, not transfers. Loan principal is not repeated as an initial increase record.
- Wealth amounts are exact decimal strings. Link existing accounts/loans instead of duplicating their values manually. Do not invent historical valuations, snapshots or reviews.

The full guide is authoritative for this profile. “Copy schema only” supplies the structural schema, but accounting and relationship rules also require the guide and runtime checks. Changes that break the profile require a version increase and matching tests/documentation.

## Safety and compatibility

Preview retains the exact validated payload in ViewModel memory, not a saveable Android Bundle or file URI that can change between review and confirmation. Leaving review or finishing drops the preview. Rotation uses the existing ViewModel; process death requires selecting the file again.

Confirmation revalidates the payload and uses the existing transactional restore path. It rejects conflicting IDs and foreign keys and retains the existing wealth validator. The AI profile does not import app preferences, change app lock, set screenshot permissions, clear existing data or replace the wallet. Native backup behavior is unchanged for older backups. Repeat imports with unchanged IDs/data are accepted; legacy account sync flags are ignored only for conflict comparison because the existing account mapper normalizes them on save.

Whole-file and per-record validation reject malformed JSON, unsupported fields, missing IDs, invalid types/currencies, negative or non-finite amounts, orphan references, duplicate IDs/reservations, broken transfers and invalid loan/wealth links. Validation does not prove solvency, reconcile against a bank statement or ensure the AI paired source transfer rows correctly.

No data payload is logged by the new flow. English UI strings are resource-backed; translation review is pending. The schema's technical field names remain canonical regardless of display language.

## Fictional marketing backup

Run `node scripts/generate-marketing-backup.cjs` to create:

- `artifacts/fixtures/marketing-2026-10/Jvy_Fictional_Marketing_2026-10.json`
- the same data in a ZIP containing exactly one JSON file;
- `manifest.json`, with record counts, balances and the JSON SHA-256.

This full native backup already loads through the **Jvy/Ivy backup restore option in 0.4.2**; the new AI entry is not required. It intentionally includes fictional demo settings and explicit net-worth reviews, so it is NOT an AI-profile input.

Use a separate test installation/device/profile (for example Samsung Secure Folder where available). Native restore merges data and imports the demo currency/name/theme; it does not replace or safely isolate a real wallet. Do not clear your real installation. Export and protect your real backup before any test restore.

The deterministic fixture contains 6 AED accounts, 61 transactions spanning September–25 October 2026, 3 budgets, 3 goals, 4 reservations, 4 upcoming-payment rules, 2 loans and 12 wealth items. All names, figures and histories are fictional. Expected all-account balance is **92,205.00 AED** (includes the investment account excluded from the everyday balance); net worth on **25 October 2026** is **209,905.00 AED**, with **15,800.00 AED** in liabilities. Select the matching reporting period when capturing screenshots.

The committed fixture under `shared/data/core/src/test/resources/backups/jvy-marketing-2026-10.json` is verified against the production serializers, backup restore/round trip, planning metadata and wealth calculation. Generated deliverables stay ignored. Schema/guide exports from unit tests are public contract artifacts under `shared/data/core/build/ai-import/`.

## Verification

Run the data/core and domain unit tests, import-data unit/render tests and onboarding/app compilation. Existing CSV/backup regression tests must remain green. Adaptive candidate previews cover phones, square folds, DeX, large text, landscape and AMOLED; do not promote screenshot goldens without owner approval. Real file-picker, clipboard, TalkBack and on-device import checks remain device QA.

Recorded checkpoint (25 October 2026): 472 checks passed with zero failures, errors or skips: data/core 183, domain 94, shared UI/core 60, import-data 48, Budgets 33, Goals 28 and Loans 26. Import-data covers four content states and both guide/review Back-control bounds across eight windows. Back targets retain at least 48dp and remain within both viewport axes. Phone, DeX and large-text candidate renders were inspected; screenshot reference images were not changed. Full app Kotlin compilation also passed during verification; no APK was assembled and version 0.4.2 / 226 is unchanged.

An independent Draft 2020-12 JSON Schema validator accepted the schema itself, its copied example and the supported subset of the fictional fixture. The generated JSON matches the committed/tested fixture byte-for-byte; its SHA-256 is `156ddbd2d2af556f792234a8b7609bc58c0dd326b983c307c09c43d7cdf29f19`. The ZIP contains exactly that one JSON file. No device installation, external AI upload, commit, push or release publication was performed for this change.
