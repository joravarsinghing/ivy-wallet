# Joravar Agentic Roadmap Status

Last updated: 2026-10-13 (Asia/Dubai)

## Current gate

**Wave 0 — Reconnaissance: COMPLETE / GATE BLOCKED ON TEST HARNESS**

The local Android build baseline is now available. Product implementation remains unauthorized until the Hilt unit-test aggregation stall is explained or explicitly accepted as a documented Gate 0 exception.

## Workspace

- Working copy: `E:\AI\joravar-ivy-wallet`
- Upstream remote: `https://github.com/Ivy-Apps/ivy-wallet.git`
- Initial branch: `main` tracking `upstream/main`
- Fork control branch: `joravar/wave-0-control-plane`
- Integration base before control-plane docs: `710733dbe4729a437d573cdfb8429c50e617ded1`
- Control-plane documentation commit: `04ec41256cffce3db97c34f1ed24e0c1c5e62e5c`
- Upstream checkout is not modified by this workspace.
- The project mirror's `sources/` remains read-only; the user supplied the artifacts at `E:\` and they are copied into `docs/source-of-truth/` with verified matching hashes.
- Isolated baseline toolchains: Java 17 at `E:\AI\toolchains\jdk17`; Android SDK API 34/build-tools 34.0.0 at `E:\Android`; Gradle cache at `E:\AI\gradle-cache`.

## Source-of-truth status

| Artifact | Status | Action |
|---|---|---|
| `JORAVAR_IVY_WALLET_PRD.md` | Present at `docs/source-of-truth/` | SHA-256 matches `E:\JORAVAR_IVY_WALLET_PRD.md`. |
| `JORAVAR_AGENTIC_ROADMAP.md` | Present at `docs/source-of-truth/` | SHA-256 matches `E:\JORAVAR_AGENTIC_ROADMAP.md`. |
| Ivy Wallet upstream repository | Present | Pinned to commit recorded in the Wave 0 report. |

## Reconnaissance workstreams

| ID | Scope | Owner | Write scope | Status |
|---|---|---|---|---|
| R0-BUILD | Gradle, modules, dependency topology | Ramanujan | None; report only | Complete |
| R0-IMPORT | Ivy CSV/backup import and compatibility invariants | Dirac | None; report only | Complete |
| R0-DATA | Data, domain, Room, money model | Halley | None; report only | Complete |
| R0-VERIFY | Tests, CI, tooling, baseline commands | Fermat | None; report only | Complete; environment blocked |
| R0-UI | Navigation and product/UI flows | Hume | None; report only | Complete |

Recon agents must not edit production files, shared contracts, or control-plane files. The orchestrator owns consolidation.

## Non-negotiable constraints

1. Ivy Wallet import compatibility is a release gate, not a best-effort feature.
2. Never edit, rename, move, or delete the original upstream checkout.
3. No Wave 1 implementation before baseline build/tests and source-artifact recovery status are recorded.
4. Parallel work uses disjoint write scopes and frozen contracts; architectural changes require an ADR before implementation.
5. Financial calculations must preserve exactness and explainability; compatibility adapters must isolate legacy representations.

## Exit criteria for Wave 0

- [x] All five reconnaissance reports received and consolidated.
- [x] Repository status, branches, remotes, commit, module graph, and build system recorded.
- [x] Ivy import paths, schemas, fixtures, and invariants recorded with exact file references.
- [x] Baseline build and test commands executed or blocked with evidence.
- [x] Source PRD and roadmap copied into a non-synced project-controlled location with matching hashes.
- [x] Branch/worktree conventions and initial ADRs recorded.
- [x] Wave 1 packets remain unstarted until this gate is satisfied.

## Gate failures / blockers

- `testDebugUnitTest` reaches `:feature:accounts:hiltAggregateDepsDebugUnitTest` and then remains silent for several minutes; a second run reproduces the stall. No source changes were made to work around it.
- The focused compatibility run reaches `:feature:import-data:hiltAggregateDepsDebugUnitTest` and reproduces the same stall, so Ivy import tests have not completed.
- The first unfiltered `assembleDemo` run completed local build/shrink/package work but failed at the external `:app:uploadCrashlyticsMappingFileDemo` task because DNS could not resolve `firebasecrashlyticssymbols.googleapis.com`.
- No emulator/device baseline was established, so connected instrumentation remains unrun.

## Reconnaissance records

- `docs/recon/R0-BUILD-TOPOLOGY.md`
- `docs/recon/R0-IMPORT-COMPATIBILITY.md`
- `docs/recon/R0-DATA-FINANCIAL.md`
- `docs/recon/R0-UI-FLOWS.md`
- `docs/recon/R0-VERIFICATION.md`
- `docs/adr/0001` through `docs/adr/0011`

## Next orchestrator actions

1. Diagnose the Hilt unit-test aggregation stall, beginning with Gradle/Hilt worker diagnostics rather than application edits.
2. Establish an emulator/device baseline for connected data/domain tests.
3. Freeze ADR 0001–0010 at Gate 0 after reviewing package, money, time, DB, balance, module, backup, net-worth, and explainability decisions.
4. Prepare Wave 1 packets only after the baseline gates pass or the user explicitly accepts a documented exception.
