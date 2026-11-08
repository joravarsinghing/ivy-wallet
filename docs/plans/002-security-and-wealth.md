# Security sessions and the net worth workspace

## Implemented behavior

- A process-owned authentication session survives configuration changes. A real background transition starts a monotonic 60-second grace period. Screen-off and device locking invalidate the session immediately; process restart requires authentication when protection is enabled.
- Authentication requests have separate request identities and session epochs. A stale callback cannot authorize a newer request. Modern destination ViewModel stores no longer clear the Activity's root/authentication store.
- Pure preview/screenshot hosts do not need an Activity-owned ViewModel store. Real Activity hosts still retain destination stores across configuration changes; goals previews cover this boundary.
- Security offers a six-digit Jvy PIN, biometrics, either method, and Off. Existing device-authentication configurations migrate without silently changing their allowed method.
- PIN/recovery verifiers use randomized salts and PBKDF2-HMAC-SHA256 (600,000 iterations). AES-GCM records use an Android Keystore key under `noBackupFilesDir`. Retry delays persist across process restart and reboot. Recovery requires a replacement PIN and recovery code; verification alone never exposes the ledger.
- Financial backups exclude installation security. Restoring an older backup cannot disable the destination device's lock.
- Application-scoped security observation refreshes the balance widget when protection changes, even after the settings Activity closes. Initialization waits for the vault before updating the legacy widget privacy flag.
- Net worth has Overview, Assets, Liabilities, History, review, valuation and FX editors, and CSV exports. Accounts and More link to the workspace. Wide windows show a separate details pane.
- Manual items cover property, vehicles, investments, retirement, gold, crypto, business interests, receivables and debts. A portfolio uses a total valuation or constituent holdings, never both. Native values, effective dates, ownership and explicit inclusion remain separate from wallet balances and goal reservations.

## Accounting rules

`net worth = positive signed positions - absolute negative signed positions`.

Manual debt values are entered as positive amounts owed and converted to negative positions. Linked accounts have an explicit balance convention. A bank overdraft is debt; a credit-card overpayment is an asset. Loan adapters reuse posted principal records and exclude records marked as interest from principal reduction.

`holding value = quantity × unit price`; `owned value = value × ownership / 100`. One troy ounce is 31.1034768 grams. Debt and asset ownership are independent: a property value is not reduced by its mortgage until the final liabilities subtraction.

A payment of 1,000 from cash against principal changes cash by -1,000 and the signed debt position by +1,000: net worth is unchanged. Actual interest and fees reduce wealth. Posted transfers retain both native sides. Planned transactions with no posted date and goal reservations are not assets or liabilities.

FX rates apply only on or after their effective date. Direct and inverse pairs are supported. Wallet-cache imports are explicit observations made today and warn that the original rate date is unknown. Missing values/rates produce an incomplete known subtotal, not a substituted zero. Stale valuations and rates show review warnings.

Manual portfolios cannot automatically identify which real-world investments a wallet account represents. Explicitly exclude a wallet account if a manually entered holdings portfolio already represents its full value. Linked loan/account duplicates have an explicit replacement selector. A gold holding's unit is fixed after creation so old quantities retain their meaning.

Policies and valuations are effective-dated. Historical corrections append a replacement event rather than overwrite an old record. Snapshots retain their serialized wealth inputs, source positions, selected rates, checksum, and the replaced snapshot ID. Old account-only snapshots remain retained as legacy data, without guessing their missing reporting currency.

Changes are reconciled across account, debt, manual valuation/flow, FX and inclusion movements. They are not presented as investment returns. Coverage requires explicit user review of assets/debts and inclusion decisions for existing accounts/loans. It cannot guarantee that undisclosed assets or debts have been entered.

## Compatibility and verification

Room 133 → 134 adds `wealth_records` without changing existing ledger tables. Full backup wealth documents have their own version and preserve IDs/relationships; historical document IDs cannot be overwritten with different payloads. Older Ivy backups remain readable and the 16-column Ivy CSV contract is unchanged. Wealth CSV exports are separate from transaction CSV.

Automated coverage includes lock timing and stale epochs; PIN derivation; transfer conservation and posting-date boundaries; principal/interest accounting; debt sign changes; portfolio duplication; ownership precision; missing/future FX; dated archival/corrections; backup validation and versioned serialization. Layout snapshots cover phone, square/foldable and DeX widths, including hidden balances and empty states.

Device acceptance still requires real biometric/Keyguard interaction: rotate while unlocked and during the OS prompt, fold/unfold, resize in DeX, return at 59/60 seconds, lock the device within the grace period, recover a PIN, and interrupt recovery/configuration with a device lock. Instrumented migration tests require a connected Android device/emulator.

## Research provenance

The product specification and existing financial contracts were inspected locally. Public requests to Android documentation, Actual Budget, YNAB and NerdWallet returned HTTP 403 from this environment, including an elevated retry. No current third-party behavior is claimed as verified.

Reference targets for a future accessible comparison: https://developer.android.com/identity/sign-in/biometric-auth, https://actualbudget.org/docs/, https://support.ynab.com/, https://help.monarchmoney.com/, and https://www.nerdwallet.com/article/finance/net-worth-calculator. Core formulas above are explicit implementation rules, not attributed quotations.

The initial implementation did not generate an APK or release version. Subsequent distributable builds require the official numbered APK workflow.

## Verification results — 2026-10-21

Two full application/Hilt compilation passes succeeded. The focused regression pass succeeded with 253 tests: 6 session/crypto, 5 security ViewModel, 84 domain, 145 data/backup, 1 navigation ownership, and 12 phone/fold/landscape-DeX layout snapshots. Snapshots were visually inspected, including masked balances and the wide two-pane layout. The fractional-second ordering regression is included in the domain count.

The Room migration instrumentation sources compile successfully. Their runtime execution, Android biometric/Keyguard behavior, and physical folding/DeX interactions remain pending because no Android device/emulator is connected. `git diff --check` passes.

### Backup-coverage fix — 2026-10-23

The previously logged omission of `goals`, `goal_allocations`, and `category_rules` is fixed. Optional backup fields include active/archived goals, reservations and active/inactive category rules. Export reads linked Room collections in one transaction; import writes them in the existing atomic restore transaction, remapping account/category IDs using the existing compatibility path. A restored goal replaces its own allocation set so removed reservations do not survive. Goals absent from an older backup are left untouched.

Historical metadata referring to deleted accounts/categories is retained without inventing ledger records. Invalid target/reservation amounts are rejected before database writes. Eight additional unit regressions cover fresh restores, precision, ID adaptation, older backups, goals-only imports, invalid amounts, replacement allocation sets, deleted references and installation-lock isolation. A real-Room fresh-database round-trip instrumentation test was also added; runtime execution still needs a device.

The official numbered PowerShell workflow now works on Windows and Linux, serializes version allocation, rejects artifact collisions and only persists a version after the nonempty APK and checksum manifest exist. Its isolated contract test uses a fake launcher, not a distributed debug APK.

### Release verification — 2026-10-23

The final combined results contain 278 passing automated tests with zero failures/errors: security/session 6, security ViewModel 5, domain 84, data/backup 153, navigation ownership 1, net-worth golden layouts 12, budgets 1, planned payments 2, adaptive main navigation 3, goals 4 (including light/dark previews), and saved analytics/reports 7. The preview regression found during the broader run is fixed and the focused rerun succeeded. The numbered-build workflow also passed 12 isolated contract assertions.

Migration and real-Room backup instrumentation sources compile. Physical-device authentication, rotation/folding/DeX behavior and instrumentation execution still require a connected device. No runtime device coverage is claimed.

### Numbered APK — v0.3.0 / 223

Built through `scripts/build-jvy-apk.ps1 -Variant demo -Bump minor`. The canceled earlier attempt did not allocate or persist a version. The successful baseline advances from the existing v0.2.16 / 222 artifact to v0.3.0 / 223.

`artifacts/apk/Jvy_wallet_v0.3.0.apk` is 14,729,290 bytes (14.7 MB), minified/resource-shrunk, and verified `debuggable=false`. Its `com.ivy.wallet.debug` package and signing certificate match v0.2.16 for in-place updates; this is the existing-key demo distribution, not a private production-key release. ZIP CRCs, APK signature, alignment, app label, minimum SDK 28 and version metadata checks passed. The generated checksum manifest matches the binary.

SHA-256: `b4402242d2031b095c5b2a13fb9ec19577524574578492589a46a349aaa93a7b`.
