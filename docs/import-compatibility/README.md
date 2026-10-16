# Import compatibility notes

Jvy Wallet has format-specific CSV adapters for the ten source apps shown in the README, plus manual CSV import and Jvy/Ivy backup JSON or ZIP restore.

This distinction matters:

- **Adapter present** means the app has a source-specific mapping in `CSVMapper` and an import flow entry.
- **Contract smoke-tested** means a fixture exercises the expected column positions and the mapper’s required fields.
- **Real-export verified** means a genuine export or backup file from that source has been parsed through the import path.

## Evidence matrix

| Source | File path supported by Jvy | Evidence currently available | Notes |
|---|---|---|---|
| Ivy Wallet | CSV, JSON, ZIP | Real backup restore tests; user-supplied UTF-16BE ZIP inspected | Full backup restore preserves accounts, categories, transactions and settings where the schema provides them. |
| Money Manager | CSV after export/conversion | Adapter present; public schema reference | Current public exports are commonly XLSX, so convert to CSV before using this flow. |
| Wallet by BudgetBakers | CSV | Adapter present; official docs and public CSV examples | BudgetBakers also supports XLS/XLSX and OFX, but Jvy’s current flow is CSV-only. |
| Spendee | CSV | Public schema reference; adapter present | Public examples use `Date,Wallet,Type,Category name,Amount,Currency,Note` with optional trailing columns. |
| Monefy | CSV | Original Ivy sample reference; adapter present | Uses date/account/category/amount/currency/title and infers type from the amount sign. |
| 1Money | CSV | Public schema example; adapter present | Supports the fixed export shape used by the legacy adapter, including account and currency pairs. |
| Bluecoins | CSV | Public format documentation; adapter present | Bluecoins’ public tooling also documents JSON configuration, which is not the Jvy CSV path. |
| Money Manager (KTW) | CSV | Adapter present; real-export fixture still required | Keep a real export for regression testing before claiming version-wide compatibility. |
| Fortune City | CSV | Adapter present; public encoding guidance; real-export fixture still required | Encoding detection is enabled, but a current sample is still needed to validate column drift. |
| Financisto | CSV | Public default-format schema; adapter present | The mapper also joins paired transfer rows. |

The repository now includes a contract smoke test and sanitized one-row fixtures for all ten registered CSV adapters. These fixtures validate the registered column positions and required fields; they are not substitutes for release-grade testing with a real export from each app. One real, non-personal export per app is still required before claiming version-wide compatibility. No private user export is committed to the repository.

## Public references

- [Ivy Wallet data model and CSV fields](https://github.com/Ivy-Apps/ivy-wallet/wiki)
- [Wallet by BudgetBakers import rules](https://support.budgetbakers.com/hc/en-us/articles/7077275632274-Import-your-transactions-or-files)
- [Wallet by BudgetBakers export flow](https://support.budgetbakers.com/hc/en-us/articles/7151606064018-How-to-export-transactions-from-Wallet)
- [Spendee export shape](https://money-talks.app/import/from-spendee/)
- [1Money export example](https://github.com/mtotschnig/MyExpenses/issues/1039)
- [Bluecoins format documentation](https://github.com/kwongtn/Bluecoins-ImportTool)
- [Financisto default CSV schema](https://docs.google.com/spreadsheets/d/1ggg07PeW0mpJpHVl05BhgiqAr7xqXyxACfD1ZEAUvNw/edit)

## How to finish real-export verification

1. Export one small file from each source app using its standard export settings.
2. Remove personal descriptions, account names, and amounts while preserving headers, delimiters, date formats, sign conventions, and optional columns.
3. Add it under a local-only fixture directory and run the import flow against a clean database.
4. Record imported, skipped, duplicate and failed-row counts.
5. Commit only sanitized fixtures that are legally redistributable.

Do not upload personal financial exports to the repository.
