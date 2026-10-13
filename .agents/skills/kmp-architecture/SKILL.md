---
name: kmp-architecture
description: >-
  Architectural boundaries, Shared MVI / UDF pattern, Metro compile-time DI,
  and coroutine lifecycle management across Kotlin Multiplatform and platform UI shells.
  Use when adding or refactoring business logic, ViewModels, dependency injection,
  or coroutine scopes.
---

# Kotlin Multiplatform Architecture & Lifecycle Runbook

## Purpose & Triggers

This skill defines the architectural boundaries, design patterns, dependency injection standards, and coroutine lifecycle rules for this repository.
Use this skill when:
- Designing or modifying shared business logic, data models, or repositories in `shared/commonMain`.
- Creating or refactoring shared MVI ViewModels, state, intents, effects, or state flows.
- Configuring compile-time dependency injection with Metro (`dev.zacsweers.metro`).
- Managing coroutine scopes, structured concurrency, or ViewModel teardown.
- Bridging shared Kotlin code to Android Compose or Apple SwiftUI view shells.

---

## 1. Core Architectural Boundaries

The application follows a strict Kotlin Multiplatform (KMP) division of responsibilities:

### A. Shared Business Logic & Modularization Migration
All core logic MUST reside in shared Kotlin code. The repository is executing a phased modularization migration from a monolithic `:shared` module to **Vertical Feature Modules with an Umbrella Framework** (see [`specs/researches/modularization_evaluation.md`](specs/researches/modularization_evaluation.md) and Specs [46](specs/46_modularization_foundation_core_extraction.md)–[49](specs/49_modularization_umbrella_and_linking_optimization.md)):
- **Foundational Core Modules**: `:core:model`, `:core:mvi`, `:core:network`.
- **Vertical Feature Modules**: `:feature:feed`, `:feature:details`, `:feature:search`, `:feature:collections`, `:feature:profile` (owning their repositories and API clients).
- **Umbrella Assembly & Gateway**: `:shared` compiles the unified Apple native framework and aggregates the root Metro DI graph.
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

## 2. Shared MVI & Unidirectional Data Flow (UDF) Pattern

All presentation logic in this repository is built upon **Shared MVI / Unidirectional Data Flow (UDF)** centered around [`MviViewModel`](shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt):

### A. Core MVI Components
- **State**: [`UiState`](shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiState.kt) represents immutable visual conditions streamed via `state: StateFlow<S>` and `iosState: CommonFlow<S>`.
- **Intent**: [`UiIntent`](shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiIntent.kt) represents user interactions and lifecycle actions dispatched via `viewModel.dispatch(intent)`.
- **Effect**: [`UiEffect`](shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiEffect.kt) represents one-off side effects (navigation commands, toast messages, sensory haptics) emitted through buffered channels exposed via `effects: Flow<E>` and `iosEffects: CommonFlow<E>`.

### B. Coroutine Lifecycles & Structured Concurrency
- `MviViewModel` extends the official multiplatform `androidx.lifecycle.ViewModel`.
- All asynchronous workloads (networking, debounced flows, search queries) are launched strictly within `viewModelScope`.
- When a screen is popped or dismissed, `viewModelScope` is cancelled automatically via `onCleared()`.
- Darwin runtimes (SwiftUI) invoke `viewModel.clear()` inside `deinit` to deterministically trigger job cancellation and effect channel closure.

---

## 3. Compile-Time Dependency Injection (Metro)

Dependency injection is handled at compile time using **Metro** (`dev.zacsweers.metro`). Dynamic, reflection-based DI frameworks (like Koin) are strictly prohibited.

### A. Graph & Scoping Architecture
- The root dependency container is defined via `@DependencyGraph interface ApplicationGraph` in `shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt`.
- Application-level singletons are scoped using `@SingleIn(AppScope::class)`.
- Core networking and repository bindings are declared in `AppModule`.
- Access to the graph is centralized through `MetroHelper.graph`.

### B. ViewModel Injection Patterns
1. **Un-parameterized ViewModels** (e.g. `FeedViewModel`, `CollectionsFeedViewModel`, `UnifiedSearchViewModel`, `RandomPhotoViewModel`):
   - Defined with `@Inject constructor(...)`.
   - Exposed directly on `ApplicationGraph` as properties (e.g., `val feedViewModel: FeedViewModel`).
2. **Parameterized ViewModels** (e.g. `PhotoDetailsViewModel`, `CollectionDetailViewModel`, `UserProfileViewModel`):
   - Use `@AssistedInject` and `@Assisted` injection for runtime parameters (e.g. `photoId`, `collectionId`, `username`).
   - Define dedicated `@AssistedFactory` interfaces exposed on `ApplicationGraph` (e.g., `val photoDetailsViewModelFactory: PhotoDetailsViewModel.Factory`).

---

## 4. Platform Integration Guidelines

### Android (`androidApp`)
- Render views using pure Jetpack Compose following Material 3 guidelines.
- Navigation uses **Jetpack Navigation 3** (`NavBackStack`, `NavDisplay`, `NavKey`).
- Scope ViewModels using standard Compose `viewModel(key = ...) { MetroHelper.get...() }` within `rememberViewModelStoreNavEntryDecorator()`.
- Observe state via `collectAsStateWithLifecycle()` and collect effects via `LaunchedEffect`.
- Route UI actions directly into `viewModel.dispatch(intent)`.

### Apple (`iosApp` & `macosApp`)
- Render views using pure SwiftUI following Apple Human Interface Guidelines (HIG) and `GlassTheme`.
- Platform ViewModel wrappers bridge Kotlin `iosState` and `iosEffects` to Swift `@Observable` properties on `@MainActor`.
- Invoke `viewModel.clear()` inside `deinit` to cancel background coroutines upon screen dismissal.
