# ADR 0007: Feature module dependency rules

- Status: Proposed; freeze at Gate 0
- Date: 2026-10-13

Features own presentation and event/ViewModel behavior; `shared:domain` owns reusable financial calculations; `shared:data:core` owns persistence/import/backup; `shared:data:model` owns canonical contracts. Features should not calculate financial truth or bypass repositories with direct DAO access. `app`, navigation, build logic, and shared UI are single-writer hotspots.
