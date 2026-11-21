# Project Wiki — Unsplash Image Feed (KMP)

Welcome to the living documentation for the **Unsplash Image Feed** Kotlin Multiplatform application. This directory is the **single source of current truth** for system architecture, product and technical business rules, UI design systems, performance engineering, and architectural decisions.

---

## Documentation Layer Boundaries

| Layer | Location | Purpose | Mutability |
| :--- | :--- | :--- | :--- |
| **Root Invariants** | [`AGENTS.md`](../AGENTS.md) | Non-negotiable architectural boundaries & index (< 75 lines) | Rarely changed |
| **Procedural Runbooks** | [`.agents/skills/`](../.agents/skills) | Step-by-step build, test, styling, and issue workflows | Evolves via Issue Log Resolution |
| **Living System Wiki** | `wiki/*.md` | Current architecture, business rules, design systems, & ADRs | Updated on each completed spec & Issue Log Resolution |
| **Session Issue Queue** | [`wiki/issue-log.md`](issue-log.md) | Systemic conflicts, doc drift, skill gaps, & deferred tech debt | Appended post-session; triaged manually |
| **Active Work Queue** | [`specs/`](../specs/README.md) | Draft or in-progress specifications + compact [`steps.md`](../specs/steps.md) index | Active until completion |
| **Immutable Archive** | [`specs/archive/`](../specs/archive) | Finalized historical specifications (`Implemented (Final)`) | **Frozen / Read-only** |

---

## Wiki Index

* **[Business Rules & API Compliance](business-rules.md)** — Unsplash API mandates, API key injection, Retina CDN sizing math, pagination/deduplication rules, search debouncing, and typed error & localization invariants.
* **[Architecture & Modules](architecture-and-modules.md)** — Vertical feature modularization (`:core:*`, `:feature:*`, `:shared` umbrella), compile-time Metro DI graph, and Shared MVI / UDF lifecycle.
* **[Design Systems & Navigation](design-systems-and-navigation.md)** — Android Material 3 Expressive + Jetpack Navigation 3 adaptive scenes, and Apple iOS/macOS Liquid Glass + multi-target navigation.
* **[Performance & Memory](performance-and-memory.md)** — BlurHash decoding & caches, Coil / `ImageMemoryCache` pipelines, Compose & SwiftUI hot-path optimizations, static map snapshots, and R8 / Baseline Profiles.
* **[Architecture Decision Record (ADR) Index](adr-index.md)** — Chronological log of major technical decisions linking to immutable archived specifications.
* **[Known Issues & Evolution Queue](issue-log.md)** — Post-session capture via [`session-issue-capture`](../.agents/skills/session-issue-capture/SKILL.md); triaged manually via [`issue-log-resolution`](../.agents/skills/issue-log-resolution/SKILL.md).
