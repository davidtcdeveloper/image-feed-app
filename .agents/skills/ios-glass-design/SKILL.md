---
name: ios-glass-design
description: >-
  Design and implement SwiftUI views for iOS and macOS following Apple HIG, GlassTheme
  design tokens, materials, detented sheets, background interactions, and sensory feedback.
---

# Apple HIG, Glass Design & SwiftUI Material System Runbook

## Purpose & Triggers

This skill guides the design, styling, motion, and interaction patterns for Apple platforms (iOS and macOS) within `iosApp`.
Use this skill when:
- Implementing or refactoring SwiftUI views, sheets, navigation bars, or cards.
- Applying `GlassTheme` design tokens, Apple semantic materials, and vibrant typography.
- Designing interactive detented sheets and full-canvas inspection experiences.
- Adding native iOS 17+ scroll transitions or declarative sensory haptics.
- Supporting accessibility (Reduce Transparency, Dynamic Type) and macOS desktop adaptations.

---

## 1. SwiftUI Materials & Translucency Standards

All Apple UI components MUST adhere to native Human Interface Guidelines (HIG) and the modular `iosApp/iosApp/DesignSystem/` token system ([`SurfaceTokens.swift`](../../../iosApp/iosApp/DesignSystem/SurfaceTokens.swift), [`PlatformToolbarButton.swift`](../../../iosApp/iosApp/DesignSystem/PlatformToolbarButton.swift), [`PhotoGridCard.swift`](../../../iosApp/iosApp/DesignSystem/PhotoGridCard.swift)).

### A. Tiered Material Selection Policy (`Matte → Satin → Float`)
- **Prohibited**: Hardcoded opaque dark backgrounds for bars, cards, or floating controls (e.g., solid `#0F0F11` or `#1E1E24`).
- **Prohibited**: Obscuring photos with heavy, opaque linear gradients (e.g. `LinearGradient(colors: [.clear, .black.opacity(0.85)])`).
- **Prohibited**: Stacking `.ultraThinMaterial` / `.thinMaterial` cards inside already-blurred sheets, over opaque sidebars/canvases, or over solid-color image placeholders (redundant blur passes).
- **Mandatory**: Use tiered materials based on rendering frequency and layer:
  - `.matteSurface()` / `.matteCapsule()`: Opaque, zero-blur semantic tonal fill (`GlassTheme.surfaceBackgroundColor`) + 0.5pt hairline border (`GlassTheme.matteBorderColor`) for collection mosaics, search user cards, analytics/insights cards, and interior inspector cards.
  - `GlassStyle.lightweight`: High-frequency 120Hz masonry grid attribution pills inside `PhotoGridCard` where real-time backdrop blur degrades compositor throughput.
  - `.ultraThinMaterial` / `.thinMaterial`: Persistent floating toolbars, active category/search bars, and inspection sheets over image content.
  - `.regularMaterial` / `.floatGlass(shape:)`: Modal navigation bars, dialog containers, context menus, and OS 26+ native Liquid Glass floating controls.
  - `.thickMaterial` / `.ultraThickMaterial`: Base chrome backgrounds where high contrast is required.

### B. Specular Edge Lighting & Ambient Shadows
Glass elements feature subtle specular highlights to define boundaries against dynamic photo backdrops:
- **Mandatory**: Use the centralized convenience modifiers and design tokens defined in [`SurfaceTokens.swift`](../../../iosApp/iosApp/DesignSystem/SurfaceTokens.swift):
  - View modifiers: `.matteSurface(...)`, `.matteCapsule(...)`, `.glassBackground(...)`, `.glassCapsule()`, `.glassCard()`, `.floatGlass(shape:)`.
  - Design tokens: `GlassTheme.specularBorderGradient`, `GlassTheme.matteBorderColor`, `GlassTheme.specularBorderWidth`, `GlassTheme.fallbackBorderWidth`.
- **Prohibited**: Inlining raw `.overlay` stroke borders with hardcoded color opacity gradients.
- **Ambient Shadows**: Standardize drop shadows using `GlassTheme.shadowColor`, `GlassTheme.shadowRadius`, and `GlassTheme.shadowY`.

---

## 2. Accessibility & "Reduce Transparency" Fallbacks

Always honor `@Environment(\.accessibilityReduceTransparency)`:
- When enabled, translucent material backgrounds must degrade gracefully to high-contrast opaque surfaces (e.g., `Color(uiColor: .secondarySystemBackground)` or a solid dark surface).
- Apply this logic through `GlassTheme` view modifiers so fallbacks remain consistent throughout the application.

---

## 3. Vibrant Typography & Contrast Compliance

- **Prohibited**: Low-contrast static grey text over variable photo backgrounds or translucent materials.
- **Mandatory**: Use Apple's semantic hierarchical styles:
  - `.foregroundStyle(.primary)`: High-vibrancy title and primary content text on glass.
  - `.foregroundStyle(.secondary)`: Metadata, timestamps, and captions on glass.
  - `.foregroundStyle(.tertiary)`: Non-critical annotations and icons.
  - `.glassVibrancy(.primary)` / `.glassVibrancy(.secondary)`: Semantic vibrancy tokens defined in `GlassTheme.swift`.
- Attribution over photos must reside in a compact, floating glass capsule rather than a heavy full-width bottom scrim.

### A. Container-Width Layout & Retina Display Scale for CDN Imagery
Never read `UIScreen.main.bounds.width` or `NSScreen.main?.frame.width` for grid column counts or CDN image widths:
- Measure actual container/pane geometry using `.measureContainerWidth($containerWidth)` (backed by `.onGeometryChange` on iOS 18+ / macOS 15+ and `PreferenceKey` fallback on iOS 17 / macOS 14) in [`AdaptiveLayoutHelper.swift`](../../../iosApp/iosApp/AdaptiveLayoutHelper.swift).
- Layout calculations in SwiftUI yield logical points, not physical pixels.
- Always multiply container points by the device's display scale (`pixelWidth = Int(containerPointWidth * displayScale)` using `AdaptiveLayoutHelper.displayScale()` — `UIScreen.main.scale` on iOS and `NSScreen.main?.backingScaleFactor ?? 2.0` on macOS) to prevent soft/blurry 1x assets on Retina screens.

### B. Zero Token Leakage & Theme Parity
- **Prohibited**: Hardcoded raw color constants (e.g. `.foregroundColor(.gray)`, `.foregroundColor(.white)`, `ProgressView().tint(.white)`, or `Color.white.opacity(...)` on light/dark adaptive canvases).
- All views (top bars, search screen, details sheet, user profile, charts, and skeletons) must dynamically adapt to both Light and Dark mode using `SurfaceTokens.swift` semantic tokens (`canvasBackgroundColor`, `surfaceBackgroundColor`, `subtleFillColor`, `activePillFillColor`, `activePillForegroundColor`, `fallbackBackgroundColor`) and Apple semantic styles (`.primary`, `.secondary`).

---

## 4. Modern Navigation Chrome & Translucent Bars

- **Prohibited**: Overriding `UITabBarAppearance` or `UINavigationBarAppearance` with opaque backgrounds (`appearance.configureWithOpaqueBackground()`).
- **Mandatory (iOS 26+ Native Liquid Glass `TabView`)**: Under `#available(iOS 26.0, *)` in [`PlatformNavigationShell.swift`](../../../iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift) and [`ContentView.swift`](../../../iosApp/iosApp/ContentView.swift), use Apple's native `TabView` with 4-destination parity (`Photos`, `Collections`, `Favorites`, and `Tab(..., role: .search)` hosting `SearchTabView`) and `.tabBarMinimizeBehavior(.onScrollDown)`. Never suppress `UITabBar` or apply `.toolbar(.hidden, for: .tabBar)` on root tabs on iOS 26+ (gate legacy tab bar hiding via `adaptiveTabBarVisibility()`).
- **Mandatory (iOS 17–25 Fallback)**: Isolate `UITabBar` suppression, `CapsuleScrollCoordinator`, and [`FloatingTabCapsule.swift`](../../../iosApp/iosApp/FloatingTabCapsule.swift) strictly behind `#unavailable(iOS 26)` inside [`PlatformNavigationShell.swift`](../../../iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift), keeping 4-tab parity and `accessibilityReduceTransparency` / `colorSchemeContrast == .increased` fallbacks intact.
- **Mandatory (macOS Sidebar & `⌘F` Ergonomics)**: Expose `.search` in [`MacOSContentView.swift`](../../../iosApp/iosApp/MacOSContentView.swift)'s `NavigationSplitView` sidebar rendering `SearchTabView`, and route window-level `⌘F` (and `CollectionsFeedTabView` `onSearchClick`) via `openRootSearch` / `searchFocusTrigger` rather than opening nested search stacks inside feed tabs. Keep `FeedCategoryBar` and `SearchTabBar` persistent on macOS (never auto-hiding on scroll).
- **Mandatory (Satin Scroll Edges & OS 26 Float Glass Bars)**: Never stack `.toolbarBackground(.visible, for: .navigationBar)` above `.safeAreaInset(edge: .top)` bars (double-bar seam). Apply `.satinNavigationBar()` and `.satinScrollEdge()` from [`SurfaceTokens.swift`](../../../iosApp/iosApp/DesignSystem/SurfaceTokens.swift) across all scrollable screens (using native `.scrollEdgeEffectStyle(.soft, for: .top)` on iOS/macOS 26+ and a lightweight zero-blur canvas-color top gradient fade on OS 17–25 / macOS 14–25). Style floating category/search capsules with `.floatGlass(shape: Capsule())` (applying `.glassEffect(.regular.interactive(), in:)` inside `.background` so child `CategoryTabButton` pills retain full Light/Dark contrast), and use `PlatformToolbarButton` in all child `.toolbar` slots for native macOS AppKit / macOS 26 Tahoe buttons.

---

## 5. iOS 17+ Motion, Transitions & Sensory Feedback

### Unified `AppRoute`, Zoom Transitions & Avatar Continuity
- Route all cross-screen pushes through unified `AppRoute` (`photo`, `collection`, `user`, `search`) and `.appNavigationDestinations(path:heroNamespace:)` in [`AppNavigation.swift`](../../../iosApp/iosApp/DesignSystem/AppNavigation.swift), seeding `MetroHelper.shared` `NavigationPreviewCache` via `NavigationPreviewSeeder` across all photo grids, collection cards, user rows, and random shuffle triggers.
- On iOS 18+, retire `ZStack` hero overlays and use `.matchedTransitionSource(id:in:)` (note: `MatchedTransitionSourceConfiguration.clipShape` only supports `RoundedRectangle`) on [`PhotoGridCard.swift`](../../../iosApp/iosApp/DesignSystem/PhotoGridCard.swift) (`photo-\(id)` with `12pt` radius and `user-\(username)` avatar source), [`SearchView.swift`](../../../iosApp/iosApp/SearchView.swift) `UserCardView`, and [`CollectionDetailView.swift`](../../../iosApp/iosApp/CollectionDetailView.swift) curator avatar, paired with `.navigationTransition(.zoom(sourceID:in:))` on pushed `PhotoDetailsView` and `UserProfileView` destinations.
- Preserve `#unavailable(iOS 18.0)` `ZStack` overlay fallbacks (`selectedPhotoForHero` + subtle `FeedCategoryBar` cross-fade) on iOS 17, and use pure `NavigationStack` with brisk `~250–300ms` transitions on macOS.

### Scroll-Driven Transitions & Symbol Effects
- Apply native iOS 17 scroll transitions for cards and feed items using [`.feedScrollTransition()`](../../../iosApp/iosApp/AnimationPrimitives.swift).
- Apply `.symbolEffect(.bounce, value:)` on Shuffle triggers in [`PhotosFeedView.swift`](../../../iosApp/iosApp/PhotosFeedView.swift).

### Declarative Sensory Feedback
Use SwiftUI declarative `.sensoryFeedback` instead of imperative feedback generators:
- `.sensoryFeedback(.impact(weight: .light), trigger: value)` for tab switching, category changes, and filter selections.
- `.sensoryFeedback(.success, trigger: value)` for photo download completions or bookmarking actions.

---

## 6. Full-Canvas Presentation & Interactive Detents

### Full-Canvas Photo Inspection (iPhone)
- In detail views, high-resolution imagery takes center stage with full-width canvas presentation, pinch-to-zoom (1.0x to 4.0x), and double-tap toggle.
- Non-modal interactive inspector sheets are configured using SwiftUI detents:
  - Reference canonical sheet configuration in [`PhotoDetailsView.swift`](../../../iosApp/iosApp/PhotoDetailsView.swift).
  - Multi-tier detents: `.presentationDetents([.fraction(0.35), .fraction(0.70), .large])` with background interaction enabled via `.presentationBackgroundInteraction(.enabled(upThrough: .fraction(0.70)))`.
  - Contrast & Scrim: In `.presentationBackground`, layer `GlassTheme.highContrastScrimColor.opacity(0.82)` over `.regularMaterial` (or fallback to `GlassTheme.fallbackBackgroundColor` when `accessibilityReduceTransparency` is true) to ensure WCAG AAA ($\ge 7:1$) contrast over bright imagery.
- Automatically dismiss the inspector sheet prior to navigation push when users tap tags or photographer profiles.

### Adaptive Split-Pane (iPad & macOS)
- When `horizontalSizeClass == .regular` (or running on macOS), display photo canvas and inspector side-by-side instead of using a bottom sheet.
- Wrap iOS-specific sheet modifiers and size class queries in `#if os(iOS)` / `#if os(macOS)` compiler guards.

### macOS Compile-Safety Checklist
`iosApp/iosApp` sources are shared with the `macosApp` target. Guard these iOS-only APIs with `#if os(iOS)` (providing an AppKit/SwiftUI alternative under `#if os(macOS)` when behavior is needed):
- UIKit types: `UIScreen`, `UIImage`, `UIApplication`, `UIImpactFeedbackGenerator`.
- Sheet and navigation modifiers: `presentationDetents`, `presentationBackgroundInteraction`, `navigationBarTitleDisplayMode`, `.topBarLeading` / `.topBarTrailing` toolbar placements.
- Text and chrome modifiers: `textInputAutocapitalization`, `statusBarHidden`.
- Size-class queries that assume touch layouts.
- Living reference for guarded platform branching: [`AdaptiveLayoutHelper.swift`](../../../iosApp/iosApp/AdaptiveLayoutHelper.swift).

---

## 7. MVI ViewModel Lifecycle & Swift 6 Concurrency

- All Swift `@Observable` wrappers MUST be declared `@MainActor @Observable final class` with `@ObservationIgnored private nonisolated(unsafe) var ...Closeable: Closeable?` and `MainActor.assumeIsolated` inside KMP `watch` closures (see [`FavoritesViewModel.swift`](../../../iosApp/iosApp/FavoritesViewModel.swift)).
- Swift ViewModels MUST cancel closeables and invoke `viewModel.clear()` upon dismissal or inside `deinit` to cancel in-flight coroutines and close effect channels.

