# Specification: Material 3 Modern UX Capabilities (SearchBar, PullToRefresh, Motion & Haptics)

**Status:** New / Not Implemented

## Overview
This specification delivers modern Material 3 user experience capabilities to the Android application, building upon the foundational theme (`specs/24_material_3_design_foundation.md`) and screen tokenization (`specs/25_material_3_screen_tokenization.md`). 

The primary goals are:
1. Elevate search with Material 3 `SearchBar` / `DockedSearchBar` featuring smooth expand/collapse transitions and integrated suggestions.
2. Introduce native Material 3 `PullToRefreshBox` across all main feeds, removing the need for manual app bar refresh buttons.
3. Integrate TopAppBar scroll behaviors (`pinnedScrollBehavior` / `enterAlwaysScrollBehavior`) for responsive scrolling surfaces.
4. Implement Material 3 haptic feedback and motion patterns.

---

## Detailed Feature Specifications

### 1. Modern Material 3 SearchBar & DockedSearchBar (`SearchScreen.kt`)
*   **Problem**: Currently, search is an ad-hoc `Row` containing a `TextField` with hardcoded `52.dp` height and manual trailing clear icons. It does not leverage Material 3's animated search transitions or responsive docked behaviors.
*   **Solution**:
    *   On Compact screens (< 600dp width): Implement M3 `SearchBar` which expands smoothly to take over the screen when focused, displaying search history and suggestions seamlessly.
    *   On Medium / Expanded screens (tablets & foldables): Implement M3 `DockedSearchBar` anchored at the top, allowing results or dual-pane grids to stay visible underneath while typing.
    *   Retain the filter drawer action inside the trailing icon slot of the `SearchBar`.

### 2. Standard Material 3 Pull-to-Refresh (`PullToRefreshBox`)
*   **Problem**: Users currently cannot pull down to refresh feeds. They must tap a small refresh icon in the top app bar or hit retry on error states.
*   **Solution**:
    *   Integrate `androidx.compose.material3.pulltorefresh.PullToRefreshBox` and `rememberPullToRefreshState()` in:
        1. **Editorial Feed** (`MainActivity.kt`): Pulling reloads the initial page of the feed via `presenter.refresh()`.
        2. **Collections Feed** (`CollectionsFeedScreen.kt`): Pulling refreshes curated and featured collections via `collectionsPresenter.refresh()`.
        3. **Search Results** (`SearchScreen.kt`): Pulling re-executes current query and active filters via `presenter.refresh()`.
    *   Deprecate or demote the redundant refresh `IconButton` in the TopAppBar to declutter the header.

### 3. TopAppBar Scroll Behaviors
*   **Problem**: `CenterAlignedTopAppBar` currently sits statically with fixed background color, never reacting to list scroll offset or showing elevation changes.
*   **Solution**:
    *   Connect `TopAppBarDefaults.enterAlwaysScrollBehavior()` or `pinnedScrollBehavior()` to the `LazyVerticalStaggeredGrid` or `LazyColumn` scroll state.
    *   As users scroll down through photo feeds, the app bar gently elevates, shifting to `surfaceContainer` tint or tucking away to maximize screen canvas for photography.

### 4. Haptic Feedback & Tactile Feedback
*   **Integration**:
    *   Add `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` or `TextHandleMove` when:
        *   A pull-to-refresh gesture hits the trigger threshold.
        *   A search filter chip is toggled on or off.
        *   A photo download is initiated.

### 5. Predictive Back Gesture Readiness
*   **Integration**:
    *   Ensure back navigation smoothly cooperates with Navigation 3's `NavDisplay` and predictive back transitions on Android 14+ (API 34/35+), utilizing predictive back progress where supported by the Navigation 3 runtime.

---

## Verification & Acceptance Criteria
1. **Interactive Search**: Search bar smoothly expands on mobile and anchors as docked search bar on tablets/foldables.
2. **Pull to Refresh**: Pulling the top of the photo grid triggers the Material 3 refresh spinner indicator and refreshes the data without layout jank.
3. **Scroll Elevation**: TopAppBars display subtle tonal elevation when list content scrolls underneath them.
4. **Haptics**: Subtle haptic feedback fires on filter toggles and pull-to-refresh triggers on physical devices.
5. **Quality & Compilation**:
   * `./gradlew :androidApp:assembleDebug` builds cleanly.
   * `./gradlew ktlintCheck detekt` passes without warnings.