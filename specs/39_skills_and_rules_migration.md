# Specification: Universal Agent Guidelines, Skills & Rules Architecture Migration

**Status:** Approved

## 1. Overview & Objectives

The project currently organizes agent instructions using a root [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) that directs models to load secondary guidance files under [`ai-rules/`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/ai-rules/) via [`ai-rules/rule-loading.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/ai-rules/rule-loading.md).

This setup has two primary shortcomings:
1. **Multi-Hop Fragility**: It manually attempts to simulate progressive disclosure through [`ai-rules/rule-loading.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/ai-rules/rule-loading.md), requiring models to traverse multiple hops before loading domain guidance.
2. **Harness Silos & Inconsistency**: The instructions are fragmented in a custom format that does not take full advantage of emerging open agent standards, making cross-harness support inconsistent across **Google Antigravity**, **Android Studio (Gemini / Studio Bot)**, **OpenCode CLI**, and other developer tools (such as Cursor or Claude Code).

### Strategic Goal: Universal Harness Interoperability
Establish a **harness-agnostic, dual-tier architecture** that functions consistently across:
- **Google Antigravity**: Discovers root [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) rules and auto-indexes `.agents/skills/<name>/SKILL.md` via native `<skills>` prompt injection.
- **Android Studio (Gemini / Studio Bot)**: Discovers root `GEMINI.md` / [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) and follows explicit markdown links to skills.
- **OpenCode CLI**: Discovers standard Agent Skills via `.agents/skills/` (or `skills/`) and standard [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md).
- **Generic LLM / Agent CLI Runners**: Navigates instructions via the fallback Skills Catalog in [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md).

---

## 2. Multi-Harness Discovery Architecture

```
image-feed-app/
├── AGENTS.md                          # Universal Source of Truth: Core Invariants & Skill Catalog
├── GEMINI.md                          # Compatibility link to AGENTS.md (for Android Studio / Gemini)
├── .agents/
│   └── skills/                        # Universal Agent Skills Standard
│       ├── verification-and-linting/
│       │   └── SKILL.md
│       ├── kmp-testing/
│       │   └── SKILL.md
│       ├── android-m3-design/
│       │   └── SKILL.md
│       └── ios-glass-design/
│           └── SKILL.md
└── skills/ -> .agents/skills          # Root symlink for tools scanning root skills/
```

### Discovery Matrix

| Developer Harness | Entrypoint / Discovery Mechanism | Progressive Disclosure Strategy |
| :--- | :--- | :--- |
| **Google Antigravity** | Automatically loads `AGENTS.md` as active project rules; traverses `.agents/skills/` for dynamic tool-assisted skill injection. | **Tier 1 (Native):** Skills descriptions injected into `<skills>` block; agent invokes `view_file` on demand. |
| **Android Studio (Gemini)** | Native detection of root `GEMINI.md` and `AGENTS.md`. | **Tier 2 (Catalog Fallback):** Reads concise Skills Catalog in `AGENTS.md` and follows direct markdown file links. |
| **OpenCode CLI** | Scans workspace root for `AGENTS.md` and `.agents/skills/` or `skills/`. | **Tier 1 & Tier 2:** Reads `SKILL.md` metadata natively or falls back to prompt catalog in `AGENTS.md`. |
| **Cursor / Claude Code / Other CLI** | Scans workspace root for `AGENTS.md` / `GEMINI.md`. | **Tier 2 (Catalog Fallback):** Explicit markdown links and trigger descriptions in `AGENTS.md`. |

---

## 3. Component Architecture: Invariants (Rules) vs. Runbooks (Skills)

### A. Core Invariants in `AGENTS.md` (Universal Rules)
Rules represent **non-negotiable constraints, boundaries, and policies** that must be observed on every change regardless of task type:
1. **Architecture Boundaries**: KMP `commonMain` business logic, declarative platform shells (`androidApp`, `iosApp`, `macosApp`), and compile-time Metro DI (`dev.zacsweers.metro`).
2. **API Security & Unsplash Compliance**: No API keys in source control, `BuildKonfig` usage, image hotlinking, preserving `ixid`, photographer attribution, and download tracking.
3. **Hygiene Policies**: Zero Dead Code Policy, Zero Compiler Warning Policy, Dual-Platform Build Policy, and Sandbox Execution Policy (`BypassSandbox: true` for build/test tools).
4. **Commit & Planning Conventions**: Traceable commit format (Title, Description, Spec, Model) and spec synchronization in `specs/`.
5. **Universal Skills Catalog**: A concise markdown index of all available skills with their triggers and relative file paths, serving as the universal discovery mechanism across all harnesses.

### B. Standardized Workspace Skills in `.agents/skills/`
Skills adhere to the open Agent Skills standard (YAML frontmatter + Markdown body) and provide **actionable runbooks and domain procedures**:

#### 1. `.agents/skills/verification-and-linting/SKILL.md`
* **Triggers**: Running builds, validating PRs, diagnosing compile errors or warnings, checking static analysis, or formatting code.
* **Frontmatter**:
  ```yaml
  ---
  name: verification-and-linting
  description: >-
    Run verification commands, compile checks, static analysis, and code formatters
    across Kotlin (shared/androidApp) and Swift (iosApp/macosApp). Use when diagnosing
    build errors, verifying changes, checking warnings, or formatting code prior to commit.
  ---
  ```
* **Runbook Details**:
  - Verification commands (`./gradlew :shared:allTests`, `./gradlew :androidApp:assembleDebug`, `xcodegen` + `xcodebuild`).
  - Formatting & linting runbooks (`./gradlew ktlintFormat`, `./gradlew ktlintCheck detekt`, `swiftformat .`, `swiftlint lint iosApp/iosApp`).
  - Host execution requirements across CLI harnesses (`BypassSandbox: true` / host privileges).

#### 2. `.agents/skills/kmp-testing/SKILL.md`
* **Triggers**: Writing, refactoring, or running tests for shared code or presenters.
* **Frontmatter**:
  ```yaml
  ---
  name: kmp-testing
  description: >-
    Author, refactor, and execute integration and unit tests in shared KMP commonTest.
    Use when adding tests for presenters, state flows, or fake repository interactions.
  ---
  ```
* **Runbook Details**:
  - Presenter test scaffolding (`TestPresenterScopeFactory`, `TestDispatcherProvider`, `FakeUnsplashRepository`).
  - StateFlow assertion patterns (testing observable outcomes, avoiding private internals).
  - Test boundary explanation (platform shells lack dedicated unit test targets).

#### 3. `.agents/skills/android-m3-design/SKILL.md`
* **Triggers**: Implementing or updating Android Compose UI, Material 3 styling, Navigation 3, or adaptive layouts.
* **Frontmatter**:
  ```yaml
  ---
  name: android-m3-design
  description: >-
    Design and implement Jetpack Compose UI for Android following Material 3 guidelines,
    design tokens, Jetpack Navigation 3, adaptive layouts, and predictive back animations.
  ---
  ```
* **Runbook Details**:
  - Material 3 color tokens (`MaterialTheme.colorScheme`), surface roles, and typography.
  - Jetpack Navigation 3 patterns (`NavBackStack`, `NavDisplay`, `NavKey`, shared element transitions).
  - Adaptive column calculation (`StaggeredGridCells.Adaptive`) and foldable split-pane layouts.
  - Haptics and gesture-driven predictive back handlers.

#### 4. `.agents/skills/ios-glass-design/SKILL.md`
* **Triggers**: Implementing or updating SwiftUI views for iOS or macOS.
* **Frontmatter**:
  ```yaml
  ---
  name: ios-glass-design
  description: >-
    Design and implement SwiftUI views for iOS and macOS following Apple HIG, GlassTheme
    design tokens, materials, detented sheets, background interactions, and sensory feedback.
  ---
  ```
* **Runbook Details**:
  - `GlassTheme` design tokens and semantic materials (`.ultraThinMaterial`, `.regularMaterial`).
  - Interactive sheet detents (`.presentationBackgroundInteraction(.enabled(upThrough: ...))`).
  - Dark scrim contrast layering for WCAG AAA compliance.
  - Sensory feedback (`.sensoryFeedback`) and scroll transition effects.

---

## 4. Multi-Harness Compatibility Setup

### A. Root `GEMINI.md` Symlink / Alias
For Android Studio (which natively prioritizes `GEMINI.md`):
- Create `GEMINI.md` at repository root as a symlink to `AGENTS.md` (or a redirect document), ensuring Android Studio Studio Bot/Gemini accesses identical instructions without maintenance overhead.

### B. Root `skills/` Symlink
For CLI tools (such as OpenCode or generic runners) that look for `skills/` directly in the project root:
- Create a symlink `skills -> .agents/skills` to guarantee path resolution across all agent runtime configurations.

### C. Self-Documenting Skills Catalog in `AGENTS.md`
In `AGENTS.md`, replace the old `ai-rules/` index with:
```markdown
## Workspace Skills & Runbooks

For detailed procedural instructions, consult the relevant skill:
* [Verification & Linting](.agents/skills/verification-and-linting/SKILL.md) — Build, test, lint, and formatting commands across Kotlin and Swift.
* [KMP Testing Strategy](.agents/skills/kmp-testing/SKILL.md) — Shared presenter test harness, fake repositories, and coroutine testing.
* [Android Material 3 & Navigation 3](.agents/skills/android-m3-design/SKILL.md) — Compose M3 tokens, Navigation 3 scenes, predictive back, adaptive layouts.
* [Apple Glass Design & HIG](.agents/skills/ios-glass-design/SKILL.md) — SwiftUI GlassTheme, materials, detents, dark scrims, sensory feedback.
```
This guarantees that **any** harness or LLM unable to auto-discover YAML frontmatter can still follow direct markdown links.

---

## 5. Execution Plan & Phasing

### Phase 1: Skill Structure & Symlinks
1. Create `.agents/skills/` directory structure with the four standardized `SKILL.md` files.
2. Populate each `SKILL.md` with standard YAML frontmatter and comprehensive instructions migrated from `ai-rules/`.
3. Create root `skills -> .agents/skills` symlink for cross-tool compatibility.
4. Create root `GEMINI.md -> AGENTS.md` symlink for Android Studio compatibility.

### Phase 2: Invariants Consolidation in `AGENTS.md`
1. Consolidate core architectural invariants from [`ai-rules/architecture.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/ai-rules/architecture.md), [`ai-rules/git-guidelines.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/ai-rules/git-guidelines.md), and [`ai-rules/specs-and-commits.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/ai-rules/specs-and-commits.md) into [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md).
2. Add the **Workspace Skills & Runbooks Catalog** to [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md).
3. Remove references to [`ai-rules/rule-loading.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/ai-rules/rule-loading.md).

### Phase 3: Retirement of `ai-rules/`
1. Remove [`ai-rules/rule-loading.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/ai-rules/rule-loading.md).
2. Clean up or archive legacy files in [`ai-rules/`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/ai-rules/).

### Phase 4: Verification Across Harnesses
1. Verify frontmatter validity across all created `SKILL.md` files.
2. Confirm symlinks resolve cleanly on macOS/Linux.
3. Verify that `./gradlew ktlintCheck detekt`, `swiftlint lint iosApp/iosApp`, and `swiftformat --lint .` execute cleanly.
4. Update [`specs/steps.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/steps.md) marking Step 41 as completed.

---

## 6. Success & Validation Criteria

- [ ] All four skills exist under `.agents/skills/` with valid YAML frontmatter and self-contained markdown documentation.
- [ ] Root `GEMINI.md` resolves to [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) for Android Studio / Gemini compatibility.
- [ ] Root `skills/` resolves to `.agents/skills/` for CLI and open-agent tooling compatibility.
- [ ] [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) provides a comprehensive, self-contained set of invariants and a clear Markdown-linked Skills Catalog.
- [ ] No remaining references to `ai-rules/rule-loading.md`.
- [ ] Codebase linting and static analysis pass cleanly.
