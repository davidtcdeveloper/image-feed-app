import SwiftUI

// MARK: - Platform Toolbar Controls

/// Renders native SwiftUI toolbar buttons on macOS and tactile/Liquid Glass orbs on iOS.
public struct PlatformToolbarButton: View {
    public let systemName: String
    public let accessibilityLabel: LocalizedStringKey?
    public let action: () -> Void

    public init(
        systemName: String,
        accessibilityLabel: LocalizedStringKey? = nil,
        action: @escaping () -> Void)
    {
        self.systemName = systemName
        self.accessibilityLabel = accessibilityLabel
        self.action = action
    }

    public var body: some View {
        #if os(macOS)
        Button(action: action) {
            Label(accessibilityLabel ?? LocalizedStringKey(systemName), systemImage: systemName)
        }
        #else
        if #available(iOS 26.0, *) {
            Button(action: action) {
                Label(accessibilityLabel ?? LocalizedStringKey(systemName), systemImage: systemName)
            }
        } else {
            GlassToolbarButton(
                systemName: systemName,
                accessibilityLabel: accessibilityLabel,
                action: action)
        }
        #endif
    }
}

/// Tactile circular glass orb preserved for `PhotoDetailsView` overlay compatibility (`Spec 89`).
public struct GlassToolbarButton: View {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    public let systemName: String
    public let accessibilityLabel: LocalizedStringKey?
    public let action: () -> Void

    public init(
        systemName: String,
        accessibilityLabel: LocalizedStringKey? = nil,
        action: @escaping () -> Void)
    {
        self.systemName = systemName
        self.accessibilityLabel = accessibilityLabel
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 15, weight: .semibold))
                .symbolRenderingMode(.hierarchical)
                .glassVibrancy(.primary)
                .frame(width: GlassTheme.tactileOrbSize, height: GlassTheme.tactileOrbSize)
                .background {
                    if reduceTransparency {
                        Circle().fill(GlassTheme.fallbackBackgroundColor)
                    } else {
                        Circle().fill(.thinMaterial)
                    }
                }
                .overlay {
                    if reduceTransparency || colorSchemeContrast == .increased {
                        Circle().strokeBorder(GlassTheme.fallbackBorderColor, lineWidth: GlassTheme.fallbackBorderWidth)
                    } else {
                        Circle().strokeBorder(GlassTheme.specularBorderGradient, lineWidth: GlassTheme.tactileOrbBorderWidth)
                    }
                }
                .clipShape(Circle())
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(accessibilityLabel ?? LocalizedStringKey(systemName)))
    }
}
