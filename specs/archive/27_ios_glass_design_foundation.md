# Specification: iOS Glass Design Foundation & SwiftUI Material System

**Status:** Implemented

## Overview
This specification establishes the core Glassmorphic & Modern Apple Interface design foundation for the iOS and macOS application (`iosApp`). Currently, the app relies on flat, hardcoded dark-mode hex colors (`#0F0F11`, `#1E1E24`, `#2C2C35`), manual opacity fills, and lacks a centralized SwiftUI Material theme infrastructure, specular edge lighting definitions, and dynamic accessibility accommodations (such as "Reduce Transparency" and "Increase Contrast").

This spec delivers an atomic, foundational glass and material design infrastructure (`GlassTheme.swift`) establishing reusable material layers, specular gradient borders, vibrant typography hierarchy, and automatic fallback behavior for accessibility without altering individual screens yet.

---

## Current State & Deficiencies
1. **Hardcoded Solid Colors**: Views across `ContentView.swift`, `PhotoDetailsView.swift`, and `CollectionsFeedView.swift` hardcode `Color(hex: "0F0F11")` and `Color(hex: "1E1E24")`.
2. **Missing Specular Edge Lighting**: Glass surfaces on modern Apple platforms (iOS 17/18 and macOS 14/15) employ subtle gradient borders to convey physical translucency and edge separation against dynamic photo content. Currently, views use flat fills or ad-hoc strokes.
3. **No Accessibility "Reduce Transparency" & "Increase Contrast" Accommodations**: When users enable "Reduce Transparency" in iOS Accessibility settings, translucent backgrounds become unreadable or disorienting. When "Increase Contrast" is active, faint specular edges become invisible. There is no infrastructure to substitute opaque high-contrast surfaces and high-visibility borders.
4. **Scattered Material Definitions**: In some places `.ultraThinMaterial` is used directly in an ad-hoc manner (`PhotoDetailsView`), while other views use opaque gradients or solid black overlays (`Color.black.opacity(...)`).
5. **No Centralized Vibrancy or Elevation Tokens**: No standardized primitives exist for cards, pills, capsules, and typography vibrancy hierarchy (`.foregroundStyle(.primary)` / `.secondary` / `.tertiary` / `.quaternary`).

---

## Architectural Changes & Implementation Plan

### 1. File Structure & Component Responsibilities
Create a single cohesive design system file `iosApp/iosApp/GlassTheme.swift`:
*   `GlassTheme`: Design tokens namespace containing specular edge gradients, high-contrast borders, border widths, and ambient drop shadows.
*   `GlassStyle`: Preset configurations for standard floating glass elements (`.ultraThin`, `.thin`, `.regular`, `.thick`, `.ultraThick`).
*   `GlassBackgroundModifier<S: InsettableShape>`: A custom `ViewModifier` conforming to `InsettableShape` that renders frosted materials, ambient shadows, specular borders, and automatic accessibility fallbacks.
*   `GlassContainer<Content: View, S: InsettableShape>`: A container view wrapping child content in an accessible glass surface with convenient initializers for cards.
*   `View Extensions`: Semantic shortcuts (`.glassBackground(...)`, `.glassCapsule(...)`, `.glassCard(...)`).
*   `GlassVibrancy`: Semantic typography hierarchy modifier conforming to Apple HIG (`.primary`, `.secondary`, `.tertiary`, `.quaternary`).

### 2. Core Glass Design Tokens
*   **Materials Hierarchy**:
    *   `.ultraThinMaterial`: Inset floating attribution capsules, active category pills, and floating action buttons over imagery.
    *   `.thinMaterial`: Floating headers, category selection strips, and inspector deck cards.
    *   `.regularMaterial`: Modal navigation bars, dialog backdrops, and interactive sheets.
    *   `.thickMaterial`: Root navigation bars and persistent chrome.
    *   `.ultraThickMaterial`: High-contrast grounded sheets and elevated modal surfaces.
*   **Specular Edge Gradient**:
    *   A hairline (`0.5pt`) border with a subtle gradient from top-leading to bottom-trailing:
        ```swift
        LinearGradient(
            colors: [Color.white.opacity(0.28), Color.white.opacity(0.06)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        ```
    *   **High-Contrast / Accessibility Fallback Stroke**:
        When `accessibilityReduceTransparency` or `colorSchemeContrast == .increased` is active, adapt the stroke to a visible semantic separator:
        - iOS: `Color(uiColor: .separator)` with `lineWidth: 1.0`
        - macOS: `Color(nsColor: .separatorColor)` with `lineWidth: 1.0`
*   **Depth & Soft Ambient Shadows**:
    *   Ambient blur: `color: .black.opacity(0.18), radius: 10, x: 0, y: 4`.

---

### 3. Swift Implementation Blueprint (`GlassTheme.swift`)

```swift
import SwiftUI

// MARK: - GlassTheme Design Tokens

public enum GlassTheme {
    /// Standard specular edge hairline gradient (top-leading catchlight to bottom-trailing fade)
    public static let specularBorderGradient = LinearGradient(
        colors: [Color.white.opacity(0.28), Color.white.opacity(0.06)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    public static let specularBorderWidth: CGFloat = 0.5
    public static let fallbackBorderWidth: CGFloat = 1.0

    /// Subtle ambient drop shadow token
    public static let shadowColor = Color.black.opacity(0.18)
    public static let shadowRadius: CGFloat = 10
    public static let shadowX: CGFloat = 0
    public static let shadowY: CGFloat = 4

    /// Cross-platform semantic high-contrast background fallback
    public static var fallbackBackgroundColor: Color {
        #if os(iOS)
        return Color(uiColor: .secondarySystemBackground)
        #elseif os(macOS)
        return Color(nsColor: .windowBackgroundColor)
        #else
        return Color.secondary.opacity(0.2)
        #endif
    }

    /// Cross-platform semantic high-contrast border fallback
    public static var fallbackBorderColor: Color {
        #if os(iOS)
        return Color(uiColor: .separator)
        #elseif os(macOS)
        return Color(nsColor: .separatorColor)
        #else
        return Color.secondary
        #endif
    }
}

// MARK: - GlassStyle Presets

public enum GlassStyle {
    case ultraThin, thin, regular, thick, ultraThick

    public var material: Material {
        switch self {
        case .ultraThin: return .ultraThinMaterial
        case .thin: return .thinMaterial
        case .regular: return .regularMaterial
        case .thick: return .thickMaterial
        case .ultraThick: return .ultraThickMaterial
        }
    }
}

// MARK: - Accessible Glass ViewModifier

public struct GlassBackgroundModifier<S: InsettableShape>: ViewModifier {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    public let material: Material
    public let shape: S
    public let showBorder: Bool
    public let hasShadow: Bool

    public init(
        material: Material = .ultraThinMaterial,
        shape: S,
        showBorder: Bool = true,
        hasShadow: Bool = false
    ) {
        self.material = material
        self.shape = shape
        self.showBorder = showBorder
        self.hasShadow = hasShadow
    }

    public init(
        style: GlassStyle,
        shape: S,
        showBorder: Bool = true,
        hasShadow: Bool = false
    ) {
        self.init(material: style.material, shape: shape, showBorder: showBorder, hasShadow: hasShadow)
    }

    public func body(content: Content) -> some View {
        content
            .clipShape(shape)
            .contentShape(shape)
            .background {
                backgroundShape
            }
            .overlay {
                if showBorder {
                    borderShape
                        .allowsHitTesting(false)
                }
            }
    }

    @ViewBuilder
    private var backgroundShape: some View {
        let base = Group {
            if reduceTransparency {
                shape.fill(GlassTheme.fallbackBackgroundColor)
            } else {
                shape.fill(material)
            }
        }

        if hasShadow {
            base.shadow(
                color: GlassTheme.shadowColor,
                radius: GlassTheme.shadowRadius,
                x: GlassTheme.shadowX,
                y: GlassTheme.shadowY
            )
        } else {
            base
        }
    }

    @ViewBuilder
    private var borderShape: some View {
        if reduceTransparency || colorSchemeContrast == .increased {
            shape.strokeBorder(GlassTheme.fallbackBorderColor, lineWidth: GlassTheme.fallbackBorderWidth)
        } else {
            shape.strokeBorder(GlassTheme.specularBorderGradient, lineWidth: GlassTheme.specularBorderWidth)
        }
    }
}

// MARK: - GlassContainer View

public struct GlassContainer<Content: View, S: InsettableShape>: View {
    private let shape: S
    private let material: Material
    private let showBorder: Bool
    private let hasShadow: Bool
    private let content: Content

    public init(
        shape: S,
        material: Material = .ultraThinMaterial,
        showBorder: Bool = true,
        hasShadow: Bool = false,
        @ViewBuilder content: () -> Content
    ) {
        self.shape = shape
        self.material = material
        self.showBorder = showBorder
        self.hasShadow = hasShadow
        self.content = content()
    }

    public init(
        shape: S,
        style: GlassStyle,
        showBorder: Bool = true,
        hasShadow: Bool = false,
        @ViewBuilder content: () -> Content
    ) {
        self.init(shape: shape, material: style.material, showBorder: showBorder, hasShadow: hasShadow, content: content)
    }

    public var body: some View {
        content.modifier(
            GlassBackgroundModifier(
                material: material,
                shape: shape,
                showBorder: showBorder,
                hasShadow: hasShadow
            )
        )
    }
}

public extension GlassContainer where S == RoundedRectangle {
    init(
        cornerRadius: CGFloat = 16,
        material: Material = .thinMaterial,
        showBorder: Bool = true,
        hasShadow: Bool = true,
        @ViewBuilder content: () -> Content
    ) {
        self.init(
            shape: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous),
            material: material,
            showBorder: showBorder,
            hasShadow: hasShadow,
            content: content
        )
    }

    init(
        cornerRadius: CGFloat = 16,
        style: GlassStyle,
        showBorder: Bool = true,
        hasShadow: Bool = true,
        @ViewBuilder content: () -> Content
    ) {
        self.init(
            shape: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous),
            material: style.material,
            showBorder: showBorder,
            hasShadow: hasShadow,
            content: content
        )
    }
}

// MARK: - Semantic View Extensions

public extension View {
    /// Applies a glass material background with specular border and accessibility fallbacks.
    func glassBackground<S: InsettableShape>(
        material: Material = .ultraThinMaterial,
        shape: S,
        showBorder: Bool = true,
        hasShadow: Bool = false
    ) -> some View {
        modifier(GlassBackgroundModifier(material: material, shape: shape, showBorder: showBorder, hasShadow: hasShadow))
    }

    /// Applies a glass material background with specular border using a GlassStyle preset.
    func glassBackground<S: InsettableShape>(
        style: GlassStyle,
        shape: S,
        showBorder: Bool = true,
        hasShadow: Bool = false
    ) -> some View {
        glassBackground(material: style.material, shape: shape, showBorder: showBorder, hasShadow: hasShadow)
    }

    /// Convenience modifier for floating glass capsules (attribution pills, category badges).
    func glassCapsule(
        material: Material = .ultraThinMaterial,
        showBorder: Bool = true,
        hasShadow: Bool = false
    ) -> some View {
        glassBackground(material: material, shape: Capsule(), showBorder: showBorder, hasShadow: hasShadow)
    }

    /// Convenience modifier for floating glass capsules using a GlassStyle preset.
    func glassCapsule(
        style: GlassStyle,
        showBorder: Bool = true,
        hasShadow: Bool = false
    ) -> some View {
        glassCapsule(material: style.material, showBorder: showBorder, hasShadow: hasShadow)
    }

    /// Convenience modifier for rounded glass cards (info decks, sheets, modal panels).
    func glassCard(
        cornerRadius: CGFloat = 16,
        material: Material = .thinMaterial,
        showBorder: Bool = true,
        hasShadow: Bool = true
    ) -> some View {
        glassBackground(
            material: material,
            shape: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous),
            showBorder: showBorder,
            hasShadow: hasShadow
        )
    }

    /// Convenience modifier for rounded glass cards using a GlassStyle preset.
    func glassCard(
        cornerRadius: CGFloat = 16,
        style: GlassStyle,
        showBorder: Bool = true,
        hasShadow: Bool = true
    ) -> some View {
        glassCard(cornerRadius: cornerRadius, material: style.material, showBorder: showBorder, hasShadow: hasShadow)
    }
}

// MARK: - Vibrant Typography Hierarchy

public enum GlassVibrancy {
    case primary, secondary, tertiary, quaternary
}

public extension View {
    /// Applies semantic vibrancy foreground styling conforming to Apple HIG.
    @ViewBuilder
    func glassVibrancy(_ vibrancy: GlassVibrancy = .primary) -> some View {
        switch vibrancy {
        case .primary: self.foregroundStyle(.primary)
        case .secondary: self.foregroundStyle(.secondary)
        case .tertiary: self.foregroundStyle(.tertiary)
        case .quaternary: self.foregroundStyle(.quaternary)
        }
    }
}

// MARK: - Previews

#Preview("Glass Design Tokens & Hierarchy") {
    ZStack {
        LinearGradient(
            colors: [.indigo, .purple, .black],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .ignoresSafeArea()

        VStack(spacing: 20) {
            // Capsule Demo
            HStack(spacing: 8) {
                Image(systemName: "camera.fill")
                Text("Photographer Attribution")
                    .font(.caption.weight(.semibold))
            }
            .glassVibrancy(.primary)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .glassCapsule(style: .ultraThin)

            // Card Demo via GlassContainer
            GlassContainer(cornerRadius: 16, style: .thin) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("EXIF Details")
                        .font(.headline)
                        .glassVibrancy(.primary)
                    Text("ISO 100 • 50mm • f/1.8 • 1/250s")
                        .font(.subheadline)
                        .glassVibrancy(.secondary)
                    Text("Shot on Sony A7 IV")
                        .font(.caption)
                        .glassVibrancy(.tertiary)
                }
                .padding(16)
            }

            // Button Demo
            Button(
                action: {},
                label: {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 16, weight: .semibold))
                        .glassVibrancy(.primary)
                        .padding(12)
                        .glassBackground(style: .ultraThin, shape: Circle())
                }
            )
        }
        .padding()
    }
}

#Preview("Glass - Dark Mode") {
    ZStack {
        Color.black.ignoresSafeArea()
        VStack(spacing: 16) {
            Text("Dark Mode Preview")
                .font(.headline)
                .glassVibrancy(.primary)

            HStack {
                Image(systemName: "photo")
                Text("Attribution Pill")
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .glassCapsule()
        }
        .padding(24)
        .glassCard()
    }
    .preferredColorScheme(.dark)
}
```

---

### 4. Implementation Checklist
- [x] Create `iosApp/iosApp/GlassTheme.swift` with `GlassTheme` design tokens, `GlassStyle`, and `GlassBackgroundModifier<S: InsettableShape>`.
- [x] Support `@Environment(\.accessibilityReduceTransparency)` with opaque system background fallbacks (`secondarySystemBackground` / `windowBackgroundColor`).
- [x] Support `@Environment(\.colorSchemeContrast)` with high-visibility separator borders (`Color(uiColor: .separator)` / `Color(nsColor: .separatorColor)`).
- [x] Implement `GlassContainer` and semantic convenience extensions (`.glassBackground(...)`, `.glassCapsule()`, `.glassCard()`).
- [x] Implement `GlassVibrancy` hierarchy extensions (`.glassVibrancy(...)`).
- [x] Include interactive `#Preview` exercising glass capsules, cards, and vibrancy against colorful backdrops.
- [x] Re-generate Xcode project: `cd iosApp && xcodegen`.
- [x] Verify iOS build: `cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`.
- [x] Verify macOS build: `cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme macosApp -destination 'generic/platform=macOS' build CODE_SIGNING_ALLOWED=NO`.
- [x] Run SwiftLint: `swiftlint lint iosApp/iosApp/GlassTheme.swift`.
