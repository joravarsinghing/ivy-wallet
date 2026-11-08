# Branch and Worktree Conventions

These conventions are established for the Joravar fork during Wave 0. They are intentionally conservative until the source roadmap is restored.

## Repository roles

- `upstream`: read-only reference remote for Ivy Wallet.
- `main`: local mirror of the upstream baseline; do not develop directly on it.
- `joravar/wave-0-control-plane`: orchestrator-owned branch for status, ADRs, and reconnaissance records.
- `joravar/<wave>/<packet>-<short-name>`: implementation branch for one roadmap packet.

## Worktree rules

- The primary working copy is `E:\AI\joravar-ivy-wallet`.
- Each implementation agent receives a separate worktree created from the orchestrator branch or the explicitly named integration base.
- Never reuse the upstream checkout for feature work.
- Never reset, force-checkout, or clean another agent's worktree.
- A worktree may modify only the files listed in its task packet. New shared contracts require an ADR and orchestrator approval before dependent work starts.

## Ownership rules

- The orchestrator is the single writer for `ROADMAP_STATUS.md`, ADR indexes, agent packets, and integration decisions.
- Import compatibility has a single owner in Wave 1. No other agent may alter importer schemas, mapping rules, or compatibility fixtures without an explicit handoff.
- Financial core contracts have a single owner in Wave 1. UI agents consume frozen interfaces rather than changing them opportunistically.
- Verification owns golden fixtures and release-gate tests once the baseline contract is approved.

## Integration rules

1. Every agent starts from a named base commit and records it in its handoff.
2. Every change has a narrow file scope, tests, and a handoff note.
3. Merge order follows the dependency DAG, not completion time.
4. Conflicts in protected files are resolved by the orchestrator or designated single writer.
5. A failing Ivy import compatibility test blocks integration regardless of feature value.

## Naming

- ADRs: `docs/adr/NNNN-<decision>.md`
- Recon reports: `docs/recon/R0-<scope>.md`
- Agent packets: `docs/agentic/packets/W<wave>-<packet>-<short-name>.md`
- Handoffs: `AGENT_HANDOFF.md` in the agent worktree, never in the shared control branch unless explicitly consolidated.
