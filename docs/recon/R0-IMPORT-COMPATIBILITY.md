# R0-IMPORT — Ivy compatibility reconnaissance

Status: complete, read-only reconnaissance

## Compatibility contract

Stock Ivy CSV compatibility is a permanent release gate. The current 16-column header is:

`Date, Title, Category, Account, Amount, Currency, Type, Transfer Amount, Transfer Currency, To Account, Receive Amount, Receive Currency, Description, Due Date, ID`

Exported CSV is UTF-16; dates are local ISO date-times without an offset. Expense/income amounts are non-negative. Transfer rows use source amount in `Transfer Amount`, destination account in `To Account`, and destination amount in `Receive Amount`; `Amount` is zero.

## Observed behavior to preserve and test

- Legacy/source-specific and manual import paths differ. Ivy CSV is read as UTF-16 by the legacy path; manual mapping uses OpenCSV and treats the first row as a header.
- Amounts are normalized to absolute positive values; transaction type carries direction.
- Accounts/categories are reconciled by case-insensitive exact name in CSV import; new records receive generated IDs/metadata.
- Account currency is authoritative; missing/invalid currency falls back to the configured base currency. Transfer destination currency falls back to the source account currency.
- Transfers require distinct source/destination accounts; missing destination amount falls back to source amount in the legacy path.
- CSV IDs are ignored and new UUIDs are generated; backup JSON relationships/IDs are reconciled separately.
- CSV dates tolerate many formats and invalid dates fall back to approximately one year ago; manual invalid dates fail. Both use device-local interpretation before UTC storage.
- Backup JSON has top-level collections for accounts, budgets, categories, loans/records, planned payments, settings, transactions, shared preferences, tags, and tag associations.

## High-risk finding

`CSVImporterV2` currently parses the transfer source amount from the destination amount field and parses that field again for `toAmount`. This is a known defect to capture in a compatibility regression test before fixing in a later packet.

## References

`shared/domain/.../csv/IvyCsvRow.kt`, `ExportCsvUseCase.kt`, `feature/import-data/.../CSVImporterV2.kt`, `temp/legacy-code/.../CSVImporter.kt`, `shared/data/core/.../BackupDataUseCase.kt`, committed backup fixtures.
