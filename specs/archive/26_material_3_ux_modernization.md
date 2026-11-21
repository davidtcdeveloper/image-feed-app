# Specification: Material 3 Modern UX Capabilities (SearchBar, PullToRefresh, Motion & Haptics)

**Status:** Implemented

## Overview
This specification delivers modern Material 3 user experience capabilities to the Android application, building upon the foundational theme (`specs/24_material_3_design_foundation.md`) and screen tokenization (`specs/25_material_3_screen_tokenization.md`). 

The primary goals are:
1. Elevate search with Material 3 `SearchBar` (compact) and `DockedSearchBar` (medium/expanded) using a **single source of truth** in `UnifiedSearchPresenter` / `SearchState`—strictly avoiding local `remember` state variables for search expansion or active modes.
2. Introduce native Material 3 `PullToRefreshBox` across all main feeds, extending `UnifiedSearchPresenter` with `refresh()` and `isRefreshing` to maintain consistent KMP state across platforms.
3. Integrate TopAppBar scroll behaviors (`pinnedScrollBehavior` for tabbed feeds; `enterAlwaysScrollBehavior` for photo collection grids) with dynamic tonal elevation.
4. Implement refined Material 3 haptic feedback distinguishing subtle tactile ticks from heavier confirmation/download actions.
5. Enable system predictive back gesture readiness on Android 14+ (API 34/35+) via `AndroidManifest.xml` and coordinated `BackHandler` routing.

---

## Detailed Feature Specifications

### 1. Modern Material 3 SearchBar & DockedSearchBar (`SearchScreen.kt`)
*   **Problem**: Currently, search is an ad-hoc `Row` containing a `TextField` with hardcoded `52.dp` height and manual trailing clear icons. It does not leverage Material 3's animated search transitions or responsive docked behaviors.
*   **State Architecture (Single Source of Truth in Shared Presenter)**:
    *   Do NOT maintain separated local UI properties like `var isSearchActive by remember { mutableStateOf(...) }`.
    *   Update [`SearchState`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/UnifiedSearchPresenter.kt) in `shared/commonMain` to include:
        ```kotlin
        data class SearchState(
            val query: String = "",
            val isSearchActive: Boolean = false,
            val isRefreshing: Boolean = false,
            // ... remaining fields
        )
        ```
    *   Expose explicit transition functions on [`UnifiedSearchPresenter`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/UnifiedSearchPresenter.kt):
        *   `fun setSearchActive(active: Boolean)`: Toggles search expansion/focus state.
        *   `fun submitSearch(query: String)`: Updates query, collapses `isSearchActive = false`, and executes the search.
    *   When navigating to search via a clicked tag (`initialQuery.isNotEmpty()`), `isSearchActive` remains `false` so photo results display immediately without hijacking the screen with suggestions.
*   **Predictive Back & Collapse Navigation**:
    *   Bind a Compose `BackHandler` directly to the presenter state:
        ```kotlin
        BackHandler(enabled = state.isSearchActive) {
            presenter.setSearchActive(false)
        }
        ```
    *   When active, pressing the system back gesture or tapping the leading back icon collapses the search bar via `presenter.setSearchActive(false)`.
    *   When inactive (`state.isSearchActive == false`), back navigation pops back to the previous screen via the root `onBack()` callback.
*   **Adaptive Layout Behavior**:
    *   Observe `LocalAdaptiveLayoutInfo.current.windowSizeClass.windowWidthSizeClass`.
    *   **Compact Screens (< 600dp width)**: Render M3 `SearchBar` which expands smoothly to full screen when `state.isSearchActive` is true, hosting `SearchSuggestionsAndHistory` inside the expanded content slot.
    *   **Medium & Expanded Screens (≥ 600dp width)**: Render M3 `DockedSearchBar` anchored at the top, allowing the results grid underneath to remain visible while typing and displaying suggestions in an anchored floating surface.
*   **Trailing Icons & Filter Sheet**:
    *   The trailing icon slot renders a `Row` accommodating:
        1. Clear icon (`Icons.Default.Clear`): Visible when `state.query.isNotEmpty()`, invoking `presenter.updateQuery("")`.
        2. Filter icon (`Icons.AutoMirrored.Filled.List`): Opens the existing `ModalBottomSheet` filter menu (`showFiltersSheet = true`), highlighted with `MaterialTheme.colorScheme.primary` when non-default filters are active.
*   **Category Tabs Placement**:
    *   `SecondaryTabRow` (Photos, Collections, Users) displays beneath the search bar when `state.isSearchActive` is false, switching view modes over search results.

---

### 2. Standard Material 3 Pull-to-Refresh (`PullToRefreshBox`)
*   **Problem**: Users currently cannot pull down to refresh feeds and search results.
*   **Shared Presenter Support**:
    *   `FeedPresenter` and `CollectionsFeedPresenter` already expose `refresh()` and `isRefreshing`.
    *   Add `fun refresh()` to `UnifiedSearchPresenter` and `isRefreshing: Boolean` to `SearchState` so pulling search results re-queries active filters without resetting existing items or showing disruptive skeleton loaders.
*   **Integration Targets**:
    1. **Feed Screen (`MainActivity.kt`)**: Wrap `LazyVerticalStaggeredGrid` with `PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = { presenter.refresh() })`. Pulling refreshes the currently active topic or editorial feed.
    2. **Collections Feed (`CollectionsFeedScreen.kt`)**: Wrap collection grid with `PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = { presenter.refresh() })`.
    3. **Search Results (`SearchScreen.kt`)**: Wrap results container with `PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = { presenter.refresh() })` when not in suggestions mode.
*   **App Bar Actions Distinction**:
    *   Retain the `IconButton` in `FeedScreen`'s top app bar with `contentDescription = "Randomize"` and action `{ handleShake() }`. It serves as the accessible desktop and UI fallback for the physical shake gesture. To prevent confusion with feed refresh, use `Icons.Default.Shuffle` or `Icons.Default.Refresh` with unambiguous labeling.

---

### 3. TopAppBar Scroll Behaviors & Elevation
*   **Problem**: Top app bars currently sit statically with fixed container colors and lack dynamic elevation or reactive scroll behaviors.
*   **Target Implementations**:
    1. **Feed Screen (`MainActivity.kt`)**:
       * Use `TopAppBarDefaults.pinnedScrollBehavior()`.
       * **Rationale**: Because `FeedScreen` features a sticky `SecondaryScrollableTabRow` directly below `CenterAlignedTopAppBar`, `pinnedScrollBehavior` keeps the category navigation pinned while smoothly elevating the app bar container to `MaterialTheme.colorScheme.surfaceContainer` tint as photos scroll beneath.
    2. **Curated Collections (`CollectionsFeedScreen.kt`)**:
       * Use `TopAppBarDefaults.enterAlwaysScrollBehavior()`.
       * **Rationale**: Collections has a single header with no sub-tabs. Scrolling down gently tucks the header away to maximize visual canvas for collection covers, returning immediately on upward scroll.
    3. **Nested Scroll Coordination**:
       * Apply `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` to the outer `Scaffold` container, ensuring seamless cooperation between list fling gestures, app bar reactions, and `PullToRefreshBox`.

---

### 4. Haptic Feedback & Tactile Standards
*   **Tactile Classification**:
    *   **Light Sensory Ticks (`HapticFeedbackType.TextHandleMove`)**:
        *   **Pull-to-Refresh Activation**: Fire haptic tick when the pull gesture crosses the refresh threshold (using threshold state edge detection).
        *   **Filter Chip Toggles**: Fire haptic tick whenever a `FilterChip` in `SearchScreen`'s filter sheet is selected or toggled.
    *   **Strong Confirmation Feedback (`HapticFeedbackType.LongPress` / `VibrationEffect`)**:
        *   **Photo Download Trigger**: Fire when the user clicks the "Download High Resolution Image" button in `PhotoDetailsScreen.kt`, providing clear tactile confirmation before delegating to the browser/download manager.

---

### 5. Predictive Back Gesture Readiness
*   **Android Manifest Opt-In**:
    *   Add `android:enableOnBackInvokedCallback="true"` to `<application>` in [`androidApp/src/main/AndroidManifest.xml`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/AndroidManifest.xml).
*   **Navigation 3 Interoperability**:
    *   Ensure back navigation smoothly cooperates with Navigation 3's `NavDisplay`.
    *   `BackHandler(enabled = state.isSearchActive)` takes precedence, collapsing the active search before system back reaches the navigation backstack.

---

## Verification & Acceptance Criteria

1. **State & Architecture**:
   * Search active state and refresh flags originate solely from `UnifiedSearchPresenter` and `SearchState`. No local `remember` state variables govern search expansion.
   * `SearchState` changes compile cleanly on both Android and iOS (`SearchViewModel.swift`).
2. **Interactive Search**:
   * Compact screens smoothly expand `SearchBar` on focus and collapse on back press.
   * Wide screens display `DockedSearchBar` anchored at the top with suggestions in a floating panel.
   * Tag search navigation (`initialQuery`) immediately shows results without launching suggestions.
3. **Pull-to-Refresh**:
   * Pulling triggers M3 refresh indicators across Feed, Collections, and Search Results.
   * Feeds reload data seamlessly without jumping or resetting to skeleton states during refresh.
4. **Scroll Dynamics**:
   * `FeedScreen` app bar shows subtle tonal elevation tint on scroll while tabs remain pinned.
   * `CollectionsFeedScreen` app bar collapses smoothly on downward scroll and reappears on upward scroll.
5. **Tactile Feedback**:
   * Light haptic feedback fires on pull-to-refresh trigger and filter chip toggles.
   * Confirmation haptic fires on photo download trigger.
6. **Quality & Dual-Platform Verification**:
   * `./gradlew :shared:allTests` passes all unit and integration tests.
   * `./gradlew :androidApp:assembleDebug` builds cleanly without warnings.
   * `cd iosApp && xcodegen && xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO` builds successfully.
   * `./gradlew ktlintCheck detekt` passes without warnings.