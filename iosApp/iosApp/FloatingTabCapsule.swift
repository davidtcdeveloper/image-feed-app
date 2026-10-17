import SwiftUI

public struct FloatingTabCapsule: View {
    @Binding var selectedTab: Int
    var isMinimized: Bool
    @Namespace private var tabIndicatorNamespace
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    public init(selectedTab: Binding<Int>, isMinimized: Bool = false) {
        self._selectedTab = selectedTab
        self.isMinimized = isMinimized
    }

    public var body: some View {
        HStack(spacing: 4) {
            tabButton(index: 0, title: "Photos", systemImage: "photo.stack")
            tabButton(index: 1, title: "Collections", systemImage: "square.grid.2x2")
        }
        .padding(4)
        .background {
            if reduceTransparency {
                Capsule()
                    .fill(GlassTheme.fallbackBackgroundColor)
            } else {
                Capsule()
                    .fill(.regularMaterial)
            }
        }
        .overlay {
            if reduceTransparency || colorSchemeContrast == .increased {
                Capsule()
                    .strokeBorder(GlassTheme.fallbackBorderColor, lineWidth: GlassTheme.fallbackBorderWidth)
            } else {
                Capsule()
                    .strokeBorder(
                        LinearGradient(
                            colors: [Color.white.opacity(0.40), Color.white.opacity(0.05)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing),
                        lineWidth: GlassTheme.specularBorderWidth)
            }
        }
        .shadow(
            color: GlassTheme.shadowColor,
            radius: GlassTheme.shadowRadius,
            x: GlassTheme.shadowX,
            y: GlassTheme.shadowY)
        .scaleEffect(isMinimized ? 0.88 : 1.0)
        .offset(y: isMinimized ? 22 : 0)
        .opacity(isMinimized ? 0.65 : 1.0)
        .animation(.spring(response: 0.35, dampingFraction: 0.75), value: isMinimized)
        .animation(.spring(response: 0.35, dampingFraction: 0.75), value: selectedTab)
        .sensoryFeedback(.impact(weight: .light), trigger: selectedTab)
    }

    private func tabButton(index: Int, title: LocalizedStringKey, systemImage: String) -> some View {
        let isSelected = selectedTab == index

        return Button {
            if selectedTab != index {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                    selectedTab = index
                }
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: systemImage)
                    .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                Text(title)
                    .font(.subheadline.weight(isSelected ? .bold : .medium))
                    .lineLimit(1)
            }
            .glassVibrancy(isSelected ? .primary : .secondary)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background {
                if isSelected {
                    Capsule()
                        .fill(Color.primary.opacity(0.14))
                        .matchedGeometryEffect(id: "ActiveTabIndicator", in: tabIndicatorNamespace)
                }
            }
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(title))
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }
}
