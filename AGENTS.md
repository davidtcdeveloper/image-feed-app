# Agent Guidelines & Codebase Rules

## Purpose
Agents act as senior project collaborators for this KMP image-feed app. Keep responses concise, follow non-negotiable invariants, consult [Project Wiki](wiki/README.md) for living system/business knowledge, and load skills on-demand.

## Knowledge Base, Skills & Runbooks
* [Project Wiki](wiki/README.md) — Current architecture, business rules, design systems, ADRs, and [issue-log.md](wiki/issue-log.md).
* [Session Issue Capture](.agents/skills/session-issue-capture/SKILL.md) — Post-session capture of systemic conflicts, doc drift, and tooling friction into `wiki/issue-log.md`.
* [Issue Log Resolution](.agents/skills/issue-log-resolution/SKILL.md) — Manual on-demand evaluation and resolution of open issues in `wiki/issue-log.md`.
* [KMP Architecture & Lifecycle](.agents/skills/kmp-architecture/SKILL.md) — Shared boundaries, MVI pattern, Metro DI, and coroutine scopes.
* [Verification & Linting](.agents/skills/verification-and-linting/SKILL.md) — Build, test, lint, and formatting commands across Kotlin and Swift.
* [UI Visual Review](.agents/skills/ui-visual-review/SKILL.md) — Ephemeral Before/After UI captures (Compose Preview Sidecar, iOS/macOS snapshot suites) and visual review gate.
* [KMP Testing Strategy](.agents/skills/kmp-testing/SKILL.md) — Shared MVI ViewModel test harness, fake repositories, and coroutine testing.
* [Android Material 3 & Navigation 3](.agents/skills/android-m3-design/SKILL.md) — Compose M3 tokens, Navigation 3 scenes, predictive back, adaptive layouts.
* [Apple Glass Design & HIG](.agents/skills/ios-glass-design/SKILL.md) — SwiftUI GlassTheme, materials, detents, dark scrims, sensory feedback.
* [Localization](.agents/skills/localization/SKILL.md) — Multiplatform string tables, AppError mapping, native formatters, RTL safety.

## Core Invariants

1. **KMP Architecture Boundaries & Shared MVI / UDF**:
   * All business logic, models, networking, and presentation state reside in shared Kotlin code.
   * **Modular Structure**: Vertical feature modules (`:feature:*`), foundational core (`:core:*`), and `:shared` umbrella framework with Metro DI (see [Architecture Wiki](wiki/architecture-and-modules.md) and archived Specs [46](specs/archive/46_modularization_foundation_core_extraction.md)–[49](specs/archive/49_modularization_umbrella_and_linking_optimization.md)).
   * Platform modules (`androidApp`, `iosApp`, `macosApp`) remain thin, declarative view shells (Compose / SwiftUI). `macosApp` is a target in `iosApp/project.yml` that compiles the same `iosApp/iosApp` sources, so every Swift edit also builds for macOS.
   * Dependency injection is handled at compile time using **Metro** (`dev.zacsweers.metro`). Do not use Koin or dynamic DI.
   * **Presentation Architecture**: Standardized on **Shared MVI / UDF** built on [`MviViewModel`](core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt), [`UiState`](core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiState.kt), [`UiIntent`](core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiIntent.kt), and [`UiEffect`](core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiEffect.kt).
   * **Lifecycle & Teardown**: ViewModels manage coroutines within `viewModelScope`. Android scopes via Compose `viewModel { ... }`; iOS/macOS `@Observable` wrappers invoke `viewModel.clear()` in `deinit`.

2. **Platform Build Policy**:
   * Whenever changes affect `shared/`, `:core:*`, `:feature:*`, or platform logic, verify **BOTH** Android and iOS; commands live in the verification skill.
   * Apple-specific changes (`iosApp/**`, Apple Kotlin source sets, Kotlin/Native framework config) must **also** build the `macosApp` scheme.
   * Never assume shared changes work in Swift just because Kotlin compiles.

3. **Quality & Hygiene Standards**:
   * **Zero Compiler Warning Policy**: Monitor and resolve compiler warnings immediately after significant changes.
   * **Zero Dead Code Policy**: Eliminate obsolete properties, parameters, and imports in the same change set.
   * **Living Code & Anti-Staleness Policy**: Instructions and skills must never include verbatim boilerplate code samples or multi-line implementation blocks. Reference living, compiler-verified source files via file links or specify guidelines declaratively. Micro-snippets (< 4 lines) are restricted to compiler suppressions, CLI commands, or formulas.
   * **Host Permissions**: Build, test, and lint commands require unsandboxed host execution when running in a restricted agent sandbox (access to JDK, Android SDK, ~/.gradle, Xcode).
   * **Strict Commit Scoping**: Stage and commit only specifically requested files; clean up temporary scratch files in `.agents/` prior to commit.
   * **Spec Completeness**: Validate all acceptance criteria checkboxes (`- [x]`) before marking a spec `Implemented (Final)`.

4. **Security & Unsplash API Compliance**:
   * **Never commit API keys or credentials**. Keys are loaded from `local.properties` via `BuildKonfig`.
   * Hotlink exact Unsplash photo URLs; never cache image files locally or on third-party servers.
   * Preserve the `ixid` parameter on all photo requests.
   * Prominently display photographer attribution on all cards/screens and trigger download tracking on save.
   * Multiply container layout points by display scale (`UIScreen.main.scale` on iOS, `NSScreen.backingScaleFactor` on macOS) for CDN image requests to ensure sharp Retina rendering.

5. **Platform Localization & Typed Errors**:
   * Zero hardcoded UI strings; externalize to Android XML and Apple String Catalogs.
   * Map typed `AppError` per-platform; use platform-native plural and compact number formatters.

## Commit & Planning Conventions

Every commit message must follow this structure:

```text
<short title>

<quick description>

Spec: specs/[archive/]<actual-spec-file>.md
Model: <model name>
```

* **Planning, Wiki & Specs**: Active specs live in `specs/`. On completion: verify `- [x]`, update [`wiki/`](wiki/README.md) and affected [`.agents/skills/`](.agents/skills), log systemic/unresolved session issues in [`wiki/issue-log.md`](wiki/issue-log.md), mark spec `Implemented (Final)`, archive to `specs/archive/` (never edit archived specs), and add a row to [`specs/steps.md`](specs/steps.md).
