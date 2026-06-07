# Agent Guidelines & Codebase Rules

Welcome! If you are an AI developer agent working on this codebase, please adhere to these guidelines to ensure consistency, performance, and compliance with the project architecture.

> [!IMPORTANT]
> **Keep this file small:**
> To ensure maximum readability and context efficiency, **do not make this `AGENTS.md` file too large.** If you need to add comprehensive rules, guidelines, or logs, break them out into separate files inside the `specs/` directory (e.g., `specs/concurrency_guidelines.md`) and link them here.

---

## Core Architectural Rules

### 1. Kotlin Multiplatform (KMP) Architecture
*   All business logic, data models, network client configuration, and UI state-management/pagination MUST reside in the `shared` module under `commonMain`.
*   Platform modules (`androidApp` and `iosApp`) MUST remain thin, declarative view shells (Jetpack Compose and SwiftUI). Do not implement duplicate data parsing or page offset calculations.

### 2. State & Presenters
*   Use the **Shared Presenter** pattern. All states (loading, paging, error) are represented by Kotlin data classes and streamed via `StateFlow` from `shared`.
*   Platform UIs must simply bind to these states (with Compose state collection on Android, and a Swift ViewModel mapping flows to SwiftUI variables on iOS).

### 3. API Key & Security
*   **NEVER commit API access keys, client secrets, or credentials** to this repository.
*   The project loads the API access key from `local.properties` (which is git-ignored) and exposes it via `BuildKonfig` to the shared client. 
*   If you introduce new configurations, follow this pattern: load them in Gradle and inject via BuildKonfig.

---

## Unsplash API Compliance

You MUST follow the Unsplash API developer guidelines when editing the application logic or UI:

*   **Hotlinking Image Files:** Do NOT cache image files on any third-party server or local database. Use the exact Unsplash URL returned by the API.
*   **Retaining parameters:** All image requests must preserve the `ixid` parameter in the URL.
*   **Photographer Attribution:** Every image card or detail page must prominently display the photographer’s name, their profile image (if available), and link back to their Unsplash profile and to Unsplash itself.
*   **Download Tracking:** When downloading or saving an image, call the endpoint `photo.links.download_location` to trigger the download tracking count.

---

## UI/UX & Performance Guidelines

*   **Dynamic Resizing:** Do not download original full-sized images. Read the screen or container width, and append query parameters to the raw image URL (`&w=calculatedWidth&q=80&auto=format`).
*   **BlurHash Placeholders:** Use the `blur_hash` string associated with each image to display a blurred placeholder during load transitions.
*   **Fluid Scrolling:** Infinite scrolling pagination must trigger pre-fetching of the next page before the user reaches the end of the scroll container to ensure frictionless layout updates.
*   **Adaptive Layout for Tablets/iPads:** View layouts must adapt dynamically to screen sizes. Grid displays should use auto-calculating adaptive columns (e.g., `StaggeredGridCells.Adaptive` on Compose, or flexible grid layout columns on SwiftUI) to scale column count gracefully on tablets/iPads instead of using hardcoded column counts. Side-by-side split panes should be used for detailed views on large screens.

---

## Commit Message Guidelines

Every commit message should follow this structure:

1. A short title summarizing the change.
2. A quick description of what changed and why.
3. A pointer to the spec file that generated the change.
4. The name of the model that performed the commit.

Use this format:

<short title>

<quick description>

Spec: specs/implementation_plan.md or specs/steps.md
Model: <model name>

Keep the title concise and make the spec/model lines explicit so each commit is traceable.

---

## Plan and Specs Requirements

Every change must be accompanied by a planning artifact in the `specs/` folder.

*   If the current work matches an existing implementation/spec document, update that existing plan instead of creating a duplicate.
*   Any new or updated plan must also be referenced from `specs/steps.md` so the implementation path stays traceable.
*   When working on a feature, bug fix, or refactor, confirm which spec file applies before starting, then update that spec and the related step notes together.

This keeps implementation, planning, and execution aligned.

---

## Reference Specs Directory
Refer to the `specs/` folder for detailed implementation details:
*   [Implementation Plan](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/implementation_plan.md)
*   [Step-by-Step Guide](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/steps.md)
