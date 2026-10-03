---
name: kmp-testing
description: >-
  Author, refactor, and execute integration and unit tests in shared KMP commonTest.
  Use when adding tests for presenters, state flows, or fake repository interactions.
---

# KMP Shared Testing Strategy & Scaffolding Runbook

## Purpose & Triggers

This skill defines the testing philosophy, test harness patterns, and execution procedures for the shared Kotlin Multiplatform module (`shared`).
Use this skill when:
- Authoring new unit or integration tests for shared logic in `shared/src/commonTest`.
- Refactoring shared presenters, view models, or state flows.
- Verifying coroutine lifecycle management, job cancellation, or error recovery.
- Updating or expanding fake repository fixtures.

---

## 1. Testing Philosophy & Architecture

### Integration & Package-Level Testing
- Prefer integration and package-level tests over brittle class-by-class unit tests.
- Drive tests through the public presenter or state-holder interface and observe emitted states.
- Assert observable outcomes (e.g., loaded photos, selected topic, pagination state, error messages, refresh indicators).
- **Prohibited**: Asserting against private implementation details, helper method invocation counts, or intermediate private state transitions.

### Platform Boundary & Test Targets
- The platform UI modules (`androidApp`, `iosApp`, `macosApp`) are thin declarative shells (Compose and SwiftUI) without dedicated unit test targets in `project.yml` or Gradle.
- The shared test suite in `shared/src/commonTest` (`./gradlew :shared:allTests`) serves as the primary automated regression gate for business logic, pagination, filtering, and state management.

---

## 2. DI-Driven Presenter Test Scaffolding

Shared presenters rely on compile-time Metro DI and injected coroutine lifecycle abstractions (`PresenterScope` / `PresenterScopeFactory` and `DispatcherProvider`).

### Canonical Test Fixture Setup
When authoring presenter tests, construct the presenter using the project's test fixtures:

```kotlin
import com.example.imagefeed.model.Topic
import com.example.imagefeed.presentation.FeedPresenter
import com.example.imagefeed.presentation.TestPresenterScopeFactory
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedPresenterTest {

    @Test
    fun loadsInitialFeedAndTopics() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val presenterScopeFactory = TestPresenterScopeFactory(testDispatcher)
        val repository = FakeUnsplashRepository(
            topics = listOf(Topic("t1", "nature", "Nature", totalPhotos = 2)),
            photosByPage = mapOf("editorial" to mapOf(1 to listOf(photo("p1"), photo("p2")))),
        )

        val presenter = FeedPresenter(
            repository = repository,
            presenterScopeFactory = presenterScopeFactory,
        )

        advanceUntilIdle()

        val state = presenter.state.value
        assertFalse(state.isLoading)
        assertTrue(state.photos.isNotEmpty())
        assertEquals("editorial", state.selectedTopicSlug)

        presenter.clear()
    }
}
```

---

## 3. What and How to Test

### High-Value User Flows
Always prioritize end-to-end user journeys within `shared/commonMain`:
1. **Initial Load & Pagination**: Verify initial data loads on creation and that `loadNextPage()` appends items without duplicating.
2. **Category / Topic Switching**: Verify that switching topics resets the photo list and updates the selected filter cleanly.
3. **Pull-to-Refresh**: Verify that invoking `refresh()` updates state flags, reloads fresh data, and clears transient error banners.
4. **Error Handling & Retry**: Verify that network failures set appropriate error messages in state, and subsequent retries restore the healthy state.
5. **Search Querying**: Verify debounce behavior, query updates, and multi-facet filtering (photos, collections, users).

### Coroutine Lifecycle & Cancellation Verification
Verify that presenter coroutines respect lifecycle teardown:
- In-flight network calls or jobs launched within `presenterScope` must be cancelled when `presenter.clear()` is called.
- Presenters must guard updates using `CoroutineContext.isActive()` or `MutableStateFlow.updateIfActive(...)`.
- Confirm that no state updates emit after `clear()` has been invoked.

---

## 4. Test Execution & Verification

Run the shared test suite across all configured multiplatform test targets:
```bash
./gradlew :shared:allTests
```

If presenter models or state contracts were modified, also verify that platform applications compile cleanly:
```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
```
