# Specification: Living Code References & Anti-Staleness Rules for Agent Skills

**Status:** Implemented

## 1. Overview & Problem Statement

Agent instruction files across this repository ([`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) and `.agents/skills/*/SKILL.md`) currently contain varying degrees of inlined code snippets. While originally intended to guide models toward canonical patterns, inlined code blocks introduce several systemic risks in modern agentic pair-programming workflows:

1. **Silent Drift & API Staleness**:
   Markdown code blocks are uncompiled, unformatted by linters, and invisible to compiler refactoring tools. When constructors, domain models, or library versions evolve (e.g. coroutine testing utilities, Jetpack Navigation 3 APIs, SwiftUI modifiers), inlined markdown snippets silently rots. When models reproduce stale snippets, they introduce compilation errors and regressions.
2. **Anchoring Bias & Suppressed Invention**:
   Large Language Models exhibit strong copy-paste bias when presented with concrete multi-line code implementations in their prompt context. Rather than reasoning about the best, most modern, or context-appropriate API, models reflexively copy-paste verbose boilerplate. This suppresses cleaner, more inventive, or platform-native solutions.
3. **Context Window Bloat & Attention Dilution**:
   Multi-line code blocks consume significant token budget. When skills are injected into the agent's context, verbose boilerplate dilutes model attention away from task-specific requirements and user instructions.
4. **Competing Sources of Truth**:
   Inlined snippets duplicate real code that already exists in the project (such as [`FeedPresenterTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/FeedPresenterTest.kt) or [`GlassTheme.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/GlassTheme.swift)). Maintaining duplicate implementations across markdown and source code violates the Single Source of Truth (DRY) principle.

---

## 2. Core Architectural Principles & Paradigm Shift

### A. Living Code References over Markdown Duplication
Instead of inlining multi-line classes, functions, or UI components into Markdown:
- Point models directly to **living source files** in the repository using clickable markdown links (e.g. `[FeedPresenterTest.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/FeedPresenterTest.kt)`).
- Because coding agents have file inspection tools (`view_file`), they read living, active code that is continuously verified by `./gradlew :shared:allTests`, `./gradlew ktlintCheck`, and `xcodebuild`.

### B. Declarative Guidelines over Prescriptive Templates
Agent instructions should define **invariants, semantic tokens, and constraints**, leaving syntactic execution to the model:
- **Good (Declarative)**: "All cards must use `MaterialTheme.colorScheme.surfaceContainer` and `MaterialTheme.shapes.medium`. Never hardcode raw color literals or corner radii." (As demonstrated in [`android-m3-design/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/android-m3-design/SKILL.md)).
- **Bad (Prescriptive)**: Inlining 30 lines of a hardcoded Compose card or SwiftUI view layout.

### C. Encapsulation in Codebase Utilities
If a UI pattern or setup logic is complex and frequently repeated:
- Encapsulate the pattern as a reusable component or modifier directly in the codebase (e.g., [`GlassTheme.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/GlassTheme.swift) view modifiers).
- Instruct the skill to call that shared modifier rather than inlining raw SwiftUI or Compose overlay mechanics in markdown.

### D. Micro-Snippets Policy (Permitted Exceptions)
Code snippets in skills and instructions are restricted to concise micro-snippets (maximum 3–4 lines) strictly for:
- Esoteric cross-language suppressions or annotations that models cannot infer from public training data (e.g., `@Suppress("unused") // Invoked on Swift code`).
- Non-ambiguous command-line triggers (e.g., `./gradlew :shared:allTests`, `xcodegen generate ... && xcodebuild ...`).
- Precise mathematical/hardware formulas (e.g., Retina scale factor calculations: `let pixelWidth = Int(pointWidth * scale)`).

---

## 3. Implementation Steps

### Phase 1: Repository Invariant in `AGENTS.md`
Add a non-negotiable **Living Code & Anti-Staleness Policy** to [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) under `Core Invariants` -> `3. Quality & Hygiene Standards`:
- Mandate that all model instructions, skills, and prompt files must never contain verbatim boilerplate code templates or multi-line implementation blocks.
- Require referencing living, compiler-verified source files via file links or stating guidelines declaratively.
- Enforce the micro-snippet restriction (< 4 lines) for suppressions, CLI commands, or formulas.
- Maintain root rule brevity (< 75 lines).

### Phase 2: Refactoring Existing Skills

1. **[`kmp-testing/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/kmp-testing/SKILL.md)**:
   - Remove the 40-line `FeedPresenterTest` code block (lines 42–80).
   - Replace with a concise, declarative summary and a direct markdown link pointing to [`FeedPresenterTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/FeedPresenterTest.kt) as the canonical testing harness.
   - Outline key testing mechanisms (`TestPresenterScopeFactory`, `FakeUnsplashRepository`, `advanceUntilIdle()`) without duplicating test code.

2. **[`ios-glass-design/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/ios-glass-design/SKILL.md)**:
   - Remove the 12-line raw specular border `.overlay(...)` snippet (lines 38–49).
   - Remove the 15-line `.sheet(...)` inspector layout block (lines 129–143).
   - Point to [`GlassTheme.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/GlassTheme.swift) tokens and modifiers (`.glassBackground(...)`, `.glassCard()`, `GlassTheme.specularBorderGradient`, `.presentationDetents`).
   - Retain the concise 4-line Retina display scale formula as an allowed hardware formula exception.

3. **[`kmp-architecture/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/kmp-architecture/SKILL.md)**:
   - Retain the 2-line `@Suppress("unused") // Invoked on Swift code` micro-snippet (allowed exception).
   - Clarify the `updateIfActive` state flow guard by pointing to its shared extension definition rather than expanding inline code.

### Phase 3: Steps & Tracking Synchronization
- Register Step 42 in [`specs/steps.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/steps.md).
- Validate that all five skills in `.agents/skills/` remain well-formed with valid YAML frontmatter.

---

## 4. Verification & Acceptance Criteria

- [x] [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) contains the **Living Code & Anti-Staleness Policy** under Quality & Hygiene Standards and remains strictly under 75 lines.
- [x] [`kmp-testing/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/kmp-testing/SKILL.md) has all 40 lines of verbatim `FeedPresenterTest` code removed and replaced with a living link to [`FeedPresenterTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/FeedPresenterTest.kt).
- [x] [`ios-glass-design/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/ios-glass-design/SKILL.md) has raw overlay and sheet boilerplate removed, referencing [`GlassTheme.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/GlassTheme.swift) view modifiers instead.
- [x] No skill file contains boilerplate implementation blocks $> 4$ lines.
- [x] [`specs/steps.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/steps.md) indexes Step 42 corresponding to this specification.
