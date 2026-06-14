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
2. Use a fake repository or test double at the network boundary to keep the tests deterministic.
3. Drive the behavior through the real presenter or state holder, then assert on emitted state.
4. Keep assertions broad and outcome-based rather than verifying private implementation details.

## Verification

- Run `./gradlew :shared:allTests` after adding or changing shared behavior tests.
- If a change affects Android or Apple UI shells, also run the relevant platform build task to ensure the integration path still works.
