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

All Apple UI components MUST adhere to native Human Interface Guidelines (HIG) and the project's `GlassTheme` system.

### A. Semantic Materials over Solid Fills
- **Prohibited**: Hardcoded opaque dark backgrounds for bars, cards, or floating controls (e.g., solid `#0F0F11` or `#1E1E24`).
- **Prohibited**: Obscuring photos with heavy, opaque linear gradients (e.g. `LinearGradient(colors: [.clear, .black.opacity(0.85)])`).
- **Mandatory**: Use native Apple semantic `Material` backgrounds:
  - `.ultraThinMaterial`: Inset floating attribution capsules, active chips, floating toolbars, and inspection sheets over image content.
  - `.thinMaterial`: Floating category filter bars, secondary inspection cards.
  - `.regularMaterial`: Modal navigation bars, dialog containers, and context menus.
  - `.thickMaterial` / `.ultraThickMaterial`: Base chrome backgrounds where high contrast is required.

### B. Specular Edge Lighting & Ambient Shadows
Glass elements feature subtle specular highlights to define boundaries against dynamic photo backdrops:
- **Mandatory**: Use the centralized convenience modifiers and design tokens defined in [`GlassTheme.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/GlassTheme.swift):
  - View modifiers: `.glassBackground(...)`, `.glassCapsule()`, `.glassCard()`.
  - Design tokens: `GlassTheme.specularBorderGradient`, `GlassTheme.specularBorderWidth`, `GlassTheme.fallbackBorderWidth`.
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

### A. Retina Display Scale for CDN Imagery
When computing target photo dimensions for network requests (e.g. in `AdaptiveLayoutHelper`):
- Layout calculations in SwiftUI yield logical points, not physical pixels.
- Always multiply points by the device's display scale to prevent soft/blurry 1x assets on Retina screens:
  ```swift
  #if os(iOS)
  let scale = UIScreen.main.scale
  #elseif os(macOS)
  let scale = NSScreen.main?.backingScaleFactor ?? 2.0
  #endif
  let pixelWidth = Int(pointWidth * scale)
  ```

### B. Zero Token Leakage & Theme Parity
- **Prohibited**: Hardcoded raw color constants (e.g. `.foregroundColor(.gray)`, `.foregroundColor(.white)`).
- All views (top bars, search screen, details sheet, user profile) must dynamically adapt to both Light and Dark mode using `GlassTheme.colors` and Apple semantic styles.

---

## 4. Modern Navigation Chrome & Translucent Bars

- **Prohibited**: Overriding `UITabBarAppearance` or `UINavigationBarAppearance` with opaque backgrounds (`appearance.configureWithOpaqueBackground()`).
- **Mandatory**: Use native SwiftUI toolbar background modifiers:
  ```swift
  .toolbarBackground(.ultraThinMaterial, for: .navigationBar, .tabBar)
  .toolbarBackground(.visible, for: .navigationBar, .tabBar)
  ```
- Allow imagery and scrollable content to draw edge-to-edge under the navigation and tab chrome.

---

## 5. iOS 17+ Motion, Transitions & Sensory Feedback

### Scroll-Driven Transitions
Apply native iOS 17 scroll transitions for cards and feed items using the shared [`.feedScrollTransition()`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/AnimationPrimitives.swift) modifier defined in `AnimationPrimitives.swift`.

### Declarative Sensory Feedback
Use SwiftUI declarative `.sensoryFeedback` instead of imperative feedback generators:
- `.sensoryFeedback(.impact(weight: .light), trigger: value)` for tab switching, category changes, and filter selections.
- `.sensoryFeedback(.success, trigger: value)` for photo download completions or bookmarking actions.

---

## 6. Full-Canvas Presentation & Interactive Detents

### Full-Canvas Photo Inspection (iPhone)
- In detail views, high-resolution imagery takes center stage with full-width canvas presentation, pinch-to-zoom (1.0x to 4.0x), and double-tap toggle.
- Non-modal interactive inspector sheets are configured using SwiftUI detents:
  - Reference canonical sheet configuration in [`PhotoDetailsView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsView.swift).
  - Multi-tier detents: `.presentationDetents([.fraction(0.35), .fraction(0.70), .large])` with background interaction enabled via `.presentationBackgroundInteraction(.enabled(upThrough: .fraction(0.70)))`.
  - Contrast & Scrim: In `.presentationBackground`, layer `GlassTheme.highContrastScrimColor.opacity(0.82)` over `.regularMaterial` (or fallback to `GlassTheme.fallbackBackgroundColor` when `accessibilityReduceTransparency` is true) to ensure WCAG AAA ($\ge 7:1$) contrast over bright imagery.
- Automatically dismiss the inspector sheet prior to navigation push when users tap tags or photographer profiles.

### Adaptive Split-Pane (iPad & macOS)
- When `horizontalSizeClass == .regular` (or running on macOS), display photo canvas and inspector side-by-side instead of using a bottom sheet.
- Wrap iOS-specific sheet modifiers and size class queries in `#if os(iOS)` / `#if os(macOS)` compiler guards.

---

## 7. Presenter Lifecycle in Swift

- Platform wrappers (Swift `ObservableObject` ViewModels) own shared presenter lifecycle.
- Swift ViewModels MUST invoke `presenter.clear()` upon dismissal or inside `deinit` to cancel in-flight coroutines.
