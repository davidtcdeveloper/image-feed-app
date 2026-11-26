# Specification 90: Apple iPad Masonry Grid Layout, Height Balancing & Pagination Hardening

**Status:** Approved

* **Depends On:** [Spec 68](archive/68_ios_swiftui_performance_phase1_algorithmic_and_layout_architecture.md), [Spec 83](archive/83_apple_design_system_matte_and_light_mode_hygiene.md), [Spec 88](archive/88_apple_satin_headers_zoom_transitions_and_unified_routes.md)
* **Target Files:**
  * [`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift)
  * [`PhotosFeedView.swift`](../iosApp/iosApp/PhotosFeedView.swift)
  * [`FavoritesView.swift`](../iosApp/iosApp/FavoritesView.swift)
  * [`SearchView.swift`](../iosApp/iosApp/SearchView.swift)
  * [`CollectionDetailView.swift`](../iosApp/iosApp/CollectionDetailView.swift)
  * [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift)
  * [`CollectionsFeedView.swift`](../iosApp/iosApp/CollectionsFeedView.swift)
  * [`FeedViewModel.swift`](../iosApp/iosApp/FeedViewModel.swift), [`FavoritesViewModel.swift`](../iosApp/iosApp/FavoritesViewModel.swift), [`SearchViewModel.swift`](../iosApp/iosApp/SearchViewModel.swift), [`CollectionDetailViewModel.swift`](../iosApp/iosApp/CollectionDetailViewModel.swift), [`UserProfileViewModel.swift`](../iosApp/iosApp/UserProfileViewModel.swift)
  * [`UIPreviewFixtures.swift`](../iosApp/iosApp/UIPreviewFixtures.swift), [`AdaptiveLayoutHelperTests.swift`](../iosApp/iosAppTests/AdaptiveLayoutHelperTests.swift), [`UIVisualSnapshotTests.swift`](../iosApp/iosAppTests/UIVisualSnapshotTests.swift)

---

## 1. Overview & Problem Statement

On iPadOS (3–5 column layouts), the SwiftUI photo grid exhibits large blank white voids below and between rendered columns—for example, showing 5 stacked photos in Column 0 while Column 1 stops after 1 photo and Column 2 shows a gap beneath the top bar and stops after 2 photos.

Investigation across the Apple UI and shared KMP feed stack identified **four compounding causes**:

1. **Nested & Sibling `LazyVStack` Culling/Layout Failure (`PhotosFeedView`, `FavoritesView`, `SearchView`, `CollectionDetailView`, `UserProfileView`)**:
   * [`PhotosFeedView.swift`](../iosApp/iosApp/PhotosFeedView.swift#L120-L169), [`FavoritesView.swift`](../iosApp/iosApp/FavoritesView.swift#L87-L141), and [`SearchView.swift`](../iosApp/iosApp/SearchView.swift#L138-L161) nest `ScrollView` → outer `LazyVStack` → `HStack(alignment: .top)` → `ForEach(0..<columnCount)` → inner `LazyVStack`, while [`CollectionDetailView.swift`](../iosApp/iosApp/CollectionDetailView.swift#L158-L178) and [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift#L436-L459) use sibling `LazyVStack` columns inside `HStack`.
   * Because [`FeedViewModel.kt`](../feature/feed/src/commonMain/kotlin/com/example/imagefeed/presentation/feed/FeedViewModel.kt#L98-L149) fetches `perPage = 15` photos and [`AdaptiveLayoutHelper.partitionIntoColumns`](../iosApp/iosApp/AdaptiveLayoutHelper.swift#L179-L190) distributes items by `offset % totalColumns`, 5 items in Column 0 mathematically guarantees 4–5 items exist in Columns 1 and 2—and [`PhotoGridCard.swift`](../iosApp/iosApp/DesignSystem/PhotoGridCard.swift#L128-L138) renders a colored `RoundedRectangle` + `ProgressView()` while loading/failing. Pure white gaps prove SwiftUI's sibling `LazyVStack`s stopped materializing or prematurely culled existing cards on scroll, topic switch, or column-count change.
   * Unlike [`CollectionsFeedView.swift`](../iosApp/iosApp/CollectionsFeedView.swift#L95), none of the 5 photo grids apply `.frame(maxWidth: .infinity, alignment: .top)` to column stacks inside `HStack`.

2. **Naive Modulo Partitioning Ignores Aspect Ratios ([`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift#L179-L190))**:
   * `partitionIntoColumns` assigns photo `i` to `i % totalColumns` without considering `photo.width` and `photo.height`. On 3–5 column iPad/macOS layouts, a column receiving portrait images grows significantly taller than columns receiving landscape images, leaving large trailing blank areas at the bottom of shorter columns (unlike Android's `LazyVerticalStaggeredGrid`, which places each item in the shortest column).

3. **Fragile Single-Item `photos.last?.id` Pagination Trigger**:
   * [`PhotosFeedView.swift`](../iosApp/iosApp/PhotosFeedView.swift#L161-L165), [`CollectionDetailView.swift`](../iosApp/iosApp/CollectionDetailView.swift#L169-L173), and [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift#L450-L454) trigger `loadNextPage()` only when `photo.id == viewModel.photos.last?.id`. When the last item lands in a column that finishes higher up or fails to materialize, pagination never fires to fill the viewport. ([`CollectionsFeedView.swift`](../iosApp/iosApp/CollectionsFeedView.swift#L329) already guards with `index >= count - max(4, columnCount * 2)`.)

4. **Initial `4 → 3` Column Churn, Topic-Switch Animation & Missing iPad Snapshot Coverage**:
   * Before `.measureContainerWidth($containerWidth)` resolves (`containerWidth == 0`), [`AdaptiveLayoutHelper.getColumnCount`](../iosApp/iosApp/AdaptiveLayoutHelper.swift#L99-L104) falls back to `4` for `.regular` size class, then flips to `3` on iPad portrait (`600..<840` pt).
   * [`PhotosFeedView.swift`](../iosApp/iosApp/PhotosFeedView.swift#L301-L303) wraps `viewModel.selectTopic(slug:)` in `withAnimation(.spring(...))`, animating grid container replacement alongside `FeedCategoryBar` pill selection.
   * [`UIVisualSnapshotTests.swift`](../iosApp/iosAppTests/UIVisualSnapshotTests.swift#L39-L60) only covers `photosFeed` at compact iPhone size (`393x852`) with 5 fixture photos, leaving iPad 3–5 column layouts untested.

---

## 2. Technical Architecture & Design

### Workstream A: Replace Nested/Sibling `LazyVStack` Columns with Deterministic Masonry Layout
1. **Eliminate `LazyVStack > HStack > [LazyVStack]`**:
   * Introduce a reusable SwiftUI masonry grid container (`PhotoMasonryGrid` / custom `MasonryLayout: Layout` or page-chunked balanced column container) under `iosApp/iosApp/DesignSystem/` used consistently across all 5 photo grid screens (`PhotosFeedView`, `FavoritesView`, `SearchView`, `CollectionDetailView`, `UserProfileView`), and audit `CollectionsFeedView` to remove any nested `LazyVStack` hazards while preserving `.frame(maxWidth: .infinity, alignment: .top)`.
   * **Design Requirement**: Every card's `(x, y, width, height)` frame is resolved deterministically inside a single layout/scroll pass—never by sibling `LazyVStack`s racing on independent viewport bounds. If chunked virtualization is retained for large feeds (`100+` items), chunk at the **page/batch level** inside a single outer `LazyVStack` of non-lazy masonry blocks (each block rendering balanced `VStack` columns with `.frame(maxWidth: .infinity, alignment: .top)` or `MasonryLayout`), never placing `LazyVStack` inside `HStack`.
   * Preserve existing `.feedScrollTransition()`, `.matchedTransitionSource` (`photo-\(id)` and `user-\(username)`), `NavigationPreviewSeeder`, context menus, and `containerWidth` Retina CDN sizing on `PhotoGridCard`.

### Workstream B: Aspect-Ratio-Balanced Shortest-Column Assignment
1. **Shortest-Column Greedy Partitioning in [`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift)**:
   * Update `partitionPhotosIntoColumns(from:totalColumns:)` (or the `MasonryLayout` placement algorithm) to track accumulated normalized column heights `columnHeights` using each photo's clamped aspect ratio (`CGFloat(photo.height) / CGFloat(max(1, photo.width))` plus normalized vertical spacing) and place each next photo into the column with the minimum current height.
   * **Pagination Stability Invariant**: Because greedy shortest-column placement is deterministic from index `0`, appending a new page (`photos + uniqueNewPhotos`) must place existing items in the exact same columns without reshuffling already-visible cards for a given `columnCount`.

### Workstream C: Pagination, Layout Stability & iPad Regression Tests
1. **Threshold-Based Pagination Across All Photo Grids**:
   * Replace `photo.id == viewModel.photos.last?.id` checks in [`PhotosFeedView.swift`](../iosApp/iosApp/PhotosFeedView.swift), [`CollectionDetailView.swift`](../iosApp/iosApp/CollectionDetailView.swift), and [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift) with `indexed.index >= photos.count - max(4, columnCount * 2)` (matching `CollectionsFeedView`), ensuring pagination triggers reliably regardless of which column reaches the viewport edge first.
2. **Container-Width & Topic-Switch Hygiene**:
   * Prevent `4 → 3` initial column-count flicker when `containerWidth == 0` on iPad portrait (deferring grid layout or initializing from available container bounds before mounting columns), and remove `withAnimation` around `viewModel.selectTopic(slug:)` in `PhotosFeedView` (keeping spring animation scoped to `FeedCategoryBar`'s active capsule).
   * Confirm `matchedTransitionSource` avatar IDs (`user-\(username)`) do not collide visually or log warnings if a photographer appears in multiple cards on the same screen (or scope/uniquify where appropriate without breaking `AppNavigation` zoom routing).
3. **iPad Test & Snapshot Coverage**:
   * Extend [`AdaptiveLayoutHelperTests.swift`](../iosApp/iosAppTests/AdaptiveLayoutHelperTests.swift) with unit tests verifying balanced shortest-column distribution across 3–5 columns for mixed portrait/landscape aspect ratios and prefix stability on append.
   * Extend [`UIPreviewFixtures.swift`](../iosApp/iosApp/UIPreviewFixtures.swift) with 15+ mixed-aspect fixture photos and add iPad portrait (`834x1194`, `.regular`) and landscape (`1194x834`, `.regular`) `photosFeed` cases to [`UIVisualSnapshotTests.swift`](../iosApp/iosAppTests/UIVisualSnapshotTests.swift).

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **Constraints**:
  * Preserve `macosApp` compilation parity (`iosApp/project.yml` compiles `iosApp/iosApp/**` for both iOS and macOS) and keep zero Swift compiler warnings (`Zero Compiler Warning Policy`).
  * Preserve KMP boundary invariants: column layout and pixel sizing remain in SwiftUI; shared Kotlin `FeedViewModel` / repository pagination behavior stays unchanged unless a shared deduplication gap is uncovered.
* **Non-Goals**:
  * Modifying Android's `LazyVerticalStaggeredGrid` implementation (already shortest-column balanced).
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Document unified Apple masonry grid container and shortest-column balancing across iPhone, iPad, and macOS.
  * [`wiki/performance-and-memory.md`](../wiki/performance-and-memory.md) — Document elimination of nested/sibling `LazyVStack` columns in `HStack` and deterministic pagination threshold rules.
* **Skill & Guideline Impact**:
  * [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md) — Add a guardrail prohibiting sibling `LazyVStack`s inside `HStack` (and nested `LazyVStack`s) for multi-column masonry grids, referencing the living masonry grid component.
  * [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) — Include iPad `.regular` (`3–5` column) snapshot verification for multi-column grid changes.

---

## 4. Verification & UI Visual Review Plan

* **Dual-Target Xcode Builds, Tests & Linters** (per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md)):
  ```bash
  xcodegen generate --spec iosApp/project.yml
  xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPad Pro 11-inch (M4)' CODE_SIGNING_ALLOWED=NO
  xcodebuild test -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' CODE_SIGNING_ALLOWED=NO
  swiftformat --lint . && swiftlint lint iosApp/iosApp iosApp/iosAppTests iosApp/iosAppUITests
  ```
* **Mandatory Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Capture Mode A (`iosAppTests` iPhone + iPad portrait/landscape + `macosAppTests`) and Mode B (`iosAppUITests` on iPad Simulator, including scrolled feed and topic switch) Before/After PNGs into `<Artifact Directory Path>/ui-review/spec-90/{before,after}/`.
  * Verify all 3–5 columns remain populated from top to bottom with balanced trailing edges and zero blank column voids, and obtain user approval before marking `Implemented (Final)`.

---

## 5. Acceptance Criteria

- [ ] Sibling/nested `LazyVStack` columns inside `HStack` are replaced across `PhotosFeedView`, `FavoritesView`, `SearchView`, `CollectionDetailView`, and `UserProfileView` with a unified masonry grid container that renders all cards reliably across scroll, topic switching, and window resizing.
- [ ] Column assignment balances heights across 2–5 columns using photo aspect ratios (shortest-column placement) with deterministic prefix stability on pagination append.
- [ ] Photo grid pagination triggers via `index >= photos.count - max(4, columnCount * 2)` across `PhotosFeedView`, `CollectionDetailView`, and `UserProfileView`.
- [ ] `PhotosFeedView` avoids `4 → 3` initial iPad column churn and unscoped `withAnimation` on `selectTopic`.
- [ ] `AdaptiveLayoutHelperTests` and `UIVisualSnapshotTests` cover iPad multi-column (`834x1194` / `1194x834`) balanced rendering with 15+ mixed-aspect photos.
- [ ] Mode A & Mode B Before/After iPad visual review is approved, and `iosApp`, `macosApp`, `swiftformat --lint .`, and `swiftlint` pass with zero warnings/errors.
- [ ] Affected `wiki/` pages and `.agents/skills/` runbooks are updated, and the spec is moved to `specs/archive/` with `specs/steps.md` updated.
