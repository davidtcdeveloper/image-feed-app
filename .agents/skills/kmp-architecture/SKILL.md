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

## 2. Architecture Models: Legacy Presenter & Modern MVI Transition State

The repository is actively transitioning from the legacy **Shared Presenter** pattern to **Shared MVI / Unidirectional Data Flow (UDF)**. Both patterns coexist during the migration phase:

### A. Transition Invariants & Coexistence Rules
- **Legacy Components**: Existing presenters remain supported and operational until their scheduled migration. Do not prematurely break legacy call sites.
- **New & Migrated Components**: All newly authored presentation logic or migrated screens MUST adopt the MVI architecture based on [`MviViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt).
- **Core MVI Types**:
  - State: [`UiState`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiState.kt) represents immutable visual conditions emitted via `StateFlow`.
  - Intent: [`UiIntent`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiIntent.kt) represents sealed user interactions routed into `viewModel.dispatch(intent)`.
  - Effect: [`UiEffect`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiEffect.kt) represents one-off, single-shot side effects (navigation, snackbars, sensory haptics) emitted via buffered channels.
- **Platform Integration**:
  - **Android (Compose)**: Binds state via `collectAsStateWithLifecycle()` and collects effects via `LaunchedEffect`. UI interactions dispatch typed intents to `viewModel.dispatch(intent)`.
  - **Apple (SwiftUI)**: Swift `@Observable` classes watch `viewModel.iosState` and `viewModel.iosEffects`, route user actions into `viewModel.dispatch(intent:)`, and trigger `viewModel.clear()` upon `deinit`.
- **Coroutine Lifecycles**: MVI ViewModels manage asynchronous workloads within official multiplatform `viewModelScope`, automatically cancelled when the host screen clears.

### B. Legacy Shared Presenter Pattern (Under Active Deprecation)
The UI across all platforms was previously driven by the **Shared Presenter** pattern:

#### 1. Unidirectional State Flow
- Presenters expose UI state through immutable Kotlin data classes streamed via `StateFlow<T>`.
- State classes represent all possible visual conditions (loading, pagination, error, empty, content).
- Platform views observe this state reactively:
  - **Android**: `presenter.state.collectAsStateWithLifecycle()` in Compose.
  - **iOS / macOS**: Swift `ObservableObject` ViewModels observe Kotlin `CommonFlow` and publish updates to SwiftUI `@Published` properties.

#### 2. Presenter Lifecycle & Teardown
- Presenters implement a lifecycle teardown contract (such as `fun clear()`).
- Presenter coroutines MUST be launched within an injected `PresenterScope` / `PresenterScopeFactory` tied to the presenter lifecycle.
- When `clear()` is invoked, all in-flight coroutines and child jobs are deterministically cancelled.
- **Platform Ownership**: Platform wrappers own presenter teardown:
  - On Android: Back-stack scoped `PresenterViewModel` invokes `presenter.clear()` when its `ViewModelStore` is cleared.
  - On iOS/macOS: Swift ViewModels invoke `presenter.clear()` in `deinit` or upon view dismissal.

#### 3. State Update Guarding
To prevent emissions after scope cancellation, guard state updates using the shared [`updateIfActive`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/PresenterScope.kt) extension on `MutableStateFlow`. Avoid repeating ad-hoc `if (!isActive())` checks across individual emission points.

---

## 3. Compile-Time Dependency Injection (Metro)

Dependency injection is handled at compile time using **Metro** (`dev.zacsweers.metro`). Dynamic, reflection-based DI frameworks (like Koin) are strictly prohibited.

### A. Graph & Scoping Architecture
- The root dependency container is defined via `@DependencyGraph interface ApplicationGraph` in `shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt`.
- Application-level singletons are scoped using `@SingleIn(AppScope::class)`.
- Core networking and system services are declared in `NetworkModule` and bound via `@ContributesBinding`.
- Access to the graph is centralized through `MetroHelper.graph`.

### B. Presenter & ViewModel Injection Patterns
1. **Un-parameterized Presenters / ViewModels** (e.g. `FeedPresenter`, `UnifiedSearchPresenter`):
   - Migrated to standard `@Inject constructor(...)`.
   - Exposed as factories on `ApplicationGraph`.
2. **Parameterized Presenters / ViewModels** (e.g. `PhotoDetailsPresenter`, `CollectionDetailPresenter`, `UserProfilePresenter`):
   - Use `@Assisted` injection for runtime parameters (e.g. `photoId`, `collectionId`, `username`).
   - Define dedicated `@AssistedFactory` interfaces exposed via `ApplicationGraph`.

---

## 4. Platform Integration Guidelines

### Android (`androidApp`)
- Render views using pure Jetpack Compose following Material 3 guidelines.
- Navigation must use **Jetpack Navigation 3** (`NavBackStack`, `NavDisplay`, `NavKey`).
- Scope presenters and `MviViewModel` instances to navigation back-stack entries using `rememberEntryPresenter` and `rememberViewModelStoreNavEntryDecorator()`.
- Collect state via `collectAsStateWithLifecycle()` and route user events into `viewModel.dispatch(intent)`.

### Apple (`iosApp` & `macosApp`)
- Render views using pure SwiftUI following Apple Human Interface Guidelines (HIG) and `GlassTheme`.
- Platform ViewModel wrappers bridge Kotlin `StateFlow` to `@Published` or Swift `@Observable` properties on `@MainActor`.
- Invoke `presenter.clear()` or `viewModel.clear()` inside `deinit` to cancel background coroutines upon screen dismissal.
