# Joravar Agentic Roadmap Status

Last updated: 2026-10-13 (Asia/Dubai)

## Current gate

**Wave 0 — Reconnaissance: COMPLETE / GATE BLOCKED**

No product implementation is authorized until the environment baseline is remediated and the Gate 0 decisions are frozen.

## Workspace

- Working copy: `E:\AI\joravar-ivy-wallet`
- Upstream remote: `https://github.com/Ivy-Apps/ivy-wallet.git`
- Initial branch: `main` tracking `upstream/main`
- Fork control branch: `joravar/wave-0-control-plane`
- Integration base before control-plane docs: `710733dbe4729a437d573cdfb8429c50e617ded1`
- Upstream checkout is not modified by this workspace.
- The project mirror's `sources/` remains read-only; the user supplied the artifacts at `E:\` and they are copied into `docs/source-of-truth/` with verified matching hashes.

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

- [ ] All five reconnaissance reports received and consolidated.
- [ ] Repository status, branches, remotes, commit, module graph, and build system recorded.
- [ ] Ivy import paths, schemas, fixtures, and invariants recorded with exact file references.
- [ ] Baseline build and test commands executed or blocked with evidence.
- [x] Source PRD and roadmap copied into a non-synced project-controlled location with matching hashes.
- [x] Branch/worktree conventions and initial ADRs recorded.
- [x] Wave 1 packets remain unstarted until this gate is satisfied.

## Gate failures / blockers

- Android API 34 is missing from the detected `E:\Android` SDK; both `testDebugUnitTest` and `assembleDemo` stop before execution.
- No Java 17 installation is available for the JVM CI-action test toolchain; `:ci-actions:base:test` stops before tests.
- SDK command-line tooling reports an SDK XML v4 versus supported up-to-3 mismatch.
- No emulator/device baseline was established, so connected instrumentation remains unrun.

## Reconnaissance records

- `docs/recon/R0-BUILD-TOPOLOGY.md`
- `docs/recon/R0-IMPORT-COMPATIBILITY.md`
- `docs/recon/R0-DATA-FINANCIAL.md`
- `docs/recon/R0-UI-FLOWS.md`
- `docs/recon/R0-VERIFICATION.md`
- `docs/adr/0001` through `docs/adr/0011`

## Next orchestrator actions

1. Install/point the workspace at Android API 34 and Java 17, then rerun the baseline gates.
2. Resolve or explicitly accept the SDK command-line tooling mismatch.
3. Freeze ADR 0001–0010 at Gate 0 after reviewing package, money, time, DB, balance, module, backup, net-worth, and explainability decisions.
4. Prepare Wave 1 packets only after the baseline gates pass or the user explicitly accepts a documented exception.
