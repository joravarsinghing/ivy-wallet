# Jvy Wallet APK generation standard

This is the release standard for every numbered Jvy Wallet APK.

## Signing safety gate

Public distribution requires `-Variant release` and a privately held keystore. Set `SIGNING_STORE_FILE` (defaults to `sign.jks`), `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD` through secure environment values. Never commit the private keystore or passwords. Raw Gradle release signing is also guarded against the repository's known public test certificate.

For cloud builds, `JVY_SIGNING_STORE_BASE64` may transport the owner's keystore through secure direct environment values. Base64 is encoding, not encryption. Before calling the official script, decode it into a uniquely created temporary directory outside the checkout (directory mode `0700`, file mode `0600`), and set `SIGNING_STORE_FILE` for the build process only. Verify the alias, passwords and certificate without printing credentials; never pass passwords in command-line arguments. Remove only that temporary copy in a `finally` cleanup, leaving the owner's original file and configured secure values intact. The numbered script does not itself decode Base64. A Windows path is not accessible from the cloud machine.

Legacy-compatible demo updates are non-debuggable but **not privately signed**. They now require explicit `-AllowPublicTestKey` acknowledgement; do not publish them as production releases. The numbered script no longer accepts debug APK builds. Changing signing key or application ID cannot be done silently: existing demo installations need an owner-approved signing/data migration strategy before moving to private releases.

Inherited automatic APK, release/tagging and Telegram publishing workflows have been removed. Unit, lint and screenshot checks remain. The numbered script is the only authorized APK distribution path; do not reinstate raw `assembleDemo` uploads or version allocation from `libs.versions.toml`.

## Version format

Every distributed APK uses semantic versioning:

```text
Jvy_wallet_v<major>.<minor>.<patch>.apk
```

Examples:

- `Jvy_wallet_v0.2.1.apk`
- `Jvy_wallet_v0.3.0.apk`
- `Jvy_wallet_v1.0.0.apk`

The rules are:

- **Patch**: bug fixes, visual fixes, copy changes, and small safe improvements.
- **Minor**: backward-compatible user-facing features.
- **Major**: incompatible data, import, or product changes requiring explicit migration.
- Android `versionCode` increases by exactly one for every successful official APK build, regardless of semantic bump type.
- A failed build does not consume a version number.
- Version numbers are never reused after an APK has been successfully generated.

The authoritative state is tracked in [`version.properties`](../../version.properties), not a hard-coded version in this document. The next official build increments the selected semantic version component and Android version code.

## Official build command

From the repository root, use:

```powershell
.\scripts\build-jvy-apk.ps1 -Variant release
```

On Linux/macOS with PowerShell 7, run the same workflow:

```sh
pwsh -NoProfile -File ./scripts/build-jvy-apk.ps1 -Variant release -Bump minor
```

The script selects `gradlew.bat` on Windows and `gradlew` elsewhere. It locks version allocation, refuses artifact collisions, and only persists the new baseline after a nonempty APK and checksum manifest exist. `scripts/test-build-jvy-apk.ps1` exercises these contracts with an isolated fake Gradle launcher (no installable APKs).

Numbered APK builds skip Crashlytics mapping uploads for both supported variants. Building an APK does not authorize publishing to Firebase, an app store or a release service.

The script:

1. Reads the last successful version.
2. Allocates the next patch version and numeric code.
3. Builds the explicitly selected, signing-checked APK with those values.
4. Verifies that Gradle produced a nonempty APK.
5. Copies the numbered APK to `artifacts/apk/` and writes its SHA-256 JSON manifest.
6. Persists the new version only after both deliverables exist.

For planned feature or major releases:

```powershell
.\scripts\build-jvy-apk.ps1 -Variant release -Bump minor
.\scripts\build-jvy-apk.ps1 -Variant release -Bump major
```

`demo` preserves older installations, but is no longer silently distributable. For a knowingly accepted legacy test update only:

```powershell
.\scripts\build-jvy-apk.ps1 -Variant demo -AllowPublicTestKey
```

The demo **build type is not debuggable**: R8 minification and resource shrinking are enabled. Its signing key and `com.ivy.wallet.debug` application ID deliberately match older demo installations, allowing in-place updates. This is not a private production-key-signed release and does not prevent Play Protect prompts.

A release-signed artifact is explicit:

```powershell
.\scripts\build-jvy-apk.ps1 -Variant release
```

## Agent and commit rules

- Agents may use raw Gradle tasks for compilation and tests; those tasks do not advance APK numbering.
- Agents must use the official script for any APK intended for installation, sharing, QA, or release.
- After a successful numbered build, commit `version.properties` together with the source changes and report the APK filename, version code, checksum, and build variant.
- APK binaries and generated manifests stay in the ignored `artifacts/apk/` directory; do not commit binaries to Git.
- Never manually edit `versionCode` or reuse an older version name without recording an explicit release decision.
- The APK must remain Ivy Wallet import-compatible and retain the Jvy Wallet application identity.
