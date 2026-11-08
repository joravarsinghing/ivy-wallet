# R0-VERIFY — Tests, CI, and baseline verification

Status: complete; local build baseline available; unit-test gate blocked by repeatable Hilt aggregation stall

## Repository verification surface

- 73 JVM test source files, 4 instrumentation test files, 13 Paparazzi test classes, and 52 committed screenshot baselines were observed.
- Room schema fixtures cover versions 101–130. Backup fixtures exist as JSON and ZIP under `shared/data/core` test resources/assets.
- CI-prescribed gates include `testDebugUnitTest`, `lintRelease`, `detekt`, `verifyPaparazziDebug`, `assembleDemo`, Compose stability, CI-action tests, and shared data/domain connected instrumentation tests.
- Instrumentation coverage is concentrated in `shared:data:core` and `shared:domain`; no app-level end-to-end smoke gate was found.

## Commands run

| Command | Result | Evidence |
|---|---|---|
| `gradlew tasks --offline` | Pass | Gradle 8.8 configured successfully; task graph listed. |
| `gradlew :ci-actions:base:test` | Pass | Java 17 isolated toolchain; `BUILD SUCCESSFUL` after online dependency resolution. |
| `gradlew testDebugUnitTest --offline` | Blocked at unit-test setup | After SDK/JDK remediation, the task reaches `:feature:accounts:hiltAggregateDepsDebugUnitTest` and remains silent for several minutes. A second run reproduced the stall; no test assertions completed. |
| `gradlew :feature:import-data:testDebugUnitTest --offline` | Blocked at unit-test setup | Reaches `:feature:import-data:hiltAggregateDepsDebugUnitTest` and reproduces the same silent stall; compatibility tests did not complete. |
| `gradlew assembleDemo --offline` | Pass with external upload excluded | `-x :app:uploadCrashlyticsMappingFileDemo`; `BUILD SUCCESSFUL` after R8/resource shrinking/package. APK: `app/build/outputs/apk/demo/app-demo.apk` (13,294,464 bytes). |
| `gradlew assembleDemo --offline` | Blocked by external service | Local build work completed, then Crashlytics symbol upload failed with `UnknownHostException` for `firebasecrashlyticssymbols.googleapis.com`. |

## Environment blockers

- Android API 34 and build-tools 34.0.0 are now installed under `E:\Android`.
- Java 17 is now available at `E:\AI\toolchains\jdk17`; Gradle’s JVM CI-action test passes with it.
- SDK command-line tooling previously emitted an XML version 4 warning; it is retained as a toolchain hygiene note, but it no longer blocks the local Android build.
- The Hilt aggregation stall is unresolved and is currently the primary test-gate blocker.
- Crashlytics symbol upload requires network/DNS access and is excluded from the local APK verification command.
- No emulator/device baseline was established; instrumentation is not attempted.

## Release-gate candidates

Protect Ivy CSV/backup round-trip, import idempotency, transfer pairing, account/category reconciliation, exact amount precision, Room migrations, balance integrity, deterministic calculation explanations, lint/detekt, Paparazzi, and connected data/domain tests.
