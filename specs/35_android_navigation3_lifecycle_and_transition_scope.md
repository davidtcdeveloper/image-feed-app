# Specification: Android Navigation 3 Lifecycle & Transition Scope Hardening

**Status:** Approved

## 1. Overview & Objectives

Following an in-depth architectural audit of the Android navigation setup against the Jetpack Navigation 3 (`androidx.navigation3:1.2.0-alpha07`) and Lifecycle Navigation 3 (`androidx.lifecycle:lifecycle-viewmodel-navigation3:2.11.0`) specifications, critical defects were uncovered in two intertwined areas:
1. **Broken Shared Element Scope:** Screen composables wrap their content in a redundant, static `AnimatedVisibility(visible = true)` to synthesize an `AnimatedVisibilityScope`. Because `visible` is permanently true, transitions never animate, disconnecting `Modifier.sharedElement()` from `NavDisplay`'s actual scene animations and rendering hero transitions between Feed/Collections and Photo Details inoperative.
2. **Composition-Bound Presenter Lifecycle:** Presenters for detail and search destinations (`UnifiedSearchPresenter`, `PhotoDetailsPresenter`, `UserProfilePresenter`, `CollectionDetailPresenter`) are held via Compose `remember(key)` with immediate teardown inside `DisposableEffect { onDispose { presenter.clear() } }`. Whenever a destination is covered by a new screen on the back stack, `NavDisplay` uncomposes the covered scene, triggering premature `presenter.clear()`. Upon popping back, all state (such as typed search queries, active filters, and scroll positions) is permanently wiped and must be re-fetched. Furthermore, root `FeedPresenter` and `CollectionsFeedPresenter` are tethered to `MainActivity` as lazy properties and cleared in an activity-level `DisposableEffect`, causing complete teardown and re-fetch on every device rotation/configuration change.

This specification details the end-to-end plan to rectify these issues, along with immediate correctness hygiene (click debouncing via `dropUnlessResumed`, elimination of dead `finish()` calls, and removal of obsolete Navigation 2 dependencies).

---

## 2. Identified Issues & Detailed Technical Specifications

### A. Shared Element Hero Transition Scope Fix (Finding F1)
* **Problem**:
  In `MainActivity.kt` lines 364–517, every `NavEntry` wraps its target screen composable in:
  ```kotlin
  AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
      val animatedVisibilityScope = this
      FeedScreen(
          sharedTransitionScope = sharedTransitionScope,
          animatedVisibilityScope = animatedVisibilityScope,
          ...
      )
  }
  ```
  `visible = true` never changes. The exit and enter transitions never fire. Because `Modifier.sharedElement` requires an `AnimatedVisibilityScope` that participates in the layout container's active scene transition, nesting it inside a static `AnimatedVisibility` isolates the shared content state from `NavDisplay`'s internal `AnimatedContent`.
* **Root Cause in Prior Specs**:
  [Spec 21 (Android Fluid Animations)](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/21_android_fluid_animations_plan.md) assumed `NavEntry` provided `this@NavEntry` as an `AnimatedVisibilityScope`. In Navigation 3, `NavEntry` is a pure data/content mapping function without a receiver scope.
* **Remediation**:
  1. Leverage `androidx.navigation3.ui.LocalNavAnimatedContentScope.current`. In `navigation3-ui:1.2.0-alpha07`, `NavDisplay` automatically provides `LocalNavAnimatedContentScope` containing the ambient `AnimatedContentScope` for the active entry transition.
  2. Pass `sharedTransitionScope = this` directly to `NavDisplay` (`NavDisplay` in `1.2.0-alpha07` natively accepts `sharedTransitionScope: SharedTransitionScope?`).
  3. Remove the redundant `AnimatedVisibility(visible = true)` wrappers entirely from all entries.
  4. Pass `LocalNavAnimatedContentScope.current` as the `animatedVisibilityScope` to `FeedScreen`, `CollectionsFeedScreen`, `CollectionDetailScreen`, `SearchScreen`, `PhotoDetailsScreen`, and `UserProfileScreen`.

### B. Back-Stack Entry Scoped Presenter Lifecycle Management (Finding F2 & F12)
* **Problem**:
  - `SearchScreen.kt`: `val presenter = remember { MetroHelper.getUnifiedSearchPresenter() }` with `DisposableEffect(presenter) { onDispose { presenter.clear() } }`. When pushing `PhotoDetails`, `SearchScreen` is uncomposed, destroying the presenter. Navigating back recreates the presenter from scratch, erasing query strings, active facets, and scroll history.
  - `PhotoDetailsScreen.kt`, `UserProfileScreen.kt`, `CollectionDetailScreen.kt`: All use local `remember(key)` + `DisposableEffect` teardown. Returning to a previous detail screen triggers an expensive re-fetch of metadata, EXIF, and photos.
  - `MainActivity.kt`: `presenter` and `collectionsPresenter` are `by lazy` fields on `MainActivity`, and destroyed on `DisposableEffect(Unit)` when the activity content disposes during configuration change/rotation.
* **Remediation**:
  1. **Configure NavDisplay Decorators**:
     Incorporate `rememberViewModelStoreNavEntryDecorator()` alongside `rememberSaveableStateHolderNavEntryDecorator()` in `NavDisplay`'s `entryDecorators`.
     ```kotlin
     entryDecorators = listOf(
         rememberSaveableStateHolderNavEntryDecorator(),
         rememberViewModelStoreNavEntryDecorator()
     )
     ```
  2. **Create Platform Lifecycle Holder (`PresenterViewModel`)**:
     To comply with `AGENTS.md` ("Platform wrappers own presenter lifecycle and must trigger `presenter.clear()` upon dismissal or `deinit`"), introduce an Android `ViewModel` that wraps a KMP presenter:
     ```kotlin
     package com.example.imagefeed.android.util

     import androidx.lifecycle.ViewModel
     import androidx.lifecycle.ViewModelProvider
     import androidx.lifecycle.viewmodel.compose.viewModel

     class PresenterViewModel<P : Any>(
         val presenter: P,
         private val onClear: (P) -> Unit
     ) : ViewModel() {
         override fun onCleared() {
             super.onCleared()
             onClear(presenter)
         }

         class Factory<P : Any>(
             private val creator: () -> P,
             private val onClear: (P) -> Unit
         ) : ViewModelProvider.Factory {
             @Suppress("UNCHECKED_CAST")
             override fun <T : ViewModel> create(modelClass: Class<T>): T {
                 return PresenterViewModel(creator(), onClear) as T
             }
         }
     }
     ```
  3. **Provide Composable Presenter Scoping Helper**:
     ```kotlin
     @Composable
     inline fun <reified P : Any> rememberEntryPresenter(
         key: String? = null,
         noinline onClear: (P) -> Unit = { /* reflection-free clear or interface */ },
         crossinline creator: () -> P
     ): P {
         val factory = remember(key) {
             PresenterViewModel.Factory(
                 creator = { creator() },
                 onClear = { onClear(it) }
             )
         }
         val vm: PresenterViewModel<P> = viewModel(key = key, factory = factory)
         return vm.presenter
     }
     ```
  4. **Migrate Screens to Entry-Scoped Presenters**:
     - `FeedScreen`: Scoped to `Screen.Feed` via `rememberEntryPresenter(key = "feed", onClear = { it.clear() }) { MetroHelper.getFeedPresenter() }`. Survives rotation and tab switches.
     - `CollectionsFeedScreen`: Scoped to `Screen.Collections` via `rememberEntryPresenter(key = "collections", onClear = { it.clear() }) { MetroHelper.getCollectionsFeedPresenter() }`.
     - `SearchScreen`: Scoped to `Screen.Search` via `rememberEntryPresenter(key = "search", onClear = { it.clear() }) { MetroHelper.getUnifiedSearchPresenter() }`. Preserves search state during photo inspection.
     - `PhotoDetailsScreen`: Scoped to `Screen.PhotoDetails(photoId)` with `key = photoId`.
     - `UserProfileScreen`: Scoped to `Screen.UserProfile(username)` with `key = username`.
     - `CollectionDetailScreen`: Scoped to `Screen.CollectionDetails(collectionId)` with `key = collectionId`.
  5. **Duplicate Key Safeguard (F12)**:
     If a navigation flow pushes the same route key twice (e.g., `PhotoDetails("123") -> UserProfile -> PhotoDetails("123")`), Navigation 3 requires distinct identities if their state/ViewModel stores should be independent. Ensure navigation keys support unique identity if recursive navigation occurs, or document that recursive navigation to identical IDs intentionally shares the existing cached ViewModel store.

### C. Eliminate Dead Root Back Press Handling (Finding F4)
* **Problem**:
  In `MainActivity.kt` lines 349–355:
  ```kotlin
  onBack = {
      if (backStackState.size > 1) {
          backStackState.removeAt(backStackState.size - 1)
      } else {
          finish()
      }
  }
  ```
  In Navigation 3 (`NavigationEventHandler.kt`), `isBackEnabled` is evaluated as `sceneState.currentScene.previousEntries.isNotEmpty()`. When only the root entry remains (`size == 1`), `isBackEnabled` is `false`. The dispatcher delegates directly to Android's system back dispatcher, meaning `finish()` is never reached and duplicate manual calls to `finish()` are dead code.
* **Remediation**:
  Simplify `onBack` on `NavDisplay` to `backStackState.removeLastOrNull()`, allowing the system and Predictive Back to handle activity exit naturally when the root is reached.

### D. Navigation Click Debouncing with `dropUnlessResumed` (Finding F5)
* **Problem**:
  Rapid double-taps on photo cards, collection tiles, or search suggestions invoke `backStackState.add(...)` multiple times in the same frame, pushing duplicate screens onto the back stack.
* **Remediation**:
  In accordance with official Navigation 3 recipes and Google's Compose guidelines, wrap navigation callbacks at invocation points with `androidx.lifecycle.compose.dropUnlessResumed`:
  ```kotlin
  onPhotoClick = dropUnlessResumed { photo ->
      backStackState.add(Screen.PhotoDetails(photo.id))
  }
  ```
  Since Navigation 3 assigns each `NavEntry` its own `LifecycleOwner` via `BackStackAwareLifecycleNavEntryDecorator`, when the top entry transitions to `PAUSED`/`STOPPED`, subsequent click events are automatically dropped until the new destination is fully resumed.

### E. Clean Obsolete Navigation 2 Dependencies (Finding F8)
* **Problem**:
  `gradle/libs.versions.toml` and `androidApp/build.gradle.kts` still declare `libs.androidx.navigation.compose` (`androidx.navigation:navigation-compose:2.9.8`), violating the `AGENTS.md` core rule: *"Android Navigation: Use Jetpack Navigation 3... Do not use legacy NavController or Navigation 2 XML / Compose graph setups."*
* **Remediation**:
  1. Remove `androidx-navigation = "2.9.8"` and `androidx-navigation-compose` from `gradle/libs.versions.toml`.
  2. Remove `implementation(libs.androidx.navigation.compose)` from `androidApp/build.gradle.kts`.

---

## 3. Implementation Plan & Execution Checklist

- [ ] **Phase 1: Dependency Cleanup**
  - [ ] Remove `androidx-navigation-compose` from `gradle/libs.versions.toml` and `androidApp/build.gradle.kts`.
  - [ ] Verify Gradle sync succeeds with host permissions (`BypassSandbox: true`).

- [ ] **Phase 2: Transition Scope & Hero Animation Realignment**
  - [ ] Pass `sharedTransitionScope = this` directly to `NavDisplay` in `MainActivity.kt`.
  - [ ] Remove all 6 `AnimatedVisibility(visible = true)` wrapping blocks inside the `NavDisplay` entry provider.
  - [ ] Pass `LocalNavAnimatedContentScope.current` as `animatedVisibilityScope` to screen composables.
  - [ ] Verify that photo card-to-detail hero transitions animate seamlessly.

- [ ] **Phase 3: Back-Stack Scoped Presenter Infrastructure**
  - [ ] Add `rememberViewModelStoreNavEntryDecorator()` to `NavDisplay.entryDecorators`.
  - [ ] Create `PresenterViewModel.kt` in `androidApp/src/main/java/com/example/imagefeed/android/util/PresenterViewModel.kt`.
  - [ ] Implement `rememberEntryPresenter` helper.
  - [ ] Refactor `FeedScreen`, `CollectionsFeedScreen`, `SearchScreen`, `PhotoDetailsScreen`, `UserProfileScreen`, and `CollectionDetailScreen` to consume entry-scoped presenters.
  - [ ] Remove `presenter` and `collectionsPresenter` lazy properties and root `DisposableEffect` from `MainActivity.kt`.

- [ ] **Phase 4: Click Debouncing & Back Handling Cleanup**
  - [ ] Replace custom `finish()` branch in `NavDisplay.onBack` with `backStackState.removeLastOrNull()`.
  - [ ] Wrap all navigation click triggers in `dropUnlessResumed`.

- [ ] **Phase 5: Verification & Zero-Warning Validation**
  - [ ] Run `./gradlew ktlintCheck detekt`.
  - [ ] Run `./gradlew :androidApp:compileDebugKotlin` and verify zero compiler warnings.
  - [ ] Run `./gradlew :androidApp:assembleDebug`.
  - [ ] Run `./gradlew :shared:allTests`.
