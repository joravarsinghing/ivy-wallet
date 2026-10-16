# Jvy Wallet

Jvy Wallet is a calm, beginner-friendly Android money manager for understanding spending, planning ahead, and steadily growing net worth.

The project is actively maintained by [Joravar Singh](https://github.com/joravarsinghing). It was adopted from the open-source Ivy Wallet codebase, and preserving compatibility with existing Ivy Wallet imports remains a hard product constraint.

[![Latest release](https://img.shields.io/github/v/release/joravarsinghing/ivy-wallet?label=release)](https://github.com/joravarsinghing/ivy-wallet/releases)
[![Build APK](https://github.com/joravarsinghing/ivy-wallet/actions/workflows/apk.yml/badge.svg)](https://github.com/joravarsinghing/ivy-wallet/actions)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/joravarsinghing/ivy-wallet?style=social)](https://github.com/joravarsinghing/ivy-wallet)

## What Jvy Wallet does

- Track income, expenses, transfers, accounts, and balances.
- Understand spending with categories, reports, budgets, and planned payments.
- Create savings goals for purchases, emergency funds, account balances, or any personal target.
- Reserve money for goals so it is visibly separated from Safe to Spend.
- Import existing Ivy Wallet data without losing the compatibility that makes the fork useful.
- Build toward a clearer picture of cash flow, financial safety, and net worth.

## Current focus

The current development focus is making goals and reserved money feel reliable and easy to understand. A goal can have a target amount, deadline, and funding assigned from one or more accounts. Reserved money is shown separately from spendable money, while the underlying account balances remain accurate.

## Future scope

The roadmap is intentionally staged so the app becomes more helpful without becoming confusing:

1. Smarter goal funding plans, progress forecasts, and on-track explanations.
2. Account- and category-scoped saving pools with clearer allocation history.
3. Net-worth tracking with historical snapshots and an explainable change breakdown.
4. Monthly money check-ins, emergency-fund coverage, and contextual financial education.
5. Debt payoff planning and investment tracking after the core ledger and calculations are trustworthy.

## Getting started

### Requirements

- Android Studio with the bundled JDK 17.
- An Android device or emulator running a supported Android version.

### Build a numbered APK

From the repository root:

```powershell
.\scripts\build-jvy-apk.ps1
```

This is the official APK workflow. It generates a successive artifact such as `Jvy_wallet_v0.2.3.apk` under `artifacts/apk/`, records the Android `versionCode`, and writes a checksum manifest beside it. Use `-Bump minor` for a feature release or `-Bump major` for an intentional compatibility-breaking release.

Raw Gradle commands remain available for compilation and tests, but they do not allocate APK release numbers. See the [APK generation standard](docs/standards/APK-GENERATION.md) and [agent instructions](AGENTS.md).

### Run tests

```powershell
.\gradlew.bat test
```

Before changing import, balance, money, or reservation behavior, run the relevant compatibility and financial-calculation tests as well as the normal regression suite.

## Documentation

- [Roadmap status](ROADMAP_STATUS.md)
- [Developer guidelines](docs/guidelines/README.md)
- [Goal and saving-pool plan](docs/plans/001-goals-and-saving-pools.md)
- [APK generation standard](docs/standards/APK-GENERATION.md)
- [Agent instructions](AGENTS.md)
- [Contributing guide](CONTRIBUTING.md)
- [License](LICENSE)

## Contributing

Issues and pull requests are welcome in this repository. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and the [developer guidelines](docs/guidelines/README.md) before starting work.

Keep changes small, testable, and compatible with existing Ivy Wallet imports. For larger changes, document the decision and the user-facing behavior before implementation.

## Contact

- [LinkedIn](https://www.linkedin.com/in/joravarsingh/)
- [GitHub](https://github.com/joravarsinghing)
- [Website / portfolio](https://joravarsinghing.github.io/portfolio/)

## License and attribution

Jvy Wallet is distributed under the [GNU General Public License v3.0](LICENSE). The project is an independent continuation and should be treated as Jvy Wallet in its user-facing branding, documentation, and releases.
