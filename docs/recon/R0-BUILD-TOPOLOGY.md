# R0-BUILD — Repository and build topology

Status: complete, read-only reconnaissance

## Findings

- Gradle wrapper 8.8; AGP 8.5.2; Kotlin 2.0.20; KSP 2.0.20-1.0.24; JVM target 17.
- Android min SDK 28; compile/target SDK 34; app version `2025.07.17` / version code 206.
- 45 declared modules across `app`, `feature`, `shared`, `temp`, `widget`, and `ci-actions`.
- Convention plugins are in `buildSrc`; global blast-radius files include `settings.gradle.kts`, the version catalog, root Gradle scripts, and `buildSrc`.
- `app` is the integration hotspot. Navigation changes generally pair `shared/ui/navigation`, a feature module, and `app/IvyNavGraph.kt`.
- `temp:legacy-code` and `temp:old-design` are still consumed by multiple features/widgets and are compatibility boundaries, not cleanup targets.
- `:shared:common-ui` is included by settings but no corresponding directory/build script was found; configuration must confirm whether this is a real blocker.

## Ownership boundaries

Feature agents stay within one feature module and its tests/snapshots. Shared model/domain/data, UI infrastructure, navigation, build logic, CI, and legacy migration each require dedicated owners.

## References

`settings.gradle.kts`, `gradle/libs.versions.toml`, `buildSrc/src/main/kotlin/**`, `app/build.gradle.kts`, `all_modules.gv` (historical only).
