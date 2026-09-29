# Specification: Cross-Platform Parity, Token Hardening & Navigation Reconciliation

**Status:** Implemented

## 1. Overview & Context

Following an audit of all specifications with status `Implemented` ([Specs 24–32](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs)), several architectural gaps, contradictions between documentation and code, design token leaks, and cross-platform UX discrepancies were identified between `androidApp/`, `iosApp/`, and `shared/`.

This specification establishes a concrete, actionable plan to reconcile these differences. It addresses:
1. **Search Navigation Reconciliation**: Aligning Android and iOS collection search results to navigate in-app to `CollectionDetails` rather than launching external browser links, satisfying [Spec 31](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/31_readme_and_navigation_documentation.md) and [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md).
2. **Search State & Pull-to-Refresh Parity**: Bringing iOS `SearchView` into compliance with [Spec 26](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/26_material_3_ux_modernization.md) by binding to `UnifiedSearchPresenter`'s `isRefreshing` and `refresh()` via `.refreshable`, and connecting query lifecycle transitions.
3. **Comprehensive iOS Design Token Hardening**: Eradicating remaining hardcoded hex colors (`Color(hex: "0F0F11")`, `Color(hex: "1E1E24")`, `Color(hex: "2C2C35")`), ad-hoc `.font(.system(size: ...))`, and un-vibrated text colors across `SearchView`, `UserProfileView`, `CollectionDetailView`, `CollectionsFeedView`, and `PhotoDetailsView`.
4. **Photo Card Attribution & Unsplash Compliance Parity**: Resolving the missing photographer attribution on iOS search results to ensure Unsplash API guidelines compliance, and harmonizing photographer attribution tap destinations across all screens.
5. **Dynamic CDN Image Sizing**: Replacing hardcoded screen halves (`screenWidth / 2`) and static `urls.small` on iOS with column-adaptive CDN width calculations (`&w=calculatedWidth&q=80&auto=format`), matching Android's `calculatePhotoItemWidthPx(columnCount)`.
6. **Action Controls & Hero Animation Scope**: Standardizing floating buttons on iOS `PhotoDetailsView` to use `GlassToolbarButton` / `GlassTheme`, and expanding `matchedGeometryEffect` hero transitions to Search and Collection Detail grids.

---

## 2. Identified Inconsistencies & Detailed Gaps

### A. Collection Search Result Navigation
* **Contradiction**: [Spec 31 (Section 3.2)](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/31_readme_and_navigation_documentation.md#L64) and [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md#L122) state `SearchScreen -->|"Tap Collection Result"| CollectionDetails`. However, both [Android `SearchScreen.kt:640`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L640) and [iOS `SearchView.swift:180`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift#L180) open external Unsplash web links via browser intents / `URLHelper.open(url)`.
* **Remedy**: Add in-app collection selection callbacks to `SearchScreen` and `SearchView`, routing directly to `Screen.CollectionDetails(collectionId)` (Android) and `CollectionPathItem(id: colId, type: .collection)` (iOS).

### B. iOS Search State & Pull-to-Refresh Parity
* **Gap**: [Spec 26](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/26_material_3_ux_modernization.md) added `isRefreshing`, `isSearchActive`, `refresh()`, `setSearchActive()`, and `submitSearch()` to `UnifiedSearchPresenter` and `SearchViewModel`. Android leverages these via `SearchBar`/`DockedSearchBar` and `PullToRefreshBox`. iOS `SearchView.swift` bypasses them with local `@State private var searchText = ""` and completely lacks `.refreshable`.
* **Remedy**: Wire `.refreshable { viewModel.refresh() }` to `SearchView.swift`, and synchronize search submission with `viewModel.submitSearch(query:)`.

### C. Design Token Leaks & "Zero Hardcoded Tokens" Enforcement on iOS
* **Gap**: Despite [Specs 27–30](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/27_ios_glass_design_foundation.md) claiming tokenization is complete:
  * `SearchView.swift`: Uses `Color(hex: "0F0F11")`, `Color(hex: "1E1E24")`, and `Color(hex: "2C2C35")` across backgrounds, skeleton loaders, and filter controls.
  * `CollectionsFeedView.swift`: Root background still contains `Color(hex: "0F0F11")`.
  * `UserProfileView.swift`: Root background uses `Color(hex: "0F0F11")`, placeholder uses `Color(hex: "1E1E24")`, and chart/tabs use raw `.system(size:)` and static `.foregroundColor(.gray)`.
  * `CollectionDetailView.swift`: Root background uses `Color(hex: "0F0F11")`, and placeholder uses `Color(hex: "1E1E24")`.
  * `PhotoDetailsView.swift`: Uses `Color(hex: "0F0F11")`, `Color(hex: "070709")`, over 15 ad-hoc `.font(.system(size: ...))` declarations, and static `.foregroundColor(.white)` / `.foregroundColor(.gray)`.
* **Remedy**: Purge all hardcoded hex values and raw system fonts across `iosApp/`, binding instead to `GlassTheme`, semantic materials, Dynamic Type font styles (`.headline`, `.subheadline`, `.caption`, etc.), and `.glassVibrancy(...)`.

### D. Missing Photo Attribution & Inconsistent Attribution Destinations
* **Gap 1 (Compliance Violation)**: In [iOS `SearchView.swift:145–167`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift#L145-L167), photos rendered in search results have **zero photographer attribution** (no avatar, no name, no profile link), violating Unsplash API Guidelines and [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md#L83).
* **Gap 2 (Inconsistent Destination)**: In [iOS `ContentView.swift` (`PhotoCard`)](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L475), tapping photographer attribution navigates in-app to `UserProfileView`. In [iOS `CollectionDetailView.swift` (`CollectionPhotoGridCard:329`)](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/CollectionDetailView.swift#L329), tapping photographer attribution opens Safari.
* **Remedy**: 
  1. Standardize photo cards in `SearchView.swift` to include the floating glass attribution capsule with photographer avatar and name.
  2. Harmonize `CollectionPhotoGridCard` to route photographer taps to in-app `UserProfileView(username)` via an `onUserSelect` callback.

### E. Dynamic CDN Image Sizing on iOS
* **Gap**: [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md#L111) mandates dynamic CDN image resizing (`&w=calculatedWidth&q=80&auto=format`). Android adapts dynamically to active column counts (`calculatePhotoItemWidthPx(columnCount)`). iOS hardcodes `Int(screenWidth / 2)` in `ContentView.swift` and `CollectionDetailView.swift` (even on 3-column or 4-column iPad/macOS layouts) and uses static `urls.small` in `SearchView.swift` and `UserProfileView.swift`.
* **Remedy**: Introduce a shared helper or standard calculation in `AdaptiveLayoutHelper.swift`:
  ```swift
  static func calculateItemWidth(screenWidth: CGFloat, columnCount: Int) -> Int {
      let spacing: CGFloat = 8 * CGFloat(columnCount + 1)
      let availableWidth = max(200, screenWidth - spacing)
      return Int(availableWidth / CGFloat(max(1, columnCount)))
  }
  ```
  Apply this across `PhotoCard`, `CollectionPhotoGridCard`, `SearchView`, and `UserProfileView`.

### F. Glass Action Controls & Hero Zoom Scope
* **Gap 1**: In `PhotoDetailsView.swift:442, 455`, download and inspector toggle buttons use ad-hoc `.background(Circle().fill(.white.opacity(0.12)))` instead of `GlassToolbarButton` or `GlassTheme.specularBorderGradient`.
* **Gap 2**: Only `ContentView.swift` wires `matchedGeometryEffect` for photo hero zoom transitions. Grids in `SearchView.swift` and `CollectionDetailView.swift` do not support hero expansion when navigating to `PhotoDetailsView`.
* **Remedy**: Standardize floating controls using `GlassToolbarButton` / `GlassTheme`, and propagate hero animation namespaces where view hierarchies permit.

---

## 3. Detailed Architectural Solutions & Action Plan

### Part A: Search Navigation Reconciliation

#### 1. Android Navigation Routing
* Update [`SearchScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt):
  * Add parameter: `onCollectionClick: (CollectionSummary) -> Unit`.
  * Update `CollectionRowCard` click handler to invoke `onCollectionClick(collection)`.
* Update [`MainActivity.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt):
  * In `Screen.Search` entry in `NavDisplay`, wire `onCollectionClick = { collection -> backStackState.add(Screen.CollectionDetails(collection.id)) }`.

#### 2. iOS Navigation Routing
* Update [`SearchView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift):
  * Add parameter to `init`: `onCollectionSelect: @escaping (String) -> Void`.
  * In the `.collections` tab, change `CollectionCardView` button action to invoke `onCollectionSelect(collection.id)`.
* Update [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift):
  * Pass `onCollectionSelect: { colId in path.append(FeedPathItem(id: colId, type: .collection)) }` in both `PhotosFeedTabView` and `CollectionsFeedTabView`.

---

### Part B: iOS Search State & Pull-to-Refresh Parity

* In [`SearchView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift):
  * Wrap the results scroll view with `.refreshable { viewModel.refresh() }`.
  * Ensure `viewModel.refresh()` re-queries active filters and results without UI glitches.
  * In `.onSubmit(of: .search)` or `.onChange(of: searchText)`, invoke `viewModel.submitSearch(query: searchText)`.

---

### Part C: iOS Design Token Hardening & Zero-Hardcoded-Value Enforcement

* **Background Surfaces**:
  * Replace `Color(hex: "0F0F11")` and `Color(hex: "070709")` across root containers in `SearchView.swift`, `CollectionsFeedView.swift`, `UserProfileView.swift`, `CollectionDetailView.swift`, and `PhotoDetailsView.swift` with standard semantic backgrounds or materials (`GlassTheme.fallbackBackgroundColor` / `.background(.background)`).
* **Card & Metadata Backings**:
  * In `SearchView.swift`, replace `Color(hex: "1E1E24")` and `Color(hex: "2C2C35")` with `.glassCard(cornerRadius: 12, style: .ultraThin)` or semantic secondary surface fills.
  * In `UserProfileView.swift`, replace `Color(hex: "1E1E24")` with semantic container tokens.
* **Typography & Dynamic Type**:
  * In `PhotoDetailsView.swift`, `UserProfileView.swift`, and `MacOSContentView.swift`:
    * Replace all occurrences of `.font(.system(size: ...))` with semantic Dynamic Type tokens (`.title`, `.headline`, `.subheadline`, `.body`, `.callout`, `.footnote`, `.caption`, `.caption2`).
    * Replace static `.foregroundColor(.white)` with `.glassVibrancy(.primary)`.
    * Replace static `.foregroundColor(.gray)` with `.glassVibrancy(.secondary)`.

---

### Part D: Photo Card Attribution & Unsplash Compliance Parity

* **iOS Search Results**:
  * Refactor `SearchView.swift` photo results to render `PhotoCard` or encapsulate photo image + floating attribution capsule:
    ```swift
    PhotoCard(
        photo: photo,
        viewModel: feedViewModel, // or search card equivalent
        heroNamespace: heroNamespace,
        isHeroSource: true,
        onSelect: { onPhotoSelect(photo.id) },
        onUserSelect: { onUserSelect(photo.user.username) }
    )
    ```
  * Ensures photographer name, avatar, and navigation are present on every photo search result.
* **Collection Detail Attribution**:
  * In `CollectionDetailView.swift`, update `CollectionPhotoGridCard` to accept `onUserSelect: (String) -> Void`.
  * Tapping the attribution capsule invokes `onUserSelect(photo.user.username)` to push `CollectionPathItem(id: username, type: .user)`, maintaining parity with `PhotoCard`.

---

### Part E: Dynamic CDN Image Resizing on iOS

* In `AdaptiveLayoutHelper.swift`, add:
  ```swift
  public static func calculateItemWidthPx(screenWidth: CGFloat, columnCount: Int) -> Int {
      let spacing: CGFloat = 8 * CGFloat(columnCount + 1)
      let availableWidth = max(200, screenWidth - spacing)
      return Int(availableWidth / CGFloat(max(1, columnCount)))
  }
  ```
* Update call sites:
  1. `ContentView.swift` (`PhotoCard`): Use `AdaptiveLayoutHelper.calculateItemWidthPx(screenWidth: screenWidth, columnCount: columnCount)`.
  2. `CollectionDetailView.swift` (`CollectionPhotoGridCard`): Use `calculateItemWidthPx`.
  3. `SearchView.swift`: Formulate photo URL with `&w=\(itemWidth)&q=80&auto=format` instead of `photo.urls.small`.
  4. `UserProfileView.swift`: Formulate photo URL with `&w=\(itemWidth)&q=80&auto=format` instead of `photo.urls.small`.

---

### Part F: Glass Action Controls & Hero Zoom Scope

* In `PhotoDetailsView.swift`:
  * Replace the ad-hoc download and info buttons in `compactFloatingBar` with `GlassToolbarButton` or `.glassBackground(style: .ultraThin, shape: Circle(), showBorder: true, hasShadow: true)` to ensure specular catchlights and high-contrast accessibility borders.
* In `SearchView.swift` and `CollectionDetailView.swift`:
  * *Hero Zoom Architecture Note*: Hero zoom transitions via `matchedGeometryEffect` require source and destination views to reside within the same layout/view hierarchy, as implemented for root feed photos in `ContentView.swift` (`selectedPhotoForHero`). For secondary grid views (`SearchView` and `CollectionDetailView`), photo taps push onto `NavigationStack` via `NavigationPath`, where SwiftUI natively manages standard platform push/pop transitions rather than custom in-tree geometry matching.

---

## 4. Documentation & Existing Specification Synchronization

To ensure project documentation and existing specifications remain coherent and free of contradictions:

1. **[`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md)**:
   * Keep the Mermaid diagram flow `SearchScreen -->|"Tap Collection Result"| CollectionDetails` as the source of truth, aligning code to fulfill this contract.
2. **[`specs/steps.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/steps.md)**:
   * Add **Step 35: Cross-Platform Parity, Token Hardening & Navigation Reconciliation** referencing this spec.
3. **[`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md)**:
   * Add `specs/33_cross_platform_parity_and_token_hardening.md` to the Reference Specs Directory.
4. **Historical Specs ([Spec 25](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/25_material_3_screen_tokenization.md), [Spec 29](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/29_ios_cards_attribution_glass_redesign.md))**:
   * Add clarification notes in Spec 25 and Spec 29 documenting the deliberate platform-specific attribution pattern: Android uses the Material 3 photographic dark gradient scrim, while iOS/macOS uses the inset floating translucent glass pill (`.glassCapsule`), both fulfilling Unsplash attribution requirements and navigating in-app to photographer profiles.

---

## 5. Implementation Checklist

- [x] **Part 1: Search Navigation Reconciliation**:
  - [x] Add `onCollectionClick` callback to Android `SearchScreen.kt` and wire to `Screen.CollectionDetails` in `MainActivity.kt`.
  - [x] Add `onCollectionSelect` callback to iOS `SearchView.swift` and wire to `CollectionPathItem` in `ContentView.swift`.
- [x] **Part 2: iOS Search State & Pull-to-Refresh Parity**:
  - [x] Add `.refreshable { viewModel.refresh() }` to `SearchView.swift`.
  - [x] Connect search submission to `viewModel.submitSearch`.
- [x] **Part 3: iOS Design Token Hardening**:
  - [x] Remove all raw `Color(hex: "0F0F11")`, `Color(hex: "1E1E24")`, and `Color(hex: "2C2C35")` in `SearchView.swift`.
  - [x] Remove `Color(hex: "0F0F11")` in `CollectionsFeedView.swift`.
  - [x] Remove `Color(hex: "0F0F11")` and `Color(hex: "1E1E24")` in `UserProfileView.swift`.
  - [x] Remove `Color(hex: "0F0F11")` and `Color(hex: "1E1E24")` in `CollectionDetailView.swift`.
  - [x] Purge all `.font(.system(size: ...))` and static `.foregroundColor` in `PhotoDetailsView.swift` and `UserProfileView.swift`, replacing with Dynamic Type and `GlassVibrancy`.
  - [x] Replace `.foregroundColor(.gray)` in `MacOSContentView.swift` with `.foregroundStyle(.secondary)`.
- [x] **Part 4: Photo Card Attribution & Unsplash Compliance**:
  - [x] Add photographer attribution capsule to photo results in `SearchView.swift`.
  - [x] Wire photographer attribution click in `CollectionPhotoGridCard` to in-app `UserProfileView`.
- [x] **Part 5: Dynamic CDN Resizing on iOS**:
  - [x] Implement `calculateItemWidthPx` in `AdaptiveLayoutHelper.swift` (incorporating display scale for Retina crispness).
  - [x] Apply dynamic `&w=` sizing in `PhotoCard`, `CollectionPhotoGridCard`, `SearchView.swift`, and `UserProfileView.swift`.
- [x] **Part 6: Action Controls & Hero Zoom Scope**:
  - [x] Modernize floating buttons in `PhotoDetailsView.swift` using `GlassToolbarButton` / `GlassTheme`.
  - [x] Document SwiftUI `NavigationStack` push transition architecture for secondary grids vs modal hero zoom.
- [x] **Part 7: Spec & Rule Documentation Updates**:
  - [x] Update `specs/steps.md` with Step 35.
  - [x] Update `AGENTS.md` reference specs index.

---

## 6. Verification & Dual-Platform Acceptance Criteria

1. **In-App Search Collection Navigation**:
   * Selecting a collection in Search on both Android and iOS pushes the in-app `CollectionDetails` screen rather than opening a browser.
2. **Pull-to-Refresh on iOS Search**:
   * Pulling down on search results triggers a smooth refresh indicator and re-fetches active search query results.
3. **Zero Hardcoded Design Tokens**:
   * Grepping for `Color(hex: "0F0F11")`, `Color(hex: "1E1E24")`, `Color(hex: "2C2C35")`, and `.font(.system(size:` across `iosApp/iosApp` yields zero matches (except for domain photo average colors).
4. **Unsplash Attribution Compliance**:
   * Every photo rendered in Search and Collection Detail views displays photographer name, avatar, and navigates to the photographer's profile in-app.
5. **Dynamic CDN Resizing**:
   * Image URLs on iOS scale proportionally to the calculated column width across compact, regular, and large display widths.
6. **Dual-Platform Builds & Lints**:
   * `./gradlew :shared:allTests` passes cleanly.
   * `./gradlew :androidApp:assembleDebug` builds cleanly without warnings.
   * `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO` succeeds.
   * `swiftlint lint iosApp/iosApp` and `./gradlew ktlintCheck detekt` pass without errors.
