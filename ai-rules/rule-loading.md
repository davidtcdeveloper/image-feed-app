# Rule Loading Guide

Load these rules only when they match the current task.

## Primary Rule

Always start with this file when the request involves code changes, spec updates, or commit preparation.

## Rule Index

- `architecture.md` — shared KMP boundaries, presenter/state flow, platform shell responsibilities.
- `build-and-deps.md` — Gradle, version catalog, dependency modernization, and Apple/Android build tooling.
- `specs-and-commits.md` — which spec file to update, how to amend `specs/steps.md`, and how to write traceable commit messages.
- `testing.md` — how to add integration/package-level tests for shared behavior and how to verify them.
- `git-guidelines.md` — commit hygiene, traceability, and the repository’s expected commit-message structure.
- `design-principles.md` — cross-platform UI/UX principles, design token discipline, accessibility, attribution, and sensory guidelines.
- `material-design.md` — Android Material 3 design tokens, typography, shapes, semantic surfaces, and modern component guidelines.
- `apple-design.md` — Apple Human Interface Guidelines (HIG), SwiftUI materials, glassmorphism, translucency, and modern iOS 17/18 interactions.

## Quick Reference

- When changing shared logic or UI state: load `architecture.md` and `specs-and-commits.md`.
- When writing or updating Android Compose UI: load `design-principles.md`, `material-design.md`, `architecture.md`, and `specs-and-commits.md`.
- When writing or updating iOS SwiftUI / macOS UI: load `design-principles.md`, `apple-design.md`, `architecture.md`, and `specs-and-commits.md`.
- When adding or changing tests: load `testing.md`, `architecture.md`, and `build-and-deps.md` when the test setup or Gradle configuration changes.
- When changing Gradle, dependencies, or build tooling: load `build-and-deps.md` and `specs-and-commits.md`.
- When preparing a commit or reviewing commit hygiene: load `git-guidelines.md` and `specs-and-commits.md`.
- When updating docs or agent guidance: load `specs-and-commits.md` first, then the relevant domain rule if implementation is affected.

## Loading Strategy

1. Start with the smallest set of rules that match the request.
2. Add a spec rule whenever the change affects behavior, architecture, or planning.
3. Keep the loaded rules focused so the model stays on the project’s actual patterns.
