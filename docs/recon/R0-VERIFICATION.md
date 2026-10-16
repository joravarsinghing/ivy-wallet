# R0-VERIFY — Tests, CI, and baseline verification

Status: complete; baseline execution blocked by local environment

## Repository verification surface

- 73 JVM test source files, 4 instrumentation test files, 13 Paparazzi test classes, and 52 committed screenshot baselines were observed.
- Room schema fixtures cover versions 101–130. Backup fixtures exist as JSON and ZIP under `shared/data/core` test resources/assets.
- CI-prescribed gates include `testDebugUnitTest`, `lintRelease`, `detekt`, `verifyPaparazziDebug`, `assembleDemo`, Compose stability, CI-action tests, and shared data/domain connected instrumentation tests.
- Instrumentation coverage is concentrated in `shared:data:core` and `shared:domain`; no app-level end-to-end smoke gate was found.

## Commands run

| Command | Result | Evidence |
|---|---|---|
| `gradlew tasks --offline` | Pass | Gradle 8.8 configured successfully; task graph listed. |
| `gradlew testDebugUnitTest --offline` | Blocked before tests | `Failed to find target with hash string 'android-34' in: E:\\Android`; SDK XML v4 warning. |
| `gradlew assembleDemo --offline` | Blocked before compilation/package | Same missing `android-34` target; failure surfaced at `:app:minifyDemoWithR8`. |
| `gradlew :ci-actions:base:test --offline` | Blocked before tests | No locally installed Java toolchain matching exact language version 17. |

## Environment blockers

- Android SDK root `E:\Android` has newer platforms but no API 34 platform.
- Android Studio JBR resolves to OpenJDK 21; another installed JDK is 23. Gradle’s JVM toolchain lookup requires Java 17 for the JVM CI module and finds none.
- The SDK emits an XML version 4 warning against the current command-line tooling.
- No emulator/device baseline was established; instrumentation is not attempted.

## Release-gate candidates

Protect Ivy CSV/backup round-trip, import idempotency, transfer pairing, account/category reconciliation, exact amount precision, Room migrations, balance integrity, deterministic calculation explanations, lint/detekt, Paparazzi, and connected data/domain tests.
