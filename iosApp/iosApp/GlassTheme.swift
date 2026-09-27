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

// MARK: - Glass Toolbar Controls

public struct GlassToolbarButton: View {
    public let systemName: String
    public let accessibilityLabel: String?
    public let action: () -> Void

    public init(
        systemName: String,
        accessibilityLabel: String? = nil,
        action: @escaping () -> Void
    ) {
        self.systemName = systemName
        self.accessibilityLabel = accessibilityLabel
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 14, weight: .semibold))
                .glassVibrancy(.primary)
                .frame(width: 34, height: 34)
                .glassBackground(style: .ultraThin, shape: Circle(), showBorder: true, hasShadow: true)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(accessibilityLabel ?? systemName))
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
