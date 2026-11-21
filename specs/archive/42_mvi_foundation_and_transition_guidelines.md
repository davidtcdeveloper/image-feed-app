# Specification 42: Shared MVI Foundation & Architecture Transition Guidelines

**Status:** Implemented

## 1. Overview & Problem Statement

The image-feed-app repository is migrating its presentation tier from the custom Shared Presenter pattern to **Shared Model-View-Intent (MVI) / Unidirectional Data Flow (UDF)** built on official multiplatform `androidx.lifecycle.ViewModel` in `shared/commonMain` (as established in [`specs/researches/architecture_evaluation_mvvm_mvi.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/researches/architecture_evaluation_mvvm_mvi.md#L202-L244)).

To ensure continuous development velocity and prevent breaking existing screens, this migration requires:
1. Adding official multiplatform `androidx.lifecycle:lifecycle-viewmodel` to `shared/commonMain`.
2. Establishing lightweight, boilerplate-free base contracts (`UiState`, `UiIntent`, `UiEffect`, `MviViewModel`).
3. Formally documenting an **Active Transition State** in [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) and [`.agents/skills/kmp-architecture/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/kmp-architecture/SKILL.md) so both legacy Presenters and new MVI ViewModels coexist cleanly during migration, with clear rules for ongoing work.

---

## 2. Technical Architecture & Component Design

### A. Dependency Configuration
- Add `androidx-lifecycle-viewmodel` (already defined in [`gradle/libs.versions.toml`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/gradle/libs.versions.toml)) as an `implementation` dependency in `shared/build.gradle.kts` under `commonMain.dependencies`.

### B. Core MVI Contracts (`shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/`)
1. **Marker Interfaces**:
   - `UiState`: Marker interface for immutable screen state data classes.
   - `UiIntent`: Marker interface for typed user actions and interactions.
   - `UiEffect`: Marker interface for single-shot, ephemeral side effects (navigation commands, snackbars, haptic triggers).

2. **Base ViewModel (`MviViewModel`)**:
   ```kotlin
   abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
       initialState: S,
   ) : ViewModel() {
       private val _state = MutableStateFlow(initialState)
       val state: StateFlow<S> = _state.asStateFlow()
       val iosState: CommonFlow<S> = CommonFlow(state)

       private val _effects = Channel<E>(Channel.BUFFERED)
       val effects: Flow<E> = _effects.receiveAsFlow()
       val iosEffects: CommonFlow<E> = CommonFlow(effects)

       abstract fun dispatch(intent: I)

       protected fun updateState(transform: (S) -> S) {
           _state.update(transform)
       }

       protected fun sendEffect(effect: E) {
           viewModelScope.launch {
               _effects.send(effect)
           }
       }

       fun clear() {
           // Darwin-friendly hook for Swift deinit triggers
           // Cleans up any resources before ViewModelStore clearing
       }
   }
   ```

### C. Transition State Rules for `AGENTS.md` and `kmp-architecture/SKILL.md`
The guideline documents will be updated with an explicit **Architecture Transition Phase** section:
- **Coexistence Policy**: Legacy Presenters (`*Presenter`) and new MVI ViewModels (`*ViewModel`) coexist safely in `shared/commonMain`.
- **New Feature Invariant**: Any newly authored presentation logic or screen component MUST use `MviViewModel` and follow UDF (`State`, `Intent`, `Effect`).
- **Teardown Parity**:
  - Legacy Presenters require manual `presenter.clear()`.
  - MVI ViewModels rely on `viewModelScope` and `onCleared()`; on iOS/macOS, Swift `@Observable` wrappers invoke `viewModel.clear()` inside `deinit`.
- **Dependency Injection Invariant**: [`ApplicationGraph`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt) in Metro DI provides both legacy Presenter factories and MVI ViewModel factories during the transition.

---

## 3. Implementation Steps

- [x] **Step 1: Update Build Dependencies**
  - Add `libs.androidx.lifecycle.viewmodel` to `commonMain.dependencies` in [`shared/build.gradle.kts`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/build.gradle.kts).
  - Verify Gradle sync and shared module compilation.

- [x] **Step 2: Implement Base MVI Infrastructure**
  - Create package `com.example.imagefeed.presentation.mvi`.
  - Implement `UiState.kt`, `UiIntent.kt`, `UiEffect.kt`, and `MviViewModel.kt`.
  - Ensure zero compilation warnings and proper Swift export visibility.

- [x] **Step 3: Update `AGENTS.md` with Transition Architecture Rules**
  - Add "Active Transition State: Presenter to MVI ViewModel" to Core Invariants.
  - Specify boundaries for both legacy presenters and modern MVI ViewModels.

- [x] **Step 4: Update `.agents/skills/kmp-architecture/SKILL.md`**
  - Add transition section describing `MviViewModel`, `UiState`, `UiIntent`, and `UiEffect`.
  - Detail how Compose and SwiftUI observe state and dispatch intents during transition.

- [x] **Step 5: Verification & Linting**
  - Execute `./gradlew ktlintCheck detekt` to guarantee hygiene compliance.
  - Verify dual-platform compilation (`./gradlew :androidApp:assembleDebug` and iOS xcodebuild).

---

## 4. Acceptance Criteria

- [x] Multiplatform `androidx.lifecycle.ViewModel` is available in `shared/commonMain`.
- [x] `MviViewModel<S, I, E>` is compiled and tested with reactive state updates and buffered effect channels.
- [x] [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) and [`.agents/skills/kmp-architecture/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/kmp-architecture/SKILL.md) clearly document the coexistence rules and transition standards.
- [x] Existing presenter code remains 100% functional with zero regressions.
- [x] Both Android and iOS project builds succeed cleanly with zero warnings.
