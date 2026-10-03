# Testing and Coverage Rules

## Test Strategy

- Prefer integration and package-level tests over brittle class-by-class assertions.
- Exercise real shared presenter flows and repository boundaries instead of private helpers or internal state transitions.
- Assert observable behavior such as loaded photos, selected topic, refresh results, and error states.
- Avoid tests that depend on implementation details, helper call order, or private fields.

## What to Test

- High-value user flows in `shared/commonMain`, especially presenter/state behavior.
- The shared feed, search, detail, and collection flows that represent the app’s main behavior surface.
- Refresh, pagination, topic switching, and error recovery through the public presenter interface.

## How to Add Tests

1. Add tests under `shared/src/commonTest/...` for shared KMP behavior.
2. Use the project's DI-driven coroutine test harness (`TestDispatcherProvider` and `TestPresenterScopeFactory`) along with a fake repository (`FakeUnsplashRepository`):
   ```kotlin
   val testDispatcher = StandardTestDispatcher()
   val dispatcherProvider = TestDispatcherProvider(testDispatcher)
   val presenterScopeFactory = TestPresenterScopeFactory(dispatcherProvider)
   val repository = FakeUnsplashRepository()
   val presenter = FeedPresenter(repository, presenterScopeFactory, dispatcherProvider)
   ```
3. Drive the behavior through the real presenter or state holder, then assert on emitted state.
4. Keep assertions broad and outcome-based rather than verifying private implementation details.
5. Note that platform UIs (`androidApp`, `iosApp`, `macosApp`) are thin declarative shells without dedicated platform test targets in `project.yml` or Gradle; `shared/src/commonTest` (`./gradlew :shared:allTests`) serves as the primary automated regression gate.

## Verification

- Run `./gradlew :shared:allTests` after adding or changing shared behavior tests.
- If a change affects Android or Apple UI shells, also run the relevant platform build task to ensure the integration path still works.
