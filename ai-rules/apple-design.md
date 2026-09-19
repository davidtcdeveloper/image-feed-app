# Apple Human Interface & Modern Glass Design Rules

## Purpose
Enforce modern Apple Human Interface Guidelines (HIG) and Glassmorphic interface standards across the iOS and macOS applications (`iosApp`). This rule inherits and implements the cross-platform standards defined in `ai-rules/design-principles.md`. All new SwiftUI views and refactoring of existing screens MUST adhere to these rules.

---

## 1. SwiftUI Materials & Translucency Standards

### Semantic Materials over Solid Fills
*   **PROHIBITED**: Hardcoded opaque dark backgrounds for toolbars, sheets, or overlaid content (e.g., solid `#0F0F11` or `#1E1E24` on floating controls and bars).
*   **PROHIBITED**: Obscuring photos with heavy, opaque linear gradients (e.g. `LinearGradient(colors: [.clear, .black.opacity(0.85)])`).
*   **MANDATORY**: Apply Apple's native semantic `Material` backgrounds:
    *   `.ultraThinMaterial`: Inset floating attribution capsules, active chips, floating toolbars, and inspection sheets over image content.
    *   `.thinMaterial`: Floating category filter bars, secondary inspection cards.
    *   `.regularMaterial`: Modal navigation bars, dialog containers, and context menus.
    *   `.thickMaterial` / `.ultraThickMaterial`: Base chrome backgrounds where high contrast is required.

### Specular Edge Lighting & Depth
*   **MANDATORY**: Glass elements must feature subtle specular highlights to define boundaries against dynamic photo backdrops:
    ```swift
    .overlay(
        RoundedRectangle(cornerRadius: r)
            .strokeBorder(
                LinearGradient(
                    colors: [Color.white.opacity(0.25), Color.white.opacity(0.05)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                lineWidth: 0.5
            )
    )
    ```
*   **Shadows**: Use low-opacity, wide-radius drop shadows (`.shadow(color: .black.opacity(0.18), radius: 10, y: 4)`) instead of harsh, high-opacity shadows.

---

## 2. Accessibility & "Reduce Transparency" Support

*   **MANDATORY**: Always honor the user's `@Environment(\.accessibilityReduceTransparency)` preference.
*   When `accessibilityReduceTransparency == true`, translucent `Material` backgrounds must degrade gracefully to high-contrast opaque semantic colors:
    *   Fallback: `Color(uiColor: .secondarySystemBackground)` or high-contrast dark surface.
*   Use a standardized view modifier or extension (e.g. `.glassBackground(...)`) so fallback logic is consistent application-wide.

---

## 3. Vibrant Typography & Contrast Compliance

*   **PROHIBITED**: Low-contrast static grey text over variable image content or translucent materials.
*   **MANDATORY**: Leverage Apple's semantic hierarchical styles:
    *   `.foregroundStyle(.primary)`: High-vibrancy title and primary content text on glass.
    *   `.foregroundStyle(.secondary)`: Metadata, timestamps, captions on glass.
    *   `.foregroundStyle(.tertiary)`: Subtle iconography and non-critical annotations.
*   Text over photo backgrounds must either reside within a material capsule or utilize a subtle text shadow when rendered directly over image pixels.

---

## 4. Modern Navigation Chrome & Bars

*   **PROHIBITED**: Overriding `UITabBarAppearance` or `UINavigationBarAppearance` with opaque background colors (`appearance.configureWithOpaqueBackground()`).
*   **MANDATORY**: Utilize native SwiftUI navigation and toolbar background modifiers:
    *   `.toolbarBackground(.ultraThinMaterial, for: .navigationBar, .tabBar)`
    *   `.toolbarColorScheme(.dark, for: .navigationBar)` (when dark palette is desired)
*   Allow content to scroll seamlessly under navigation and tab bars to showcase native blur and depth.

---

## 5. iOS 17+ Motion, Scroll Transitions & Feedback

*   **Scroll-Driven Transitions**: Use native iOS 17 `.scrollTransition` for fluid entry and exit of feed items:
    ```swift
    .scrollTransition(topLeading: .interactive, bottomTrailing: .interactive) { content, phase in
        content
            .scaleEffect(phase.isIdentity ? 1.0 : 0.96)
            .opacity(phase.isIdentity ? 1.0 : 0.85)
    }
    ```
*   **Declarative Haptics**: Replace imperative `UIImpactFeedbackGenerator` calls with SwiftUI's declarative `.sensoryFeedback`:
    *   `.sensoryFeedback(.impact(weight: .light), trigger: value)` for tab, category, or filter selection.
    *   `.sensoryFeedback(.success, trigger: value)` for download completion or bookmark actions.

---

## 6. Full-Canvas & Interactive Detents

*   **Photo Inspection**: Large media content must take center stage. Avoid rigid vertical lists where the image scrolls away.
*   Use native `.sheet(isPresented:)` with `.presentationDetents([.fraction(0.35), .fraction(0.7), .large])` and `.presentationBackground(.ultraThinMaterial)` for photo metadata, EXIF, and interactive charts on iPhone.
