# Specification 75: Spec Archive, Living Wiki & Two-Stage Knowledge Evolution Loop

**Status:** Implemented (Final)

## 1. Overview & Problem Statement

The `specs/` directory has grown to 75 implemented numbered specifications (`01`..`74`, including three `21_*` files), `specs/implementation_plan.md`, and a 1,053-line `specs/steps.md` (~91 KB). Because all specifications sit in a single active folder alongside historical architectural choices (such as Koin DI before Spec 19, Presenters before Spec 42, and the monolithic `:shared` module before Specs 46–49), humans and AI agents searching `specs/` encounter superseded designs mixed with current system behavior.

This specification introduces a clean separation between:
1. **Living System & Business Knowledge (`wiki/`)**: A single source of current truth covering modules, MVI/DI architecture, product and technical business rules, UI design systems, performance pipelines, ADRs, and an open issue log.
2. **Active Work Queue (`specs/`)**: Contains only active/draft specifications, exploratory research (`specs/researches/`), and a compact index (`specs/steps.md`).
3. **Immutable Historical Archive (`specs/archive/`)**: Stores finalized, point-in-time specifications (`Implemented (Final)`) and the historical `steps_01_to_74.md` log as read-only records.
4. **Two-Stage Knowledge Evolution Loop (`.agents/skills/session-issue-capture/SKILL.md`, `.agents/skills/issue-log-resolution/SKILL.md` + `wiki/issue-log.md`)**:
   * **Stage 1 (`session-issue-capture`)**: Record newly discovered systemic conflicts, documentation drift, skill gaps, and deferred technical debt into `wiki/issue-log.md` before ending a session or committing.
   * **Stage 2 (`issue-log-resolution`)**: Triggered **manually** by the developer (never via an automatic background schedule) to inspect `[OPEN]` items in `wiki/issue-log.md` and route fixes to `wiki/`, `.agents/skills/`, `AGENTS.md`, or a new spec in `specs/`.

---

## 2. Architecture & Lifecycle Design

### 2.1 Documentation Layer Boundaries

| Layer | Location | Purpose | Mutability |
| :--- | :--- | :--- | :--- |
| **Root Invariants** | [`AGENTS.md`](../../AGENTS.md) | Non-negotiable policies & index (< 75 lines) | Rarely changed |
| **Procedural Runbooks** | [`.agents/skills/`](../../.agents/skills) | Step-by-step build, test, styling, and issue workflows | Evolves via Issue Log Resolution |
| **Living Wiki & Business Rules** | [`wiki/README.md`](../../wiki/README.md) | Current architecture, business rules, design systems, performance, ADRs | Updated on each completed spec & Issue Log Resolution |
| **Session Issue Queue** | [`wiki/issue-log.md`](../../wiki/issue-log.md) | Captured conflicts, doc drift, skill gaps, & deferred code issues | Appended post-session; triaged manually |
| **Active Work Queue** | [`specs/README.md`](../README.md) | Draft or in-progress specs + compact [`steps.md`](../steps.md) index | Active until completion |
| **Immutable Archive** | [`specs/archive/`](.) | Finalized historical specs (`01`..`75`, `implementation_plan.md`, `steps_01_to_74.md`) | **Frozen / Read-only** |

### 2.2 Living Wiki Pages (`wiki/`)

1. [`wiki/README.md`](../../wiki/README.md): Wiki entrypoint, layer boundary contract, and page index.
2. [`wiki/business-rules.md`](../../wiki/business-rules.md): Unsplash API compliance and security rules, Retina CDN image sizing math, pagination and deduplication contracts, unified search debouncing/filtering, and typed `AppError` & localization invariants.
3. [`wiki/architecture-and-modules.md`](../../wiki/architecture-and-modules.md): Vertical feature modularization (`:core:*`, `:feature:*`, `:shared` umbrella), Metro DI graph & scoping, and Shared MVI / UDF lifecycle.
4. [`wiki/design-systems-and-navigation.md`](../../wiki/design-systems-and-navigation.md): Android Material 3 Expressive + Jetpack Navigation 3 adaptive scenes, and Apple iOS/macOS Liquid Glass design tokens + multi-target navigation.
5. [`wiki/performance-and-memory.md`](../../wiki/performance-and-memory.md): Image pipelines, BlurHash caching, Coil / `ImageMemoryCache` tuning, Compose/SwiftUI hot-path optimizations, static map previews, and Android Baseline Profiles.
6. [`wiki/adr-index.md`](../../wiki/adr-index.md): Concise Architecture Decision Record log linking to immutable archived specs.
7. [`wiki/issue-log.md`](../../wiki/issue-log.md): Structured queue for post-session capture and manual on-demand evaluation, seeded with initial audit findings (`ISS-001` through `ISS-003`).

### 2.3 Two-Stage Knowledge Evolution Workflow

1. **Stage 1 — Post-Session Capture** (documented in [`AGENTS.md`](../../AGENTS.md), [`verification-and-linting`](../../.agents/skills/verification-and-linting/SKILL.md), and [`session-issue-capture`](../../.agents/skills/session-issue-capture/SKILL.md)):
   * Only log systemic, recurring, or unresolved issues (`wiki-drift`, `skill-gap`, `code-inconsistency`, `tooling-friction`). Do not log transient typos fixed in the same turn.
2. **Stage 2 (`issue-log-resolution`)**:
   * Never scheduled automatically; invoked explicitly by the user.
   * Verifies `[OPEN]` items and recent spec `Unverified / Gaps` against living code, enforces single-owner rule placement (anti-context-poison), routes fixes to the right layer (`wiki/`, `.agents/skills/`, `AGENTS.md` < 75 lines, or `specs/`), and closes items as `[RESOLVED]` or `[DISMISSED]` with rationale.

---

## 3. Implementation Steps

1. Author this specification (`specs/75_spec_archive_wiki_and_knowledge_evolution.md`).
2. Create the living `wiki/` directory (`README.md`, `business-rules.md`, `architecture-and-modules.md`, `design-systems-and-navigation.md`, `performance-and-memory.md`, `adr-index.md`, `issue-log.md`) adhering strictly to the Living Code & Anti-Staleness Policy.
3. Create two focused, vendor-neutral skills: `.agents/skills/session-issue-capture/SKILL.md` (Stage 1 post-session capture) and `.agents/skills/issue-log-resolution/SKILL.md` (Stage 2 manual on-demand resolution).
4. Move all 76 existing specs (`01`..`74`, including three `21_*` files) and `implementation_plan.md` into `specs/archive/`, move the 1,053-line `specs/steps.md` to `specs/archive/steps_01_to_74.md`, and create `specs/README.md` + a compact index `specs/steps.md`.
5. Update `AGENTS.md` (keeping it strictly `< 75 lines`), `GEMINI.md`, `README.md`, `.agents/skills/kmp-architecture/SKILL.md`, and `.agents/skills/verification-and-linting/SKILL.md` to integrate `wiki/`, `specs/archive/`, `session-issue-capture`, and `issue-log-resolution`.
6. Mark this specification `**Status:** Implemented (Final)`, archive it to `specs/archive/75_spec_archive_wiki_and_knowledge_evolution.md`, and verify all markdown links and `AGENTS.md` line count.

---

## 4. Acceptance Criteria

- [x] `wiki/` directory exists with `README.md`, `business-rules.md`, `architecture-and-modules.md`, `design-systems-and-navigation.md`, `performance-and-memory.md`, `adr-index.md`, and `issue-log.md` (seeded with `ISS-001`..`ISS-003`), adhering to the Living Code & Anti-Staleness Policy.
- [x] `.agents/skills/session-issue-capture/SKILL.md` and `.agents/skills/issue-log-resolution/SKILL.md` separate post-session issue capture (`verification-gap` aware) from manual-only on-demand resolution (`[RESOLVED]` / `[DISMISSED]` with rationale and anti-context-poison checks) with no tool/vendor lock-ins.
- [x] All historical specs (`01`..`75`, including three `21_*` files), `implementation_plan.md`, and `steps_01_to_74.md` reside in `specs/archive/` and are treated as immutable (`Implemented (Final)`).
- [x] Root `specs/` contains only `README.md`, the compact index `specs/steps.md`, `researches/`, and `archive/`.
- [x] `AGENTS.md` stays strictly under 75 lines (`wc -l AGENTS.md < 75`), and `AGENTS.md`, `GEMINI.md`, `README.md`, `kmp-architecture/SKILL.md`, and `verification-and-linting/SKILL.md` reflect the new lifecycle and fix stale `core/mvi` and `specs/archive/` links.
- [x] Every markdown link across `AGENTS.md`, `GEMINI.md`, `README.md`, `wiki/*.md`, `specs/README.md`, `specs/steps.md`, `specs/archive/75_spec_archive_wiki_and_knowledge_evolution.md`, and the updated `.agents/skills/*/SKILL.md` files resolves to an existing file on disk.

---

## 5. Verification Record

* **Verified**:
  * `wc -l AGENTS.md GEMINI.md`: `AGENTS.md` = 64 lines (`< 75`), `GEMINI.md` = 7 lines.
  * Directory checks: `specs/` contains only `README.md`, `steps.md`, `researches/`, `archive/`; `specs/archive/` contains 79 files; `.agents/skills/` contains `session-issue-capture` and `issue-log-resolution`.
  * Portability grep (`BypassSandbox`, `antigravity`, `hooks.json`, `gemini`, `Studio Bot`, `/learn`, `/schedule`, `/automation`) across `AGENTS.md`, `wiki/`, `specs/README.md`, `specs/steps.md`, and `.agents/skills/`: 0 matches (`grep_exit=1`).
  * Python relative link checker across all 17 created/updated Markdown files: 0 broken links.
  * `git status --short`: confirmed only documentation, wiki, skill, and `specs/archive/` changes (zero `.kt` or `.swift` edits).
* **Unverified / Gaps**:
  * Gradle, `xcodebuild`, `ktlintCheck`, `detekt`, `swiftformat`, and `swiftlint` were not run because zero `.kt`, `.swift`, `.kts`, or Apple project files were modified in this documentation/workflow migration.
  * Pre-existing stale links in untouched skills (`kmp-testing`, `localization`, `ios-glass-design`), the legacy diagram in `README.md`, and `:shared` model `typealias` shims were intentionally deferred as `ISS-001`–`ISS-003` in `wiki/issue-log.md`.
