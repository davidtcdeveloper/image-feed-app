# Specification 91: Android Search Bar Edge-to-Edge Window Insets Fix

**Status:** Implemented (Final)

* **Depends On:** [Spec 79](79_jetpack_compose_1_13_and_m3_1_5_modernization.md), [Spec 85](85_android_satin_headers_and_shared_transitions.md)
* **Exclusive File Ownership:** [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt), [`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt), `androidApp/src/androidTest/java/com/example/imagefeed/android/SearchScreenInsetsTest.kt`

---

## 1. Overview & Problem Statement

On physical Android devices running edge-to-edge ([`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L94)), the collapsed search input pill on [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L311-L322) renders directly underneath the Android system status bar (`y = 8.dp` from the top edge of the display). Status bar clock text, notification badges, Wi-Fi/battery indicators, and top display cutouts overlap the back arrow, `"Search Photos, Collections, Users..."` placeholder, and filter button.

---

## 2. Root Cause Analysis

1. **Unpadded Edge-to-Edge Container Chain**:
   * [`MainActivity.onCreate`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L94) calls `enableEdgeToEdge()`, and `MainAppShell`'s outer `Scaffold` ([`MainActivity.kt#L172-L174`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L172-L174)) sets `contentWindowInsets = WindowInsets(0, 0, 0, 0)` so child screens draw edge-to-edge beneath system bars.
   * [`SatinHeaderScaffold`](../androidApp/src/main/java/com/example/imagefeed/android/designsystem/SatinHeaderScaffold.kt#L206-L219) mounts `topBar()` at `Alignment.TopStart` without applying status-bar insets itself, expecting each screen's top bar component to handle top/horizontal system-bar and cutout insets.
2. **State-Based M3 `SearchBar` Lacks Default `windowInsets`**:
   * Every other screen (`FeedScreen`, `CollectionsFeedScreen`, `CollectionDetailScreen`, `FavoritesScreen`, `UserProfileScreen`, `PhotoDetailsScreen`) uses Material 3 `TopAppBar` or `CenterAlignedTopAppBar`, which automatically applies `TopAppBarDefaults.windowInsets` (`systemBars` + `displayCutout` on Top and Horizontal).
   * During the Compose 1.13 / M3 1.5.0 migration ([Spec 79](79_jetpack_compose_1_13_and_m3_1_5_modernization.md)), [`SearchScreenContent`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L311-L322) adopted the state-based `SearchBar(state = searchBarState, inputField = searchInputField, ...)` overload inside a plain `Column`. Unlike the deprecated value-based `SearchBar` overload (which defaulted to `windowInsets = SearchBarDefaults.windowInsets`) or `AppBarWithSearch`, the state-based `SearchBar(state, inputField)` is an un-inset pill `Surface` with zero `windowInsets` padding.
   * Consequently, the collapsed `SearchBar` draws inside the status bar region (`8.dp` from the physical top edge), `SearchColorDial` + `SecondaryTabRow` shift upward by the status bar height, and `state.collapsedBounds` records an un-inset origin for `ExpandedFullScreenSearchBar` and `ExpandedDockedSearchBar`.
3. **Why Static Previews Missed It**:
   * [`PreviewScaffold`](../androidApp/src/main/java/com/example/imagefeed/android/preview/UiPreviewFixtures.kt) renders with zero system-bar `WindowInsets`, masking top-bar inset omissions in Compose Preview Sidecar snapshots.

---

## 3. Technical Design & Proposed Fix

1. **Apply `SearchBarDefaults.windowInsets` & Width Parity to `SearchScreenContent` Top Bar (`SearchScreen.kt`)**:
   * Add `Modifier.windowInsetsPadding(SearchBarDefaults.windowInsets)` and `horizontalAlignment = Alignment.CenterHorizontally` to the `Column` inside `SatinHeaderScaffold(topBar = { ... })` at [`SearchScreen.kt#L317`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L317).
   * Constrain the collapsed `SearchBar` with `.widthIn(max = SEARCH_BAR_MAX_WIDTH)` (`720.dp`, matching Material 3 `AppBarWithSearch` max width), pass `modifier = Modifier.fillMaxWidth()` to `SearchBarDefaults.InputField`, and add `maxLines = 1, overflow = TextOverflow.Ellipsis` on its placeholder `Text`.
   * Using `SearchBarDefaults.windowInsets` plus `InputField` width parity ensures:
     * The collapsed `SearchBar` renders cleanly below the status bar and top display cutout while preserving its existing `12.dp`/`16.dp` horizontal and `8.dp` vertical spacing.
     * `SearchColorDial` and `SecondaryTabRow` sit below `SearchBar` when expanded and clear horizontal display cutouts in compact landscape; when collapsed via `scrollBehavior`, `SearchBar` stays pinned below the status bar.
     * `SatinHeaderScaffold`'s measured `headerHeightPx` and `maxHeaderHeightPx` automatically include the status bar inset, expanding `satinProgressiveBlur` across the status bar region and offsetting `headerTop` / `LocalSatinHeaderPadding` for `SearchSuggestionsAndHistory`, loading skeletons, `NoResultsView`, `SatinPullToRefreshBox`, and result grids without content overlap.
     * `state.collapsedBounds` reflects the inset position so `ExpandedFullScreenSearchBar` (compact `Dialog`, which independently handles `SearchBarDefaults.fullScreenWindowInsets`) and `ExpandedDockedSearchBar` (medium/expanded `Popup`) expand/anchor cleanly below the status bar and match the collapsed `SearchBar` width 1:1 (instead of shrinking to `360.dp` behind the popup blur or wrapping the placeholder onto two lines).
2. **Consume `Start` Insets on `ExpressiveNavigationRail` Sibling `Scaffold` (`MainActivity.kt`)**:
   * In `MainAppShell` ([`MainActivity.kt#L175`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L175)), when `useRail` is true (`ExpressiveNavigationRail` already consumes `WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)` as a `Row` sibling), apply `Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))` to `Scaffold` so `SearchScreen`'s `SearchBarDefaults.windowInsets` (and `TopAppBar` on other screens) does not double-pad left display cutouts inside the content pane.
3. **Compose UI Inset & Width-Parity Regression Tests (`SearchScreenInsetsTest.kt`)**:
   * Add instrumented Compose UI tests in `androidApp/src/androidTest/java/com/example/imagefeed/android/SearchScreenInsetsTest.kt` using `DeviceConfigurationOverride.WindowInsets` to verify:
     * Collapsed `SearchBar` clears simulated `48.dp` status-bar and `24.dp` horizontal cutout insets, and `Photos` tab + `POPULAR TOPICS` sit sequentially below without overlap in discovery and results states.
     * `ExpandedDockedSearchBar` (`900×700`) matches the collapsed `SearchBar` screen position (`positionOnScreen`) and width (`> 360.dp`) without shrinking.
     * `ExpandedFullScreenSearchBar` (`412×892`) clears the system status bar inside its `Dialog`.
     * `SearchScreenContent` alongside `ExpressiveNavigationRail` + `consumeWindowInsets(WindowInsetsSides.Start)` does not double-pad the start display cutout.

---

## 4. Constraints, Wiki & Skill Impact

* **Scope**: Strictly confined to `androidApp` (`MainActivity.kt`, `SearchScreen.kt`, and `androidTest`). Shared KMP modules (`:core:*`, `:feature:*`, `:shared`) and Apple targets (`iosApp`, `macosApp`) are unaffected.
* **Design Tokens & M3 Policy**: Retain Material 3 `SearchBarDefaults.windowInsets` (zero hardcoded status bar `dp` values) per [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md).
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Note that state-based `SearchBar` in `SearchScreen` applies `SearchBarDefaults.windowInsets` inside `SatinHeaderScaffold` (unlike `TopAppBar`, which applies insets internally).
* **Skill & Guideline Impact**:
  * [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md) — Document in §2 (`Search` / `App Bars`) that state-based `SearchBar` inside `SatinHeaderScaffold` requires explicit `windowInsetsPadding(SearchBarDefaults.windowInsets)` (or `AppBarWithSearch`) because `SearchBar(state, inputField)` does not apply top/cutout insets automatically.

---

## 5. Verification & UI Visual Review Plan

* **Build, Unit Tests, Android Test Compilation & Linters**:
  ```bash
  ./gradlew :androidApp:assembleDebug :androidApp:testDebugUnitTest :androidApp:compileDebugAndroidTestKotlin ktlintCheck detekt
  ```
* **Instrumented Emulator Test (`SearchScreenInsetsTest`)**:
  ```bash
  ./gradlew :androidApp:connectedDebugAndroidTest
  ```
* **Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Capture Before and After Compose Preview Sidecar PNGs for `SearchScreenPreview`, `SearchScreenDarkPreview`, `SearchScreenExpandedPreview`, and `SearchResultsScreenPreview` into `<Artifact Directory Path>/ui-review/spec-91/{before,after}/android/` (noting that Compose `@Preview` runs with zero system-bar insets, while `SearchScreenInsetsTest` verifies non-zero `WindowInsets` on-window), plus Before device and After live emulator captures in `<Artifact Directory Path>/ui-review/spec-91/{before/device,after/emulator}/`, presented in `spec-91-visual-review.md`.

---

## 6. Acceptance Criteria

- [x] `SearchScreenContent` applies `Modifier.windowInsetsPadding(SearchBarDefaults.windowInsets)` (with `MainActivity` consuming `WindowInsetsSides.Start` on the `ExpressiveNavigationRail` sibling `Scaffold`) so the collapsed `SearchBar`, `SearchColorDial`, and `SecondaryTabRow` clear the status bar and display cutouts without double-padding start cutouts across Compact, Medium, and Expanded widths.
- [x] `ExpandedDockedSearchBar` (`InputField` `fillMaxWidth()` + `SEARCH_BAR_MAX_WIDTH` `720.dp` parity + single-line placeholder ellipsis) and `ExpandedFullScreenSearchBar` match collapsed bounds/width and clear status bars cleanly.
- [x] `SatinHeaderScaffold` progressive blur/scrim and `headerTop` / `LocalSatinHeaderPadding` offsets cover the inset header cleanly in both discovery and results states.
- [x] `SearchScreenInsetsTest` verifies `SearchBar`, `ExpandedDockedSearchBar`, `ExpandedFullScreenSearchBar`, and rail `Start`-inset consumption clear simulated status-bar and cutout `WindowInsets` without double-padding or overlapping tabs/discovery content.
- [x] `:androidApp:assembleDebug`, `:androidApp:testDebugUnitTest`, `:androidApp:compileDebugAndroidTestKotlin`, `ktlintCheck`, and `detekt` pass with zero warnings (plus `:androidApp:connectedDebugAndroidTest` if an emulator is available).
- [x] Before/After Compose Preview Sidecar renders and visual review artifact (`spec-91-visual-review.md`) are produced for user approval prior to marking `Implemented (Final)` and archiving.

---

## 7. Verification Record

### Verified
* `ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :androidApp:assembleDebug :androidApp:testDebugUnitTest :androidApp:compileDebugAndroidTestKotlin ktlintCheck detekt` — passed (exit code `0`, zero compiler warnings, zero `ktlintCheck` violations).
* `ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :androidApp:connectedDebugAndroidTest` on `Resizable_Experimental(AVD) - 17` (`emulator-5554`) — passed (`18` tests, `0` failures, `0` errors, `0` skipped), including `SearchScreenInsetsTest` (`5/5` tests verifying `48.dp` status-bar clearance, `24.dp` horizontal cutout clearance, `ExpandedDockedSearchBar` position/width parity, `ExpandedFullScreenSearchBar` dialog status-bar clearance, and zero double-padding alongside `ExpressiveNavigationRail`).
* Live `MainActivity` `adb shell screencap` + `uiautomator` verification on `emulator-5554`:
  * Compact portrait (`Search` tab collapsed and `ExpandedFullScreenSearchBar`) cleanly clears the system status bar.
  * Landscape dual-pane with `tall` display cutout overlay (`ExpressiveNavigationRail` + `SearchScreen`) shows flush alignment of `SearchBar`, `SearchColorDial`, `SecondaryTabRow`, and `POPULAR TOPICS` with single-line placeholder text and no start double-padding.
  * Tablet (`2200×1800`) `ExpandedDockedSearchBar` matches the collapsed `SearchBar` width 1:1 below the status bar with no narrow `360.dp` shrink or clipped pill behind the blur.
* Rendered and inspected Before/After Compose Preview Sidecar PNGs (`SearchScreenPreview`, `SearchScreenDarkPreview`, `SearchScreenExpandedPreview`, `SearchResultsScreenPreview`), `Before` device screenshot, and `After` live emulator screenshots in `<Artifact Directory Path>/ui-review/spec-91/` and created `spec-91-visual-review.md`.
* Updated [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) and [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md).

### Unverified / Gaps
* Physical-device hardware notch visual check and interactive live photo-grid scroll collapse (`PhotosResultGrid` requires a populated Unsplash API key; emulator returned HTTP 401 on live feed/search calls).
* Visual review artifact `spec-91-visual-review.md` approved by user; spec finalized and archived.
