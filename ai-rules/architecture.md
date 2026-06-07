# Architecture Rules

## Core Boundaries

- Keep business logic, models, networking, and presentation state in `shared/commonMain`.
- Keep `androidApp`, `iosApp`, and `macosApp` thin UI shells that observe shared state.
- Avoid duplicating pagination, network parsing, or offset logic in platform modules.

## Shared Presenter Pattern

- Prefer shared `StateFlow` or presenter-style state objects for feed, detail, and search flows.
- Keep platform code responsible for rendering, input handling, and navigation shell behavior.
- Preserve the existing KMP/SwiftUI/Compose separation instead of moving logic into the UI layers.

## Image and API Compliance

- Preserve Unsplash image URL parameters such as `ixid`.
- Do not introduce server-side caching of image files.
- Keep photographer attribution and download-tracking expectations intact when touching image-related screens.

## Platform Notes

- Android work should stay aligned with Compose-based screens and existing shared state collection.
- iOS/macOS work should keep SwiftUI integration lightweight and avoid reintroducing duplicated platform logic.
