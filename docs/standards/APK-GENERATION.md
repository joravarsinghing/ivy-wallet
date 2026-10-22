# Jvy Wallet APK generation standard

This is the release standard for every numbered Jvy Wallet APK.

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
.\scripts\build-jvy-apk.ps1
```

On Linux/macOS with PowerShell 7, run the same workflow:

```sh
pwsh -NoProfile -File ./scripts/build-jvy-apk.ps1 -Variant demo -Bump minor
```

The script selects `gradlew.bat` on Windows and `gradlew` elsewhere. It locks version allocation, refuses artifact collisions, and only persists the new baseline after a nonempty APK and checksum manifest exist. `scripts/test-build-jvy-apk.ps1` exercises these contracts with an isolated fake Gradle launcher (no installable APKs).

The script:

1. Reads the last successful version.
2. Allocates the next patch version and numeric code.
3. Builds the demo APK with those values.
4. Verifies that Gradle produced a nonempty APK.
5. Copies the numbered APK to `artifacts/apk/` and writes its SHA-256 JSON manifest.
6. Persists the new version only after both deliverables exist.

For planned feature or major releases:

```powershell
.\scripts\build-jvy-apk.ps1 -Bump minor
.\scripts\build-jvy-apk.ps1 -Bump major
```

`demo` is the default distributable build because it is signed with the repository's debug key.

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
