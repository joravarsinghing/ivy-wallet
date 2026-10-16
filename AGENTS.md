# Jvy Wallet agent instructions

## APK versioning is mandatory

For any APK intended for installation, sharing, QA, or release, use the official numbered build workflow:

```powershell
.\scripts\build-jvy-apk.ps1
```

Do not distribute the raw `app/build/outputs/apk/` filename. The deliverable is named `Jvy_wallet_v<major>.<minor>.<patch>.apk`, with a strictly increasing Android `versionCode`.

The authoritative version is `version.properties`. The script increments the patch by default and persists the new version only after a successful build. Use `-Bump minor` for a backward-compatible feature release and `-Bump major` only for an intentional compatibility-breaking release.

Raw Gradle test, lint, compile, and verification tasks do not consume APK numbers. After an official build, commit `version.properties` with the related source changes, report the numbered artifact and SHA-256 checksum, and preserve Ivy Wallet import compatibility.

See [`docs/standards/APK-GENERATION.md`](docs/standards/APK-GENERATION.md) for the full standard.
