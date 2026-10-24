# Project-local Android and UI/UX skills

Both requested repositories are installed as ordinary Codex project skills under `skills/`. These are real, self-contained copies, not temporary-directory symlinks. The app source, Gradle dependencies, APK, signing configuration and version are unchanged by this installation.

## Installed collections

- **Android Skills:** 33 official modules, including adaptive layouts/foldables/desktop, edge-to-edge, Compose performance, testing setup, permissions/intent security, ANR auditing and R8 analysis. All upstream skill reference directories are included; Apache-2.0 license terms are present in each module.
- **UI/UX Pro Max:** `ui-ux-pro-max`, `design`, `design-system`, `ui-styling`, `brand`, `banner-design` and `slides`. Includes the local Python search/design-system tools, datasets, Jetpack Compose guidance, companion resources and MIT license.

Exact source revisions and installation methods are recorded in [`sources.json`](sources.json). UI/UX Pro Max was generated using the reviewed installer source at that revision, not an unverified newer npm release. Its source CLI version was not available on npm at installation time; no global package was installed.

## Discovery and usage

Open this project in a new Codex session to refresh project skill discovery. These files do not register account-wide cloud plugins or change the cloud `skills.list` catalog in the current session. If automatic discovery is unavailable, read the relevant `skills/<name>/SKILL.md` directly and follow its required references.

Before using any module, read its full instructions. Apply only guidance relevant to the user's request. Some official Android modules require newer libraries or architectural migrations; their installation alone is not permission to upgrade Jvy or replace its navigation. Existing project compatibility, financial correctness, privacy and APK-generation rules remain in force.

The UI/UX search runs locally with Python 3 and the standard library. From the project root:

```sh
PYTHONDONTWRITEBYTECODE=1 python3 .agents/skills/ui-ux-pro-max/scripts/search.py "adaptive accessibility navigation" --stack jetpack-compose -n 2
PYTHONDONTWRITEBYTECODE=1 python3 .agents/skills/ui-ux-pro-max/scripts/validate_data.py
```

## Installation checks

- All 40 `SKILL.md` files have valid YAML frontmatter, unique names, nonempty descriptions and matching directory names.
- No literal `references/`, `scripts/` or `assets/` links in the skill entrypoints are missing.
- UI/UX command help, a Jetpack Compose search and data validation run successfully. Data validation covers 12 domain files, 22 stack files and the reasoning dataset.
- The installed runtime test suite passed 137 tests. Four additional upstream test modules require the original source repository's development layout, rather than an installed skill folder; running full test discovery inside this project reports those four import errors. Keep those upstream tests unmodified and run them in the pinned upstream checkout instead.
- The complete suite in the pinned upstream checkout passed all 164 tests, including those source-repository-only checks.

Installation does not verify every future Android workflow: device tools, SDK/library versions and permissions must be checked when a particular module is used. No app build or APK is needed to add these skills.

For updates, review a newer upstream revision and generate a clean staging copy before replacing files. Preserve licenses and local changes; update `sources.json` and repeat validation.
