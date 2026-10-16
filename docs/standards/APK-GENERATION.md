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

The authoritative state is tracked in [`version.properties`](../../version.properties). The current release is `0.2.10` / version code `216`; the next patch APK is `Jvy_wallet_v0.2.11.apk` / version code `217`.

## Official build command

From the repository root, use:

```powershell
.\scripts\build-jvy-apk.ps1
```

The script:

1. Reads the last successful version.
2. Allocates the next patch version and numeric code.
3. Builds the demo APK with those values.
4. Persists the version only after Gradle succeeds.
5. Copies the numbered APK to `artifacts/apk/`.
6. Writes a matching JSON manifest containing the SHA-256 checksum.

For planned feature or major releases:

```powershell
.\scripts\build-jvy-apk.ps1 -Bump minor
.\scripts\build-jvy-apk.ps1 -Bump major
```

`demo` is the default distributable build because it is signed with the repository's debug key. A release-signed artifact is explicit:

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
