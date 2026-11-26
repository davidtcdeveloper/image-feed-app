# Specification 87: Apple Float Glass Navigation Shell, iOS 26 Liquid Glass TabView & macOS Unified Search

**Status:** Implemented (Final)

* **Wave:** Wave 2 — Parallel Feature & Craft Execution
* **Depends On:** [Spec 83](83_apple_design_system_matte_and_light_mode_hygiene.md)
* **Parallelizable With:** [Spec 84](84_android_float_navigation_and_random_mvi.md), [Spec 85](85_android_satin_headers_and_shared_transitions.md), [Spec 86](86_android_immersive_photo_details_and_zoom.md), [Spec 88](88_apple_satin_headers_zoom_transitions_and_unified_routes.md), [Spec 89](89_apple_photo_details_inspector_parity_and_glass_morphing.md)
* **Research Mapping:** [Cross-Platform Material Craft Evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) — Craft `2.5` (iOS 26 Liquid Glass `TabView` + 4-tab parity + macOS sidebar Search)
* **Exclusive File Ownership:** [`ContentView.swift`](../iosApp/iosApp/ContentView.swift), [`MacOSContentView.swift`](../iosApp/iosApp/MacOSContentView.swift), [`FloatingTabCapsule.swift`](../iosApp/iosApp/FloatingTabCapsule.swift), `iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift`

---

## 1. Overview & Codebase Baseline

Following [Spec 83](83_apple_design_system_matte_and_light_mode_hygiene.md)'s extraction of `PhotosFeedView.swift` and `PlatformNavigationShell.swift` scaffolding, Apple's top-level navigation chrome still exhibits a major platform-tax gap (Craft `2.5`):

1. **Hidden Native `UITabBar` on iOS 26+**: [`ContentView.swift`](../iosApp/iosApp/ContentView.swift#L84-L147) unconditionally sets `UITabBar.appearance().isHidden = true` and `.toolbar(.hidden, for: .tabBar)`, substituting a custom `.regularMaterial` [`FloatingTabCapsule.swift`](../iosApp/iosApp/FloatingTabCapsule.swift) driven by `CapsuleScrollCoordinator`. On iOS 26+ (compiled against the installed Xcode `27.2` / `iphoneos27.2` SDK), this blocks Apple's native refractive Liquid Glass `TabView`, Clear/Tinted glass preferences, and native scroll minimization.
2. **3-Tab vs. 4-Tab Parity Gap**: Android exposes 4 top-level destinations (`Photos`, `Collections`, `Favorites`, `Search`), whereas iOS ([`ContentView.swift`](../iosApp/iosApp/ContentView.swift#L107-L131)) and macOS ([`MacOSContentView.swift`](../iosApp/iosApp/MacOSContentView.swift#L5-L27)) expose only 3 tabs/sidebar items, burying Search inside child toolbar icons.

---

## 2. Scope & Technical Workstreams

### Workstream A (Craft `2.5`): iOS 26+ Native Liquid Glass `TabView` & 4-Tab Parity
1. Implement `DesignSystem/PlatformNavigationShell.swift` and rewire [`ContentView.swift`](../iosApp/iosApp/ContentView.swift):
   * Under `#available(iOS 26, *)`, do **not** hide `UITabBar` or apply `.toolbar(.hidden, for: .tabBar)`.
   * Render a native iOS 26 `TabView` with 4 destinations — `Photos`, `Collections`, `Favorites`, and **`Tab(role: .search)`** hosting `SearchView` — delivering 4-destination parity with Android.
   * Attach **`.tabBarMinimizeBehavior(.onScrollDown)`** so Apple natively minimizes the floating tab bar during feed scrolling, retiring `CapsuleScrollCoordinator` scroll-offset overhead on iOS 26+ and honoring system Liquid Glass / Reduce Transparency automatically.

### Workstream B: iOS 17–25 Backward-Compatible `FloatingTabCapsule` Path
1. Isolate `UITabBar` suppression, [`FloatingTabCapsule.swift`](../iosApp/iosApp/FloatingTabCapsule.swift), and `CapsuleScrollCoordinator` strictly behind `#unavailable(iOS 26)` inside `PlatformNavigationShell.swift` / `ContentView.swift` (preserving `deploymentTarget: "17.0"` in [`iosApp/project.yml`](../iosApp/project.yml#L11)).
2. Align destination parity on iOS 17–25 (supporting the 4th `Search` tab inside `FloatingTabCapsule`) while maintaining Light/Dark mode, `accessibilityReduceTransparency`, and `colorSchemeContrast == .increased` fallbacks.

### Workstream C: macOS `NavigationSplitView` Search Destination & `⌘F` Ergonomics
1. Add `.search` (`Label("Search", systemImage: "magnifyingglass")`) to [`MacOSContentView.swift`](../iosApp/iosApp/MacOSContentView.swift#L5-L44)'s `SidebarSelection` enum, sidebar `List`, and `detail` switch (rendering `SearchView` in the detail pane with `minWidth: 500`).
2. Bind window-level `⌘F` keyboard shortcut on macOS to focus/select the `.search` sidebar destination cleanly without opening nested duplicate search stacks inside feed tabs.

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **Disjoint Ownership**: Do not edit `PhotosFeedView.swift`, `SearchView.swift`, `CollectionsFeedView.swift`, `FavoritesView.swift`, `AppNavigation.swift` (`Spec 88`), or `PhotoDetailsView.swift` (`Spec 89`). Keep `PhotosFeedTabView`, `CollectionsFeedTabView`, `FavoritesTabView`, and `SearchView` wrapper interfaces compatible across Specs 87 and 88.
* **macOS Guarding**: All `UITabBar`, `.tabBarMinimizeBehavior`, and iOS `FloatingTabCapsule` logic must be guarded under `#if os(iOS)` so `macosApp` builds cleanly with zero warnings.
* **No `project.yml` Edits**: All target files were registered in `Spec 83`.
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Document iOS 26+ native Liquid Glass `TabView` (`.tabBarMinimizeBehavior(.onScrollDown)` + `Tab(role: .search)`), iOS 17–25 `FloatingTabCapsule` fallback, and macOS sidebar Search + `⌘F`.
* **Skill & Guideline Impact**:
  * [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md) — Update §4 (`Modern Navigation Chrome`) to mandate native `#available(iOS 26, *)` Liquid Glass `TabView` + `.tabBarMinimizeBehavior(.onScrollDown)` + 4-tab parity (`Tab(role: .search)`), isolating `FloatingTabCapsule` strictly under `#unavailable(iOS 26)`, plus macOS `NavigationSplitView` sidebar Search (`⌘F`).
  * [`README.md`](../README.md) — Update the `Screen Navigation Architecture` Mermaid diagram and iOS/macOS navigation bullets (`#L69-L137`) to reflect 4-destination parity (`Photos | Collections | Favorites | Search`) across Android, iOS, and macOS.

---

## 4. Verification & UI Visual Review Plan

* **Dual-Target Xcode Builds & Linters** (per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md)):
  ```bash
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' build CODE_SIGNING_ALLOWED=NO
  swiftformat --lint . && swiftlint lint iosApp/iosApp iosApp/iosAppTests iosApp/iosAppUITests
  ```
* **Mandatory Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Capture **Mode A** (`macosAppTests` split-view shell Light/Dark) and **Mode B** (`iosAppUITests` `UIWindowServerSnapshotUITests` + `Xcode MCP RenderPreview`) Before/After snapshots into `<Artifact Directory Path>/ui-review/spec-87/{before,after}/` to verify native Liquid Glass `TabView` / `FloatingTabCapsule` and macOS sidebar Search.
  * Inspect PNGs with `read_file` and obtain user approval before marking `Implemented (Final)`.

---

## 5. Acceptance Criteria

- [x] On iOS 26+, `ContentView` / `PlatformNavigationShell` uses native `TabView` with Liquid Glass, `.tabBarMinimizeBehavior(.onScrollDown)`, and 4 tabs (`Photos`, `Collections`, `Favorites`, `Tab(role: .search)`), retiring `CapsuleScrollCoordinator` on OS 26+.
- [x] On iOS 17–25, `FloatingTabCapsule` + `CapsuleScrollCoordinator` remain isolated under `#unavailable(iOS 26)` with full `accessibilityReduceTransparency` and Light/Dark parity.
- [x] `MacOSContentView` exposes `.search` in `SidebarSelection` with `⌘F` keyboard activation.
- [x] Mode A (`macosAppTests`) and Mode B (`iosAppUITests`) Before/After visual review is approved, and `iosApp` + `macosApp` builds, `swiftformat --lint .`, and `swiftlint` pass with zero warnings.
- [x] Affected [`wiki/`](../wiki/README.md) pages, [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md), and [`README.md`](../README.md) are updated.

---

## 6. Verification Record

### Verified
* `swiftformat --lint .` — Exited `0` (`0/35` files require formatting).
* `swiftlint lint iosApp/iosApp iosApp/iosAppTests iosApp/iosAppUITests` — Exited `0` (`0` errors; pre-existing repo-wide `opening_brace` / `trailing_comma` / legacy length warnings documented in [`ISS-007`](../../wiki/issue-log.md)).
* `xcodegen generate --spec iosApp/project.yml` + `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' CODE_SIGNING_ALLOWED=NO` — Exited `0` with **zero Swift compiler warnings**; `macosAppTests` passed **`7/7`** (`AdaptiveLayoutHelperTests` `3/3`, `PlatformNavigationShellTests` `3/3`, `UIVisualSnapshotTests` `1/1` rendering 11 macOS PNGs including `macos_navigation_shell_light`, `macos_navigation_shell_dark`, and `macos_navigation_shell_search_light`).
* `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 18 Pro' CODE_SIGNING_ALLOWED=NO` — Exited `0` with **zero Swift compiler warnings**; `iosAppTests` passed **`6/6`** (`AdaptiveLayoutHelperTests` `3/3`, `PlatformNavigationShellTests` `2/2`, `UIVisualSnapshotTests` `1/1` rendering 13 iOS PNGs including `ios_floating_tab_capsule_light` and `ios_floating_tab_capsule_dark`) and `iosAppUITests` passed **`1/1`** (`UIWindowServerSnapshotUITests` rendering `ios_window_photoDetails`, `ios_window_navigationShell`, and `ios_window_navigationShellSearch` on iOS 27.2 Simulator).
* `./gradlew :androidApp:assembleDebug` — Exited `0` (`BUILD SUCCESSFUL`).
* Before and After Mode A & Mode B snapshots inspected with `read_file` and **approved by the user** in session artifact `spec_87_ui_visual_review.md`.
* Updated [`wiki/design-systems-and-navigation.md`](../../wiki/design-systems-and-navigation.md), [`.agents/skills/ios-glass-design/SKILL.md`](../../.agents/skills/ios-glass-design/SKILL.md), [`README.md`](../../README.md), and logged [`ISS-008`](../../wiki/issue-log.md) covering the Spec 83 `adaptiveTabBarVisibility()` / `⌘F` ownership drift.

### Unverified / Gaps
* Live runtime execution on an iOS 17–25 Simulator runtime was not performed on this host (Xcode `27.2` ships `iOS 27.x` runtimes); the `#unavailable(iOS 26)` `LegacyFloatingTabShell` code path was verified by compilation (`deploymentTarget: "17.0"`), Mode A Light/Dark `FloatingTabCapsule` snapshots, and `CapsuleScrollCoordinator` unit tests.
