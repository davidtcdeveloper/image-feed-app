---
name: kmp-architecture
description: >-
  Architectural boundaries, Shared Presenter pattern, Metro compile-time DI,
  and coroutine lifecycle management across Kotlin Multiplatform and platform UI shells.
  Use when adding or refactoring business logic, presenters, dependency injection,
  or coroutine scopes.
---

# Kotlin Multiplatform Architecture & Lifecycle Runbook

## Purpose & Triggers

This skill defines the architectural boundaries, design patterns, dependency injection standards, and coroutine lifecycle rules for this repository.
Use this skill when:
- Designing or modifying shared business logic, data models, or repositories in `shared/commonMain`.
- Creating or refactoring shared presenters, view models, or state flows.
- Configuring compile-time dependency injection with Metro (`dev.zacsweers.metro`).
- Managing coroutine scopes, structured concurrency, or presenter teardown.
- Bridging shared Kotlin code to Android Compose or Apple SwiftUI view shells.

---

## 1. Core Architectural Boundaries

The application follows a strict Kotlin Multiplatform (KMP) division of responsibilities:

### A. Shared Business Logic (`shared/commonMain`)
All core logic MUST reside in the shared module:
- Network client configuration (Ktor 3).
- Data parsing, domain models, and serialization (Kotlinx Serialization).
- Repository abstractions and caching policies.
- Presentation state holders, pagination logic, and search query debounce.
- Platform modules (`androidApp`, `iosApp`, `macosApp`) MUST remain thin, declarative view shells (Jetpack Compose and SwiftUI). Do not duplicate data parsing or pagination offsets in platform code.

### B. Cross-Language Reference Auditing & Swift Interop
Before refactoring or removing declarations in `shared/commonMain`, search both Kotlin files (`androidApp/`, `shared/`) and Swift files (`iosApp/`).
Keep Swift interop name transformations in mind:
- Kotlin `description` property maps to Swift `description_` to avoid collision with Swift's `CustomStringConvertible`.
- Kotlin closures and coroutines map to Swift completion handlers or async signatures.
- Declarations invoked exclusively from Swift that appear "unused" to Kotlin analysis must be annotated with:
  ```kotlin
  @Suppress("unused") // Invoked on Swift code
  ```

### C. Zero Dead Code Policy
Do not introduce speculative, unused helper functions, extension methods, or properties. When replacing a parameter or pattern during refactoring, eliminate obsolete code, variables, and imports across all call sites in the same change set.

---

## 2. Shared Presenter & State Flow Pattern

The UI across all platforms is driven by the **Shared Presenter** pattern:

### A. Unidirectional State Flow
- Presenters expose UI state through immutable Kotlin data classes streamed via `StateFlow<T>`.
- State classes represent all possible visual conditions (loading, pagination, error, empty, content).
- Platform views observe this state reactively:
  - **Android**: `presenter.state.collectAsStateWithLifecycle()` in Compose.
  - **iOS / macOS**: Swift `ObservableObject` ViewModels observe Kotlin `CommonFlow` and publish updates to SwiftUI `@Published` properties.

### B. Presenter Lifecycle & Teardown
- Presenters implement a lifecycle teardown contract (such as `fun clear()`).
- Presenter coroutines MUST be launched within an injected `PresenterScope` / `PresenterScopeFactory` tied to the presenter lifecycle.
- When `clear()` is invoked, all in-flight coroutines and child jobs are deterministically cancelled.
- **Platform Ownership**: Platform wrappers own presenter teardown:
  - On Android: Back-stack scoped `PresenterViewModel` invokes `presenter.clear()` when its `ViewModelStore` is cleared.
  - On iOS/macOS: Swift ViewModels invoke `presenter.clear()` in `deinit` or upon view dismissal.

### C. State Update Guarding
To prevent emissions after scope cancellation, guard state updates with shared lifecycle helpers:
```kotlin
stateFlow.updateIfActive(coroutineContext) { currentState ->
    currentState.copy(...)
}
```
Avoid repeating ad-hoc `if (!isActive())` checks across individual emission points.

---

## 3. Compile-Time Dependency Injection (Metro)

Dependency injection is handled at compile time using **Metro** (`dev.zacsweers.metro`). Dynamic, reflection-based DI frameworks (like Koin) are strictly prohibited.

### A. Graph & Scoping Architecture
- The root dependency container is defined via `@DependencyGraph interface ApplicationGraph` in `shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt`.
- Application-level singletons are scoped using `@SingleIn(AppScope::class)`.
- Core networking and system services are declared in `NetworkModule` and bound via `@ContributesBinding`.
- Access to the graph is centralized through `MetroHelper.graph`.

### B. Presenter Injection Patterns
1. **Un-parameterized Presenters** (e.g. `FeedPresenter`, `UnifiedSearchPresenter`):
   - Migrated to standard `@Inject constructor(...)`.
   - Exposed as factories on `ApplicationGraph`.
2. **Parameterized Presenters** (e.g. `PhotoDetailsPresenter`, `CollectionDetailPresenter`, `UserProfilePresenter`):
   - Use `@Assisted` injection for runtime parameters (e.g. `photoId`, `collectionId`, `username`).
   - Define dedicated `@AssistedFactory` interfaces exposed via `ApplicationGraph`.

---

## 4. Platform Integration Guidelines

### Android (`androidApp`)
- Render views using pure Jetpack Compose following Material 3 guidelines.
- Navigation must use **Jetpack Navigation 3** (`NavBackStack`, `NavDisplay`, `NavKey`).
- Scope presenters to navigation back-stack entries using `rememberEntryPresenter` and `rememberViewModelStoreNavEntryDecorator()`.

### Apple (`iosApp` & `macosApp`)
- Render views using pure SwiftUI following Apple Human Interface Guidelines (HIG) and `GlassTheme`.
- Platform ViewModel wrappers bridge Kotlin `StateFlow` to `@Published` properties on `@MainActor`.
- Invoke `presenter.clear()` inside `deinit` to cancel background coroutines upon screen dismissal.
