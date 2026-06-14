# Test Coverage Strategy for Integration and Package-Level Behavior

This project now has a lightweight test foundation focused on high-value, user-visible behavior rather than class-by-class assertions.

## Objective

Add coverage that validates the app at the package and integration level:

- the shared feed presenter loads topics and the first page of photos
- the refresh flow replaces stale content with fresh content
- the topic-switch flow loads the selected topic without relying on internal implementation details

## Test Design Principles

1. Prefer package-level tests over unit tests for individual classes.
2. Drive the tests through real presenter flows and a fake repository boundary.
3. Assert observable outcomes such as loaded photos, selected topic, and refresh state.
4. Avoid assertions about private state, helper calls, or implementation mechanics.

## Recommended Test Shape

- `shared/src/commonTest/...` for Kotlin Multiplatform tests that run across the shared module.
- Fake repository implementations to simulate the network boundary.
- `FeedPresenter` tests as the main integration seam, because it represents the app’s shared behavior layer.

## Verification Command

Run the following command after changes to the shared layer:

- `./gradlew :shared:allTests`

This keeps the test suite focused on app behavior and makes future regression coverage easy to extend without adding brittle assertions.
