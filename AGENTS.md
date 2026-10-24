# Jvy Wallet agent instructions

## Installed project skills

Project-local Codex skills live in [`.agents/skills/`](.agents/skills/). Both requested upstream collections are installed: Google's Android Skills (33 modules) and UI/UX Pro Max (the main skill plus six bundled companion skills). See [`.agents/README.md`](.agents/README.md) for provenance and validation.

Use the relevant Android skills for Android work and UI/UX Pro Max for interface work. Select only the modules that match the task, and read each selected `SKILL.md` and its required references before applying it. Project skills may need a fresh Codex session to appear in discovery; they are not account-wide cloud plugins.

Installing a skill does not authorize app changes, dependency upgrades or migrations. Preserve this project's financial calculations, backup/import compatibility, offline privacy, app identity and numbered APK workflow. If upstream guidance needs newer APIs or a different architecture, assess compatibility and obtain the required scope before migrating.

## UI design language is mandatory

Before any UI work, read [`docs/design/JVY-DESIGN-LANGUAGE.md`](docs/design/JVY-DESIGN-LANGUAGE.md) and the relevant [`Mobile UI Standards`](docs/guidelines/Mobile-UI-Standards.md). Use the shared Jvy typography, shape, theme, logo and button components; retain Ivy's bundled Raleway/Open Sans families. Do not add a feature-specific font, duplicated global navigation, baked-in logo tile or another account net-worth summary.

Every new or modified feature must follow this design language, including plain-language financial explanations, hidden-balance behavior, large-text/phone/foldable/DeX layouts and protected credential flows. Update the standard and token tests when intentionally changing shared tokens. Render candidate previews and request visual approval before updating screenshot goldens. UI work must preserve financial calculations, stored data, backups, app identity and signing.

## APK versioning is mandatory

For any APK intended for installation, sharing, QA, or release, use the official numbered build workflow:

```powershell
.\scripts\build-jvy-apk.ps1 -Variant release
```

On Linux/macOS use the same script with PowerShell 7: `pwsh -NoProfile -File ./scripts/build-jvy-apk.ps1 -Variant release`. Private releases require the owner's signing credentials. Legacy-compatible demo updates require explicit acknowledgement with `-Variant demo -AllowPublicTestKey`; never silently choose the repository's public test key for public distribution. Debug APKs are not accepted by the numbered workflow. Do not change key/package identity without an approved migration plan. The workflow selects the platform's Gradle wrapper automatically.

When `JVY_SIGNING_STORE_BASE64` is supplied securely for cloud builds, materialize it only in a uniquely created private temporary directory outside the checkout, set `SIGNING_STORE_FILE` for the build process, and remove that temporary copy in `finally`. Never log the encoded key/passwords, put them in Git, pass passwords as command-line arguments, or substitute a disposable/test key. See the signing safety gate in the APK standard.

Do not distribute the raw `app/build/outputs/apk/` filename. The deliverable is named `Jvy_wallet_v<major>.<minor>.<patch>.apk`, with a strictly increasing Android `versionCode`.

The authoritative version is `version.properties`. The script increments the patch by default and persists the new version only after a successful build. Use `-Bump minor` for a backward-compatible feature release and `-Bump major` only for an intentional compatibility-breaking release.

Raw Gradle test, lint, compile, and verification tasks do not consume APK numbers. After an official build, commit `version.properties` with the related source changes, report the numbered artifact and SHA-256 checksum, and preserve Ivy Wallet import compatibility.

See [`docs/standards/APK-GENERATION.md`](docs/standards/APK-GENERATION.md) for the full standard.

## GitHub ownership and contribution identity

This fork is owned and maintained by the personal GitHub account [`joravarsinghing`](https://github.com/joravarsinghing).

- All new commits, tags, releases, and branch updates for Jvy Wallet must use the personal identity `Joravar Singh <joravarsinghing@users.noreply.github.com>`.
- Do not use company GitHub accounts, company email addresses, or company commit-signing identities for Jvy Wallet development.
- Before committing, verify the repository-local identity with `git config --local user.name` and `git config --local user.email`.
- Do not rewrite inherited Ivy Wallet contributor history; this rule applies to new Jvy-owned work only.
