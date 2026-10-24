---
name: kmp-testing
description: >-
  Author, refactor, and execute integration and unit tests in shared KMP commonTest.
  Use when adding tests for MVI ViewModels, state flows, or fake repository interactions.
---

# KMP Shared Testing Strategy & Scaffolding Runbook

## Purpose & Triggers

This skill defines the testing philosophy, test harness patterns, and execution procedures for the shared Kotlin Multiplatform module (`shared`).
Use this skill when:
- Authoring new unit or integration tests for shared logic in `shared/src/commonTest`.
- Refactoring shared MVI ViewModels, UI state, intents, or effect flows.
- Verifying coroutine lifecycle management, job cancellation, or error recovery.
- Updating or expanding fake repository fixtures.

---

## 1. Testing Philosophy & Architecture

### Integration & Package-Level Testing
- Prefer integration and package-level tests over brittle class-by-class unit tests.
- Drive tests through the public `MviViewModel` interface via `viewModel.dispatch(intent)` and observe emitted states and effects.
- Assert observable outcomes (e.g., loaded photos, selected topic, pagination state, error messages, refresh indicators).
- **Prohibited**: Asserting against private implementation details, helper method invocation counts, or intermediate private state transitions.

### Platform Boundary & Test Targets
- The platform UI modules (`androidApp`, `iosApp`, `macosApp`) are thin declarative shells (Compose and SwiftUI) without dedicated unit test targets in `project.yml` or Gradle.
- The shared test suite in `shared/src/commonTest` (`./gradlew :shared:allTests`) serves as the primary automated regression gate for business logic, pagination, filtering, and state management.
- `:shared:allTests` includes `macosArm64Test`; run `./gradlew :shared:macosArm64Test` alone for a fast host-native Kotlin/Native check.

---

## 2. DI & Coroutine Test Scaffolding for MVI ViewModels

Shared ViewModels extend `MviViewModel` and run asynchronous workloads within `viewModelScope`.

### Canonical Test Fixture Reference
Do not duplicate test fixture boilerplate in documentation. Reference the living, compiler-verified test suite in [`FeedViewModelTest.kt`](shared/src/commonTest/kotlin/com/example/imagefeed/presentation/feed/FeedViewModelTest.kt) as the canonical pattern for MVI ViewModel tests.

Key scaffolding patterns:
- **Coroutines & Dispatcher Scoping**: Set up `Dispatchers.setMain(testDispatcher)` in `@BeforeTest` and `Dispatchers.resetMain()` in `@AfterTest`.
- **Deterministic Scheduling**: Advance asynchronous coroutines deterministically using `advanceUntilIdle()` or `testScheduler.runCurrent()`.
- **Fakes over Mocks**: Use `FakeUnsplashRepository` (configured with seed topics, collections, or photos) rather than dynamic mocking frameworks.
- **Teardown**: Optionally invoke `viewModel.clear()` at the end of tests to verify clean coroutine cancellation and prevent cross-test job leakage.

---

## 3. What and How to Test

### High-Value User Flows
Always prioritize end-to-end user journeys within `shared/commonMain`:
1. **Initial Load & Pagination**: Verify initial data loads on creation or initial intent and that `LoadNextPage` appends items without duplicating.
2. **Category / Topic Switching**: Verify that switching topics resets the photo list and updates the selected filter cleanly.
3. **Pull-to-Refresh**: Verify that dispatching `Refresh` updates state flags, reloads fresh data, and clears transient error banners.
4. **Error Handling & Retry**: Verify that network failures set appropriate error messages in state or emit error side-effects, and subsequent retries restore the healthy state.
5. **Search Querying**: Verify debounce behavior, query updates, and multi-facet filtering (photos, collections, users).

### Coroutine Lifecycle & Cancellation Verification
Verify that ViewModel coroutines respect lifecycle teardown:
- In-flight network calls or jobs launched within `viewModelScope` are cancelled when `viewModel.clear()` is called.
- Confirm that no unhandled exceptions leak into the test harness upon teardown.

---

## 4. Test Execution & Verification

Run the shared test suite across all configured multiplatform test targets:
```bash
./gradlew :shared:allTests
```

If ViewModel models or state contracts were modified, also verify that platform applications compile cleanly:
```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
```
