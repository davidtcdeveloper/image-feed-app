# Specification: Android Navigation 3 Motion & Predictive Back Navigation

**Status:** Approved

## 1. Overview & Objectives

With the core lifecycle (Spec 35) and multi-stack navigation state (Spec 36) established, the user experience requires modernization in fluid motion and gesture fidelity. Currently:
1. **Legacy BackHandler in Search:** In `SearchScreen.kt` (line 125), in-screen search bar dismissals rely on the legacy `androidx.activity.compose.BackHandler(enabled = state.isSearchActive)`. As outlined in the `navigation-event` skill, apps targeting `compileSdk 36+` (this app targets SDK 37) should utilize `NavigationBackHandler` with `rememberNavigationEventState` to support smooth back progress interception and predictive gestures without callback registration overhead.
2. **Missing Explicit Motion Transitions:** `NavDisplay` currently relies on unconfigured default transitions. While screen changes occur, they lack the polished choreography prescribed by Material 3 Motion and the Navigation 3 Animations recipe, such as coordinated horizontal slide-and-fade for forward navigation, reverse slide for back pops, and progress-driven interactive predictive back transforms.

This specification outlines the technical plan to upgrade `SearchScreen`'s back gesture interception to `NavigationEvent` and introduce full Material 3 Motion transitions across `NavDisplay`.

---

## 2. Technical Specifications & Architecture

### A. NavigationEvent Migration in SearchScreen (`navigation-event` Skill)
* **Target Environment**:
  - `compileSdk = 37` (Already set in `androidApp/build.gradle.kts`).
  - `android:enableOnBackInvokedCallback="true"` (Already configured in `AndroidManifest.xml`).
  - `ComponentActivity` automatically implements `NavigationEventDispatcherOwner`.
* **Issue with Legacy BackHandler**:
  `BackHandler` from `androidx.activity.compose` is a binary trigger that fires only upon gesture release (`onBackPressed`), ignoring real-time predictive touch tracking.
* **Migration Plan**:
  In `SearchScreen.kt`:
  ```kotlin
  import androidx.navigationevent.compose.NavigationBackHandler
  import androidx.navigationevent.compose.rememberNavigationEventState
  import androidx.navigationevent.NavigationEventInfo

  // Inside SearchScreen:
  val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

  NavigationBackHandler(
      state = navigationState,
      isBackEnabled = state.isSearchActive,
      onBackCompleted = {
          presenter.deactivateSearch()
      }
  )
  ```
* **Guard against Known Failure Modes**:
  - **No Duplicate Handlers:** Maintain a strict 1:1 mapping between `NavigationEventState` and `NavigationBackHandler` (avoid runtime `IllegalArgumentException`).
  - **No Manual CompositionLocal Wrapping:** Do not wrap in manual `LocalNavigationEventDispatcherOwner`—`ComponentActivity` handles resolution automatically.

### B. Material 3 NavDisplay Transition Specifications (Animations Recipe)
Configure standard forward, backward, and predictive back animations on `NavDisplay`:

```kotlin
NavDisplay(
    entries = navigationState.toDecoratedEntries(entryProvider),
    onBack = { navigator.goBack() },
    sharedTransitionScope = sharedTransitionScope,
    modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
    transitionSpec = {
        // Forward navigation: Slide in from right with subtle fade
        (slideInHorizontally(
            initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(350))) togetherWith
        (slideOutHorizontally(
            targetOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(200)))
    },
    popTransitionSpec = {
        // Pop navigation: Slide in from left with subtle fade
        (slideInHorizontally(
            initialOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(350))) togetherWith
        (slideOutHorizontally(
            targetOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(200)))
    },
    predictivePopTransitionSpec = { swipeEdge ->
        // Predictive back: Edge-aware scaling and translation
        (slideInHorizontally(
            initialOffsetX = { fullWidth ->
                if (swipeEdge == NavigationEvent.EDGE_LEFT) -fullWidth else fullWidth
            },
            animationSpec = tween(durationMillis = 300)
        ) + fadeIn(animationSpec = tween(300))) togetherWith
        (slideOutHorizontally(
            targetOffsetX = { fullWidth ->
                if (swipeEdge == NavigationEvent.EDGE_LEFT) fullWidth else -fullWidth
            },
            animationSpec = tween(durationMillis = 300)
        ) + fadeOut(animationSpec = tween(200)))
    }
)
```

### C. Destination-Specific Transition Metadata
For modal-like flows (e.g., expanding a collection detail or photo detail), destinations can optionally override the default transitions via `NavEntry` metadata using `NavDisplay.TransitionKey`:
```kotlin
entry<AppRoute.PhotoDetails>(
    metadata = metadata {
        put(NavDisplay.TransitionKey) {
            slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(350)) togetherWith fadeOut(tween(200))
        }
    }
) { key ->
    // Destination content
}
```

---

## 3. Implementation Plan & Execution Checklist

- [ ] **Phase 1: SearchScreen NavigationEvent Migration**
  - [ ] Replace `androidx.activity.compose.BackHandler` with `NavigationBackHandler` and `rememberNavigationEventState`.
  - [ ] Verify that back press dismisses active search text input and returns to default suggestions without popping the screen.

- [ ] **Phase 2: Global NavDisplay Motion Choreography**
  - [ ] Implement `transitionSpec` (forward navigation slide + fade).
  - [ ] Implement `popTransitionSpec` (backward pop reverse slide + fade).
  - [ ] Implement `predictivePopTransitionSpec` with swipe-edge awareness.

- [ ] **Phase 3: Verification & Motion Testing**
  - [ ] Test back gesture on Android 14/15/16 emulator/device to verify smooth predictive back animation without visual glitches.
  - [ ] Verify shared element hero animations co-exist smoothly with NavDisplay slide transitions.
  - [ ] Run `./gradlew ktlintCheck detekt`.
  - [ ] Run `./gradlew :androidApp:compileDebugKotlin`.
  - [ ] Run `./gradlew :androidApp:assembleDebug`.
