# Specification: iOS Glass Design Foundation & SwiftUI Material System

**Status:** New / Not Implemented

## Overview
This specification establishes the core Glassmorphic & Modern Apple Interface design foundation for the iOS and macOS application (`iosApp`). Currently, the app relies on flat, hardcoded dark-mode hex colors (`#0F0F11`, `#1E1E24`, `#2C2C35`), manual opacity fills, and lacks a centralized SwiftUI Material theme infrastructure, specular edge lighting definitions, and dynamic accessibility accommodations (such as "Reduce Transparency").

This spec delivers an atomic, foundational glass and material design infrastructure (`GlassTheme.swift` / `GlassModifiers.swift`) establishing reusable material layers, specular gradient borders, vibrant typography hierarchy, and automatic fallback behavior for accessibility without altering individual screens yet.

---

## Current State & Deficiencies
1. **Hardcoded Solid Colors**: Views across `ContentView.swift`, `PhotoDetailsView.swift`, and `CollectionsFeedView.swift` hardcode `Color(hex: "0F0F11")` and `Color(hex: "1E1E24")`.
2. **Missing Specular Edge Lighting**: Glass surfaces on modern Apple platforms (iOS 17/18 and macOS 14/15) employ subtle gradient borders to convey depth and physical translucency against variable photo content. Currently, views use flat fills or ad-hoc strokes.
3. **No Accessibility "Reduce Transparency" Fallback**: When users enable "Reduce Transparency" in iOS Accessibility settings, translucent backgrounds become unreadable or disorienting. There is no mechanism to substitute opaque high-contrast surfaces.
4. **Scattered Material Definitions**: In some places `.ultraThinMaterial` is used directly in an ad-hoc manner (`PhotoDetailsView`), while other views use opaque gradients or solid black overlays (`Color.black.opacity(...)`).

---

## Architectural Changes & Implementation Plan

### 1. File Structure
Create `iosApp/iosApp/GlassTheme.swift`:
*   `GlassStyle`: Reusable styling configurations for floating glass elements (ultra-thin, thin, regular, thick).
*   `GlassModifier`: A custom `ViewModifier` providing frosted material backgrounds, subtle drop shadows, and specular edge lighting.
*   `GlassContainer`: A container view and modifier supporting dynamic fallback when `@Environment(\.accessibilityReduceTransparency)` is active.
*   `Vibrancy`: Typography styling extensions pairing text with Apple's vibrant `.foregroundStyle(.primary)` / `.secondary`.

### 2. Core Glass Design Tokens
*   **Materials Hierarchy**:
    *   `.ultraThinMaterial`: Inset floating attribution capsules, active category pills, and floating action buttons over imagery.
    *   `.thinMaterial`: Floating headers, category selection strips, and inspector deck cards.
    *   `.regularMaterial`: Modal navigation bars, dialog backdrops, and interactive sheets.
    *   `.thickMaterial`: Root navigation bars and persistent chrome.
*   **Specular Edge Gradient**:
    *   A hairline (`0.5pt`) border with a subtle gradient from top-leading to bottom-trailing:
    ```swift
    LinearGradient(
        colors: [Color.white.opacity(0.30), Color.white.opacity(0.06)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
    ```
*   **Depth & Soft Shadows**:
    *   Ambient blur: `color: .black.opacity(0.18), radius: 10, x: 0, y: 4`.

### 3. Accessible Glass ViewModifier
```swift
struct GlassBackgroundModifier<S: Shape>: ViewModifier {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    let material: Material
    let shape: S
    let showBorder: Bool

    func body(content: Content) -> some View {
        content
            .background {
                if reduceTransparency {
                    #if canImport(UIKit)
                    shape.fill(Color(uiColor: .secondarySystemBackground))
                    #elseif canImport(AppKit)
                    shape.fill(Color(nsColor: .windowBackgroundColor))
                    #endif
                } else {
                    shape.fill(material)
                }
            }
            .overlay {
                if showBorder {
                    shape.strokeBorder(
                        LinearGradient(
                            colors: [Color.white.opacity(0.28), Color.white.opacity(0.06)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 0.5
                    )
                }
            }
    }
}
```

### 4. Implementation Checklist
- [ ] Create `iosApp/iosApp/GlassTheme.swift` with `GlassBackgroundModifier`, semantic convenience extensions (`.glassBackground(...)`, `.glassCapsule()`, `.glassCard()`).
- [ ] Define specular border gradients and soft ambient shadow styles.
- [ ] Add `@Environment(\.accessibilityReduceTransparency)` fallback support across all glass primitives.
- [ ] Implement vibrant text modifiers conforming to Apple HIG hierarchy.
- [ ] Verify compilation with Xcode across iOS Simulator (`iOS 17.0+`) and macOS (`14.0+`).
