# Agent Guidelines & Codebase Rules

## Purpose

Agents act as senior project collaborators for this Kotlin Multiplatform image-feed app. Keep responses concise, adhere to non-negotiable repository invariants, and consult domain skills on-demand for procedural runbooks.

---

## Workspace Skills & Runbooks

For detailed implementation runbooks and domain guidelines, consult the relevant skill:
* [KMP Architecture & Lifecycle](.agents/skills/kmp-architecture/SKILL.md) — Shared boundaries, Presenter pattern, Metro DI, and coroutine scopes.
* [Verification & Linting](.agents/skills/verification-and-linting/SKILL.md) — Build, test, lint, and formatting commands across Kotlin and Swift.
* [KMP Testing Strategy](.agents/skills/kmp-testing/SKILL.md) — Shared presenter test harness, fake repositories, and coroutine testing.
* [Android Material 3 & Navigation 3](.agents/skills/android-m3-design/SKILL.md) — Compose M3 tokens, Navigation 3 scenes, predictive back, adaptive layouts.
* [Apple Glass Design & HIG](.agents/skills/ios-glass-design/SKILL.md) — SwiftUI GlassTheme, materials, detents, dark scrims, sensory feedback.

---

## Core Invariants

These non-negotiable boundaries and policies must be observed on every change:

1. **KMP Architecture Boundaries**:
   * All business logic, models, networking, and presentation state reside in `shared/commonMain`.
   * Platform modules (`androidApp`, `iosApp`, `macosApp`) remain thin, declarative view shells (Compose / SwiftUI).
   * Dependency injection is handled at compile time using **Metro** (`dev.zacsweers.metro`). Do not use Koin or dynamic DI.

2. **Dual-Platform Build Policy**:
   * Whenever changes affect `shared/` or platform logic, verify **BOTH** Android (`./gradlew :androidApp:assembleDebug`) and iOS (`xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`).
   * Never assume shared changes work in Swift just because Kotlin compiles.

3. **Quality & Hygiene Standards**:
   * **Zero Compiler Warning Policy**: Monitor and resolve compiler warnings immediately after significant changes.
   * **Zero Dead Code Policy**: Eliminate obsolete properties, parameters, and imports in the same change set.
   * **Host Permissions**: Build, test, and lint commands require host execution (`BypassSandbox: true` in sandboxed agents).

4. **Security & Unsplash API Compliance**:
   * **Never commit API keys or credentials**. Keys are loaded from `local.properties` via `BuildKonfig`.
   * Hotlink exact Unsplash photo URLs; never cache image files locally or on third-party servers.
   * Preserve the `ixid` parameter on all photo requests.
   * Prominently display photographer attribution on all cards/screens and trigger download tracking on save.

---

## Commit & Planning Conventions

Every commit message must follow this structure:

```text
<short title>

<quick description>

Spec: specs/<actual-spec-file>.md
Model: <model name>
```

* **Planning & Specs**: Every change must be anchored in an active specification in `specs/`. When implementation finishes, update the spec's `**Status:** Implemented` and mark the step in [`specs/steps.md`](specs/steps.md) as `(Completed)`.
* For historical implementation notes and deep architectural blueprints, refer to [`specs/steps.md`](specs/steps.md).
