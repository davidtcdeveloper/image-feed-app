# Spec 22: Presenter Coroutine Scope Lifecycle and Metro DI Hardening

## Goal

Make presenter coroutine ownership explicit and lifecycle-aware while preserving the shared-presenter architecture used by the app.

## Problem Statement

The current presenters create their own `CoroutineScope(Dispatchers.Main + SupervisorJob())` instances and are retained as app-scoped singletons through Metro. That makes the lifecycle of in-flight work ambiguous and can leave network and state updates running after a screen has already been dismissed.

This creates a few concrete issues:

- coroutines keep running after a screen is no longer active,
- state updates can reach a UI that is already torn down,
- the dependency graph does not clearly communicate which presenters are screen-scoped versus app-scoped,
- cancellation behavior is difficult to test and verify.

## Proposed Solution

### 1. Introduce explicit shared lifecycle abstractions

Add shared abstractions in `shared/commonMain` that define how presenter work is created and torn down:

- a `DispatcherProvider` for main/io testing,
- a presenter cleanup contract such as `clear()` or `close()`,
- a scope owner that can be supplied by the UI layer or a factory.

The presenters should no longer create unmanaged scopes internally.

### 2. Refactor presenters to use injected lifecycle-aware scopes

Refactor the shared presenters so that they receive their scope/dispatcher dependencies explicitly:

- `FeedPresenter`
- `CollectionsFeedPresenter`
- `UnifiedSearchPresenter`
- `RandomPhotoPresenter`
- `PhotoDetailsPresenter`
- `CollectionDetailPresenter`
- `UserProfilePresenter`

Each presenter will:

- use the injected scope and dispatcher,
- cancel its work in `clear()`/`close()`,
- stop emitting state once the presenter is disposed,
- keep the existing shared `StateFlow`-based API,
- centralize cancellation-aware state updates with shared helpers such as `CoroutineContext.isActive()` and `MutableStateFlow.updateIfActive(...)` to avoid repeating `if (!isActive())` checks around every state emission.

### 3. Move away from long-lived singleton presenter bindings

The current Metro graph exposes presenters as app-scoped singletons. That should be replaced with screen-oriented creation patterns:

- use factory-based Metro bindings for screen flows,
- create presenters at the UI boundary rather than letting them live for the app lifetime,
- preserve the public Metro helper API where possible, but make it explicit that presenters are created per screen flow.

### 4. Wire cleanup into Android and iOS wrappers

Platform code should own the lifecycle boundary and call presenter cleanup at the right time:

- Android Compose screens should create presenters in a lifecycle-aware way and clear them when the composition is disposed.
- SwiftUI/iOS view models should cancel presenters in `deinit` or a matching teardown hook.

The UI layer remains thin; it should not contain presentation logic, but it should own when the presenter is created and destroyed.

### 5. Add shared tests for cancellation and state flow behavior

Extend the shared test suite with coverage around:

- presenter cleanup cancels in-flight work,
- no state emission occurs after the presenter is cleared,
- refresh/load flows behave correctly with a test dispatcher and fake repository.

## Acceptance Criteria

- No shared presenter creates its own unmanaged coroutine scope.
- Each presenter exposes a deterministic cleanup method and cancels outstanding work when invoked.
- Metro wiring is aligned with screen-scoped presenter creation instead of app-lifetime singletons for UI flows.
- Android and iOS wrappers invoke cleanup on teardown.
- Shared tests verify that cancellation prevents further emissions and leaks.

## Verification Plan

Run the following checks after implementation:

- `./gradlew :shared:allTests`
- `./gradlew :androidApp:assembleDebug`
- `./gradlew :shared:compileKotlinIosSimulatorArm64`

## Notes

This work should stay within the existing KMP architecture: shared state logic in `shared/commonMain`, thin platform shells, and compile-time DI through Metro.
