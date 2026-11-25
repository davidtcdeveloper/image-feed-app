# Specification 88: Apple Unified AppRoute, Satin Scroll Edges, iOS 18 Zoom Hero Transitions & Avatar Continuity

**Status:** Accepted

* **Wave:** Wave 2 — Parallel Feature & Craft Execution
* **Depends On:** [Spec 81](81_kmp_color_engine_and_navigation_seed_cache.md), [Spec 83](83_apple_design_system_matte_and_light_mode_hygiene.md)
* **Parallelizable With:** [Spec 84](84_android_float_navigation_and_random_mvi.md), [Spec 85](85_android_satin_headers_and_shared_transitions.md), [Spec 86](86_android_immersive_photo_details_and_zoom.md), [Spec 87](87_apple_float_navigation_shell_and_macos_search.md), [Spec 89](89_apple_photo_details_inspector_parity_and_glass_morphing.md)
* **Research Mapping:** [Cross-Platform Material Craft Evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) — `ARCH-01` (Apple seeding), `ARCH-07`, Craft `2.3` (Apple), Craft `2.4` (Apple), Craft `2.5` (child toolbars & `.glassEffect`), Craft `2.6` (avatar transitions & shuffle bounce)
* **Exclusive File Ownership:** `iosApp/iosApp/DesignSystem/AppNavigation.swift`, `iosApp/iosApp/DesignSystem/PhotoGridCard.swift`, `iosApp/iosApp/PhotosFeedView.swift`, [`CollectionsFeedView.swift`](../iosApp/iosApp/CollectionsFeedView.swift), [`CollectionDetailView.swift`](../iosApp/iosApp/CollectionDetailView.swift), [`FavoritesView.swift`](../iosApp/iosApp/FavoritesView.swift), [`SearchView.swift`](../iosApp/iosApp/SearchView.swift), [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift)

---

## 1. Overview & Codebase Baseline

After [Spec 83](83_apple_design_system_matte_and_light_mode_hygiene.md) splits `PhotosFeedView.swift`, `AppNavigation.swift`, and `PhotoGridCard.swift` into modular files, Apple's child screens still carry three architectural and visual craft gaps from [the UI craft evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md):

1. **Duplicated Routes & Brittle `ZStack` Hero Overlays (`ARCH-07`, Craft `2.4`)**: [`ContentView.swift`](../iosApp/iosApp/ContentView.swift#L243-L265) and [`FavoritesView.swift`](../iosApp/iosApp/FavoritesView.swift#L4-L14) define three identical path enums (`FeedPathItem`, `CollectionPathItem`, `FavoritesPathItem`) and repeat `.navigationDestination` switches. `PhotosFeedTabView` and `FavoritesTabView` render `PhotoDetailsView` in a `ZStack` overlay (`selectedPhotoForHero`) for `matchedGeometryEffect` — triggering an occlusion pop behind `FeedCategoryBar` — while `SearchView`, `CollectionDetailView`, and `UserProfileView` push onto `NavigationStack` with a `dummyNamespace` ([`ContentView.swift`](../iosApp/iosApp/ContentView.swift#L205)) and zero hero animation.
2. **Double-Bar Top Chrome Seam (Craft `2.3`)**: [`ContentView.swift`](../iosApp/iosApp/ContentView.swift#L428-L481), [`SearchView.swift`](../iosApp/iosApp/SearchView.swift#L220-L242), [`FavoritesView.swift`](../iosApp/iosApp/FavoritesView.swift#L176-L181), [`CollectionsFeedView.swift`](../iosApp/iosApp/CollectionsFeedView.swift#L129-L131), and [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift#L110-L113) force hard-edged `.toolbarBackground(.ultraThinMaterial, for: .navigationBar)` + `.toolbarBackground(.visible)` above `.safeAreaInset(edge: .top)` capsules, producing a stacked double-bar seam.
3. **iOS-Style Toolbars on macOS & Missing Avatar Continuity (Craft `2.5` & `2.6`)**: Child screens still place 40pt circular `GlassToolbarButton` orbs in macOS `.toolbar` slots, omit `.glassEffect(.regular.interactive(), in:)` on iOS 26+ floating category/search bars, and lack photographer avatar hero transitions into `UserProfileView`.

---

## 2. Scope & Technical Workstreams

### Workstream A (`ARCH-07` & `ARCH-01`): Unified `AppRoute` & `NavigationPreviewCache` Seeding
1. Consolidate `FeedPathItem`, `CollectionPathItem`, and `FavoritesPathItem` into a single `enum AppRoute: Hashable` (`photo(id: String)`, `collection(id: String)`, `user(username: String)`, `search(query: String)`) and reusable `.appNavigationDestinations(...)` `ViewModifier` in `DesignSystem/AppNavigation.swift`.
2. Seed `MetroHelper.shared` `NavigationPreviewCache` (`Spec 81`) whenever navigating from any photo grid, collection card, or user row, and pass the cached `initialPhoto` into `PhotoDetailsView` across all 5 entry points (`PhotosFeedView`, `CollectionsFeedView`/`CollectionDetailView`, `FavoritesView`, `SearchView`, `UserProfileView`).

### Workstream B (`ARCH-07` & Craft `2.4`): iOS 18+ `.navigationTransition(.zoom)` & Occlusion-Free Dismissal
1. Under `#available(iOS 18, *)`, retire the `ZStack` `selectedPhotoForHero` overlay and synchronization flags (`isPhotosDetailPresented`, `isFavoritesDetailPresented`, `activePath.count` `.onChange`) in `PhotosFeedView` and [`FavoritesView.swift`](../iosApp/iosApp/FavoritesView.swift).
2. Wire `PhotoGridCard.swift` with `.matchedTransitionSource(id:in:)` and attach `.navigationTransition(.zoom(sourceID:in:))` on pushed `PhotoDetailsView` destinations inside `AppNavigation.swift` — enabling interactive swipe-to-dismiss, `12pt → 0pt` corner-radius interpolation, and native navigation-bar clipping across **all 5 photo grids** without occlusion pops.
3. Retain an `#unavailable(iOS 18)` fallback path with subtle `FeedCategoryBar` cross-fade on iOS 17, and use pure `NavigationStack` with brisk `~250–300ms` transitions on macOS.

### Workstream C (Craft `2.3`, `2.5` & `2.6`): Feathering Satin Top Edges, OS 26 `.glassEffect`, macOS Native Toolbars & Avatar Continuity
1. Implement `.satinScrollEdge()` (`SurfaceTokens.swift`) and apply it across `PhotosFeedView`, `CollectionsFeedView`, `CollectionDetailView`, `FavoritesView`, `SearchView`, and `UserProfileView`:
   * **iOS 26+ / macOS 26+ (`#available(iOS 26, macOS 26, *)`)**: Enable native **`.scrollEdgeEffectStyle(.soft, for: .top)`** and remove `.toolbarBackground(.visible, for: .navigationBar)` so photography melts smoothly under navigation chrome and floating category/search bars.
   * **iOS 17–25 / macOS 14–15**: Replace the hard double seam with a lightweight canvas-color top gradient fade beneath `FeedCategoryBar` / `SearchTabBar` (zero per-frame live `.blur` on `ScrollView`).
   * **macOS Ergonomic Guardrail**: Keep `FeedCategoryBar` and `SearchTabBar` persistent on macOS (never auto-hiding on scroll).
2. Upgrade `FeedCategoryBar` and `SearchTabBar` floating capsules to `.glassEffect(.regular.interactive(), in:)` under `#available(iOS 26, macOS 26, *)` (falling back cleanly on OS 17–25 / 14–25).
3. Replace remaining 40pt circular `GlassToolbarButton` items in child `.toolbar` slots with `PlatformToolbarButton` (`Spec 83`) so macOS renders native AppKit / macOS 26 Tahoe toolbar items; add `.symbolEffect(.bounce)` on Shuffle triggers; and wire Photographer Avatar hero continuity (`user-\(username)`) from `PhotoGridCard` and `SearchView` into `UserProfileView`.

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **Disjoint Ownership**: Do not edit `ContentView.swift` tab shell, `MacOSContentView.swift`, `PlatformNavigationShell.swift` (`Spec 87`), or `PhotoDetailsView.swift` / `PhotoInspectorView.swift` (`Spec 89`). Preserve `PhotoDetailsView.init` compatibility with default parameters (`Spec 89` keeps existing parameter surface compatible).
* **Retain `MacOSHorizontalScrollCoordinator`**: Keep horizontal wheel scrolling + chevron step buttons intact on `FeedCategoryBar` and `SearchTabBar`.
* **120Hz GPU Budget (`ADR-07`)**: Keep `PhotoGridCard` attribution capsules on `GlassStyle.lightweight`; never apply live backdrop blur or `.blur` modifiers inside scrolling grid items.
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Document unified `AppRoute`, iOS 18+ `.navigationTransition(.zoom)` across all 5 grids, iOS/macOS 26 `.scrollEdgeEffectStyle(.soft, for: .top)`, persistent macOS category/search bars, and avatar hero continuity.
  * [`wiki/performance-and-memory.md`](../wiki/performance-and-memory.md) — Document removal of `ZStack` overlay synchronization overhead and lightweight OS 17–25 top-fade fallback.
* **Skill & Guideline Impact**:
  * [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md) — Resolve §4 contradiction by replacing `.toolbarBackground(.visible, for: .navigationBar)` with `.satinScrollEdge()` (`#available(iOS 26, macOS 26, *)` `.scrollEdgeEffectStyle(.soft, for: .top)` + OS 17–25 / 14–15 top gradient fade + persistent non-collapsing category/search bars on macOS + `PlatformToolbarButton` in macOS `.toolbar`), and update §5 to mandate unified `AppRoute`, `#available(iOS 18, *)` `.navigationTransition(.zoom)` + `.matchedTransitionSource`, `NavigationPreviewCache` seeding, and photographer avatar hero transitions.
  * [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) — Add OS 26 `.scrollEdgeEffectStyle(.soft)` Satin headers and Liquid Glass bars to §2.B Mode B triggers, and update §4 snapshot-ready Apple screens (`CollectionDetailView`, `FavoritesView`).

---

## 4. Verification & UI Visual Review Plan

* **Dual-Target Xcode Builds & Linters** (per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md)):
  ```bash
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' build CODE_SIGNING_ALLOWED=NO
  swiftformat --lint . && swiftlint lint iosApp/iosApp iosApp/iosAppTests iosApp/iosAppUITests
  ```
* **Mandatory Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Capture **Mode A** (`iosAppTests` + `macosAppTests` Light/Dark) and **Mode B** (`iosAppUITests` `UIWindowServerSnapshotUITests` + `Xcode MCP RenderPreview`) Before/After snapshots into `<Artifact Directory Path>/ui-review/spec-88/{before,after}/` verifying removal of the top double-bar seam, `.scrollEdgeEffectStyle(.soft, for: .top)`, `FeedCategoryBar` / `SearchTabBar`, and native macOS toolbars.
  * Inspect PNGs with `read_file` and obtain user approval before marking `Implemented (Final)`.

---

## 5. Acceptance Criteria

- [ ] `AppRoute` + `.appNavigationDestinations(...)` unify `FeedPathItem`, `CollectionPathItem`, and `FavoritesPathItem`, seeding `NavigationPreviewCache` across all entry points.
- [ ] On iOS 18+, `PhotosFeedView` and `FavoritesView` retire `selectedPhotoForHero` `ZStack` overlays and all 5 photo grids use `.matchedTransitionSource` + `.navigationTransition(.zoom)` (with iOS 17 fallback and brisk macOS `NavigationStack` transitions).
- [ ] All scrollable Apple screens apply `.satinScrollEdge()` (iOS/macOS 26 `.scrollEdgeEffectStyle(.soft, for: .top)` without `.toolbarBackground(.visible)` seam; OS 17–25 / 14–25 top gradient fade; persistent bars on macOS).
- [ ] Child `.toolbar` buttons use `PlatformToolbarButton`, `FeedCategoryBar`/`SearchTabBar` adopt OS 26 `.glassEffect(.regular.interactive(), in:)`, Shuffle uses `.symbolEffect(.bounce)`, and photographer avatars animate into `UserProfileView`.
- [ ] Mode A and Mode B Before/After visual review is approved, and `iosApp` + `macosApp` builds, `swiftformat --lint .`, and `swiftlint` pass with zero warnings.
- [ ] Affected [`wiki/`](../wiki/README.md) pages, [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md), and [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) are updated.

---

## 6. Verification Record

### Verified
*(Pending implementation)*

### Unverified / Gaps
*(Pending implementation)*
