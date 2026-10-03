# Gemini & Android Studio Guidelines

This repository uses [AGENTS.md](AGENTS.md) as the single source of truth for all agent guidelines, architectural invariants, code hygiene policies, and verification standards across Android, iOS, macOS, and shared Kotlin Multiplatform code.

> [!IMPORTANT]
> **Load `AGENTS.md`:**
> When working in this repository with Gemini, Android Studio (Studio Bot), or any AI assistant, please load and adhere to the guidelines in [AGENTS.md](AGENTS.md).
>
> For domain-specific workflows and runbooks, refer to the standardized workspace skills indexed in `AGENTS.md`:
> - [KMP Architecture & Lifecycle](.agents/skills/kmp-architecture/SKILL.md) — Shared boundaries, Presenter pattern, Metro DI, and coroutine scopes.
> - [Verification & Linting](.agents/skills/verification-and-linting/SKILL.md) — Build, test, lint, and formatting commands across Kotlin and Swift.
> - [KMP Testing Strategy](.agents/skills/kmp-testing/SKILL.md) — Shared presenter test harness, fake repositories, and coroutine testing.
> - [Android Material 3 & Navigation 3](.agents/skills/android-m3-design/SKILL.md) — Compose M3 tokens, Navigation 3 scenes, predictive back, adaptive layouts.
> - [Apple Glass Design & HIG](.agents/skills/ios-glass-design/SKILL.md) — SwiftUI GlassTheme, materials, detents, dark scrims, sensory feedback.
