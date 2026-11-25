# Specification 83: Apple DesignSystem Modularization, Matte Surfaces, Light-Mode Parity & Swift 6 ViewModels

**Status:** Accepted

* **Wave:** Wave 1 — Disjoint Foundations
* **Depends On:** None
* **Parallelizable With:** [Spec 81](81_kmp_color_engine_and_navigation_seed_cache.md), [Spec 82](82_android_design_system_matte_and_adaptive_panes.md)
* **Research Mapping:** [Cross-Platform Material Craft Evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) — `ARCH-08`, `ARCH-09`, Craft `2.1`, Craft `2.2` (Apple feed/profile), Craft `2.7` (Apple Light Mode audit)
* **Exclusive File Ownership:** `iosApp/iosApp/DesignSystem/**`, `PhotosFeedView.swift`, [`GlassTheme.swift`](../iosApp/iosApp/GlassTheme.swift), [`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift), [`AnimationPrimitives.swift`](../iosApp/iosApp/AnimationPrimitives.swift), [`ContentView.swift`](../iosApp/iosApp/ContentView.swift) (extraction only), [`CollectionsFeedView.swift`](../iosApp/iosApp/CollectionsFeedView.swift), [`CollectionDetailView.swift`](../iosApp/iosApp/CollectionDetailView.swift), [`FavoritesView.swift`](../iosApp/iosApp/FavoritesView.swift), [`SearchView.swift`](../iosApp/iosApp/SearchView.swift), [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift), [`FeedViewModel.swift`](../iosApp/iosApp/FeedViewModel.swift), [`CollectionsFeedViewModel.swift`](../iosApp/iosApp/CollectionsFeedViewModel.swift), [`CollectionDetailViewModel.swift`](../iosApp/iosApp/CollectionDetailViewModel.swift), [`SearchViewModel.swift`](../iosApp/iosApp/SearchViewModel.swift), [`UserProfileViewModel.swift`](../iosApp/iosApp/UserProfileViewModel.swift), [`iosApp/project.yml`](../iosApp/project.yml), `iosApp/iosApp.xcodeproj/project.pbxproj`

---

## 1. Overview & Codebase Baseline

Auditing `iosApp/iosApp/` (shared by both `iosApp` and `macosApp`) exposes four structural prerequisites before iOS 26 / macOS 26 Liquid Glass, `.scrollEdgeEffectStyle(.soft)`, and iOS 18 `.navigationTransition(.zoom)` crafts can land cleanly in parallel:

1. **Monolithic Theme & Duplicated Cards (`ARCH-08`)**: [`GlassTheme.swift`](../iosApp/iosApp/GlassTheme.swift) (`647` lines) mixes surface tokens, 40×40pt circular `GlassToolbarButton` orbs, and AppKit `NSEvent` scroll-wheel bridging; photo grid cards are duplicated across [`ContentView.swift`](../iosApp/iosApp/ContentView.swift#L791), [`SearchView.swift`](../iosApp/iosApp/SearchView.swift#L329), [`CollectionDetailView.swift`](../iosApp/iosApp/CollectionDetailView.swift#L316), and [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift#L440); and [`iosApp/project.yml`](../iosApp/project.yml#L66-L239) contains duplicated test target definitions (`iosAppTests`, `macosAppTests`, `iosAppUITests`).
2. **Screen-Width Assumptions (`ARCH-08`)**: [`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift#L24-L51) reads `UIScreen.main.bounds.width` and `NSScreen.main?.frame.width`, breaking iPad Split View / Stage Manager and resizable macOS windows while hardcoding macOS `columnCount = 3` and `collectionColumnCount = 2`.
3. **Inconsistent Actor Isolation (`ARCH-09`)**: Only [`FavoritesViewModel.swift`](../iosApp/iosApp/FavoritesViewModel.swift#L4-L6) and [`PhotoDetailsViewModel.swift`](../iosApp/iosApp/PhotoDetailsViewModel.swift#L9-L11) declare `@MainActor @Observable final class`, while the other 5 Swift ViewModels omit `@MainActor` and `final`.
4. **Light Mode Leaks & Redundant Live Blur (Craft `2.2` & `2.7`)**: 15+ `ProgressView().tint(.white)` calls, hardcoded `.black`/`.white` pills in `CategoryTabButton` and `FilterButtonView`, `Color.white.opacity(0.06)` skeletons in [`AnimationPrimitives.swift`](../iosApp/iosApp/AnimationPrimitives.swift#L104-L175), `.white` Swift Charts marks + nested `.ultraThin` cards in [`InsightsView`](../iosApp/iosApp/UserProfileView.swift#L600-L681), and unframed [`CollectionMosaicCard`](../iosApp/iosApp/CollectionsFeedView.swift#L151-L297) cards degrade Light Mode legibility and iPad/macOS containment.

---

## 2. Scope & Technical Workstreams

### Workstream A (`ARCH-08`): `DesignSystem/` Split, File Topology & `project.yml` Cleanup
1. Create `iosApp/iosApp/DesignSystem/` and decompose [`GlassTheme.swift`](../iosApp/iosApp/GlassTheme.swift):
   * `SurfaceTokens.swift` — Expose `.matteSurface()` (semantic tonal fill + 0.5pt hairline border, zero backdrop blur), `.satinScrollEdge()` (scaffold modifier for Wave-2 Spec 88), and OS-gated `.floatGlass()` (`#available(iOS 26, macOS 26, *)`), while preserving `GlassTheme` / `.glassCard` / `.glassCapsule` compatibility for untouched `PhotoDetailsView` / `PhotoInspectorView` (`Spec 89`).
   * `PlatformToolbarButton.swift` — Render standard native SwiftUI `Button`/`Label` inside `.toolbar` under `#if os(macOS)` and tactile/Liquid Glass orb under `#if os(iOS)`.
   * `MacOSHorizontalScrollBridge.swift` — Isolate `MacOSHorizontalScrollCoordinator` and AppKit `NSEvent` wheel monitor.
   * `PhotoGridCard.swift` — Unify all 4 photo grid cards (`PhotoCard`, `SearchPhotoGridCard`, `CollectionPhotoGridCard`, `GridPhotosList`) into one configurable component with `GlassStyle.lightweight` attribution pill, avatar tap, and context menu support.
   * Scaffold empty/minimal `PlatformNavigationShell.swift`, `AppNavigation.swift`, and extracted `PhotosFeedView.swift` (splitting `PhotosFeedTabView` out of [`ContentView.swift`](../iosApp/iosApp/ContentView.swift#L299-L789)).
2. Remove the duplicated `iosAppTests`, `macosAppTests`, and `iosAppUITests` target blocks in [`iosApp/project.yml`](../iosApp/project.yml#L181-L239) and run `xcodegen generate --spec iosApp/project.yml` so Wave-2 Specs 87, 88, and 89 modify existing files with zero `project.pbxproj` conflicts.

### Workstream B (`ARCH-08`): Container-Width `AdaptiveLayoutHelper`
1. Replace `UIScreen.main.bounds.width` and `NSScreen.main?.frame.width` in [`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift#L24-L51) with container-aware pane width measurement (`.onGeometryChange(for: CGFloat.self)` on iOS 18+ / macOS 15+, quantized background width reader on iOS 17 / macOS 14).
2. Dynamically compute photo columns (`2–5` across iPhone, iPad Split View, and resizable macOS windows) and collection columns (`1–3`) from actual container width, multiplying container column points by display scale (`UIScreen.main.scale` / `NSScreen.main?.backingScaleFactor ?? 2.0`) per [Business Rules](../wiki/business-rules.md#L19).

### Workstream C (`ARCH-09`): `@MainActor @Observable final class` ViewModel Parity
1. Standardize [`FeedViewModel.swift`](../iosApp/iosApp/FeedViewModel.swift), [`CollectionsFeedViewModel.swift`](../iosApp/iosApp/CollectionsFeedViewModel.swift), [`CollectionDetailViewModel.swift`](../iosApp/iosApp/CollectionDetailViewModel.swift), [`SearchViewModel.swift`](../iosApp/iosApp/SearchViewModel.swift), and [`UserProfileViewModel.swift`](../iosApp/iosApp/UserProfileViewModel.swift) to `@MainActor @Observable final class` with `MainActor.assumeIsolated` inside KMP `watch` closures and `@ObservationIgnored private nonisolated(unsafe) var ...Closeable: Closeable?` matching [`FavoritesViewModel.swift`](../iosApp/iosApp/FavoritesViewModel.swift#L4-L13), preserving `viewModel.clear()` in `deinit`.

### Workstream D (Craft `2.2` & `2.7`): Matte Framing & Light-Mode Token Audit
1. Replace `.tint(.white)` `ProgressView` calls across `PhotosFeedView`, [`CollectionsFeedView.swift`](../iosApp/iosApp/CollectionsFeedView.swift#L89), [`CollectionDetailView.swift`](../iosApp/iosApp/CollectionDetailView.swift#L181), [`SearchView.swift`](../iosApp/iosApp/SearchView.swift#L205), and [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift#L473) with semantic `.tint(.primary)`.
2. Refactor `CategoryTabButton` and `FilterButtonView` to adaptive semantic foreground/capsule tokens; bind [`AnimationPrimitives.swift`](../iosApp/iosApp/AnimationPrimitives.swift#L104-L175) skeleton placeholders to `GlassTheme.surfaceBackgroundColor`.
3. Convert nested `.glassCard(cornerRadius: 12, style: .ultraThin)` cards and `.white` Swift Charts marks in [`InsightsView`](../iosApp/iosApp/UserProfileView.swift#L600-L681) to `.matteSurface()` + semantic `.primary` strokes, and wrap [`CollectionMosaicCard`](../iosApp/iosApp/CollectionsFeedView.swift#L151-L297) in `.matteSurface()`.

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **Disjoint Apple Ownership**: Do not modify `PhotoDetailsView.swift`, `PhotoInspectorView.swift`, or `PhotoDetailsViewModel.swift` (`Spec 89` owns them), and avoid touching `MacOSContentView.swift` (`Spec 87`).
* **Multi-Target Guarding**: Every edit under `iosApp/iosApp/` must compile on both `iosApp` (`iOS 17.0+`) and `macosApp` (`macOS 14.0+`) with zero compiler warnings.
* **Accessibility**: All `.matteSurface()` and `CategoryTabButton` updates must honor `accessibilityReduceTransparency` and `colorSchemeContrast == .increased`.
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Document `DesignSystem/` modular split, `PhotoGridCard`, `.matteSurface()`, and container-aware `AdaptiveLayoutHelper`.
  * [`wiki/architecture-and-modules.md`](../wiki/architecture-and-modules.md) — Document `@MainActor @Observable final class` ViewModel parity.
  * [`wiki/performance-and-memory.md`](../wiki/performance-and-memory.md) — Document removal of redundant live blurs inside `InsightsView` and accurate container-width CDN scaling.
* **Skill & Guideline Impact**:
  * [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md) — Update §1 (`SwiftUI Materials & Translucency Standards`) to reference modular `iosApp/iosApp/DesignSystem/` (`SurfaceTokens.swift`, `PlatformToolbarButton.swift`, `PhotoGridCard.swift`), define `.matteSurface()` (prohibiting nested `.ultraThinMaterial` / `.thinMaterial` cards inside already-blurred sheets or opaque sidebars), update §3.A to mandate container-width measurement via `.onGeometryChange` instead of `UIScreen.main.bounds` / `NSScreen.main?.frame`, and update §7 to require `@MainActor @Observable final class` + `MainActor.assumeIsolated` across all Swift ViewModels.
  * [`AGENTS.md`](../AGENTS.md) — Update line 15 skill index summary from `GlassTheme` to `DesignSystem` tokens if `GlassTheme.swift` is fully retired into `DesignSystem/` (preserving `< 75` lines).
  * [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) — Append newly snapshot-ready Apple views (`PhotosFeedView`, `CollectionsFeedView`, `SearchView`, `UserProfileView`) under §4 `Currently Snapshot-Ready Screens`.

---

## 4. Verification & UI Visual Review Plan

* **XcodeGen & Dual-Target Builds** (per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md)):
  ```bash
  xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' build CODE_SIGNING_ALLOWED=NO
  ```
* **Swift Formatting & Static Analysis**:
  ```bash
  swiftformat --lint . && swiftlint lint iosApp/iosApp iosApp/iosAppTests iosApp/iosAppUITests
  ```
* **Mandatory Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Add progressive `#if DEBUG` preview/fixture readiness for modified screens (Feed, Collections, Search, UserProfile Insights) and run **Mode A** (`iosAppTests` Light/Dark + `macosAppTests` Light/Dark) outputting ephemeral PNGs into `<Artifact Directory Path>/ui-review/spec-83/{before,after}/`.
  * Inspect representative PNGs with `read_file` and obtain user approval on the visual diff artifact before marking `Implemented (Final)`.

---

## 5. Acceptance Criteria

- [ ] `iosApp/iosApp/DesignSystem/` splits `GlassTheme.swift` into `SurfaceTokens.swift`, `PlatformToolbarButton.swift`, `MacOSHorizontalScrollBridge.swift`, `PhotoGridCard.swift`, and scaffolds `PlatformNavigationShell.swift` / `AppNavigation.swift`.
- [ ] `iosApp/project.yml` removes duplicated test target blocks and `xcodegen generate --spec iosApp/project.yml` cleanly regenerates `project.pbxproj`.
- [ ] `AdaptiveLayoutHelper` computes columns and Retina CDN pixel widths from actual container geometry on iOS, iPadOS, and macOS.
- [ ] All 7 Swift ViewModels conform to `@MainActor @Observable final class` with deterministic `viewModel.clear()` teardown and zero Swift 6 warnings.
- [ ] Light Mode token leaks (`ProgressView().tint(.white)`, `CategoryTabButton`, `FilterButtonView`, skeleton shimmer, `InsightsView` charts) and `CollectionMosaicCard` / `InsightsView` `.matteSurface()` conversions pass visual review.
- [ ] `iosApp` and `macosApp` builds, Mode A snapshot tests, `swiftformat --lint .`, and `swiftlint` pass with zero serious violations.
- [ ] Affected [`wiki/`](../wiki/README.md) pages, [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md), [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md), and [`AGENTS.md`](../AGENTS.md) (if `GlassTheme.swift` retires) are updated.

---

## 6. Verification Record

### Verified
*(Pending implementation)*

### Unverified / Gaps
*(Pending implementation)*
