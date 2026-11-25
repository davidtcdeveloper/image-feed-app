import SwiftUI
#if os(macOS)
import AppKit
#elseif os(iOS)
import UIKit
#endif

// MARK: - GlassTheme & Matte Surface Design Tokens

public enum GlassTheme {
    /// Standard specular edge hairline gradient (top-leading catchlight to bottom-trailing fade)
    public static let specularBorderGradient = LinearGradient(
        colors: [Color.white.opacity(0.28), Color.white.opacity(0.06)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing)

    public static let specularBorderWidth: CGFloat = 0.5
    public static let fallbackBorderWidth: CGFloat = 1.0
    public static let matteBorderWidth: CGFloat = 0.5

    /// Tactile action orb specular rim border width (0.75pt)
    public static let tactileOrbBorderWidth: CGFloat = 0.75
    /// Tactile action orb dimension (40x40pt)
    public static let tactileOrbSize: CGFloat = 40

    /// Subtle ambient drop shadow token
    public static let shadowColor = Color.black.opacity(0.18)
    public static let shadowRadius: CGFloat = 10
    public static let shadowX: CGFloat = 0
    public static let shadowY: CGFloat = 4

    /// Cross-platform semantic high-contrast background fallback
    public static var fallbackBackgroundColor: Color {
        #if os(iOS)
        Color(uiColor: .secondarySystemBackground)
        #elseif os(macOS)
        Color(nsColor: .windowBackgroundColor)
        #else
        Color.secondary.opacity(0.2)
        #endif
    }

    /// Cross-platform semantic high-contrast border fallback
    public static var fallbackBorderColor: Color {
        #if os(iOS)
        Color(uiColor: .separator)
        #elseif os(macOS)
        Color(nsColor: .separatorColor)
        #else
        Color.secondary
        #endif
    }

    /// Adaptive hairline border for Matte surfaces across Light & Dark mode
    public static var matteBorderColor: Color {
        #if os(iOS)
        Color(uiColor: .separator).opacity(0.55)
        #elseif os(macOS)
        Color(nsColor: .separatorColor).opacity(0.55)
        #else
        Color.primary.opacity(0.12)
        #endif
    }

    /// Cross-platform semantic canvas background
    public static var canvasBackgroundColor: Color {
        #if os(iOS)
        Color(uiColor: .systemBackground)
        #elseif os(macOS)
        Color(nsColor: .windowBackgroundColor)
        #else
        Color.black
        #endif
    }

    /// Cross-platform secondary surface background for cards, chips, skeletons
    public static var surfaceBackgroundColor: Color {
        #if os(iOS)
        Color(uiColor: .secondarySystemBackground)
        #elseif os(macOS)
        Color(nsColor: .controlBackgroundColor)
        #else
        Color.secondary.opacity(0.15)
        #endif
    }

    /// Adaptive subtle pill/chip background across Light & Dark mode
    public static var subtleFillColor: Color {
        Color.primary.opacity(0.08)
    }

    /// Adaptive active selection pill background (high contrast in both Light & Dark mode)
    public static var activePillFillColor: Color {
        Color.primary
    }

    /// Adaptive foreground color for text/icons rendered inside `activePillFillColor`
    public static var activePillForegroundColor: Color {
        canvasBackgroundColor
    }

    /// High-contrast dark scrim color (0x0A, 0x0A, 0x0C) for WCAG AAA compliance over bright imagery
    public static let highContrastScrimColor = Color(red: 10 / 255.0, green: 10 / 255.0, blue: 12 / 255.0)
}

// MARK: - Matte Surface Modifier (Zero Live Blur)

public struct MatteSurfaceModifier<S: InsettableShape>: ViewModifier {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    public let shape: S
    public let fillColor: Color?
    public let showBorder: Bool

    public init(shape: S, fillColor: Color? = nil, showBorder: Bool = true) {
        self.shape = shape
        self.fillColor = fillColor
        self.showBorder = showBorder
    }

    public func body(content: Content) -> some View {
        let baseFill = fillColor ?? GlassTheme.surfaceBackgroundColor
        content
            .clipShape(shape)
            .contentShape(shape)
            .background {
                shape.fill(reduceTransparency ? GlassTheme.fallbackBackgroundColor : baseFill)
            }
            .overlay {
                if showBorder {
                    if reduceTransparency || colorSchemeContrast == .increased {
                        shape.strokeBorder(GlassTheme.fallbackBorderColor, lineWidth: GlassTheme.fallbackBorderWidth)
                            .allowsHitTesting(false)
                    } else {
                        shape.strokeBorder(GlassTheme.matteBorderColor, lineWidth: GlassTheme.matteBorderWidth)
                            .allowsHitTesting(false)
                    }
                }
            }
    }
}

// MARK: - Float Glass Modifier (OS 26+ Native Liquid Glass with Material Fallback)

public struct FloatGlassModifier<S: InsettableShape>: ViewModifier {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    public let shape: S

    public init(shape: S) {
        self.shape = shape
    }

    public func body(content: Content) -> some View {
        if #available(iOS 26.0, macOS 26.0, *), !reduceTransparency {
            content
                .glassEffect(.regular.interactive(), in: shape)
        } else {
            content
                .glassBackground(style: .regular, shape: shape, showBorder: true, hasShadow: true)
        }
    }
}

// MARK: - GlassStyle Presets

public enum GlassStyle {
    case ultraThin, thin, regular, thick, ultraThick, lightweight

    public var material: Material {
        switch self {
        case .ultraThin, .lightweight: .ultraThinMaterial
        case .thin: .thinMaterial
        case .regular: .regularMaterial
        case .thick: .thickMaterial
        case .ultraThick: .ultraThickMaterial
        }
    }
}

// MARK: - Accessible Glass ViewModifier

public struct GlassBackgroundModifier<S: InsettableShape>: ViewModifier {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    public let material: Material
    public let style: GlassStyle?
    public let shape: S
    public let showBorder: Bool
    public let hasShadow: Bool

    public init(
        material: Material = .ultraThinMaterial,
        shape: S,
        showBorder: Bool = true,
        hasShadow: Bool = false)
    {
        self.material = material
        self.style = nil
        self.shape = shape
        self.showBorder = showBorder
        self.hasShadow = hasShadow
    }

    public init(
        style: GlassStyle,
        shape: S,
        showBorder: Bool = true,
        hasShadow: Bool = false)
    {
        self.material = style.material
        self.style = style
        self.shape = shape
        self.showBorder = showBorder
        self.hasShadow = (style == .lightweight) ? false : hasShadow
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
            } else if style == .lightweight {
                shape.fill(GlassTheme.highContrastScrimColor.opacity(0.62))
            } else {
                shape.fill(material)
            }
        }

        if hasShadow, style != .lightweight {
            base.shadow(
                color: GlassTheme.shadowColor,
                radius: GlassTheme.shadowRadius,
                x: GlassTheme.shadowX,
                y: GlassTheme.shadowY)
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
    private let style: GlassStyle?
    private let showBorder: Bool
    private let hasShadow: Bool
    private let content: Content

    public init(
        shape: S,
        material: Material = .ultraThinMaterial,
        showBorder: Bool = true,
        hasShadow: Bool = false,
        @ViewBuilder content: () -> Content)
    {
        self.shape = shape
        self.material = material
        self.style = nil
        self.showBorder = showBorder
        self.hasShadow = hasShadow
        self.content = content()
    }

    public init(
        shape: S,
        style: GlassStyle,
        showBorder: Bool = true,
        hasShadow: Bool = false,
        @ViewBuilder content: () -> Content)
    {
        self.shape = shape
        self.material = style.material
        self.style = style
        self.showBorder = showBorder
        self.hasShadow = (style == .lightweight) ? false : hasShadow
        self.content = content()
    }

    public var body: some View {
        if let style {
            content.modifier(
                GlassBackgroundModifier(
                    style: style,
                    shape: shape,
                    showBorder: showBorder,
                    hasShadow: hasShadow))
        } else {
            content.modifier(
                GlassBackgroundModifier(
                    material: material,
                    shape: shape,
                    showBorder: showBorder,
                    hasShadow: hasShadow))
        }
    }
}

public extension GlassContainer where S == RoundedRectangle {
    init(
        cornerRadius: CGFloat = 16,
        material: Material = .thinMaterial,
        showBorder: Bool = true,
        hasShadow: Bool = true,
        @ViewBuilder content: () -> Content)
    {
        self.init(
            shape: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous),
            material: material,
            showBorder: showBorder,
            hasShadow: hasShadow,
            content: content)
    }

    init(
        cornerRadius: CGFloat = 16,
        style: GlassStyle,
        showBorder: Bool = true,
        hasShadow: Bool = true,
        @ViewBuilder content: () -> Content)
    {
        self.init(
            shape: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous),
            style: style,
            showBorder: showBorder,
            hasShadow: hasShadow,
            content: content)
    }
}

// MARK: - Semantic View Extensions

public extension View {
    /// Applies an opaque Matte tonal surface with a crisp semantic hairline border (zero backdrop blur).
    func matteSurface(
        shape: some InsettableShape,
        fillColor: Color? = nil,
        showBorder: Bool = true) -> some View
    {
        modifier(MatteSurfaceModifier(shape: shape, fillColor: fillColor, showBorder: showBorder))
    }

    /// Convenience Matte rounded card surface (zero backdrop blur).
    func matteSurface(
        cornerRadius: CGFloat = 12,
        fillColor: Color? = nil,
        showBorder: Bool = true) -> some View
    {
        matteSurface(
            shape: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous),
            fillColor: fillColor,
            showBorder: showBorder)
    }

    /// Convenience Matte capsule surface (zero backdrop blur).
    func matteCapsule(
        fillColor: Color? = nil,
        showBorder: Bool = true) -> some View
    {
        matteSurface(shape: Capsule(), fillColor: fillColor, showBorder: showBorder)
    }

    /// Scaffold modifier for Wave-2 Satin top scroll-edge feathering (Spec 88).
    func satinScrollEdge() -> some View {
        self
    }

    /// Applies native Liquid Glass on iOS 26+ / macOS 26+ with accessible `.regularMaterial` fallback on earlier OSes.
    func floatGlass(shape: some InsettableShape) -> some View {
        modifier(FloatGlassModifier(shape: shape))
    }

    /// Applies a glass material background with specular border and accessibility fallbacks.
    func glassBackground(
        material: Material = .ultraThinMaterial,
        shape: some InsettableShape,
        showBorder: Bool = true,
        hasShadow: Bool = false) -> some View
    {
        modifier(GlassBackgroundModifier(material: material, shape: shape, showBorder: showBorder, hasShadow: hasShadow))
    }

    /// Applies a glass material background with specular border using a GlassStyle preset.
    func glassBackground(
        style: GlassStyle,
        shape: some InsettableShape,
        showBorder: Bool = true,
        hasShadow: Bool = false) -> some View
    {
        modifier(GlassBackgroundModifier(style: style, shape: shape, showBorder: showBorder, hasShadow: hasShadow))
    }

    /// Convenience modifier for floating glass capsules (attribution pills, category badges).
    func glassCapsule(
        material: Material = .ultraThinMaterial,
        showBorder: Bool = true,
        hasShadow: Bool = false) -> some View
    {
        glassBackground(material: material, shape: Capsule(), showBorder: showBorder, hasShadow: hasShadow)
    }

    /// Convenience modifier for floating glass capsules using a GlassStyle preset.
    func glassCapsule(
        style: GlassStyle,
        showBorder: Bool = true,
        hasShadow: Bool = false) -> some View
    {
        glassBackground(style: style, shape: Capsule(), showBorder: showBorder, hasShadow: hasShadow)
    }

    /// Convenience modifier for rounded glass cards (info decks, sheets, modal panels).
    func glassCard(
        cornerRadius: CGFloat = 16,
        material: Material = .thinMaterial,
        showBorder: Bool = true,
        hasShadow: Bool = true) -> some View
    {
        glassBackground(
            material: material,
            shape: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous),
            showBorder: showBorder,
            hasShadow: hasShadow)
    }

    /// Convenience modifier for rounded glass cards using a GlassStyle preset.
    func glassCard(
        cornerRadius: CGFloat = 16,
        style: GlassStyle,
        showBorder: Bool = true,
        hasShadow: Bool = true) -> some View
    {
        glassBackground(
            style: style,
            shape: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous),
            showBorder: showBorder,
            hasShadow: hasShadow)
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
        case .primary: foregroundStyle(.primary)
        case .secondary: foregroundStyle(.secondary)
        case .tertiary: foregroundStyle(.tertiary)
        case .quaternary: foregroundStyle(.quaternary)
        }
    }
}

// MARK: - Shared Error State View

struct ErrorView: View {
    let error: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text("Error Loading Feed")
                .font(.headline)
                .glassVibrancy(.primary)

            Text(error)
                .font(.subheadline)
                .glassVibrancy(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Button(action: onRetry) {
                Text("Retry")
                    .font(.subheadline.weight(.semibold))
                    .glassVibrancy(.primary)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .matteCapsule()
            }
            .buttonStyle(.plain)
        }
    }
}

// MARK: - Color Hex Parsing Extension

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 30, 30, 36)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255)
    }
}
