# Architecture Rules

## Core Boundaries

- Keep business logic, models, networking, and presentation state in `shared/commonMain`.
- Keep `androidApp`, `iosApp`, and `macosApp` thin UI shells that observe shared state.
- Avoid duplicating pagination, network parsing, or offset logic in platform modules.
- Avoid speculative dead code; when auditing shared declarations, verify they are not consumed by Swift in `iosApp`. Code that must be invoked from Swift only and has no calls from Kotlin should be annotated with `@Suppress("unused") // Invoked on Swift code`.

## Shared Presenter Pattern

- Prefer shared `StateFlow` or presenter-style state objects for feed, detail, and search flows.
- Keep platform code responsible for rendering, input handling, and navigation shell behavior.
- Preserve the existing KMP/SwiftUI/Compose separation instead of moving logic into the UI layers.

## Compile-Time Dependency Injection (Metro)

- Use **Metro** (`dev.zacsweers.metro`) for shared dependency injection at compile time.
- Define application-level bindings in `AppModule` using `@BindingContainer`, `@Provides`, and `@SingleIn(AppScope::class)`.
- Expose factories and singletons through `@DependencyGraph interface ApplicationGraph` and access them via `MetroHelper.graph` in `shared/commonMain/kotlin/com/example/imagefeed/di/Metro.kt`.
- Do not use dynamic or reflection-based DI libraries like Koin.

## Coroutine Lifecycle Standards

- Shared presenters should use lifecycle-aware scopes supplied by DI (`PresenterScope` / `PresenterScopeFactory`) instead of creating unmanaged coroutine scopes internally.
- Cancel in-flight work in teardown hooks such as `clear()`/`close()` and avoid leaving work running after a screen is dismissed.
- Centralize cancellation-aware state updates with shared helpers like `CoroutineContext.isActive()` and `MutableStateFlow.updateIfActive(...)` or `ensureActive()` rather than repeating ad-hoc active checks in every presenter.
- Platform wrappers remain responsible for presenter creation and cleanup so the UI lifecycle owns the boundary (e.g. Swift ViewModels invoke `presenter.clear()` in `deinit` or view dismissals).

## Image and API Compliance

- Preserve Unsplash image URL parameters such as `ixid`.
- Do not introduce server-side caching of image files.
- Keep photographer attribution and download-tracking expectations intact when touching image-related screens.

## Platform Notes

- Android work should stay aligned with Compose-based screens, existing shared state collection, and **Jetpack Navigation 3** (`NavBackStack`, `NavDisplay`, `NavKey`). Avoid legacy Compose Navigation 2.x `NavController` patterns.
- iOS/macOS work should keep SwiftUI integration lightweight, use native Swift ViewModels to bridge `CommonFlow` / `StateFlow` to `@Published` properties, and avoid reintroducing duplicated platform logic.
