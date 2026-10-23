# Beginner-first wallet navigation, net worth and privacy

## User journey

The starting assumption is an ordinary user who knows the balance on a statement, not financial terminology. The app should explain the next decision instead of displaying an accounting control panel.

1. Open **Net worth** directly from primary navigation, More, or the Home dropdown. Existing Accounts links still work.
2. The empty state says that nothing has been entered, rather than implying a genuine zero net worth. A replayable four-step guide explains possessions, debts, reusing existing accounts, and the meaning of the result.
3. If unreviewed wallet accounts/loans exist, a prominent action offers to link them first. They are not silently included or duplicated.
4. For another item, choose **Add what I own** or **Add what I owe**. Enter a name, type, currency, current value/remaining debt, and date. Shared ownership, portfolios, contributions, interest and review notes remain available under optional details. Existing non-default values are retained.
5. Review the account/loan list and confirm assets/debts coverage honestly. An incomplete total remains an estimate, not a fabricated complete figure.
6. Return to **Progress** to see historical observations. Missing dates remain gaps. Info buttons explain concepts and calculations without requiring a browser.

Example walkthrough (illustrative amounts, one currency): link 5,000.00 in savings once, add a car currently worth 8,000.00, and add the 3,000.00 remaining car loan. The existing engine calculates 10,000.00 net worth. The guide explicitly says this does not make 10,000.00 available to spend; savings are still 5,000.00. Purchase price, original loan principal, monthly payments and goal reservations are not substituted for current values.

## Information design

- Primary sections are Home, Accounts, Plan, Net worth and More. Plan organises budgets, upcoming payments, goals and loans; More indexes reports, search, categories, security and settings/backups.
- Phones use labelled bottom navigation. At accessibility font sizes the selected label receives extra width and all targets retain spoken names and minimum touch widths. Wider windows use a branded rail with actual content space reserved, not an overlay.
- The Home dropdown remains as a familiar shortcut, with Net worth near the top. Appearance, currency, sharing and the open-source link remain available.
- Income, expense, transfer, account creation and planned-payment entry remain reachable. Existing FAB drag shortcuts are retained alongside a conventional action sheet.
- Net-worth terms lead with “what I own” and “what I owe.” Contextual explanations cover assets, debts, current values, ownership, duplicate counting, currency conversion and history. Examples are clearly marked and never replace real values.
- Date pickers preserve the existing ISO local-date storage contract; display dates use the locale. Advanced controls are progressively disclosed, not deleted.
- Empty, incomplete, hidden-balance and stale-value states retain distinct meanings. Details and desktop panes scroll when necessary.

## Unlock and recovery

The lock surface uses the existing Jvy color logo, a centred bounded layout, clear unlock actions, a PIN alternative when configured, and a visible troubleshooting path. It scrolls on short windows and at large font sizes. Biometric request identity, session epochs, throttling, lifecycle and lock timing are unchanged.

Recovery is truthful: configured recovery codes can reset the Jvy PIN without deleting financial data. Older device-authentication-only profiles have no invented recovery code or bypass. Their help explains Android's device PIN/password/pattern alternative and recommends adding a Jvy method/recovery code after unlocking. Troubleshooting never recommends clearing storage or uninstalling.

## Screen capture policy

Security includes **Screenshots & screen recording** with an explicit privacy confirmation. It controls Android's secure-window flag; it does not implement a recorder or disable Google Play Protect.

The preference is installation-local, not part of financial backup exports. Before an explicit choice, the previous default is preserved: ordinary unlocked screens are protected when app lock is enabled, and capturable when it is off. An explicit choice can allow or block unlocked financial screens regardless of app-lock mode.

Initializing/locked screens, Security/PIN settings and a visible recovery code are always protected. Recovery-code dialogs additionally enforce a secure dialog window. A single root policy owns the Activity flag so a departing composable cannot accidentally clear another screen's protection. Enabling capture warns that financial details and unlocked recent-app previews may be exposed; use sample data or hidden balances for marketing.

## Compatibility and verification

No ledger formulas, valuation engine, Room schema, backup fields, transaction CSV columns, security credentials or app identity are changed. The new main-tab values are in-memory navigation, not persisted financial enums. The main shell composes the existing net-worth feature rather than implementing another calculator.

Verification includes capture-policy truth-table cases, authenticated visibility guards, destination coverage, adaptive clearance, explanatory-content contracts, financial/backup regressions and layout snapshots across small phone, phone, square foldable, desktop/DeX and 200% text. Representative goldens are visually inspected before verification. These are separate rendering cases, not a simulation of hardware folding, Android biometric prompts or every device/font-size combination.

The broader Home test exposed an existing expectation that omitted the already-supported Saved reports card. The expected list was corrected; the feature was not removed to make the test pass.

The implementation/validation pass did not generate an APK or advance versioning. A subsequent user-requested APK uses the official numbered build workflow. Physical-device biometric/Keyguard, screenshot/recording, keyboard, screen-reader and real folding/DeX interaction checks still require device QA; desktop rendering is not a substitute for those checks.

### Verified on 2026-10-23

- App Kotlin/Java/Hilt compilation passed (`:app:hiltJavaCompileDebug`; no APK packaging).
- 385 tests passed across security, net worth, navigation, Home, domain calculations, backup/data, goals, reports, budgets and planned payments; zero failures, errors or skips.
- The total includes 70 verified Paparazzi cases: 35 net-worth/guide views, 25 unlock/privacy views and 10 navigation/tool-hub views. All three feature `verifyPaparazziDebug` tasks passed after recording and reviewing the redesigned surfaces.
- The guide's savings/car/loan example is checked against the real valuation engine. Capture-policy tests preserve legacy defaults, explicit user choices, and mandatory protection for locked or credential-bearing screens.
- Visual review caught and corrected fragmented bottom-navigation labels, low-contrast selected net-worth controls and a misleading recovery label on biometric-only profiles. Dialog/action-sheet scrolling covers short-window and large-text access.
- `git diff --check` passed. Financial schemas/calculators, backup contracts, `version.properties` and the existing APK are unchanged.

Before shipping, test the capture toggle with Android screenshots, recording and recent-app previews; confirm that lock/PIN/recovery remain blocked; exercise PIN, biometric and recovery flows during rotation, backgrounding and device locking; check keyboard/TalkBack and actual fold/DeX resizing.

### User-requested APK: v0.4.0

Built with `scripts/build-jvy-apk.ps1 -Variant demo -Bump minor`: versionCode 224, 14,860,817 bytes. This optimized demo is **not debuggable**, with R8 and resource shrinking enabled; it uses the existing development signing key, not a private production key. The application ID and signing certificate match v0.3.0 for in-place updates.

Post-build checks passed: APK v2 signature, ZIP integrity, alignment, manifest version/identity, `debuggable=false`, increasing versionCode and checksum-manifest agreement. SHA-256: `9e6becb56e61bdd92c2c38471b2f62d39062a4728d04586c3831c25f4a09f8b2`. Device installation and the hands-on checks above remain unverified in this cloud environment.
